package org.gawst.asyncdb.purge;

import android.database.Cursor;
import android.support.annotation.Nullable;

import org.gawst.asyncdb.source.DatabaseSource;

/**
 * Helper class to purge a database with a maximum number of items and based on a date field.
 *
 * @author Created by robUx4 on 07/01/2015.
 */
public class DatabasePurgerMaxDate extends DatabaseSourcePurgerMax<Long> {
	/**
	 * Constructor for the database purger. It will purge the database after each insertion.
	 *
	 * @param maxItems       Maximum number of items to keep in the database.
	 * @param dateField      Date field in the database.
	 * @param databaseSource Database source (Sqlite, ContentProvider)
	 */
	public DatabasePurgerMaxDate(int maxItems, String dateField, DatabaseSource<?, ?> databaseSource) {
		super(maxItems, dateField, databaseSource);
	}

	/**
	 * Constructor for the database purger.
	 *
	 * @param maxItems             Maximum number of items to keep in the database.
	 * @param checkInsertFrequency The number of insertion before a purge is done. A purge is done after the first insertion.
	 * @param dateField            Date field in the database.
	 * @param databaseSource       Database source (Sqlite, ContentProvider)
	 */
	public DatabasePurgerMaxDate(int maxItems, int checkInsertFrequency, String dateField, DatabaseSource<?, ?> databaseSource) {
		super(maxItems, checkInsertFrequency, dateField, databaseSource);
	}

	@Nullable
	@Override
	protected final Long getLastFilteredElement(Cursor cursor) {
		int freshnessIndex = cursor.getColumnIndex(fieldName);
		if (cursor.isNull(freshnessIndex))
			return null;
		return cursor.getLong(freshnessIndex);
	}
}
