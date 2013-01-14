package st.gaw.db;

import java.lang.ref.WeakReference;
import java.util.concurrent.CopyOnWriteArrayList;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

/**
 * the main helper class that saves/restore item in memory using a DB storage
 * <p>
 * the storage handling is done in a separate thread
 * @author Steve Lhomme
 *
 * @param <E> the type of items stored in memory
 */
public abstract class InMemoryDbHelper<E> extends SQLiteOpenHelper {

	protected final static String TAG = "MemoryDb";
	protected final static boolean DEBUG_DB = false;

	private final Handler saveStoreHandler;
	private static final int MSG_LOAD_IN_MEMORY    = 100;
	private static final int MSG_STORE_ITEM        = 101;
	private static final int MSG_REMOVE_ITEM       = 102;
	private static final int MSG_UPDATE_ITEM       = 103;
	private static final int MSG_CLEAR_DATABASE    = 104;
	private static final int MSG_SWAP_ITEMS        = 106;
	private static final int MSG_REPLACE_ITEMS     = 107;
	private static final int MSG_CUSTOM_OPERATION  = 108;

	private WeakReference<InMemoryDbErrorHandler<E>> mErrorHandler; // not protected for now
	private final CopyOnWriteArrayList<WeakReference<InMemoryDbListener<E>>> mDbListeners = new CopyOnWriteArrayList<WeakReference<InMemoryDbListener<E>>>();
	
	private boolean mDataLoaded;

