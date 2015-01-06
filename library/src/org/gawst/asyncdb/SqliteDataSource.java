package org.gawst.asyncdb;

import java.io.File;

import org.gawst.asyncdb.adapter.UIHandler;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;

/**
 * Created by robUx4 on 12/31/2014.
 */
public class SqliteDataSource<E> extends CursorDataSource<E, Long> {

	private final Context context;
	private final SQLiteOpenHelper db;
	private final String tableName;
	private final String databaseName;

	public SqliteDataSource(@NonNull Context context, @NonNull SQLiteOpenHelper db, @NonNull String tableName, @NonNull String databaseName, @NonNull CursorSourceHandler<E> cursorSourceHandler) {
		super(cursorSourceHandler);
		this.context = context;
		this.db = db;
		this.tableName = tableName;
		this.databaseName = databaseName;
	}

	public Cursor query(String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit) {
		UIHandler.assertNotUIThread();
		return db.getReadableDatabase().query(tableName, columns, selection, selectionArgs, groupBy, having, orderBy, limit);
	}

	@Override
	public int clearAllData() {
		UIHandler.assertNotUIThread();
		int result = db.getWritableDatabase().delete(tableName, "1", null);
		SQLiteDatabase.releaseMemory();
		return result;
	}

	@Override
	public Long insert(ContentValues element) throws RuntimeException {
		long id = db.getWritableDatabase().insertOrThrow(tableName, null, element);
		if (id == -1)
			return null;
		return id;
	}

	@Override
	public int update(String selection, String[] selectionArgs, ContentValues updateValues) {
		return db.getWritableDatabase().update(tableName, updateValues, selection, selectionArgs);
	}

	@Override
	public int delete(String selection, String[] selectionArgs) {
		UIHandler.assertNotUIThread();
		return db.getWritableDatabase().delete(tableName, selection, selectionArgs);
	}

	@Override
	public void eraseSource() {
		File corruptedDbFile = context.getDatabasePath(databaseName);
		corruptedDbFile.delete();
	}
}
