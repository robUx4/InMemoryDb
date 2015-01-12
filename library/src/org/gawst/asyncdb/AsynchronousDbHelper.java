package org.gawst.asyncdb;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;

import org.gawst.asyncdb.adapter.UIHandler;
import org.gawst.asyncdb.purge.PurgeHandler;
import org.gawst.asyncdb.source.DatabaseSource;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * the main helper class that saves/restore item in memory using a DB storage
 * <p/>
 * the storage handling is done in a separate thread
 *
 * @param <E> the type of items stored in memory
 * @author Steve Lhomme
 * @see org.gawst.asyncdb.AsynchronousDbHelper.AsyncHandler
 */
public abstract class AsynchronousDbHelper<E, INSERT_ID> implements DataSource.BatchReadingCallback<E> {

	public final static String TAG = "MemoryDb";
	protected final static String STARTUP_TAG = "Startup";
	protected final static boolean DEBUG_DB = false;

	private static final int MSG_LOAD_IN_MEMORY    = 100;
	private static final int MSG_STORE_ITEM        = 101;
	private static final int MSG_STORE_ITEMS       = 102;
	private static final int MSG_REMOVE_ITEM       = 103;
	private static final int MSG_UPDATE_ITEM       = 104;
	private static final int MSG_CLEAR_DATABASE    = 105;
	private static final int MSG_SWAP_ITEMS        = 106;
	private static final int MSG_REPLACE_ITEM      = 107;
	private static final int MSG_CUSTOM_OPERATION  = 108;

	private static final UIHandler UI_HANDLER = new UIHandler();

	private WeakReference<AsynchronousDbErrorHandler<E>> mErrorHandler; // not protected for now
	private final CopyOnWriteArrayList<WeakReference<InMemoryDbListener<E>>> mDbListeners = new CopyOnWriteArrayList<WeakReference<InMemoryDbListener<E>>>();

	private final AtomicBoolean mDataLoaded = new AtomicBoolean();
	private final AtomicInteger modifyingTransactionLevel = new AtomicInteger(0);
	private final DataSource<E, INSERT_ID> dataSource;
	private final String name;

	private PurgeHandler purgeHandler;

	private final static HandlerThread handlerThread = new HandlerThread("AsynchronousDbHelper", android.os.Process.THREAD_PRIORITY_BACKGROUND);
	static {
		handlerThread.start();
	}

	/**
	 * A class similar to {@link android.content.AsyncQueryHandler} to do simple calls asynchronously with a callback when it's done
	 */
	public class AsyncHandler extends AsyncDatabaseHandler<INSERT_ID, Uri> {
		public AsyncHandler() {
			super(AsynchronousDbHelper.this, (DatabaseSource<INSERT_ID, Uri>) AsynchronousDbHelper.this.dataSource);
		}
	}

	/**
	 * @param db The already created {@link android.database.sqlite.SQLiteOpenHelper} to use as storage
	 * @param name Database name for logs
	 * @param logger The {@link org.gawst.asyncdb.Logger} to use for all logs (can be null for the default Android logs)
	 * @param initCookie Cookie to pass to {@link #preloadInit(Object)}
	 */
	@SuppressLint("HandlerLeak")
	protected AsynchronousDbHelper(DataSource<E, INSERT_ID> db, final String name, Logger logger, Object initCookie) {
		this.dataSource = db;
		this.name = name;

		if (logger!=null)
			LogManager.setLogger(logger);

		preloadInit(initCookie);

		saveStoreHandler.sendEmptyMessage(MSG_LOAD_IN_MEMORY);
	}

