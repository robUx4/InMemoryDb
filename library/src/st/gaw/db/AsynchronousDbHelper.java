package st.gaw.db;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.database.sqlite.SQLiteOpenHelper;
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
public abstract class AsynchronousDbHelper<E> extends SQLiteOpenHelper {

	protected final static String TAG = "MemoryDb";
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

	private WeakReference<InMemoryDbErrorHandler<E>> mErrorHandler; // not protected for now
	private final CopyOnWriteArrayList<WeakReference<InMemoryDbListener<E>>> mDbListeners = new CopyOnWriteArrayList<WeakReference<InMemoryDbListener<E>>>();

	private AtomicBoolean mDataLoaded = new AtomicBoolean();
	private final AtomicInteger modifyingTransactionLevel = new AtomicInteger(0);

	/**
	 * @param context Used to open or create the database
	 * @param name Database filename on disk
	 * @param version Version number of the database (starting at 1); if the database is older,
	 *     {@link #onUpgrade} will be used to upgrade the database; if the database is
	 *     newer, {@link #onDowngrade} will be used to downgrade the database
	 * @param logger The {@link Logger} to use for all logs (can be null for the default Android logs)
	 * @param initCookie Cookie to pass to {@link #preloadInit(Object, Logger)}
	 */
	@SuppressLint("HandlerLeak")
	protected AsynchronousDbHelper(Context context, final String name, int version, Logger logger, Object initCookie) {
		this(context, name, null, version, logger, initCookie);
	}

