package org.gawst.asyncdb.source;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;

import org.gawst.asyncdb.MapDatabaseElementHandler;
import org.gawst.asyncdb.source.typed.TypedSqliteMapDataSource;

/**
 * A {@link org.gawst.asyncdb.DataSource} backed by a {@link android.database.sqlite.SQLiteOpenHelper} storage that uses a
 * Key/Value based {@link DatabaseElementHandler} to read/write elements from the database
 *
 * @author Created by robUx4 on 12/31/2014.
 */
public class SqliteMapDataSource<K, V> extends TypedSqliteMapDataSource<K, V, Cursor> implements DatabaseSource<Long, Void> {
	public SqliteMapDataSource(@NonNull Context context, @NonNull SQLiteOpenHelper db, @NonNull String tableName, @NonNull MapDatabaseElementHandler<K, V> databaseElementHandler) {
		super(context, db, tableName, databaseElementHandler);
	}

	public SqliteMapDataSource(@NonNull Context context, @NonNull SQLiteOpenHelper db, @NonNull String tableName, @NonNull String databaseName, @NonNull MapDatabaseElementHandler<K, V> databaseElementHandler) {
		super(context, db, tableName, databaseName, databaseElementHandler);
	}

	@Override
	public Cursor wrapCursor(Cursor cursor) {
		return cursor;
	}
}
