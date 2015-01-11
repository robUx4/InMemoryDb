package org.gawst.asyncdb.source;

import android.database.Cursor;

import org.gawst.asyncdb.source.typed.TypedDatabaseElementHandler;

/**
 * Interface to handle the transformation from {@link E} elements to SQL query selections and from
 * {@link android.database.Cursor Cursor} to {@link E} elements.
 *
 * @param <E> Type of element read/write from the {@link DatabaseSource}.
 * @author Created by robUx4 on 08/01/2015.
 */
public interface DatabaseElementHandler<E> extends TypedDatabaseElementHandler<E, Cursor> {
}