	/**
	 * @param context Used to open or create the database
	 * @param name Database filename on disk
	 * @param factory to use for creating cursor objects, or null for the default
	 * @param version Version number of the database (starting at 1); if the database is older,
	 *     {@link #onUpgrade} will be used to upgrade the database; if the database is
	 *     newer, {@link #onDowngrade} will be used to downgrade the database
	 * @param logger The {@link Logger} to use for all logs (can be null for the default Android logs)
	 * @param initCookie Cookie to pass to {@link #preloadInit(Object, Logger)}
	 */
	@SuppressLint("HandlerLeak")
	protected AsynchronousDbHelper(Context context, final String name, CursorFactory factory, int version, Logger logger, Object initCookie) {
		super(context, name, factory, version);

		preloadInit(initCookie, logger);

		HandlerThread handlerThread = new HandlerThread(name, android.os.Process.THREAD_PRIORITY_BACKGROUND);
		handlerThread.start();

		saveStoreHandler = new Handler(handlerThread.getLooper()) {
			public void handleMessage(Message msg) {
				SQLiteDatabase db;
				ContentValues addValues;

				switch (msg.what) {
				case MSG_LOAD_IN_MEMORY:
					startLoadingInMemory();
					if (shouldReloadAllData())
						try {
							db = getWritableDatabase();
							Cursor c = db.query(getMainTableName(), null, null, null, null, null, null);
							if (c!=null)
								try {
									if (c.moveToFirst()) {
										startLoadingFromCursor(c);
										do {
											addCursorInMemory(c);
										} while (c.moveToNext());
									}

								} finally {
									c.close();
								}
						} catch (SQLException e) {
							if (e instanceof SQLiteDatabaseCorruptException || e.getCause() instanceof SQLiteDatabaseCorruptException)
								LogManager.logger.e(STARTUP_TAG, "table "+getMainTableName()+" is corrupted in "+name);
							else
								LogManager.logger.w(STARTUP_TAG, "Can't query table "+getMainTableName()+" in "+name, e);
						} finally {
							finishLoadingInMemory();
						}
					break;

				case MSG_CLEAR_DATABASE:
					try {
						db = getWritableDatabase();
						db.delete(getMainTableName(), "1", null);
					} catch (Throwable e) {
						LogManager.logger.w(TAG,"Failed to empty table "+getMainTableName()+" in "+name, e);
						sendEmptyMessage(MSG_LOAD_IN_MEMORY); // reload the DB into memory
					}
					SQLiteDatabase.releaseMemory();
					break;

				case MSG_STORE_ITEM:
					@SuppressWarnings("unchecked")
					E itemToAdd = (E) msg.obj;
					addValues = null;
					try {
						db = getWritableDatabase();
						addValues = getValuesFromData(itemToAdd, db);
						if (addValues!=null) {
							directStoreItem(db, addValues);
						}
					} catch (Throwable e) {
						notifyAddItemFailed(itemToAdd, addValues, e);
					}
					break;

				case MSG_STORE_ITEMS:
					@SuppressWarnings("unchecked")
					Collection<? extends E> itemsToAdd = (Collection<? extends E>) msg.obj;
					for (E item : itemsToAdd) {
						addValues = null;
						try {
							db = getWritableDatabase();
							addValues = getValuesFromData(item, db);
							if (addValues!=null) {
								directStoreItem(db, addValues);
							}
						} catch (Throwable e) {
							notifyAddItemFailed(item, addValues, e);
						}
					}
					break;

				case MSG_REMOVE_ITEM:
					@SuppressWarnings("unchecked")
					E itemToDelete = (E) msg.obj;
					try {
						db = getWritableDatabase();
						if (DEBUG_DB) LogManager.logger.d(TAG, name+" remove "+itemToDelete);
						if (db.delete(getMainTableName(), getItemSelectClause(itemToDelete), getItemSelectArgs(itemToDelete))==0)
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
						db = getWritableDatabase();
						updateValues = getValuesFromData(itemToUpdate, db);
						if (!directUpdate(db, itemToUpdate, updateValues)) {
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
						db = getWritableDatabase();
						ContentValues newValues = getValuesFromData(itemsToReplace.first, db);
						directUpdate(db, itemsToReplace.second, newValues);
					} catch (Throwable e) {
						notifyReplaceItemFailed(itemsToReplace.first, itemsToReplace.second, e);
					}
					break;

				case MSG_SWAP_ITEMS:
					@SuppressWarnings("unchecked")
					Pair<E,E> itemsToSwap = (Pair<E,E>) msg.obj;
					ContentValues newValuesA = null;
					try {
						db = getWritableDatabase();
						newValuesA = getValuesFromData(itemsToSwap.second, db);
						if (newValuesA!=null) {
							if (DEBUG_DB) LogManager.logger.d(TAG, name+" update "+itemsToSwap.second+" with "+newValuesA);
							directUpdate(db, itemsToSwap.first, newValuesA);
						}
					} catch (Throwable e) {
						notifyUpdateItemFailed(itemsToSwap.first, newValuesA, e);
					}
					ContentValues newValuesB = null;
					try {
						db = getWritableDatabase();
						newValuesB = getValuesFromData(itemsToSwap.first, db);
						if (newValuesB!=null) {
							if (DEBUG_DB) LogManager.logger.d(TAG, name+" update "+itemsToSwap.first+" with "+newValuesB);
							directUpdate(db, itemsToSwap.second, newValuesB);
						}
					} catch (Throwable e) {
						notifyUpdateItemFailed(itemsToSwap.second, newValuesB, e);
					}
					break;

				case MSG_CUSTOM_OPERATION:
					try {
						@SuppressWarnings("unchecked")
						AsynchronousDbOperation<E> operation = (AsynchronousDbOperation<E>) msg.obj;
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
	 * @param db Database where data will be written
	 * @param addValues Values that will be written in the database
	 * @return {@code true} if the data were written successfully
	 * @throws RuntimeException if the insertion failed
	 */
	protected boolean directStoreItem(SQLiteDatabase db, ContentValues addValues) throws SQLException {
		long id = db.insertOrThrow(getMainTableName(), null, addValues);
		if (DEBUG_DB) LogManager.logger.d(TAG, AsynchronousDbHelper.this+" insert "+addValues+" = "+id);
		if (id==-1) throw new RuntimeException("failed to add values "+addValues+" in "+db.getPath());
		return id!=-1;
	}

	/**
	 * Method to call to update the data directly in the database
	 * @param db Database where data will be updated
	 * @param itemToUpdate Item in the database that needs to be updated
	 * @param updateValues Values that will be updated in the database
	 * @return {@code true} if the data were updated successfully
	 */
	protected boolean directUpdate(SQLiteDatabase db, E itemToUpdate, ContentValues updateValues) {
		if (updateValues!=null) {
			if (DEBUG_DB) LogManager.logger.d(TAG, AsynchronousDbHelper.this+" update "+updateValues+" for "+itemToUpdate);
			return db.update(getMainTableName(), updateValues, getItemSelectClause(itemToUpdate), getItemSelectArgs(itemToUpdate)/*, SQLiteDatabase.CONFLICT_NONE*/)!=0;
		}
		return false;
	}

	/**
	 * Method called at the end of constructor, just before the data start loading
	 * @param cookie Data that may be needed to initialize all internal storage
	 * @param logger The {@link Logger} to use for all logs (can be null for the default Android logs)
	 */
	protected void preloadInit(Object cookie, Logger logger) {
		if (logger!=null)
			LogManager.setLogger(logger);
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

	@Override
	@Deprecated
	public SQLiteDatabase getReadableDatabase() {
		return super.getReadableDatabase();
	}

	/**
	 * set the listener that will receive error events
	 * @param listener null to remove the listener
	 */
	public void setDbErrorHandler(InMemoryDbErrorHandler<E> listener) {
		if (listener==null)
			mErrorHandler = null;
		else
			mErrorHandler = new WeakReference<InMemoryDbErrorHandler<E>>(listener);
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
			final InMemoryDbErrorHandler<E> listener = mErrorHandler.get(); 
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
			final InMemoryDbErrorHandler<E> listener = mErrorHandler.get(); 
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
			final InMemoryDbErrorHandler<E> listener = mErrorHandler.get(); 
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
			final InMemoryDbErrorHandler<E> listener = mErrorHandler.get(); 
			if (listener==null)
				mErrorHandler = null;
			else
				listener.onRemoveItemFailed(this, item, cause);
		}
		pushModifyingTransaction();
		popModifyingTransaction();
	}

	/**
	 * the name of the main table corresponding to the inMemory elements
	 * @return the name of the main table
	 */
	protected abstract String getMainTableName();

	/**
	 * use the data in the {@link Cursor} to store them in the memory storage
	 * @param c the Cursor to use
	 * @see #getValuesFromData(Object, SQLiteDatabase)
	 */
	protected abstract void addCursorInMemory(Cursor c);
	
	/**
	 * transform the element in memory into {@link ContentValues} that can be saved in the database
	 * <p> you can return null and fill the database yourself if you need to
	 * @param data the data to transform
	 * @param dbToFill the database that will receive new items
	 * @return a ContentValues element with all data that can be used to restore the data later from the database
	 * @see #addCursorInMemory(Cursor)
	 */
	protected abstract ContentValues getValuesFromData(E data, SQLiteDatabase dbToFill) throws RuntimeException;

	/**
	 * the where clause that should be used to update/delete the item
	 * <p> see {@link #getItemSelectArgs(Object)}
	 * @param itemToSelect the item about to be selected in the database
	 * @return a string for the whereClause in {@link SQLiteDatabase#update(String, ContentValues, String, String[])} or {@link SQLiteDatabase#delete(String, String, String[])}
	 */
	protected abstract String getItemSelectClause(E itemToSelect);
	/**
	 * the where arguments that should be used to update/delete the item
	 * <p> see {@link #getItemSelectClause(Object)}
	 * @param itemToSelect the item about to be selected in the database
	 * @return a string array for the whereArgs in {@link SQLiteDatabase#update(String, ContentValues, String, String[])} or {@link SQLiteDatabase#delete(String, String, String[])}
	 */
	protected abstract String[] getItemSelectArgs(E itemToSelect);


	/**
	 * Request to store the item in the database asynchronously
	 * <p>Will call the {@link InMemoryDbErrorHandler#onAddItemFailed(AsynchronousDbHelper, Object, ContentValues, Throwable) InMemoryDbErrorHandler.onAddItemFailed()} on failure
	 * @param item to add
	 */
	protected final void scheduleAddOperation(E item) {
		saveStoreHandler.sendMessage(Message.obtain(saveStoreHandler, MSG_STORE_ITEM, item));
		pushModifyingTransaction();
		popModifyingTransaction();
	}

	/**
	 * Request to store the items in the database asynchronously
	 * <p>Will call {@link InMemoryDbErrorHandler#onAddItemFailed(AsynchronousDbHelper, Object, ContentValues, Throwable) InMemoryDbErrorHandler.onAddItemFailed()} on each item failing
	 * @param items to add
	 */
	protected final void scheduleAddOperation(Collection<? extends E> items) {
		saveStoreHandler.sendMessage(Message.obtain(saveStoreHandler, MSG_STORE_ITEMS, items));
		pushModifyingTransaction();
		popModifyingTransaction();
	}

	/**
	 * Request to update the item in the database asynchronously
	 * <p>{@link AsynchronousDbHelper#getItemSelectArgs(Object) getItemSelectArgs()} is used to find the matching item in the database
	 * <p>Will call {@link InMemoryDbErrorHandler#onUpdateItemFailed(AsynchronousDbHelper, Object, Throwable) InMemoryDbErrorHandler.onUpdateItemFailed()} on failure
	 * @see #getValuesFromData(Object, SQLiteDatabase)
	 * @param item to update
	 */
	protected final void scheduleUpdateOperation(E item) {
		saveStoreHandler.sendMessage(Message.obtain(saveStoreHandler, MSG_UPDATE_ITEM, item));
		pushModifyingTransaction();
		popModifyingTransaction();
	}

	/**
	 * Request to replace an item in the databse with another asynchronously
	 * <p>{@link AsynchronousDbHelper#getItemSelectArgs(Object) getItemSelectArgs()} is used to find the matching item in the database
	 * <p>Will call {@link InMemoryDbErrorHandler#onReplaceItemFailed(AsynchronousDbHelper, Object, Object, Throwable) InMemoryDbErrorHandler.onReplaceItemFailed()} on failure
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
	 * <p>Will call the {@link InMemoryDbErrorHandler#onRemoveItemFailed(AsynchronousDbHelper, Object, Throwable) InMemoryDbErrorHandler.onRemoveItemFailed()} on failure
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
	protected final void scheduleCustomOperation(AsynchronousDbOperation<E> operation) {
		saveStoreHandler.sendMessage(Message.obtain(saveStoreHandler, MSG_CUSTOM_OPERATION, operation));
	}

	/**
	 * called when we are about to read all items from the disk
	 */
	protected void startLoadingInMemory() {
		pushModifyingTransaction();
		mDataLoaded.set(false);
	}

	/**
	 * called when we have the cursor to read the data from
	 * <p>
	 * useful to prepare the amount of data needed or get the index of the column we need
	 * @param c the {@link Cursor} that will be used to read the data
	 */
	protected void startLoadingFromCursor(Cursor c) {}

	/**
	 * called after all items have been read from the disk
	 */
	protected void finishLoadingInMemory() {
		mDataLoaded.set(true);
		popModifyingTransaction();
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
