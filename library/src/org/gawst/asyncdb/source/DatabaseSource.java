package org.gawst.asyncdb.source;

import android.database.Cursor;

import org.gawst.asyncdb.source.typed.TypedDatabaseSource;

/**
 * Interface for classes that read/write data in SQL queries
 *
 * @param <INSERT_ID>   Type of element returned by {@link #insert(android.content.ContentValues) insert()}
 * @param <DATABASE_ID> Type of the ID needed to use {@link org.gawst.asyncdb.AsyncDatabaseHandler}
 * @author Created by robUx4 on 06/01/2015.
 */
public interface DatabaseSource<INSERT_ID, DATABASE_ID> extends TypedDatabaseSource<INSERT_ID, DATABASE_ID, Cursor> {
}
