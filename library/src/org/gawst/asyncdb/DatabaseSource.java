package org.gawst.asyncdb;

import android.content.ContentValues;
import android.database.Cursor;

/**
 * Created by robUx4 on 06/01/2015.
 */
public interface DatabaseSource<INSERT_ID> {
	Cursor query(String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit);

	int update(String selection, String[] selectionArgs, ContentValues updateValues);

	int delete(String selection, String[] selectionArgs);

	INSERT_ID insert(ContentValues element) throws RuntimeException;

}
