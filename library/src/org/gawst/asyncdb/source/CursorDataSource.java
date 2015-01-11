package org.gawst.asyncdb.source;

import android.database.Cursor;
import android.support.annotation.NonNull;

import org.gawst.asyncdb.source.typed.TypedCursorDataSource;

/**
 * Abstract helper {@link org.gawst.asyncdb.DataSource} class reading using a {@link android.database.Cursor}.
 *
 * @param <E>           Type of element read/write from the {@link org.gawst.asyncdb.DataSource}.
 * @param <INSERT_ID>   Type of elements returned by {@link org.gawst.asyncdb.DataSource#insert(android.content.ContentValues)}
 * @param <DATABASE_ID> Type of the ID needed to use {@link org.gawst.asyncdb.AsyncDatabaseHandler}
 * @author Created by robUx4 on 12/31/2014.
 * @see org.gawst.asyncdb.source.ContentProviderDataSource
 * @see SqliteDataSource
 * @see SqliteMapDataSource
 */
public abstract class CursorDataSource<E, INSERT_ID, DATABASE_ID> extends TypedCursorDataSource<E, INSERT_ID, DATABASE_ID, Cursor> {
	/**
	 * Constructor.
	 *
	 * @param databaseElementHandler Handler to transform {@link E} elements to queries and {@code Cursor} to {@link E} elements.
	 */
	public CursorDataSource(@NonNull DatabaseElementHandler<E> databaseElementHandler) {
		super(databaseElementHandler);
	}
}
