package org.gawst.asyncdb;

import android.database.Cursor;
import android.support.annotation.NonNull;

/**
 * Created by robUx4 on 12/31/2014.
 */
public abstract class CursorDataSource<E> implements DataSource<E> {

	public interface CursorSourceHandler<E> {
		/**
		 * The where clause that should be used to update/delete the item.
		 * <p> see {@link #getItemSelectArgs(Object)}
		 *
		 * @param itemToSelect the item about to be selected in the database
		 * @return a {@link String} for the whereClause in {@link android.database.sqlite.SQLiteDatabase#update(String, android.content.ContentValues, String, String[])} or {@link android.database.sqlite.SQLiteDatabase#delete(String, String, String[])}
		 */
		String getItemSelectClause(E itemToSelect);

		/**
		 * The where arguments that should be used to update/delete the item.
		 * <p> see {@link #getItemSelectClause(Object)}
		 *
		 * @param itemToSelect the item about to be selected in the database
		 * @return a {@link String} array for the whereArgs in {@link android.database.sqlite.SQLiteDatabase#update(String, android.content.ContentValues, String, String[])} or {@link android.database.sqlite.SQLiteDatabase#delete(String, String, String[])}
		 */
		String[] getItemSelectArgs(E itemToSelect);

		/**
		 * Use the data in the {@link android.database.Cursor} to create a valid item
		 *
		 * @param cursor the Cursor to use
		 */
		E cursorToItem(Cursor cursor);
	}

	protected final CursorSourceHandler<E> cursorSourceHandler;

	public CursorDataSource(@NonNull CursorSourceHandler<E> cursorSourceHandler) {
		this.cursorSourceHandler = cursorSourceHandler;
	}

	protected abstract Cursor readAll();

	public final void queryAll(DataSource.BatchReadingCallback<E> readingCallback) {
		Cursor c = readAll();
		if (c!=null)
			try {
				if (c.moveToFirst()) {
					readingCallback.startLoadingAllItems(c.getCount());
					do {
						E item = cursorSourceHandler.cursorToItem(c);
						if (item != null)
							readingCallback.addItemInMemory(item);
					} while (c.moveToNext());
				}
			} finally {
				c.close();
			}
	}
}
