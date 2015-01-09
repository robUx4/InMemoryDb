package org.gawst.asyncdb;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;

/**
 * Abstract helper {@link org.gawst.asyncdb.DataSource} class reading using a {@link android.database.Cursor}.
 *
 * @param <E>           Type of element read/write from the {@link org.gawst.asyncdb.DataSource}.
 * @param <INSERT_ID>   Type of elements returned by {@link org.gawst.asyncdb.DataSource#insert(android.content.ContentValues)}
 * @param <DATABASE_ID> Type of the ID needed to use {@link AsyncDatabaseHandler}
 * @author Created by robUx4 on 12/31/2014.
 * @see org.gawst.asyncdb.ContentProviderDataSource
 * @see org.gawst.asyncdb.SqliteDataSource
 * @see org.gawst.asyncdb.SqliteMapDataSource
 */
public abstract class CursorDataSource<E, INSERT_ID, DATABASE_ID> implements DataSource<E, INSERT_ID>, DatabaseSource<INSERT_ID, DATABASE_ID> {

	private final DatabaseElementHandler<E> databaseElementHandler;

	/**
	 * Constructor.
	 * @param databaseElementHandler Handler to transform {@link E} elements to queries and {@code Cursor} to {@link E} elements.
	 */
	public CursorDataSource(@NonNull DatabaseElementHandler<E> databaseElementHandler) {
		if (databaseElementHandler ==null) throw new NullPointerException("null CursorSourceHandler in "+this);
		this.databaseElementHandler = databaseElementHandler;
	}

	@Override
	public final boolean update(E itemToUpdate, ContentValues updateValues) {
		return update(updateValues, databaseElementHandler.getItemSelectClause(itemToUpdate), databaseElementHandler.getItemSelectArgs(itemToUpdate))!=0;
	}

	@Override
	public final boolean delete(E itemToDelete) {
		return delete(databaseElementHandler.getItemSelectClause(itemToDelete), databaseElementHandler.getItemSelectArgs(itemToDelete))!=0;
	}

	@Override
	public final void queryAll(BatchReadingCallback<E> readingCallback) {
		Cursor c = query(null, null, null, null, null, null, null);
		if (c!=null)
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
		return delete(databaseElementHandler.getItemSelectClause(null), invalidEntry.getSelectArgs())!=0;
	}
}
