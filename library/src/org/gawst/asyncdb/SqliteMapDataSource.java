package org.gawst.asyncdb;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Created by robUx4 on 12/31/2014.
 */
public class SqliteMapDataSource<K, V> implements MapDataSource<K, V, Long>, DatabaseSource<Long> {
	private final SqliteDataSource<MapEntry<K, V>> source;

	public SqliteMapDataSource(@NonNull Context context, @NonNull SQLiteOpenHelper db, @NonNull final String tableName, @NonNull String databaseName, @NonNull final MapCursorSourceHandler<K, V> cursorSourceHandler) {
		if (cursorSourceHandler ==null) throw new NullPointerException("null MapCursorSourceHandler in "+this);
		this.source = new SqliteDataSource<MapEntry<K, V>>(context, db, tableName, databaseName, new CursorDataSource.CursorSourceHandler<MapEntry<K, V>>() {
			@Override
			public String getItemSelectClause(@Nullable MapEntry<K, V> itemToSelect) {
				return cursorSourceHandler.getKeySelectClause(null == itemToSelect ? null : itemToSelect.getKey());
			}

			@Override
			public String[] getItemSelectArgs(@NonNull MapEntry<K, V> itemToSelect) {
				return cursorSourceHandler.getKeySelectArgs(itemToSelect.getKey());
			}

			@Override
			public MapEntry<K, V> cursorToItem(Cursor cursor) throws InvalidDbEntry {
				K key = cursorSourceHandler.cursorToKey(cursor);
				return new MapEntry<K, V>(key, cursorSourceHandler.cursorToValue(cursor));
			}
		});
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
	public Long insert(ContentValues element) throws RuntimeException {
		return source.insert(element);
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
	public int update(String selection, String[] selectionArgs, ContentValues updateValues) {
		return source.update(selection, selectionArgs, updateValues);
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