	private final Handler saveStoreHandler = new Handler(handlerThread.getLooper()) {
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case MSG_LOAD_IN_MEMORY:
					loadInMemory();
					break;

				case MSG_CLEAR_DATABASE:
					clearAllData();
					break;

				case MSG_STORE_ITEM:
					@SuppressWarnings("unchecked")
					Pair<E, PurgeHandler> itemToAdd = (Pair<E, PurgeHandler>) msg.obj;
					storeItem(itemToAdd.first, itemToAdd.second);
					break;

				case MSG_STORE_ITEMS:
					@SuppressWarnings("unchecked")
					Pair<Collection<? extends E>, PurgeHandler> itemsToAdd = (Pair<Collection<? extends E>, PurgeHandler>) msg.obj;
					storeItems(itemsToAdd.first, itemsToAdd.second);
					break;

				case MSG_REMOVE_ITEM:
					@SuppressWarnings("unchecked")
					E itemToDelete = (E) msg.obj;
					removeItem(itemToDelete);
					break;

				case MSG_UPDATE_ITEM:
					@SuppressWarnings("unchecked")
					E itemToUpdate = (E) msg.obj;
					updateItem(itemToUpdate);
					break;

				case MSG_REPLACE_ITEM:
					@SuppressWarnings("unchecked")
					Pair<E, E> itemsToReplace = (Pair<E, E>) msg.obj;
					replaceItem(itemsToReplace.first, itemsToReplace.second);
					break;

				case MSG_SWAP_ITEMS:
					@SuppressWarnings("unchecked")
					Pair<E, E> itemsToSwap = (Pair<E, E>) msg.obj;
					swapItems(itemsToSwap.first, itemsToSwap.second);
					break;

				case MSG_CUSTOM_OPERATION:
					try {
						@SuppressWarnings("unchecked")
						AsynchronousDbOperation operation = (AsynchronousDbOperation) msg.obj;
						operation.runInMemoryDbOperation(AsynchronousDbHelper.this);
					} catch (Exception e) {
						LogManager.logger.w(TAG, name + " failed to run operation " + msg.obj, e);
					}
					break;
			}

