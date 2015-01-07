package org.gawst.asyncdb.purge;

import org.gawst.asyncdb.DatabaseSource;

import android.database.Cursor;

/**
 * Helper class to purge a database with a maximum number of items and based on a date field
 *
 * @author Created by robUx4 on 07/01/2015.
 */
public class DatabasePurgeMaxDate extends DatabaseSourcePurgerMax<Long> {
	public DatabasePurgeMaxDate(int maxItems, String dateField, DatabaseSource<?> databaseSource) {
		super(maxItems, dateField, databaseSource);
	}

	public DatabasePurgeMaxDate(int maxItems, int checkInsertFrequency, String fieldName, DatabaseSource<?> databaseSource) {
		super(maxItems, checkInsertFrequency, fieldName, databaseSource);
	}

	@Override
	protected final Long getLastFilteredElement(Cursor cursor) {
		int freshnessIndex = cursor.getColumnIndex(fieldName);
		if (cursor.isNull(freshnessIndex))
			return null;
		return cursor.getLong(freshnessIndex);
	}
}
