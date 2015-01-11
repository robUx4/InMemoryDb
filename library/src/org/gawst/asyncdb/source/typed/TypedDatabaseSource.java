package org.gawst.asyncdb.source.typed;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Interface for classes that read/write data in SQL queries
 *
 * @param <INSERT_ID>   Type of element returned by {@link #insert(android.content.ContentValues) insert()}
 * @param <DATABASE_ID> Type of the ID needed to use {@link org.gawst.asyncdb.AsyncDatabaseHandler}
 * @param <CURSOR> Wrapper around the raw {@code Cursor} read
 * @author Created by robUx4 on 11/01/2015.
 */
public interface TypedDatabaseSource<INSERT_ID, DATABASE_ID, CURSOR extends Cursor> {
	/**
	 * Query the {@link org.gawst.asyncdb.source.DatabaseSource} with an SQL-like syntax.
	 *
	 * @param columns       A list of which columns to return. Passing null will
	 *                      return all columns, which is inefficient.
	 * @param selection     A filter declaring which rows to return, formatted as an
	 *                      SQL WHERE clause (excluding the WHERE itself). Passing null will
	 *                      return all rows.
	 * @param selectionArgs You may include ?s in selection, which will be
	 *                      replaced by the values from selectionArgs, in the order that they
	 *                      appear in the selection. The values will be bound as Strings.
	 * @param groupBy       A filter declaring how to group rows, formatted as an SQL
	 *                      GROUP BY clause (excluding the GROUP BY itself). Passing null
	 *                      will cause the rows to not be grouped.
	 * @param having        A filter declare which row groups to include in the cursor,
	 *                      if row grouping is being used, formatted as an SQL HAVING
	 *                      clause (excluding the HAVING itself). Passing null will cause
	 *                      all row groups to be included, and is required when row
	 *                      grouping is not being used.
	 * @param orderBy       How to order the rows, formatted as an SQL ORDER BY
	 *                      clause (excluding the ORDER BY itself). Passing null will use the
	 *                      default sort order, which may be unordered.
	 * @param limit         Limits the number of rows returned by the query,
	 *                      formatted as LIMIT clause. Passing null denotes no LIMIT clause.
	 * @return a {@link android.database.Cursor} containing the result of the selection.
	 */
	CURSOR query(@Nullable String[] columns, @Nullable String selection, @Nullable String[] selectionArgs,
	             @Nullable String groupBy, @Nullable String having, @Nullable String orderBy, @Nullable String limit);

	/**
	 * Insert a raw in the database.
	 *
	 * @param values The initial values for the newly inserted row. The key is the column name for
	 *               the field. Passing an empty ContentValues will create an empty row.
	 * @return a {@link INSERT_ID} specific to the {@link org.gawst.asyncdb.source.DatabaseSource}
	 * @throws RuntimeException
	 */
	INSERT_ID insert(@NonNull ContentValues values) throws RuntimeException;

	/**
	 * Update row(s) in the {@link org.gawst.asyncdb.source.DatabaseSource}.
	 *
	 * @param updateValues  The new field values. The key is the column name for the field.	A null value will remove an existing field value.
	 * @param selection     A filter to apply to rows before updating, formatted as an SQL WHERE clause	(excluding the WHERE itself).
	 * @param selectionArgs You may include ?s in the selection clause, which
	 *                      will be replaced by the values from whereArgs. The values
	 *                      will be bound as Strings.
	 * @return the number of rows updated.
	 */
	int update(@NonNull ContentValues updateValues, @Nullable String selection, @Nullable String[] selectionArgs);

	/**
	 * Delete row(s) from the {@link org.gawst.asyncdb.source.DatabaseSource}.
	 *
	 * @param selection     A filter to apply to rows before deleting, formatted as an SQL WHERE clause	(excluding the WHERE itself).
	 * @param selectionArgs You may include ?s in the selection clause, which
	 *                      will be replaced by the values from whereArgs. The values
	 *                      will be bound as Strings.
	 * @return
	 */
	int delete(@Nullable String selection, @Nullable String[] selectionArgs);

	/**
	 * @return The database ID needed to use {@link org.gawst.asyncdb.AsyncDatabaseHandler}
	 */
	DATABASE_ID getDatabaseId();

	/**
	 * @return the raw {@link android.database.Cursor} read from the database turned into a more friendly {@link CURSOR}.
	 */
	CURSOR wrapCursor(Cursor cursor);
}
