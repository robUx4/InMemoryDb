package org.gawst.asyncdb.source.typed;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;

import org.gawst.asyncdb.DataSource;
import org.gawst.asyncdb.InvalidDbEntry;
import org.gawst.asyncdb.InvalidEntry;

/**
 * Abstract helper {@link org.gawst.asyncdb.DataSource} class reading using a {@link android.database.Cursor}.
 *
 * @param <E>      Type of the elements read from the {@code CURSOR}
 * @param <CURSOR> Wrapper around the raw {@code Cursor} read
 * @author Created by robUx4 on 11/01/2015.
 * @see TypedContentProviderDataSource
 * @see TypedSqliteDataSource
 * @see TypedSqliteMapDataSource
 */
public abstract class TypedCursorDataSource<E, INSERT_ID, DATABASE_ID, CURSOR extends Cursor> implements DataSource<E, INSERT_ID>, TypedDatabaseSource<INSERT_ID, DATABASE_ID, CURSOR> {

	private final TypedDatabaseElementHandler<E, CURSOR> databaseElementHandler;

	/**
	 * Constructor.
	 *
	 * @param databaseElementHandler Handler to transform {@link E} elements to queries and {@code Cursor} to {@link E} elements.
	 */
	public TypedCursorDataSource(@NonNull TypedDatabaseElementHandler<E, CURSOR> databaseElementHandler) {
		if (databaseElementHandler == null)
			throw new NullPointerException("null CursorSourceHandler in " + this);
		this.databaseElementHandler = databaseElementHandler;
	}

	@Override
	public final boolean update(E itemToUpdate, ContentValues updateValues) {
		return update(updateValues, databaseElementHandler.getItemSelectClause(itemToUpdate), databaseElementHandler.getItemSelectArgs(itemToUpdate)) != 0;
	}

	@Override
	public final boolean delete(E itemToDelete) {
		return delete(databaseElementHandler.getItemSelectClause(itemToDelete), databaseElementHandler.getItemSelectArgs(itemToDelete)) != 0;
	}

	@Override
	public final void queryAll(BatchReadingCallback<E> readingCallback) {
		CURSOR c = wrapCursor(query(null, null, null, null, null, null, null));
		if (c != null)
			try {
				if (c.moveToFirst()) {
					readingCallback.startLoadingAllItems(c.getCount());
					do {
						try {
							E item = databaseElementHandler.cursorToItem(c);
							readingCallback.addItemInMemory(item);
						} catch (InvalidDbEntry e) {
							readingCallback.removeInvalidEntry(e.getInvalidEntry());
						}
					} while (c.moveToNext());
				}
			} finally {
				c.close();
			}
	}

	@Override
	public boolean deleteInvalidEntry(InvalidEntry invalidEntry) {
		return delete(databaseElementHandler.getItemSelectClause(null), invalidEntry.getSelectArgs()) != 0;
	}
}
