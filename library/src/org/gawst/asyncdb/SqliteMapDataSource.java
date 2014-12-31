package org.gawst.asyncdb;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;

/**
 * Created by robUx4 on 12/31/2014.
 */
public class SqliteMapDataSource<K, V> extends MapCursorDataSource<K, V> {
	private final SqliteDataSource<MapEntry<K, V>> source;

	public SqliteMapDataSource(@NonNull Context context, @NonNull SQLiteOpenHelper db, @NonNull String tableName, @NonNull String databaseName, @NonNull final MapCursorSourceHandler<K, V> cursorSourceHandler) {
		super(cursorSourceHandler);
		this.source = new SqliteDataSource<MapEntry<K, V>>(context, db, tableName, databaseName, new CursorDataSource.CursorSourceHandler<MapEntry<K, V>>() {
			@Override
			public String getItemSelectClause(MapEntry<K, V> itemToSelect) {
				return cursorSourceHandler.getItemSelectClause(itemToSelect.getKey());
			}

			@Override
			public String[] getItemSelectArgs(MapEntry<K, V> itemToSelect) {
				return cursorSourceHandler.getItemSelectArgs(itemToSelect.getKey());
			}

			@Override
			public MapEntry<K, V> cursorToItem(Cursor cursor) {
				return new MapEntry<K, V>(cursorSourceHandler.cursorToKey(cursor), cursorSourceHandler.cursorToValue(cursor));
			}
		});
	}
}
