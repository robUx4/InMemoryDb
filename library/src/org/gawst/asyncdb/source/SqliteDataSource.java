package org.gawst.asyncdb.source;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.support.annotation.NonNull;

import org.gawst.asyncdb.source.typed.TypedSqliteDataSource;

/**
 * A {@link org.gawst.asyncdb.DataSource} backed by a {@link android.database.sqlite.SQLiteOpenHelper} storage.
 *
 * @author Created by robUx4 on 12/31/2014.
 */
public class SqliteDataSource<E> extends TypedSqliteDataSource<E, Cursor> implements DatabaseSource<Long, Void> {
	/**
	 * Constructor. (API v14 minimum)
	 *
	 * @param context                Context used to erase the database file in case it's corrupted.
	 * @param db                     The SQL database used to read/write data.
	 * @param tableName              Name of the SQL table that contains the elements to read.
	 * @param databaseElementHandler Handler to transform {@code Cursor} into {@link E} elements or {@link E} elements to selections.
	 */
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	public SqliteDataSource(@NonNull Context context, @NonNull SQLiteOpenHelper db, @NonNull String tableName, @NonNull DatabaseElementHandler<E> databaseElementHandler) {
		super(context, db, tableName, databaseElementHandler);
	}

	/**
	 * Constructor.
	 *
	 * @param context                Context used to erase the database file in case it's corrupted.
	 * @param db                     The SQL database used to read/write data.
	 * @param tableName              Name of the SQL table that contains the elements to read.
	 * @param databaseName           Name of the database file on disk, in case it's corrupted and needs to be erased.
	 * @param databaseElementHandler Handler to transform {@code Cursor} into {@link E} elements or {@link E} elements to selections.
	 */
	public SqliteDataSource(@NonNull Context context, @NonNull SQLiteOpenHelper db, @NonNull String tableName, @NonNull String databaseName, @NonNull DatabaseElementHandler<E> databaseElementHandler) {
		super(context, db, tableName, databaseName, databaseElementHandler);
	}

	@Override
	public Cursor wrapCursor(Cursor cursor) {
		return cursor;
	}
}