	/**
	 * @param context to use to open or create the database
	 * @param name of the database file, or null for an in-memory database
	 * @param version number of the database (starting at 1); if the database is older,
	 *     {@link #onUpgrade} will be used to upgrade the database; if the database is
	 *     newer, {@link #onDowngrade} will be used to downgrade the database
	 * @param logger the {@link Logger} to use for all logs (can be null for the default Android logs)
	 */
	@SuppressLint("HandlerLeak")
	protected InMemoryDbHelper(Context context, String name, int version, Logger logger) {
		super(context, name, null, version);
		
		if (logger!=null)
			LogManager.setLogger(logger);

		HandlerThread handlerThread = new HandlerThread(getClass().getSimpleName(), android.os.Process.THREAD_PRIORITY_BACKGROUND);
		handlerThread.start();

		saveStoreHandler = new Handler(handlerThread.getLooper()) {
			public void handleMessage(Message msg) {
				SQLiteDatabase db;

				switch (msg.what) {
				case MSG_LOAD_IN_MEMORY:
					startLoadingInMemory();
					try {
						try {
							Cursor c = getReadableDatabase().query(getMainTableName(), null, null, null, null, null, null);
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
							LogManager.logger.w(TAG,"Can't query table "+getMainTableName()+" in "+InMemoryDbHelper.this, e);
						}
					} finally {
						finishLoadingInMemory();
					}
					break;

				case MSG_CLEAR_DATABASE:
					try {
						db = getWritableDatabase();
						db.delete(getMainTableName(), "1", null);
					} catch (Throwable e) {
						LogManager.logger.w(TAG,"Failed to empty table "+getMainTableName()+" in "+InMemoryDbHelper.this, e);
						sendEmptyMessage(MSG_LOAD_IN_MEMORY); // reload the DB into memory
					}
					SQLiteDatabase.releaseMemory();
					break;

				case MSG_STORE_ITEM:
					@SuppressWarnings("unchecked")
					E itemToAdd = (E) msg.obj;
					try {
						db = getWritableDatabase();
						ContentValues addValues = getValuesFromData(itemToAdd, db);
						if (addValues!=null) {
							long id = db.insertOrThrow(getMainTableName(), null, addValues);
							if (DEBUG_DB) LogManager.logger.d(TAG, InMemoryDbHelper.this+" insert "+addValues+" = "+id);
							if (id==-1)
								notifyAddItemFailed(itemToAdd, new RuntimeException("failed to add values "+addValues+" in "+InMemoryDbHelper.this.getClass().getSimpleName()));
						}
					} catch (Throwable e) {
						notifyAddItemFailed(itemToAdd, e);
					}
					break;

				case MSG_REMOVE_ITEM:
					@SuppressWarnings("unchecked")
					E itemToDelete = (E) msg.obj;
					try {
						db = getWritableDatabase();
						if (db.delete(getMainTableName(), getItemSelectClause(itemToDelete), getItemSelectArgs(itemToDelete))==0)
							notifyRemoveItemFailed(itemToDelete, new RuntimeException("No item "+itemToDelete+" in "+InMemoryDbHelper.this.getClass().getSimpleName()));
					} catch (Throwable e) {
						notifyRemoveItemFailed(itemToDelete, e);
					}
					break;

				case MSG_UPDATE_ITEM:
					@SuppressWarnings("unchecked")
					E itemToUpdate = (E) msg.obj;
					try {
						db = getWritableDatabase();
						ContentValues updateValues = getValuesFromData(itemToUpdate, db);
						if (updateValues!=null) {
							if (DEBUG_DB) LogManager.logger.d(TAG, InMemoryDbHelper.this+" update "+updateValues+" for "+itemToUpdate);
							db.update(getMainTableName(), updateValues, getItemSelectClause(itemToUpdate), getItemSelectArgs(itemToUpdate));
						}
					} catch (Throwable e) {
						notifyUpdateItemFailed(itemToUpdate, e);
					}
					break;

				case MSG_REPLACE_ITEMS:
					@SuppressWarnings("unchecked")
					DoubleItems itemsToReplace = (DoubleItems) msg.obj;
					try {
						db = getWritableDatabase();
						ContentValues newValues = getValuesFromData(itemsToReplace.itemA, db);
						if (newValues!=null) {
							if (DEBUG_DB) LogManager.logger.d(TAG, InMemoryDbHelper.this+" replace "+itemsToReplace+" with "+newValues);
							db.update(getMainTableName(), newValues, getItemSelectClause(itemsToReplace.itemB), getItemSelectArgs(itemsToReplace.itemB));
						}
					} catch (Throwable e) {
						notifyReplaceItemFailed(itemsToReplace.itemA, itemsToReplace.itemB, e);
					}
					break;

				case MSG_SWAP_ITEMS:
					@SuppressWarnings("unchecked")
					DoubleItems itemsToSwap = (DoubleItems) msg.obj;
					try {
						db = getWritableDatabase();
						ContentValues newValuesA = getValuesFromData(itemsToSwap.itemB, db);
						if (newValuesA!=null) {
							if (DEBUG_DB) LogManager.logger.d(TAG, InMemoryDbHelper.this+" update "+itemsToSwap.itemB+" with "+newValuesA);
							db.update(getMainTableName(), newValuesA, getItemSelectClause(itemsToSwap.itemA), getItemSelectArgs(itemsToSwap.itemA));
						}
					} catch (Throwable e) {
						notifyUpdateItemFailed(itemsToSwap.itemA, e);
					}
					try {
						db = getWritableDatabase();
						ContentValues newValuesB = getValuesFromData(itemsToSwap.itemA, db);
						if (newValuesB!=null) {
							if (DEBUG_DB) LogManager.logger.d(TAG, InMemoryDbHelper.this+" update "+itemsToSwap.itemA+" with "+newValuesB);
							db.update(getMainTableName(), newValuesB, getItemSelectClause(itemsToSwap.itemB), getItemSelectArgs(itemsToSwap.itemB));
						}
					} catch (Throwable e) {
						notifyUpdateItemFailed(itemsToSwap.itemB, e);
					}
					break;
					
				case MSG_CUSTOM_OPERATION:
					try {
						@SuppressWarnings("unchecked")
						InMemoryDbOperation<E> operation = (InMemoryDbOperation<E>) msg.obj;
						operation.runInMemoryDbOperation(InMemoryDbHelper.this);
					} catch (Throwable e) {
						LogManager.logger.w(TAG, InMemoryDbHelper.this+" failed to run operation "+msg.obj,e);
					}
					break;


				}

				super.handleMessage(msg);
			}
		};

		preloadInit();

		saveStoreHandler.sendEmptyMessage(MSG_LOAD_IN_MEMORY);
	}

	protected void clearDataInMemory() {
		notifyDatabaseChanged();
	}

