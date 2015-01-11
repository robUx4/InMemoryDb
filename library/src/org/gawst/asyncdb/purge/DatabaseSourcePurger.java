package org.gawst.asyncdb.purge;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.gawst.asyncdb.AsynchronousDbHelper;
import org.gawst.asyncdb.AsynchronousDbOperation;
import org.gawst.asyncdb.LogManager;
import org.gawst.asyncdb.source.DatabaseSource;

/**
 * Abstract helper class to purge a database.
 *
 * @param <LAST_ELEMENT> Type of the last element to keep during the purge.
 */
public abstract class DatabaseSourcePurger<LAST_ELEMENT> implements PurgeHandler {
	private final DatabaseSource<?, ?> dataSource;
	private final int maxItems;
	private final int checkInsertFrequency;
	private Integer nextCheck;

	/**
	 * Constructor for the purger. It will purge the database after each insertion.
	 *
	 * @param maxItems       Maximum number of items to keep in the database.
	 * @param databaseSource Database source (Sqlite, ContentProvider)
	 */
	public DatabaseSourcePurger(int maxItems, DatabaseSource<?, ?> databaseSource) {
		this(maxItems, 1, databaseSource);
	}

	/**
	 * Constructor for the purger.
	 *
	 * @param maxItems             Maximum number of items to keep in the database.
	 * @param checkInsertFrequency The number of insertion before a purge is done. A purge is done after the first insertion.
	 * @param databaseSource       Database source (Sqlite, ContentProvider)
	 */
	public DatabaseSourcePurger(int maxItems, int checkInsertFrequency, DatabaseSource<?, ?> databaseSource) {
		this.dataSource = databaseSource;
		if (maxItems <= 0) throw new IllegalArgumentException("the max item in AsyncHandlerPurge must be positive");
		if (checkInsertFrequency <= 0) throw new IllegalArgumentException("the insert purge frequency in AsyncHandlerPurge must be positive");

		this.maxItems = maxItems;
		this.checkInsertFrequency = 0; // we should purge at the first insert, in case the app is closed too often without purging
		nextCheck = checkInsertFrequency;
	}

	/**
	 * @return The list of fields to read from the database when looking for the {@link LAST_ELEMENT}.
	 */
	@NonNull
	protected abstract String[] getFilterFields();

	/**
	 * @return The order to apply to elements to find elements to delete. It must use fields returned by {@link #getFilterFields()}
	 */
	@NonNull
	protected abstract String getFilterOrder();

	/**
	 * Transform the {@code Cursor} into the {@link LAST_ELEMENT} to keep during the purge.
	 * @param cursor The Cursor positioned on the last element.
	 * @return The {@link LAST_ELEMENT} to keep during the purge or {@code null} if there is no last element.
	 * @see #getDeleteClause(Object)
	 * @see #getDeleteArgs(Object)
	 */
	@Nullable
	protected abstract LAST_ELEMENT getLastFilteredElement(Cursor cursor);

	/**
	 * Get the {@code delete()} SQL clause to remove elements 'older' than {@code lastElement}
	 * @param lastElement The {@link LAST_ELEMENT} to keep in the database
	 * @see #getDeleteArgs(Object)
	 */
	@NonNull
	protected abstract String getDeleteClause(@NonNull LAST_ELEMENT lastElement);

	/**
	 * Get the {@code delete()} SQL clause arguments to remove elements 'older' than {@code lastElement}
	 * @param lastElement The {@link LAST_ELEMENT} to keep in the database
	 * @see #getDeleteClause(Object)
	 */
	@NonNull
	protected abstract String[] getDeleteArgs(@NonNull LAST_ELEMENT lastElement);

	/**
	 * @return A Select clause to filter the elements handled by the purge or {@code null} for no filtering.
	 */
	@Nullable
	protected String getPurgeFilterClause() {
		return null;
	}

	/**
	 * @return The arguments corresponding to the {@link #getPurgeFilterClause()} or {@code null} for no filtering.
	 */
	@Nullable
	protected String[] getPurgeFilterArgs() {
		return null;
	}

	@Override
	public void onElementsAdded(AsynchronousDbHelper<?, ?> db) {
		if (nextCheck != null && --nextCheck < 0) {
			nextCheck = null; // pending purge
			db.scheduleCustomOperation(new AsynchronousDbOperation() {
				@Override
				public void runInMemoryDbOperation(AsynchronousDbHelper<?, ?> db) {
					int deleted = 0;
					try {
						LAST_ELEMENT lastElement = null;
						Cursor c = dataSource.query(getFilterFields(), getPurgeFilterClause(), getPurgeFilterArgs(), null, null, getFilterOrder(), Integer.toString(maxItems) + ", 1");
						try {
							if (c.moveToFirst())
								lastElement = getLastFilteredElement(c);
						} finally {
							c.close();
						}

						if (lastElement != null) {
							try {
								deleted = dataSource.delete(getDeleteClause(lastElement), getDeleteArgs(lastElement));
							} catch (IllegalStateException e) {
								// in some case (2.x) the DB is closed unexpectedly
							} catch (Exception e) {
								// in some case (4.1) we get "cannot rollback - no transaction is active"
							}
						}
					} catch (Exception e) {
						// can crash on Samsung GT-P1000 2.3.3
					}

					if (deleted > 0) {
						LogManager.getLogger().d(AsynchronousDbHelper.TAG, "purged " + deleted + " elements in " + db);
					}

					nextCheck = checkInsertFrequency;
				}
			});
		}
	}
}
