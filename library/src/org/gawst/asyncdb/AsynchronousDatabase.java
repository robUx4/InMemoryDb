package org.gawst.asyncdb;

import java.util.Collection;

import android.content.Context;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * A {@link SQLiteOpenHelper} class with a worker thread for all disk work
 * 
 * @param <E> Type of Objects that are stored in the main table
 */
public abstract class AsynchronousDatabase<E> extends AsynchronousDbHelper<E> {

	/**
	 * Constructor for the SQLite database with a worker thread
	 * @param db The already created {@link android.database.sqlite.SQLiteOpenHelper} to use as storage
	 * @param context Used to open or create the database
	 * @param name Database filename on disk
	 * @param logger The {@link org.gawst.asyncdb.Logger} to use for all logs (can be null for the default Android logs)
	 */
	public AsynchronousDatabase(DataSource<E> db, Context context, String name, Logger logger) {
		super(db, context, name, logger, null);
	}

	@Override
	public final void addItemInMemory(E item) {
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