	protected void preloadInit() {}

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
				mDbListeners.remove(listener);
			else if (l.get()==listener)
				return;
		}
		if (mDataLoaded)
			listener.onMemoryDbChanged(this);
		mDbListeners.add(new WeakReference<InMemoryDbListener<E>>(listener));
	}

	/**
	 * delete all the data in memory and in the database
	 */
	public final void clear() {
		clearDataInMemory();
		saveStoreHandler.sendEmptyMessage(MSG_CLEAR_DATABASE);
	}

	private void notifyAddItemFailed(E item, Throwable cause) {
		if (mErrorHandler!=null) {
			final InMemoryDbErrorHandler<E> listener = mErrorHandler.get(); 
			if (listener==null)
				mErrorHandler = null;
			else
				listener.onAddItemFailed(this, item, cause);
		}
		notifyDatabaseChanged();
	}

	private void notifyReplaceItemFailed(E srcItem, E replacement, Throwable cause) {
		if (mErrorHandler!=null) {
			final InMemoryDbErrorHandler<E> listener = mErrorHandler.get(); 
			if (listener==null)
				mErrorHandler = null;
			else
				listener.onReplaceItemFailed(this, srcItem, replacement, cause);
		}
		notifyDatabaseChanged();
	}

	private void notifyUpdateItemFailed(E item, Throwable cause) {
		if (mErrorHandler!=null) {
			final InMemoryDbErrorHandler<E> listener = mErrorHandler.get(); 
			if (listener==null)
				mErrorHandler = null;
			else
				listener.onAddItemFailed(this, item, cause);
		}
		notifyDatabaseChanged();
	}

	private void notifyRemoveItemFailed(E item, Throwable cause) {
		if (mErrorHandler!=null) {
			final InMemoryDbErrorHandler<E> listener = mErrorHandler.get(); 
			if (listener==null)
				mErrorHandler = null;
			else
				listener.onRemoveItemFailed(this, item, cause);
		}
		notifyDatabaseChanged();
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
	 * request to store the item in the database, it should be kept in synch with the in memory storage
	 * <p>
	 * will call the {@link InMemoryDbErrorHandler} in case of error
	 * @param item
	 */
	protected final void scheduleAddOperation(E item) {
		saveStoreHandler.sendMessage(Message.obtain(saveStoreHandler, MSG_STORE_ITEM, item));
		notifyDatabaseChanged();
	}

	protected final void scheduleUpdateOperation(E item) {
		saveStoreHandler.sendMessage(Message.obtain(saveStoreHandler, MSG_UPDATE_ITEM, item));
		notifyDatabaseChanged();
	}

	private class DoubleItems {
		final E itemA;
		final E itemB;

		public DoubleItems(E itemA, E itemB) {
			this.itemA = itemA;
			this.itemB = itemB;
		}

	}

	protected final void scheduleReplaceOperation(E original, E replacement) {
		saveStoreHandler.sendMessage(Message.obtain(saveStoreHandler, MSG_REPLACE_ITEMS, new DoubleItems(original, replacement)));
		notifyDatabaseChanged();
	}

	protected final void scheduleSwapOperation(E itemA, E itemB) {
		saveStoreHandler.sendMessage(Message.obtain(saveStoreHandler, MSG_SWAP_ITEMS, new DoubleItems(itemA, itemB)));
		notifyDatabaseChanged();
	}

	/**
	 * request to delete the item from the database, it should be kept in synch with the in memory storage
	 * <p>
	 * will call the {@link InMemoryDbErrorHandler} in case of error
	 * @param item
	 */
	protected final void scheduleRemoveOperation(E item) {
		saveStoreHandler.sendMessage(Message.obtain(saveStoreHandler, MSG_REMOVE_ITEM, item));
		notifyDatabaseChanged();
	}
	
	/**
	 * run the operation in the internal thread
	 * @param operation
	 */
	protected final void scheduleCustomOperation(InMemoryDbOperation<E> operation) {
		saveStoreHandler.sendMessage(Message.obtain(saveStoreHandler, MSG_CUSTOM_OPERATION, operation));
	}

	/**
	 * called when we are about to read all items from the disk
	 */
	protected void startLoadingInMemory() {
		mDataLoaded = false;
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
		mDataLoaded = true;
		notifyDatabaseChanged();
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

}
