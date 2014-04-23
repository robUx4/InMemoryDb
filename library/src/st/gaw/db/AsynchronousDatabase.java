package st.gaw.db;

import java.util.Collection;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;

/**
 * A {@link SQLiteOpenHelper} class with a worker thread for all disk work
 * 
 * @param <E> Type of Objects that are stored in the main table
 */
public abstract class AsynchronousDatabase<E> extends AsynchronousDbHelper<E> {

	/**
	 * Constructor for the SQLite database with a worker thread
	 * @param context Used to open or create the database
	 * @param name Database filename on disk
	 * @param version Version number of the database (starting at 1); if the database is older,
	 *     {@link #onUpgrade} will be used to upgrade the database; if the database is
	 *     newer, {@link #onDowngrade} will be used to downgrade the database
	 * @param logger The {@link Logger} to use for all logs (can be null for the default Android logs)
	 */
	public AsynchronousDatabase(Context context, String name, int version, Logger logger) {
		super(context, name, version, logger, null);
	}

	/**
	 * Constructor for the SQLite database with a worker thread with the same signature as the base {@link SQLiteOpenHelper} constructor
	 * @param context Used to open or create the database
	 * @param name Database filename on disk
	 * @param factory to use for creating cursor objects, or null for the default
	 * @param version Version number of the database (starting at 1); if the database is older,
	 *     {@link #onUpgrade} will be used to upgrade the database; if the database is
	 *     newer, {@link #onDowngrade} will be used to downgrade the database
	 */
	public AsynchronousDatabase(Context context, String name, CursorFactory factory, int version) {
		super(context, name, factory, version, null, null);
	}

	@Override
	protected final void addCursorInMemory(Cursor c) {
		// do nothing
	}

	@Override
	protected final boolean shouldReloadAllData() {
		return false;
	}

	/**
	 * Add a new element in the database (asynchronous)
	 * <p>Helper call for {@link #scheduleAddOperation(E)}
	 * <p>If adding failed {@link AsynchronousDbErrorHandler#onAddItemFailed(AsynchronousDbHelper, Object, android.content.ContentValues, Throwable) AsynchronousDbErrorHandler.onAddItemFailed()} will be called
	 * @param item to add
	 * @see #scheduleAddOperation(E)
	 */
	public void add(E item) {
		scheduleAddOperation(item);
	}

	/**
	 * Add new elements in the database (asynchronous)
	 * <p>Helper call for {@link #scheduleAddOperation(Collection)}
	 * <p>{@link AsynchronousDbErrorHandler#onAddItemFailed(AsynchronousDbHelper, Object, android.content.ContentValues, Throwable) AsynchronousDbErrorHandler.onAddItemFailed()} will be called for each addition failure
	 * @param items to add
	 * @see #scheduleAddOperation(Collection)
	 */
	public void addAll(Collection<E> items) {
		scheduleAddOperation(items);
	}

	/**
	 * Update an element in the database
	 * <p>Helper call for {@link #scheduleUpdateOperation(E)}
	 * @param item to update
	 * @see #scheduleUpdateOperation(E)
	 */
	public void update(E item) {
		scheduleUpdateOperation(item);
	}

	/**
	 * Remove an element from the database (asynchronous)
	 * <p>Helper call for {@link #scheduleRemoveOperation(E)}
	 * <p>If the removal fails {@link AsynchronousDbErrorHandler#onRemoveItemFailed(AsynchronousDbHelper, Object, Throwable) AsynchronousDbErrorHandler.onRemoveItemFailed()} will be called
	 * @param item to remove
	 * @see #scheduleRemoveOperation(E)
	 */
	public void remove(E item) {
		scheduleRemoveOperation(item);
	}

}
