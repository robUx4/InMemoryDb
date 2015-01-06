package org.gawst.asyncdb;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;

/**
 * Created by robUx4 on 12/31/2014.
 */
public class SqliteMapDataSource<K, V> extends MapCursorDataSource<K, V> {
	public final SqliteDataSource<MapEntry<K, V>> source;

	public SqliteMapDataSource(@NonNull Context context, @NonNull SQLiteOpenHelper db, @NonNull final String tableName, @NonNull String databaseName, @NonNull final MapCursorSourceHandler<K, V> cursorSourceHandler) {
		super(cursorSourceHandler);
		this.source = new SqliteDataSource<MapEntry<K, V>>(context, db, tableName, databaseName, new CursorDataSource.CursorSourceHandler<MapEntry<K, V>>() {
			@Override
			public String getItemSelectClause(MapEntry<K, V> itemToSelect) {
				return cursorSourceHandler.getKeySelectClause(itemToSelect.getKey());
			}

			@Override
			public String[] getItemSelectArgs(MapEntry<K, V> itemToSelect) {
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
	public Object insert(ContentValues element) throws RuntimeException {
		return source.insert(element);
	}

	@Override
	public boolean deleteInvalidEntry(InvalidEntry invalidEntry) {
		return source.deleteInvalidEntry(invalidEntry);
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
