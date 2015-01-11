package org.gawst.asyncdb.purge;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.gawst.asyncdb.source.DatabaseSource;

/**
 * Abstract helper class to purge a database with a maximum number of items and based on a sorted field.
 *
 * @param <LAST_ELEMENT> Type of the last element to keep during the purge.
 */
public abstract class DatabaseSourcePurgerMax<LAST_ELEMENT> extends DatabaseSourcePurger<LAST_ELEMENT> {
	protected final String fieldName;

	/**
	 * Constructor for the database purger. It will purge the database after each insertion.
	 *
	 * @param maxItems       Maximum number of items to keep in the database.
	 * @param fieldName      Name of the sorted field in the database to determine 'old' elements
	 * @param databaseSource Database source (Sqlite, ContentProvider)
	 */
	public DatabaseSourcePurgerMax(int maxItems, String fieldName, DatabaseSource<?, ?> databaseSource) {
		this(maxItems, 1, fieldName, databaseSource);
	}

	/**
	 * Constructor for the database purger.
	 *
	 * @param maxItems             Maximum number of items to keep in the database.
	 * @param checkInsertFrequency The number of insertion before a purge is done. A purge is done after the first insertion.
	 * @param fieldName            Name of the sorted field in the database to determine 'old' elements
	 * @param databaseSource       Database source (Sqlite, ContentProvider)
	 */
	public DatabaseSourcePurgerMax(int maxItems, int checkInsertFrequency, String fieldName, DatabaseSource<?, ?> databaseSource) {
		super(maxItems, checkInsertFrequency, databaseSource);
		this.fieldName = fieldName;
	}

	@NonNull
	@Override
	protected final String[] getFilterFields() {
		return new String[]{fieldName};
	}

	@NonNull
	@Override
	protected String getFilterOrder() {
		return fieldName + " desc";
	}

	@NonNull
	@Override
	protected String getDeleteClause(LAST_ELEMENT lastElement) {
		String purgeFilterClause = getPurgeFilterClause();
		if (TextUtils.isEmpty(purgeFilterClause)) {
			return fieldName + " < ?";
		} else {
			return fieldName + " < ? AND (" + purgeFilterClause + ')';
		}
	}

	@NonNull
	@Override
	protected String[] getDeleteArgs(LAST_ELEMENT lastElement) {
		String[] args = getPurgeFilterArgs();
		if (args == null) {
			return new String[]{String.valueOf(lastElement)};
		} else {
			String[] result = new String[args.length + 1];
			result[0] = String.valueOf(lastElement);
			System.arraycopy(args, 0, result, 1, args.length);
			return result;
		}
	}
}
