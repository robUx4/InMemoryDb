package org.gawst.asyncdb.source.typed;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.gawst.asyncdb.InvalidDbEntry;

/**
 * Interface to handle the transformation from {@link E} elements to SQL query selections and from
 * {@link android.database.Cursor Cursor} to {@link E} elements.
 *
 * @param <E>      Type of element read/write from the {@link org.gawst.asyncdb.source.DatabaseSource}.
 * @param <CURSOR> Wrapper around the raw {@code Cursor} read
 * @author Created by robUx4 on 11/01/2015.
 */
public interface TypedDatabaseElementHandler<E, CURSOR extends Cursor> {
	/**
	 * The where clause that should be used to update/delete the item.
	 * <p> see {@link #getItemSelectArgs(Object)}
	 *
	 * @param itemToSelect the item about to be selected in the database
	 * @return a {@link String} for the whereClause in {@link android.database.sqlite.SQLiteDatabase#update(String, android.content.ContentValues, String, String[]) SQLiteDatabase.update()}
	 * or {@link android.database.sqlite.SQLiteDatabase#delete(String, String, String[]) SQLiteDatabase.delete()}
	 */
	@NonNull
	String getItemSelectClause(@Nullable E itemToSelect);

	/**
	 * The where arguments that should be used to update/delete the item.
	 * <p> see {@link #getItemSelectClause(Object)}
	 *
	 * @param itemToSelect the item about to be selected in the database
	 * @return a {@link String} array for the whereArgs in {@link android.database.sqlite.SQLiteDatabase#update(String, android.content.ContentValues, String, String[]) SQLiteDatabase.update()}
	 * or {@link android.database.sqlite.SQLiteDatabase#delete(String, String, String[]) SQLiteDatabase.delete()}
	 */
	@NonNull
	String[] getItemSelectArgs(@NonNull E itemToSelect);

	/**
	 * Use the data in the {@link android.database.Cursor} to create a valid item
	 *
	 * @param cursor the Cursor to use
	 * @return The element corresponding to the current Cursor position
	 * @throws org.gawst.asyncdb.InvalidDbEntry if the Cursor data cannot be used
	 */
	@NonNull
	E cursorToItem(@NonNull CURSOR cursor) throws InvalidDbEntry;
}
