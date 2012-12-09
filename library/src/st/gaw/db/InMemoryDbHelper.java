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
	 * @param listener
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

	protected abstract String getMainTableName();
	protected abstract String getDeleteClause(E itemToDelete);
	protected abstract String[] getDeleteArgs(E itemToDelete);

	/**
	 * should open the database and store in memory all the elements
	 */
	protected abstract void loadDataInMemory();

	protected abstract E getDataFromCursor(Cursor c);
	protected abstract ContentValues getValuesFromData(E data);

	protected void scheduleAddOperation(E item) {
		saveStoreHandler.sendMessage(Message.obtain(saveStoreHandler, MSG_STORE_ITEM, item));
	}

	protected void scheduleRemoveOperation(E item) {
		saveStoreHandler.sendMessage(Message.obtain(saveStoreHandler, MSG_REMOVE_ITEM, item));
	}

}
