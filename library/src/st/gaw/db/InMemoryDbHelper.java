package st.gaw.db;

import java.lang.ref.WeakReference;

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

	private final Handler saveStoreHandler;
	private static final int MSG_LOAD_IN_MEMORY = 100;
	private static final int MSG_STORE_ITEM     = 101;
	private static final int MSG_REMOVE_ITEM    = 102;

	private WeakReference<InMemoryDbErrorHandler<E>> mListener; // not protected for now

	/**
	 * @param context to use to open or create the database
	 * @param name of the database file, or null for an in-memory database
	 * @param version number of the database (starting at 1); if the database is older,
	 *     {@link #onUpgrade} will be used to upgrade the database; if the database is
	 *     newer, {@link #onDowngrade} will be used to downgrade the database
	 */
	@SuppressLint("HandlerLeak")
	protected InMemoryDbHelper(Context context, String name, int version) {
		super(context, name, null, version);

		HandlerThread handlerThread = new HandlerThread(getClass().getSimpleName(), android.os.Process.THREAD_PRIORITY_BACKGROUND);
		handlerThread.start();

		saveStoreHandler = new Handler(handlerThread.getLooper()) {
			public void handleMessage(Message msg) {
				SQLiteDatabase db;
				switch (msg.what) {
				case MSG_LOAD_IN_MEMORY:
					loadDataInMemory();	
					break;
				case MSG_STORE_ITEM:
					@SuppressWarnings("unchecked")
					E itemToAdd = (E) msg.obj;
					ContentValues addValues = getValuesFromData(itemToAdd);
					db = getWritableDatabase();
					db.beginTransaction();
					try {
						if (db.insertOrThrow(getMainTableName(), null, addValues)==-1)
							notifyAddItemFailed(itemToAdd, new RuntimeException("failed to add values "+addValues+" in "+InMemoryDbHelper.this.getClass().getSimpleName()));
						db.setTransactionSuccessful();
					} catch (Throwable e) {
						notifyAddItemFailed(itemToAdd, e);
					} finally {
						try {
							db.endTransaction();
						} catch (SQLException e) {
							notifyAddItemFailed(itemToAdd, e);
						}
					}
					break;
				case MSG_REMOVE_ITEM:
					@SuppressWarnings("unchecked")
					E itemToDelete = (E) msg.obj;
					db = getWritableDatabase();
					db.beginTransaction();
					try {
						if (db.delete(getMainTableName(), getDeleteClause(itemToDelete), getDeleteArgs(itemToDelete))==0)
							notifyRemoveItemFailed(itemToDelete, new RuntimeException("No item "+itemToDelete+" in "+InMemoryDbHelper.this.getClass().getSimpleName()));
						db.setTransactionSuccessful();
					} catch (Throwable e) {
						notifyRemoveItemFailed(itemToDelete, e);
					} finally {
						try {
							db.endTransaction();
						} catch (SQLException e) {
							notifyRemoveItemFailed(itemToDelete, e);
						}
					}
					break;
				}

				super.handleMessage(msg);
			};
		};

		saveStoreHandler.sendMessage(Message.obtain(saveStoreHandler, MSG_LOAD_IN_MEMORY));
	}

	/**
	 * set the listener that will receive error events
	 * @param listener null to remove the listener
	 */
	public void setDbListener(InMemoryDbErrorHandler<E> listener) {
		if (listener==null)
			mListener = null;
		else
			mListener = new WeakReference<InMemoryDbErrorHandler<E>>(listener);
	}

	private void notifyAddItemFailed(E item, Throwable cause) {
		if (mListener!=null) {
			final InMemoryDbErrorHandler<E> listener = mListener.get(); 
			if (listener==null)
				mListener = null;
			else
				listener.onAddItemFailed(InMemoryDbHelper.this, item, cause);
		}
	}

	private void notifyRemoveItemFailed(E item, Throwable cause) {
		if (mListener!=null) {
			final InMemoryDbErrorHandler<E> listener = mListener.get(); 
			if (listener==null)
				mListener = null;
			else
				listener.onRemoveItemFailed(InMemoryDbHelper.this, item, cause);
		}
	}

	/**
	 * the name of the main table corresponding to the inMemory elements
	 * @return the name of the main table
	 */
	protected abstract String getMainTableName();
	
	/**
	 * the where clause that should be used to delete the item
	 * @param itemToDelete the data about to be deleted
	 * @return a string for the whereClause in {@link SQLiteDatabase#delete(String, String, String[])}
	 */
	protected abstract String getDeleteClause(E itemToDelete);
	/**
	 * the where arguments that should be used to delete the item
	 * @param itemToDelete the data about to be deleted
	 * @return a string array for the whereArgs in {@link SQLiteDatabase#delete(String, String, String[])}
	 */
	protected abstract String[] getDeleteArgs(E itemToDelete);

	/**
	 * should open the database and store in memory all the elements
	 */
	protected abstract void loadDataInMemory();

	/**
	 * transform the {@link Cursor} into an element that can be used in memory
	 * @param c the Cursor to transform
	 * @return a formated element used in memory
	 * @see #getValuesFromData(Object)
	 */
	protected abstract E getDataFromCursor(Cursor c);
	/**
	 * transform the element in memory into {@link ContentValues} that can be saved in the database
	 * @param c the data to transform
	 * @return a ContentValues element with all data that can be used to restore the data later from the database
	 * @see #getDataFromCursor(Cursor)
	 */
	protected abstract ContentValues getValuesFromData(E data);

	/**
	 * request to store the item in the database, it should be kept in synch with the in memory storage
	 * <p>
	 * will call the {@link InMemoryDbErrorHandler} in case of error
	 * @param item
	 */
	protected void scheduleAddOperation(E item) {
		saveStoreHandler.sendMessage(Message.obtain(saveStoreHandler, MSG_STORE_ITEM, item));
	}

	/**
	 * request to delete the item from the database, it should be kept in synch with the in memory storage
	 * <p>
	 * will call the {@link InMemoryDbErrorHandler} in case of error
	 * @param item
	 */
	protected void scheduleRemoveOperation(E item) {
		saveStoreHandler.sendMessage(Message.obtain(saveStoreHandler, MSG_REMOVE_ITEM, item));
	}

}
