package org.gawst.asyncdb;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
* Created by robUx4 on 06/01/2015.
*/
public interface MapCursorSourceHandler<K, V> {
	/**
	 * The where clause that should be used to update/delete the item.
	 * <p> see {@link #getKeySelectArgs(Object)}
	 *
	 * @param itemToSelect the item about to be selected in the database
	 * @return a {@link String} for the whereClause in {@link android.database.sqlite.SQLiteDatabase#update(String, android.content.ContentValues, String, String[])} or {@link android.database.sqlite.SQLiteDatabase#delete(String, String, String[])}
	 */
	String getKeySelectClause(@Nullable K itemToSelect);

	/**
	 * The where arguments that should be used to update/delete the item.
	 * <p> see {@link #getKeySelectClause(Object)}
	 *
	 * @param itemToSelect the item about to be selected in the database
	 * @return a {@link String} array for the whereArgs in {@link android.database.sqlite.SQLiteDatabase#update(String, android.content.ContentValues, String, String[])} or {@link android.database.sqlite.SQLiteDatabase#delete(String, String, String[])}
	 */
	String[] getKeySelectArgs(@NonNull K itemToSelect);

	/**
	 * Use the data in the {@link android.database.Cursor} to create a valid item
	 *
	 * @param cursor the Cursor to use
	 */
	@NonNull
	K cursorToKey(@NonNull Cursor cursor) throws InvalidDbEntry;

	/**
	 * Use the data in the {@link android.database.Cursor} to create a valid item
	 *
	 * @param cursor the Cursor to use
	 */
	@NonNull
	V cursorToValue(@NonNull Cursor cursor);
}
