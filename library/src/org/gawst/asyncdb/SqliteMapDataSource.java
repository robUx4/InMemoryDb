package org.gawst.asyncdb;

import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * A {@link org.gawst.asyncdb.DataSource} backed by a {@link android.database.sqlite.SQLiteOpenHelper} storage that uses a
 * Key/Value based {@link org.gawst.asyncdb.DatabaseElementHandler} to read/write elements from the database
 *
 * @author Created by robUx4 on 12/31/2014.
 */
public class SqliteMapDataSource<K, V> implements MapDataSource<K, V, Long>, DatabaseSource<Long, Void> {
	private final SqliteDataSource<MapEntry<K, V>> source;

	/**
	 * Constructor. (API v14 minimum)
	 *
	 * @param context                Context used to erase the database file in case it's corrupted.
	 * @param db                     The SQL database used to read/write data.
	 * @param tableName              Name of the SQL table that contains the elements to read.
	 * @param databaseElementHandler Handler to transform {@code Cursor} into ({@link K},{@link V}) pairs or ({@link K},{@link V}) pairs to selections.
	 */
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	public SqliteMapDataSource(@NonNull Context context, @NonNull SQLiteOpenHelper db, @NonNull final String tableName, @NonNull final MapDatabaseElementHandler<K, V> databaseElementHandler) {
		this(context, db, tableName, db.getDatabaseName(), databaseElementHandler);
	}

	/**
	 * Constructor.
	 *
	 * @param context                Context used to erase the database file in case it's corrupted.
	 * @param db                     The SQL database used to read/write data.
	 * @param tableName              Name of the SQL table that contains the elements to read.
	 * @param databaseName           Name of the database file on disk, in case it's corrupted and needs to be erased.
	 * @param databaseElementHandler Handler to transform {@code Cursor} into ({@link K},{@link V}) pairs or ({@link K},{@link V}) pairs to selections.
	 */
	public SqliteMapDataSource(@NonNull Context context, @NonNull SQLiteOpenHelper db, @NonNull final String tableName, @NonNull String databaseName, @NonNull final MapDatabaseElementHandler<K, V> databaseElementHandler) {
		if (databaseElementHandler == null) throw new NullPointerException("null MapCursorSourceHandler in " + this);
		this.source = new SqliteDataSource<MapEntry<K, V>>(context, db, tableName, databaseName, new DatabaseElementHandler<MapEntry<K, V>>() {
			@Override
			public String getItemSelectClause(@Nullable MapEntry<K, V> itemToSelect) {
				return databaseElementHandler.getKeySelectClause(null == itemToSelect ? null : itemToSelect.getKey());
			}

			@Override
			public String[] getItemSelectArgs(@NonNull MapEntry<K, V> itemToSelect) {
				return databaseElementHandler.getKeySelectArgs(itemToSelect.getKey());
			}

			@NonNull
			@Override
			public MapEntry<K, V> cursorToItem(@NonNull Cursor cursor) throws InvalidDbEntry {
				K key = databaseElementHandler.cursorToKey(cursor);
				return new MapEntry<K, V>(key, databaseElementHandler.cursorToValue(cursor));
			}
		});
	}

	@Override
	public Void getDatabaseId() {
		return null;
	}

	@Override
	public boolean update(MapEntry<K, V> itemToUpdate, ContentValues updateValues) {
		return source.update(itemToUpdate, updateValues);
	}

	@Override
	public boolean delete(MapEntry<K, V> itemToDelete) {
		return source.delete(itemToDelete);
	}

	@Override
	public Long insert(@NonNull ContentValues values) throws RuntimeException {
		return source.insert(values);
	}

	@Override
	public boolean deleteInvalidEntry(InvalidEntry invalidEntry) {
		return source.deleteInvalidEntry(invalidEntry);
	}

	@Override
	public Cursor query(String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit) {
		return source.query(columns, selection, selectionArgs, groupBy, having, orderBy, limit);
	}

	@Override
	public int update(@NonNull ContentValues updateValues, String selection, String[] selectionArgs) {
		return source.update(updateValues, selection, selectionArgs);
	}

	@Override
	public int delete(String selection, String[] selectionArgs) {
		return source.delete(selection, selectionArgs);
	}

	@Override
	public final void queryAll(BatchReadingCallback<MapEntry<K, V>> readingCallback) {
		source.queryAll(readingCallback);
	}

	@Override
	public int clearAllData() {
		return source.clearAllData();
	}

	@Override
	public void eraseSource() {
		source.eraseSource();
	}
}
