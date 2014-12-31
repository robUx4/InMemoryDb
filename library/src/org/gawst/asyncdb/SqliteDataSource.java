package org.gawst.asyncdb;

import java.io.File;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;

/**
 * Created by robUx4 on 12/31/2014.
 */
public class SqliteDataSource<E> extends CursorDataSource<E> {

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

	@Override
	protected Cursor readAll() {
		return db.getReadableDatabase().query(tableName, null, null, null, null, null, null);
	}

	@Override
	public int clearAllData() {
		int result = db.getWritableDatabase().delete(tableName, "1", null);
		SQLiteDatabase.releaseMemory();
		return result;
	}

	@Override
	public Object insert(ContentValues element) throws RuntimeException {
		long id = db.getWritableDatabase().insertOrThrow(tableName, null, element);
		if (id == -1)
			return null;
		return id;
	}

	@Override
	public boolean update(E itemToUpdate, ContentValues updateValues) {
		return db.getWritableDatabase().update(tableName, updateValues,
				cursorSourceHandler.getItemSelectClause(itemToUpdate),
				cursorSourceHandler.getItemSelectArgs(itemToUpdate))!=0;
	}

	@Override
	public boolean delete(E itemToDelete) {
		return db.getWritableDatabase().delete(tableName,
				cursorSourceHandler.getItemSelectClause(itemToDelete),
				cursorSourceHandler.getItemSelectArgs(itemToDelete))!=0;
	}

	@Override
	public void eraseSource() {
		File corruptedDbFile = context.getDatabasePath(databaseName);
		corruptedDbFile.delete();
	}
}
