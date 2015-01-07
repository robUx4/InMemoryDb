package org.gawst.asyncdb;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
* Created by Dell990MT on 07/01/2015.
*/
public abstract class DatabaseSourcePurger<LAST_ELEMENT> implements PurgeHandler {
	private final DatabaseSource dataSource;
	private final int maxItems;
	private final int checkInsertFrequency;
	private Integer nextCheck;

	public DatabaseSourcePurger(int maxItems, DatabaseSource databaseSource) {
		this(maxItems, 1, databaseSource);
	}

	public DatabaseSourcePurger(int maxItems, int checkInsertFrequency, DatabaseSource databaseSource) {
		this.dataSource = databaseSource;
		if (maxItems <= 0) throw new IllegalArgumentException("the max item in AsyncHandlerPurge must be positive");
		if (checkInsertFrequency <= 0) throw new IllegalArgumentException("the insert purge frequency in AsyncHandlerPurge must be positive");

		this.maxItems = maxItems;
		this.checkInsertFrequency = checkInsertFrequency;
		nextCheck = checkInsertFrequency;
	}

	@NonNull
	protected abstract String[] getFilterFields();

	@NonNull
	protected abstract String getFilterOrder();

	protected abstract LAST_ELEMENT getLastFilteredElement(Cursor cursor);

	@NonNull
	protected abstract String getDeleteClause(@NonNull LAST_ELEMENT lastElement);

	@NonNull
	protected abstract String[] getDeleteArgs(@NonNull LAST_ELEMENT lastElement);

	/**
	 * @return A Select clause to filter the elements handled by the purge or {@code null}
	 */
	@Nullable
	protected String getPurgeFilterClause() {
		return null;
	}

	/**
	 * @return The arguments corresponding to the {@link #getPurgeFilterClause()} or {@code null}
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
							if (c.moveToNext())
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
