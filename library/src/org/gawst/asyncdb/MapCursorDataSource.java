package org.gawst.asyncdb;

import android.content.ContentValues;
import android.database.Cursor;

/**
 * Created by robUx4 on 12/31/2014.
 */
public class MapCursorDataSource<K, V> implements MapDataSource<K, V> {

	public interface MapCursorSourceHandler<K, V> {
		/**
		 * The where clause that should be used to update/delete the item.
		 * <p> see {@link #getItemSelectArgs(Object)}
		 *
		 * @param itemToSelect the item about to be selected in the database
		 * @return a {@link String} for the whereClause in {@link android.database.sqlite.SQLiteDatabase#update(String, android.content.ContentValues, String, String[])} or {@link android.database.sqlite.SQLiteDatabase#delete(String, String, String[])}
		 */
		String getItemSelectClause(K itemToSelect);

		/**
		 * The where arguments that should be used to update/delete the item.
		 * <p> see {@link #getItemSelectClause(Object)}
		 *
		 * @param itemToSelect the item about to be selected in the database
		 * @return a {@link String} array for the whereArgs in {@link android.database.sqlite.SQLiteDatabase#update(String, android.content.ContentValues, String, String[])} or {@link android.database.sqlite.SQLiteDatabase#delete(String, String, String[])}
		 */
		String[] getItemSelectArgs(K itemToSelect);

		/**
		 * Use the data in the {@link android.database.Cursor} to create a valid item
		 *
		 * @param cursor the Cursor to use
		 */
		K cursorToKey(Cursor cursor);

		/**
		 * Use the data in the {@link android.database.Cursor} to create a valid item
		 *
		 * @param cursor the Cursor to use
		 */
		V cursorToValue(Cursor cursor);
	}

	private final MapCursorSourceHandler<K, V> source;

	public MapCursorDataSource(MapCursorSourceHandler<K, V> source) {
		this.source = source;
	}

	@Override
	public final boolean delete(MapEntry<K, V> itemToDelete) {
		return deleteByKey(itemToDelete.getKey());
	}

	@Override
	public boolean update(MapEntry<K, V> itemToUpdate, ContentValues updateValues) {
		return updateByKey(itemToUpdate.getKey(), updateValues);
	}

	@Override
	public boolean updateByKey(K key, ContentValues updateValues) {
		return false;
	}

	@Override
	public boolean deleteByKey(K key) {
		return false;
	}

	@Override
	public void queryAll(BatchReadingCallback<MapEntry<K, V>> readingCallback) {

	}

	@Override
	public int clearAllData() {
		return 0;
	}

	@Override
	public Object insert(ContentValues element) throws RuntimeException {
		return null;
	}
\

	@Override
	public void eraseSource() {

	}
}