			super.handleMessage(msg);
		}
	};

	private void loadInMemory() {
		if (shouldReloadAllData()) {
			startLoadingInMemory();
			try {
				try {
					dataSource.queryAll(AsynchronousDbHelper.this);
				} catch (Exception e) {
					LogManager.logger.w(STARTUP_TAG, "Can't query table " + dataSource + " in " + name, e);
				}
			} catch (Exception e) {
				if (e instanceof SQLiteDatabaseCorruptException || e.getCause() instanceof SQLiteDatabaseCorruptException)
					notifyDatabaseCorrupted(dataSource, name, e);
				else
					LogManager.logger.w(STARTUP_TAG, "Can't open database " + name, e);
			} finally {
				finishLoadingInMemory();
			}
		}
	}

	private void clearAllData() {
		try {
			dataSource.clearAllData();
		} catch (Throwable e) {
			LogManager.logger.w(TAG, "Failed to empty table " + dataSource + " in " + name, e);
		} finally {
			saveStoreHandler.sendEmptyMessage(MSG_LOAD_IN_MEMORY); // reload the DB into memory
		}
	}

	private void storeItem(@NonNull E item, @Nullable PurgeHandler purgeHandler) {
		ContentValues addValues = null;
		boolean itemAdded = false;
		try {
			addValues = getValuesFromData(item);
			if (addValues != null) {
				directStoreItem(addValues);
				itemAdded = true;
			}
		} catch (Exception e) {
			notifyAddItemFailed(item, addValues, e);
		} finally {
			if (itemAdded) {
				if (purgeHandler != null) {
					purgeHandler.onElementsAdded(AsynchronousDbHelper.this);
				}
				if (!notifyOnSchedule()) {
					pushModifyingTransaction();
					popModifyingTransaction();
				}
			}
		}
	}

	private void storeItems(@NonNull Collection<? extends E> items, @Nullable PurgeHandler purgeHandler) {
		ContentValues addValues;
		boolean itemsAdded = false;
		for (E item : items) {
			addValues = null;
			try {
				addValues = getValuesFromData(item);
				if (addValues != null) {
					directStoreItem(addValues);
					itemsAdded = true;
				}
			} catch (Exception e) {
				notifyAddItemFailed(item, addValues, e);
			}
		}
		if (itemsAdded) {
			if (purgeHandler != null) {
				purgeHandler.onElementsAdded(AsynchronousDbHelper.this);
			}
			if (!notifyOnSchedule()) {
				pushModifyingTransaction();
				popModifyingTransaction();
			}
		}
	}

	private void removeItem(@NonNull E itemToDelete) {
		try {
			if (DEBUG_DB) LogManager.logger.d(TAG, name + " remove " + itemToDelete);
			if (!dataSource.delete(itemToDelete)) {
				notifyRemoveItemFailed(itemToDelete, new RuntimeException("No item " + itemToDelete + " in " + name));
			} else if (!notifyOnSchedule()) {
				pushModifyingTransaction();
				popModifyingTransaction();
			}

		} catch (Throwable e) {
			notifyRemoveItemFailed(itemToDelete, e);
		}
	}

	private void updateItem(@NonNull E itemToUpdate) {
		ContentValues updateValues = null;
		try {
			updateValues = getValuesFromData(itemToUpdate);
			if (!directUpdate(itemToUpdate, updateValues)) {
				notifyUpdateItemFailed(itemToUpdate, updateValues, new RuntimeException("Can't update " + updateValues + " in " + name));
			} else if (!notifyOnSchedule()) {
				pushModifyingTransaction();
				popModifyingTransaction();
			}
		} catch (Throwable e) {
			notifyUpdateItemFailed(itemToUpdate, updateValues, e);
		}
	}

	private void replaceItem(@NonNull E src, @NonNull E replacement) {
		try {
			ContentValues replacementValues = getValuesFromData(src);
			if (directUpdate(replacement, replacementValues)) {
				if (!notifyOnSchedule()) {
					pushModifyingTransaction();
					popModifyingTransaction();
				}
			}
		} catch (Throwable e) {
			notifyReplaceItemFailed(src, replacement, e);
		}
	}

	private void swapItems(@NonNull E first, @NonNull E second) {
		ContentValues newValuesA = null;
		try {
			newValuesA = getValuesFromData(second);
			if (newValuesA != null) {
				if (DEBUG_DB) LogManager.logger.d(TAG, name + " update " + second + " with " + newValuesA);
				directUpdate(first, newValuesA);
			}
		} catch (Throwable e) {
			notifyUpdateItemFailed(first, newValuesA, e);
		}
		ContentValues newValuesB = null;
		try {
			newValuesB = getValuesFromData(first);
			if (newValuesB != null) {
				if (DEBUG_DB) LogManager.logger.d(TAG, name + " update " + first + " with " + newValuesB);
				directUpdate(second, newValuesB);
			}
		} catch (Throwable e) {
			notifyUpdateItemFailed(second, newValuesB, e);
		}
		if (!notifyOnSchedule()) {
			pushModifyingTransaction();
			popModifyingTransaction();
		}
	}

	/**
	 * Method to call to insert data directly in the database
	 * @param addValues Values that will be written in the database
	 * @throws RuntimeException if the insertion failed
	 */
	private void directStoreItem(ContentValues addValues) throws RuntimeException {
		INSERT_ID inserted = dataSource.insert(addValues);
		if (DEBUG_DB) LogManager.logger.d(TAG, AsynchronousDbHelper.this+" insert "+addValues+" = "+inserted);
		if (inserted==null) throw new RuntimeException("failed to add values "+addValues+" in "+ dataSource);
	}

	/**
	 * Method to call to update the data directly in the database
	 * @param itemToUpdate Item in the database that needs to be updated
	 * @param updateValues Values that will be updated in the database
	 * @return {@code true} if the data were updated successfully
	 */
	protected final boolean directUpdate(E itemToUpdate, ContentValues updateValues) {
		if (updateValues!=null) {
			if (DEBUG_DB) LogManager.logger.d(TAG, AsynchronousDbHelper.this+" update "+updateValues+" for "+itemToUpdate);
			return dataSource.update(itemToUpdate, updateValues/*, SQLiteDatabase.CONFLICT_NONE*/);
		}
		return false;
	}

	/**
	 * Method called at the end of constructor, just before the data start loading
	 * @param cookie Data that may be needed to initialize all internal storage
	 *
	 */
	protected void preloadInit(Object cookie) {
	}

	/**
	 * tell the InMemory database that we are about to modify its data
	 * <p> see also {@link #popModifyingTransaction()}
	 */
	protected void pushModifyingTransaction() {
		modifyingTransactionLevel.incrementAndGet();
	}

	private final Runnable dataChanged = new Runnable() {
		@Override
		public void run() {
			for (WeakReference<InMemoryDbListener<E>> l : mDbListeners) {
				final InMemoryDbListener<E> listener = l.get();
				if (listener==null)
					mDbListeners.remove(l);
				else
					listener.onMemoryDbChanged(AsynchronousDbHelper.this);
			}
		}
	};

	/**
	 * tell the InMemory database we have finish modifying the data at this level.
	 * Once the pop matches all the pushes {@link org.gawst.asyncdb.InMemoryDbListener#onMemoryDbChanged(AsynchronousDbHelper)} is called
	 * <p> this is useful to avoid multiple calls to {@link org.gawst.asyncdb.InMemoryDbListener#onMemoryDbChanged(AsynchronousDbHelper)} during a batch of changes
	 * <p> see also {@link #pushModifyingTransaction()}
	 */
	protected void popModifyingTransaction() {
		if (modifyingTransactionLevel.decrementAndGet()==0) {
			UI_HANDLER.removeCallbacks(dataChanged);
			UI_HANDLER.runOnUiThread(dataChanged);
		}
	}

	/**
	 * @return {@code true} if all {@link org.gawst.asyncdb.InMemoryDbListener#onMemoryDbChanged(AsynchronousDbHelper)} should be called right after a schedule call or after the call is processed.
	 * @see #scheduleAddOperation(Object)
	 * @see #scheduleAddOperation(Object, org.gawst.asyncdb.purge.PurgeHandler)
	 * @see #scheduleAddOperation(java.util.Collection)
	 * @see #scheduleAddOperation(java.util.Collection, org.gawst.asyncdb.purge.PurgeHandler)
	 * @see #scheduleRemoveOperation(Object)
	 * @see #scheduleUpdateOperation(Object)
	 * @see #scheduleReplaceOperation(Object, Object)
	 * @see #scheduleSwapOperation(Object, Object)
	 */
	protected boolean notifyOnSchedule() {
		return true;
	}

	protected void clearDataInMemory() {}

	/**
	 * set the listener that will receive error events
	 * @param listener null to remove the listener
	 */
	protected void setDbErrorHandler(AsynchronousDbErrorHandler<E> listener) {
		if (listener==null)
			mErrorHandler = null;
		else
			mErrorHandler = new WeakReference<AsynchronousDbErrorHandler<E>>(listener);
	}

	protected PurgeHandler getPurgeHandler() {
		return purgeHandler;
	}

	public void setPurgeHandler(PurgeHandler purgeHandler) {
		this.purgeHandler = purgeHandler;
	}

	/**
	 * Call this after an element is inserted for the purge to do its job.
	 */
	public void triggerPurgeHandler() {
		if (null != purgeHandler) {
			purgeHandler.onElementsAdded(this);
		}
	}

	public void addListener(final InMemoryDbListener<E> listener) {
		for (WeakReference<InMemoryDbListener<E>> l : mDbListeners) {
			if (l.get()==null)
				mDbListeners.remove(l);
			else if (l.get()==listener)
				return;
		}
		mDbListeners.add(new WeakReference<InMemoryDbListener<E>>(listener));
		if (mDataLoaded.get())
			UI_HANDLER.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					listener.onMemoryDbChanged(AsynchronousDbHelper.this);
				}
			});
	}

	public void removeListener(InMemoryDbListener<E> listener) {
		for (WeakReference<InMemoryDbListener<E>> l : mDbListeners) {
			if (l.get()==null)
				mDbListeners.remove(l);
			else if (l.get()==listener)
				mDbListeners.remove(l);
		}
	}

	/**
	 * delete all the data in memory and in the database
	 */
	public final void clear() {
		pushModifyingTransaction();
		clearDataInMemory();
		popModifyingTransaction();
		saveStoreHandler.sendEmptyMessage(MSG_CLEAR_DATABASE);
	}

	private void notifyAddItemFailed(E item, ContentValues values, Throwable cause) {
		LogManager.logger.d(TAG, this+" failed to add item "+item+(DEBUG_DB ? (" values"+values) : ""), cause);
		if (mErrorHandler!=null) {
			final AsynchronousDbErrorHandler<E> listener = mErrorHandler.get();
			if (listener==null)
				mErrorHandler = null;
			else
				listener.onAddItemFailed(this, item, values, cause);
		}
		pushModifyingTransaction();
		popModifyingTransaction();
	}

	private void notifyReplaceItemFailed(E srcItem, E replacement, Throwable cause) {
		LogManager.logger.i(TAG, this+" failed to replace item "+srcItem+" with "+replacement, cause);
		if (mErrorHandler!=null) {
			final AsynchronousDbErrorHandler<E> listener = mErrorHandler.get();
			if (listener==null)
				mErrorHandler = null;
			else
				listener.onReplaceItemFailed(this, srcItem, replacement, cause);
		}
		pushModifyingTransaction();
		popModifyingTransaction();
	}

	private void notifyUpdateItemFailed(E item, ContentValues values, Throwable cause) {
		LogManager.logger.i(TAG, this+" failed to update item "+item+(DEBUG_DB ? (" values"+values) : ""), cause);
		if (mErrorHandler!=null) {
			final AsynchronousDbErrorHandler<E> listener = mErrorHandler.get();
			if (listener==null)
				mErrorHandler = null;
			else
				listener.onAddItemFailed(this, item, values, cause);
		}
		pushModifyingTransaction();
		popModifyingTransaction();
	}

	private void notifyRemoveItemFailed(E item, Throwable cause) {
		LogManager.logger.i(TAG, this+" failed to remove item "+item, cause);
		if (mErrorHandler!=null) {
			final AsynchronousDbErrorHandler<E> listener = mErrorHandler.get();
			if (listener==null)
				mErrorHandler = null;
			else
				listener.onRemoveItemFailed(this, item, cause);
		}
		pushModifyingTransaction();
		popModifyingTransaction();
	}

	private void notifyDatabaseCorrupted(DataSource<E, INSERT_ID> dataSource, String name, Throwable cause) {
		LogManager.logger.e(STARTUP_TAG, "table "+ this.dataSource +" is corrupted in "+name);
		if (mErrorHandler!=null) {
			final AsynchronousDbErrorHandler<E> listener = mErrorHandler.get();
			if (listener==null)
				mErrorHandler = null;
			else
				listener.onCorruption(this);
		}
		pushModifyingTransaction();
		dataSource.eraseSource();
		popModifyingTransaction();
	}

	/**
	 * Transform the element in memory into {@link android.content.ContentValues} that can be saved in the database.
	 * <p> you can return null and fill the database yourself if you need to
	 * @param data the data to transform
	 * @return a ContentValues element with all data that can be used to restore the data later from the database
	 * @see #addItemInMemory(Object)
	 */
	protected abstract ContentValues getValuesFromData(E data) throws RuntimeException;

	/**
	 * Request to store the item in the database asynchronously
	 * <p>Will call the {@link org.gawst.asyncdb.AsynchronousDbErrorHandler#onAddItemFailed(AsynchronousDbHelper, Object, android.content.ContentValues, Throwable)} on failure
	 * @param item to add
	 */
	protected final void scheduleAddOperation(E item) {
		scheduleAddOperation(item, purgeHandler);
	}

	/**
	 * Request to store the item in the database asynchronously
	 * <p>Will call the {@link org.gawst.asyncdb.AsynchronousDbErrorHandler#onAddItemFailed(AsynchronousDbHelper, Object, android.content.ContentValues, Throwable) AsynchronousDbErrorHandler.onAddItemFailed()} on failure
	 * @param item to add
	 * @param purgeHandler
	 */
	protected final void scheduleAddOperation(E item, PurgeHandler purgeHandler) {
		if (null != item) {
			saveStoreHandler.sendMessage(Message.obtain(saveStoreHandler, MSG_STORE_ITEM, new Pair<E, PurgeHandler>(item, purgeHandler)));
			if (notifyOnSchedule()) {
				pushModifyingTransaction();
				popModifyingTransaction();
			}
		}
	}

	/**
	 * Request to store the items in the database asynchronously
	 * <p>Will call {@link org.gawst.asyncdb.AsynchronousDbErrorHandler#onAddItemFailed(AsynchronousDbHelper, Object, android.content.ContentValues, Throwable) AsynchronousDbErrorHandler.onAddItemFailed()} on each item failing
	 * @param items to add
	 */
	protected final void scheduleAddOperation(Collection<? extends E> items) {
		scheduleAddOperation(items, purgeHandler);
	}

	/**
	 * Request to store the items in the database asynchronously
	 * <p>Will call {@link org.gawst.asyncdb.AsynchronousDbErrorHandler#onAddItemFailed(AsynchronousDbHelper, Object, android.content.ContentValues, Throwable) AsynchronousDbErrorHandler.onAddItemFailed()} on each item failing
	 * @param items to add
	 * @param purgeHandler
	 */
	protected final void scheduleAddOperation(Collection<? extends E> items, PurgeHandler purgeHandler) {
		if (null != items) {
			saveStoreHandler.sendMessage(Message.obtain(saveStoreHandler, MSG_STORE_ITEMS, new Pair<Collection<? extends E>, PurgeHandler>(items, purgeHandler)));
			if (notifyOnSchedule()) {
				pushModifyingTransaction();
				popModifyingTransaction();
			}
		}
	}

	/**
	 * Request to update the item in the database asynchronously
	 * <p>{@link org.gawst.asyncdb.source.DatabaseElementHandler#getItemSelectClause(Object)} is used to find the matching item in the database
	 * <p>Will call {@link org.gawst.asyncdb.AsynchronousDbErrorHandler#onUpdateItemFailed(AsynchronousDbHelper, Object, Throwable) AsynchronousDbErrorHandler.onUpdateItemFailed()} on failure
	 * @see #getValuesFromData(Object)
	 * @param item to update
	 */
	protected final void scheduleUpdateOperation(@NonNull E item) {
		if (null != item) {
			saveStoreHandler.sendMessage(Message.obtain(saveStoreHandler, MSG_UPDATE_ITEM, item));
			if (notifyOnSchedule()) {
				pushModifyingTransaction();
				popModifyingTransaction();
			}
		}
	}

	/**
	 * Request to replace an item in the databse with another asynchronously
	 * <p>{@link org.gawst.asyncdb.source.DatabaseElementHandler#getItemSelectClause(Object)} is used to find the matching item in the database
	 * <p>Will call {@link org.gawst.asyncdb.AsynchronousDbErrorHandler#onReplaceItemFailed(AsynchronousDbHelper, Object, Object, Throwable) AsynchronousDbErrorHandler.onReplaceItemFailed()} on failure
	 * @param original Item to replace
	 * @param replacement Item to replace with
	 */
	protected final void scheduleReplaceOperation(@NonNull E original, @NonNull E replacement) {
		saveStoreHandler.sendMessage(Message.obtain(saveStoreHandler, MSG_REPLACE_ITEM, new Pair<E, E>(original, replacement)));
		if (notifyOnSchedule()) {
			pushModifyingTransaction();
			popModifyingTransaction();
		}
	}

	protected final void scheduleSwapOperation(@NonNull E itemA, @NonNull E itemB) {
		saveStoreHandler.sendMessage(Message.obtain(saveStoreHandler, MSG_SWAP_ITEMS, new Pair<E,E>(itemA, itemB)));
		if (notifyOnSchedule()) {
			pushModifyingTransaction();
			popModifyingTransaction();
		}
	}

	/**
	 * Request to delete the item from the database
	 * <p>Will call the {@link org.gawst.asyncdb.AsynchronousDbErrorHandler#onRemoveItemFailed(AsynchronousDbHelper, Object, Throwable) AsynchronousDbErrorHandler.onRemoveItemFailed()} on failure
	 * @param item to remove
	 */
	protected final void scheduleRemoveOperation(E item) {
		if (null != item) {
			saveStoreHandler.sendMessage(Message.obtain(saveStoreHandler, MSG_REMOVE_ITEM, item));
			if (notifyOnSchedule()) {
				pushModifyingTransaction();
				popModifyingTransaction();
			}
		}
	}

	/**
	 * run the operation in the internal thread
	 * @param operation
	 */
	public final void scheduleCustomOperation(@NonNull AsynchronousDbOperation operation) {
		saveStoreHandler.sendMessage(Message.obtain(saveStoreHandler, MSG_CUSTOM_OPERATION, operation));
	}

	/**
	 * called when we are about to read all items from the disk
	 */
	protected void startLoadingInMemory() {
		pushModifyingTransaction();
		mDataLoaded.set(false);
	}

	public void startLoadingAllItems(int elementCount) {}

	/**
	 * called after all items have been read from the disk
	 */
	protected void finishLoadingInMemory() {
		mDataLoaded.set(true);
		popModifyingTransaction();
	}

	@Override
	public void removeInvalidEntry(final InvalidEntry invalidEntry) {
		scheduleCustomOperation(new AsynchronousDbOperation() {
			@Override
			public void runInMemoryDbOperation(AsynchronousDbHelper<?, ?> db) {
				// remove the element from the DB forever
				dataSource.deleteInvalidEntry(invalidEntry);
			}
		});
	}

	/**
	 * Tell whether the database loading should be done or not
	 * <p>If you don't store the elements in memory, you don't need to load the whole data 
	 */
	protected boolean shouldReloadAllData() {
		return true;
	}

	public boolean isDataLoaded() {
		return mDataLoaded.get();
	}

	/** Wait until the data are loaded */
	public void waitForDataLoaded() {
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(48);
		sb.append("{AsyncDB:");
		sb.append(name);
		sb.append(' ');
		sb.append(Integer.toHexString(System.identityHashCode(this)));
		sb.append('}');
		return sb.toString();
	}
}
