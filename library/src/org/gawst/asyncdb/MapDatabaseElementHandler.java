package org.gawst.asyncdb;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Interface to handle the transformation from a Key ({@link K}) / Value ({@link V}) pair to SQL query selections and from
 * {@link android.database.Cursor Cursor} to Key ({@link K}) / Value ({@link V}) pairs.
 *
 * @param <K> Type of the Key read/write from the {@link org.gawst.asyncdb.source.DatabaseSource} to/from the {@code Map} storage.
 * @param <V> Type of the Value read/write from the {@link org.gawst.asyncdb.source.DatabaseSource} to/from the {@code Map} storage.
 * @author Created by robUx4 on 06/01/2015.
 * @see org.gawst.asyncdb.source.DatabaseElementHandler
 */
public interface MapDatabaseElementHandler<K, V> {
	/**
	 * The where clause that should be used to update/delete the item.
	 *
	 * @param itemToSelect the item about to be selected in the database
	 * @return a {@link String} for the whereClause in {@link android.database.sqlite.SQLiteDatabase#update(String, android.content.ContentValues, String, String[])} or {@link android.database.sqlite.SQLiteDatabase#delete(String, String, String[])}
	 * @see #getKeySelectArgs(Object)
	 */
	@NonNull
	String getKeySelectClause(@Nullable K itemToSelect);

	/**
	 * The where arguments that should be used to update/delete the item.
	 *
	 * @param itemToSelect the item about to be selected in the database
	 * @return a {@link String} array for the whereArgs in {@link android.database.sqlite.SQLiteDatabase#update(String, android.content.ContentValues, String, String[])} or {@link android.database.sqlite.SQLiteDatabase#delete(String, String, String[])}
	 * @see #getKeySelectClause(Object)
	 */
	@NonNull
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
