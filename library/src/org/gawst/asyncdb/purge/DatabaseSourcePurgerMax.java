package org.gawst.asyncdb.purge;

import org.gawst.asyncdb.DatabaseSource;

import android.support.annotation.NonNull;
import android.text.TextUtils;

/**
* Created by robUx4 on 07/01/2015.
*/
public abstract class DatabaseSourcePurgerMax<LAST_ELEMENT> extends DatabaseSourcePurger<LAST_ELEMENT> {
	protected final String fieldName;

	public DatabaseSourcePurgerMax(int maxItems, String fieldName, DatabaseSource databaseSource) {
		this(maxItems, 1, fieldName, databaseSource);
	}

	public DatabaseSourcePurgerMax(int maxItems, int checkInsertFrequency, String fieldName, DatabaseSource databaseSource) {
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
			for (int i = 0; i < args.length; ++i)
				result[i + 1] = args[i];
			return result;
		}
	}
}
