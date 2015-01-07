package org.gawst.asyncdb;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.gawst.asyncdb.purge.PurgeHandler;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Pair;

/**
 * the main helper class that saves/restore item in memory using a DB storage
 * <p>
 * the storage handling is done in a separate thread
 * @author Steve Lhomme
 *
 * @param <E> the type of items stored in memory
 */
public abstract class AsynchronousDbHelper<E, INSERT_ID> implements DataSource.BatchReadingCallback<E> {

	public final static String TAG = "MemoryDb";
	protected final static String STARTUP_TAG = "Startup";
	protected final static boolean DEBUG_DB = false;

	private final Handler saveStoreHandler;
	private static final int MSG_LOAD_IN_MEMORY    = 100;
	private static final int MSG_STORE_ITEM        = 101;
	private static final int MSG_STORE_ITEMS       = 102;
	private static final int MSG_REMOVE_ITEM       = 103;
	private static final int MSG_UPDATE_ITEM       = 104;
	private static final int MSG_CLEAR_DATABASE    = 105;
	private static final int MSG_SWAP_ITEMS        = 106;
	private static final int MSG_REPLACE_ITEMS     = 107;
	private static final int MSG_CUSTOM_OPERATION  = 108;

	private WeakReference<AsynchronousDbErrorHandler<E>> mErrorHandler; // not protected for now
	private final CopyOnWriteArrayList<WeakReference<InMemoryDbListener<E>>> mDbListeners = new CopyOnWriteArrayList<WeakReference<InMemoryDbListener<E>>>();

	private final AtomicBoolean mDataLoaded = new AtomicBoolean();
	private final AtomicInteger modifyingTransactionLevel = new AtomicInteger(0);
	private final DataSource<E, INSERT_ID> dataSource;
	private PurgeHandler purgeHandler;

	/**
	 * A class similar to {@link android.content.AsyncQueryHandler} to do simple calls asynchronously with a callback when it's doe
	 */
	public class AsyncHandler extends AsyncQueryHandler<INSERT_ID> {
		public AsyncHandler() {
			super(AsynchronousDbHelper.this, (DatabaseSource<INSERT_ID>) AsynchronousDbHelper.this.dataSource);
		}
	}

