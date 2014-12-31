package org.gawst.asyncdb;

import java.io.File;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by robUx4 on 12/31/2014.
 */
public abstract class SqliteDataSource<E> implements DataSource<E> {

	private final Context context;
	private final SQLiteOpenHelper db;
	private final String tableName;
	private final String databaseName;

	public SqliteDataSource(Context context, SQLiteOpenHelper db, String tableName, String databaseName) {
		this.context = context;
		this.db = db;
		this.tableName = tableName;
		this.databaseName = databaseName;
	}

	/**
	 * The where clause that should be used to update/delete the item.
	 * <p> see {@link #getItemSelectArgs(Object)}
	 * @param itemToSelect the item about to be selected in the database
	 * @return a {@link java.lang.String} for the whereClause in {@link android.database.sqlite.SQLiteDatabase#update(String, android.content.ContentValues, String, String[])} or {@link android.database.sqlite.SQLiteDatabase#delete(String, String, String[])}
	 */
	protected abstract String getItemSelectClause(E itemToSelect);

	/**
	 * The where arguments that should be used to update/delete the item.
	 * <p> see {@link #getItemSelectClause(Object)}
	 * @param itemToSelect the item about to be selected in the database
	 * @return a {@link java.lang.String} array for the whereArgs in {@link android.database.sqlite.SQLiteDatabase#update(String, android.content.ContentValues, String, String[])} or {@link android.database.sqlite.SQLiteDatabase#delete(String, String, String[])}
	 */
	protected abstract String[] getItemSelectArgs(E itemToSelect);

	@Override
	public Cursor queryAll() {
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
		return db.getWritableDatabase().update(tableName, updateValues, getItemSelectClause(itemToUpdate), getItemSelectArgs(itemToUpdate))!=0;
	}

	@Override
	public boolean delete(E itemToDelete) {
		return db.getWritableDatabase().delete(tableName, getItemSelectClause(itemToDelete), getItemSelectArgs(itemToDelete))!=0;
	}

	@Override
	public void eraseSource() {
		File corruptedDbFile = context.getDatabasePath(databaseName);
		corruptedDbFile.delete();
	}
}
