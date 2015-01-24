package org.gawst.asyncdb.source;

import org.gawst.asyncdb.source.typed.TypedMapDatabaseElementHandler;

import android.database.Cursor;

/**
 * Interface to handle the transformation from a Key ({@link K}) / Value ({@link V}) pair to SQL query selections and from
 * {@link android.database.Cursor Cursor} to Key ({@link K}) / Value ({@link V}) pairs.
 *
 * @param <K> Type of the Key read/write from the {@link org.gawst.asyncdb.source.DatabaseSource} to/from the {@code Map} storage.
 * @param <V> Type of the Value read/write from the {@link org.gawst.asyncdb.source.DatabaseSource} to/from the {@code Map} storage.
 * @author Created by robUx4 on 06/01/2015.
 * @see org.gawst.asyncdb.source.DatabaseElementHandler
 */
public interface MapDatabaseElementHandler<K, V> extends TypedMapDatabaseElementHandler<K, V, Cursor> {
}