	/**
	 * @param db The already created {@link android.database.sqlite.SQLiteOpenHelper} to use as storage
	 * @param name Database filename on disk
	 * @param logger The {@link org.gawst.asyncdb.Logger} to use for all logs (can be null for the default Android logs)
	 * @param initCookie Cookie to pass to {@link #preloadInit(Object)}
	 */
	@SuppressLint("HandlerLeak")
	protected AsynchronousDbHelper(DataSource<E, INSERT_ID> db, final String name, Logger logger, Object initCookie) {
		this.dataSource = db;

		if (logger!=null)
			LogManager.setLogger(logger);

		preloadInit(initCookie);

		HandlerThread handlerThread = new HandlerThread(name, android.os.Process.THREAD_PRIORITY_BACKGROUND);
		handlerThread.start();

		saveStoreHandler = new Handler(handlerThread.getLooper()) {
			public void handleMessage(Message msg) {
				ContentValues addValues;

				switch (msg.what) {
				case MSG_LOAD_IN_MEMORY:
					if (shouldReloadAllData()) {
						startLoadingInMemory();
						try {
							try {
								dataSource.queryAll(AsynchronousDbHelper.this);
							} catch (Exception e) {
								LogManager.logger.w(STARTUP_TAG, "Can't query table "+ dataSource +" in "+name, e);
							}
						} catch (Exception e) {
							if (e instanceof SQLiteDatabaseCorruptException || e.getCause() instanceof SQLiteDatabaseCorruptException)
								notifyDatabaseCorrupted(dataSource, name, e);
							else
								LogManager.logger.w(STARTUP_TAG, "Can't open database "+name, e);
						} finally {
							finishLoadingInMemory();
						}
					}
					break;

				case MSG_CLEAR_DATABASE:
					try {
						dataSource.clearAllData();
					} catch (Throwable e) {
						LogManager.logger.w(TAG,"Failed to empty table "+ dataSource +" in "+name, e);
					} finally {
						sendEmptyMessage(MSG_LOAD_IN_MEMORY); // reload the DB into memory
					}
					break;

				case MSG_STORE_ITEM:
					@SuppressWarnings("unchecked")
					Pair<E, PurgeHandler> itemToAdd = (Pair<E, PurgeHandler>) msg.obj;
					addValues = null;
					boolean itemAdded = false;
					try {
						addValues = getValuesFromData(itemToAdd.first);
						if (addValues!=null) {
							directStoreItem(addValues);
							itemAdded = true;
						}
					} catch (Exception e) {
						notifyAddItemFailed(itemToAdd.first, addValues, e);
					} finally {
						if (itemAdded && itemToAdd.second != null) {
							itemToAdd.second.onElementsAdded(AsynchronousDbHelper.this);
						}
					}
					break;

				case MSG_STORE_ITEMS:
					@SuppressWarnings("unchecked")
					Pair<Collection<? extends E>, PurgeHandler> itemsToAdd = (Pair<Collection<? extends E>, PurgeHandler>) msg.obj;
					boolean itemsAdded = false;
					for (E item : itemsToAdd.first) {
						addValues = null;
						try {
							addValues = getValuesFromData(item);
							if (addValues!=null) {
								directStoreItem(addValues);
								itemsAdded = true;
							}
						} catch (Exception e) {
							notifyAddItemFailed(item, addValues, e);
						}
					}
					if (itemsAdded && itemsToAdd.second != null) {
						itemsToAdd.second.onElementsAdded(AsynchronousDbHelper.this);
					}
					break;

				case MSG_REMOVE_ITEM:
					@SuppressWarnings("unchecked")
					E itemToDelete = (E) msg.obj;
					try {
						if (DEBUG_DB) LogManager.logger.d(TAG, name+" remove "+itemToDelete);
						if (dataSource.delete(itemToDelete))
							notifyRemoveItemFailed(itemToDelete, new RuntimeException("No item "+itemToDelete+" in "+name));
					} catch (Throwable e) {
						notifyRemoveItemFailed(itemToDelete, e);
					}
					break;

				case MSG_UPDATE_ITEM:
					@SuppressWarnings("unchecked")
					E itemToUpdate = (E) msg.obj;
					ContentValues updateValues = null;
					try {
						updateValues = getValuesFromData(itemToUpdate);
						if (!directUpdate(itemToUpdate, updateValues)) {
							notifyUpdateItemFailed(itemToUpdate, updateValues, new RuntimeException("Can't update "+updateValues+" in "+name));
						}
					} catch (Throwable e) {
						notifyUpdateItemFailed(itemToUpdate, updateValues, e);
					}
					break;

				case MSG_REPLACE_ITEMS:
					@SuppressWarnings("unchecked")
					Pair<E,E> itemsToReplace = (Pair<E,E>) msg.obj;
					try {
						ContentValues newValues = getValuesFromData(itemsToReplace.first);
						directUpdate(itemsToReplace.second, newValues);
					} catch (Throwable e) {
						notifyReplaceItemFailed(itemsToReplace.first, itemsToReplace.second, e);
					}
					break;

				case MSG_SWAP_ITEMS:
					@SuppressWarnings("unchecked")
					Pair<E,E> itemsToSwap = (Pair<E,E>) msg.obj;
					ContentValues newValuesA = null;
					try {
						newValuesA = getValuesFromData(itemsToSwap.second);
						if (newValuesA!=null) {
							if (DEBUG_DB) LogManager.logger.d(TAG, name+" update "+itemsToSwap.second+" with "+newValuesA);
							directUpdate(itemsToSwap.first, newValuesA);
						}
					} catch (Throwable e) {
						notifyUpdateItemFailed(itemsToSwap.first, newValuesA, e);
					}
					ContentValues newValuesB = null;
					try {
						newValuesB = getValuesFromData(itemsToSwap.first);
						if (newValuesB!=null) {
							if (DEBUG_DB) LogManager.logger.d(TAG, name+" update "+itemsToSwap.first+" with "+newValuesB);
							directUpdate(itemsToSwap.second, newValuesB);
						}
					} catch (Throwable e) {
						notifyUpdateItemFailed(itemsToSwap.second, newValuesB, e);
					}
					break;

				case MSG_CUSTOM_OPERATION:
					try {
						@SuppressWarnings("unchecked")
						AsynchronousDbOperation operation = (AsynchronousDbOperation) msg.obj;
						operation.runInMemoryDbOperation(AsynchronousDbHelper.this);
					} catch (Exception e) {
						LogManager.logger.w(TAG, name+" failed to run operation "+msg.obj, e);
					}
					break;


				}

				super.handleMessage(msg);
			}
		};

		saveStoreHandler.sendEmptyMessage(MSG_LOAD_IN_MEMORY);
	}

