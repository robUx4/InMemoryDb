package org.gawst.asyncdb;

import android.database.sqlite.SQLiteOpenHelper;

import java.util.Collection;

/**
 * A {@link SQLiteOpenHelper} class with a worker thread for all disk work. Elements are not stored in memory.
 * 
 * @param <E> Type of Objects that are stored in the main table
 */
public abstract class AsynchronousDatabase<E, INSERT_ID> extends AsynchronousDbHelper<E, INSERT_ID> {

	/**
	 * Constructor for the SQLite database with a worker thread
	 * @param db The already created {@link android.database.sqlite.SQLiteOpenHelper} to use as storage
	 * @param name Database name for logs
	 * @param logger The {@link Logger} to use for all logs (can be null for the default Android logs)
	 */
	public AsynchronousDatabase(DataSource<E, INSERT_ID> db, String name, Logger logger) {
		super(db, name, logger, null);
	}

	@Override
	public final void addItemInMemory(E item) {
		// do nothing
	}

	@Override
	protected final boolean shouldReloadAllData() {
		return false;
	}

	@Override
	protected boolean notifyOnSchedule() {
		return false;
	}

	/**
	 * Add a new element in the database (asynchronous)
	 * <p>Helper call for {@link AsynchronousDbHelper#scheduleAddOperation(Object, org.gawst.asyncdb.purge.PurgeHandler)}
	 * <p>If adding failed {@link AsynchronousDbErrorHandler#onAddItemFailed(AsynchronousDbHelper, Object, android.content.ContentValues, Throwable) AsynchronousDbErrorHandler.onAddItemFailed()} will be called
	 * @param item to add
	 * @see AsynchronousDbHelper#scheduleAddOperation(Object, org.gawst.asyncdb.purge.PurgeHandler)
	 */
	public void add(E item) {
		scheduleAddOperation(item);
	}

	/**
	 * Add new elements in the database (asynchronous)
	 * <p>Helper call for {@link AsynchronousDbHelper#scheduleAddOperation(java.util.Collection, org.gawst.asyncdb.purge.PurgeHandler)}
	 * <p>{@link AsynchronousDbErrorHandler#onAddItemFailed(AsynchronousDbHelper, Object, android.content.ContentValues, Throwable) AsynchronousDbErrorHandler.onAddItemFailed()} will be called for each addition failure
	 * @param items to add
	 * @see AsynchronousDbHelper#scheduleAddOperation(java.util.Collection, org.gawst.asyncdb.purge.PurgeHandler)
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