	/**
	 * Method to call to insert data directly in the database
	 * @param addValues Values that will be written in the database
	 * @throws RuntimeException if the insertion failed
	 */
	protected final void directStoreItem(ContentValues addValues) throws RuntimeException {
		Object inserted = dataSource.insert(addValues);
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

	/**
	 * tell the InMemory database we have finish modifying the data at this level.
	 * Once the pop matches all the push {@link #notifyDatabaseChanged()} is called
	 * <p> this is useful to avoid multiple calls to {@link #notifyDatabaseChanged()} during a batch of changes
	 * <p> see also {@link #pushModifyingTransaction()}
	 */
	protected void popModifyingTransaction() {
		if (modifyingTransactionLevel.decrementAndGet()==0) {
			notifyDatabaseChanged();
		}
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

	protected void setPurgeHandler(PurgeHandler purgeHandler) {
		this.purgeHandler = purgeHandler;
	}

	public void addListener(InMemoryDbListener<E> listener) {
		for (WeakReference<InMemoryDbListener<E>> l : mDbListeners) {
			if (l.get()==null)
				mDbListeners.remove(l);
			else if (l.get()==listener)
				return;
		}
		if (mDataLoaded.get())
			listener.onMemoryDbChanged(this);
		mDbListeners.add(new WeakReference<InMemoryDbListener<E>>(listener));
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
		LogManager.logger.i(TAG, this+" failed to add item "+item+(DEBUG_DB ? (" values"+values) : ""), cause);
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
	 * transform the element in memory into {@link android.content.ContentValues} that can be saved in the database
	 * <p> you can return null and fill the database yourself if you need to
	 * @param data the data to transform
	 * @return a ContentValues element with all data that can be used to restore the data later from the database
	 * @see #addItemInMemory(Object)
	 */
	protected abstract ContentValues getValuesFromData(E data) throws RuntimeException;


	/**
	 * Request to store the item in the database asynchronously
	 * <p>Will call the {@link org.gawst.asyncdb.AsynchronousDbErrorHandler#onAddItemFailed(org.gawst.asyncdb.AsynchronousContentProviderHelper, Object, android.content.ContentValues, Throwable) AsynchronousDbErrorHandler.onAddItemFailed()} on failure
	 * @param item to add
	 */
	protected final void scheduleAddOperation(E item) {
		scheduleAddOperation(item, purgeHandler);
	}

	/**
	 * Request to store the item in the database asynchronously
	 * <p>Will call the {@link org.gawst.asyncdb.AsynchronousDbErrorHandler#onAddItemFailed(org.gawst.asyncdb.AsynchronousContentProviderHelper, Object, android.content.ContentValues, Throwable) AsynchronousDbErrorHandler.onAddItemFailed()} on failure
	 * @param item to add
	 * @param purgeHandler
	 */
	protected final void scheduleAddOperation(E item, PurgeHandler purgeHandler) {
		saveStoreHandler.sendMessage(Message.obtain(saveStoreHandler, MSG_STORE_ITEM, new Pair<E, PurgeHandler>(item, purgeHandler)));
		pushModifyingTransaction();
		popModifyingTransaction();
	}

	/**
	 * Request to store the items in the database asynchronously
	 * <p>Will call {@link org.gawst.asyncdb.AsynchronousDbErrorHandler#onAddItemFailed(org.gawst.asyncdb.AsynchronousContentProviderHelper, Object, android.content.ContentValues, Throwable) AsynchronousDbErrorHandler.onAddItemFailed()} on each item failing
	 * @param items to add
	 */
	protected final void scheduleAddOperation(Collection<? extends E> items) {
		scheduleAddOperation(items, purgeHandler);
	}

	/**
	 * Request to store the items in the database asynchronously
	 * <p>Will call {@link org.gawst.asyncdb.AsynchronousDbErrorHandler#onAddItemFailed(org.gawst.asyncdb.AsynchronousContentProviderHelper, Object, android.content.ContentValues, Throwable) AsynchronousDbErrorHandler.onAddItemFailed()} on each item failing
	 * @param items to add
	 * @param purgeHandler
	 */
	protected final void scheduleAddOperation(Collection<? extends E> items, PurgeHandler purgeHandler) {
		saveStoreHandler.sendMessage(Message.obtain(saveStoreHandler, MSG_STORE_ITEMS, new Pair<Collection<? extends E>, PurgeHandler>(items, purgeHandler)));
		pushModifyingTransaction();
		popModifyingTransaction();
	}

	/**
	 * Request to update the item in the database asynchronously
	 * <p>{@link org.gawst.asyncdb.AsynchronousContentProviderHelper#getItemSelectArgs(Object) getKeySelectArgs()} is used to find the matching item in the database
	 * <p>Will call {@link org.gawst.asyncdb.AsynchronousDbErrorHandler#onUpdateItemFailed(org.gawst.asyncdb.AsynchronousContentProviderHelper, Object, Throwable) AsynchronousDbErrorHandler.onUpdateItemFailed()} on failure
	 * @see #getValuesFromData(Object)
	 * @param item to update
	 */
	protected final void scheduleUpdateOperation(E item) {
		saveStoreHandler.sendMessage(Message.obtain(saveStoreHandler, MSG_UPDATE_ITEM, item));
		pushModifyingTransaction();
		popModifyingTransaction();
	}

	/**
	 * Request to replace an item in the databse with another asynchronously
	 * <p>{@link org.gawst.asyncdb.AsynchronousContentProviderHelper#getItemSelectArgs(Object) getKeySelectArgs()} is used to find the matching item in the database
	 * <p>Will call {@link org.gawst.asyncdb.AsynchronousDbErrorHandler#onReplaceItemFailed(org.gawst.asyncdb.AsynchronousContentProviderHelper, Object, Object, Throwable) AsynchronousDbErrorHandler.onReplaceItemFailed()} on failure
	 * @param original Item to replace
	 * @param replacement Item to replace with
	 */
	protected final void scheduleReplaceOperation(E original, E replacement) {
		saveStoreHandler.sendMessage(Message.obtain(saveStoreHandler, MSG_REPLACE_ITEMS, new Pair<E,E>(original, replacement)));
		pushModifyingTransaction();
		popModifyingTransaction();
	}

	protected final void scheduleSwapOperation(E itemA, E itemB) {
		saveStoreHandler.sendMessage(Message.obtain(saveStoreHandler, MSG_SWAP_ITEMS, new Pair<E,E>(itemA, itemB)));
		pushModifyingTransaction();
		popModifyingTransaction();
	}

	/**
	 * Request to delete the item from the database
	 * <p>Will call the {@link org.gawst.asyncdb.AsynchronousDbErrorHandler#onRemoveItemFailed(org.gawst.asyncdb.AsynchronousContentProviderHelper, Object, Throwable) AsynchronousDbErrorHandler.onRemoveItemFailed()} on failure
	 * @param item to remove
	 */
	protected final void scheduleRemoveOperation(E item) {
		saveStoreHandler.sendMessage(Message.obtain(saveStoreHandler, MSG_REMOVE_ITEM, item));
		pushModifyingTransaction();
		popModifyingTransaction();
	}

	/**
	 * run the operation in the internal thread
	 * @param operation
	 */
	public final void scheduleCustomOperation(AsynchronousDbOperation operation) {
		saveStoreHandler.sendMessage(Message.obtain(saveStoreHandler, MSG_CUSTOM_OPERATION, operation));
	}

	/**
	 * called when we are about to read all items from the disk
	 */
	protected void startLoadingInMemory() {
		pushModifyingTransaction();
		mDataLoaded.set(false);
	}

	public void startLoadingAllItems(int itemCount) {}

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

	protected void notifyDatabaseChanged() {
		for (WeakReference<InMemoryDbListener<E>> l : mDbListeners) {
			final InMemoryDbListener<E> listener = l.get();
			if (listener==null)
				mDbListeners.remove(l);
			else
				listener.onMemoryDbChanged(this);
		}
	}

	public boolean isDataLoaded() {
		return mDataLoaded.get();
	}

	/** Wait until the data are loaded */
	public void waitForDataLoaded() {}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(48);
		sb.append('{');
		sb.append(getClass().getSimpleName());
		sb.append(' ');
		sb.append(Integer.toHexString(System.identityHashCode(this)));
		sb.append('}');
		return sb.toString();
	}
}
