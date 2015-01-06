package org.gawst.asyncdb;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

/**
 * Created by Dell990MT on 06/01/2015.
 */
class AsyncQueryHandler<E, INSERT_ID> {

	private final AsynchronousDbHelper<E, INSERT_ID> asynchronousDbHelper;
	private final Handler mHandler = new Handler(Looper.getMainLooper());
	private final DatabaseSource<INSERT_ID> dataSource;

	public AsyncQueryHandler(AsynchronousDbHelper<E, INSERT_ID> asynchronousDbHelper, DataSource<E, INSERT_ID> dataSource) {
		if (!(dataSource instanceof DatabaseSource)) {
			throw new IllegalArgumentException("Invalid dataSource for AsyncQueryHandler "+dataSource);
		}
		this.asynchronousDbHelper = asynchronousDbHelper;
		this.dataSource = (DatabaseSource<INSERT_ID>) dataSource;
	}

	/**
	 * This method begins an asynchronous query. When the query is done
	 * {@link #onQueryComplete} is called.
	 *
	 * @param token A token passed into {@link #onQueryComplete} to identify
	 *  the query.
	 * @param cookie An object that gets passed into {@link #onQueryComplete}
	 * @param uri The URI, using the content:// scheme, for the content to
	 *         retrieve.
	 * @param projection A list of which columns to return. Passing null will
	 *         return all columns, which is discouraged to prevent reading data
	 *         from storage that isn't going to be used.
	 * @param selection A filter declaring which rows to return, formatted as an
	 *         SQL WHERE clause (excluding the WHERE itself). Passing null will
	 *         return all rows for the given URI.
	 * @param selectionArgs You may include ?s in selection, which will be
	 *         replaced by the values from selectionArgs, in the order that they
	 *         appear in the selection. The values will be bound as Strings.
	 * @param orderBy How to order the rows, formatted as an SQL ORDER BY
	 *         clause (excluding the ORDER BY itself). Passing null will use the
	 *         default sort order, which may be unordered.
	 */
	public void startQuery(final int token, final Object cookie, Uri uri,
	                       final String[] projection, final String selection, final String[] selectionArgs,
	                       final String orderBy) {
		asynchronousDbHelper.scheduleCustomOperation(new AsynchronousDbOperation<E, INSERT_ID>() {
			@Override
			public void runInMemoryDbOperation(AsynchronousDbHelper<E, INSERT_ID> db) {
				Cursor cursor1;
				try {
					cursor1 = dataSource.query(projection, selection, selectionArgs, null, null, orderBy, null);
				} catch (Exception e) {
					cursor1 = null;
				}

				final Cursor cursor = cursor1;
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						onQueryComplete(token, cookie, cursor);
					}
				});
			}
		});
	}

	/**
	 * This method begins an asynchronous insert. When the insert operation is
	 * done {@link #onInsertComplete} is called.
	 *
	 * @param token A token passed into {@link #onInsertComplete} to identify
	 *  the insert operation.
	 * @param cookie An object that gets passed into {@link #onInsertComplete}
	 * @param uri the Uri passed to the insert operation.
	 * @param initialValues the ContentValues parameter passed to the insert operation.
	 */
	public final void startInsert(final int token, final Object cookie, Uri uri,
	                              final ContentValues initialValues) {
		asynchronousDbHelper.scheduleCustomOperation(new AsynchronousDbOperation<E, INSERT_ID>() {
			@Override
			public void runInMemoryDbOperation(AsynchronousDbHelper<E, INSERT_ID> db) {
				INSERT_ID cursor1;
				try {
					cursor1 = dataSource.insert(initialValues);
				} catch (Exception e) {
					cursor1 = null;
				}

				final INSERT_ID cursor = cursor1;
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						onInsertComplete(token, cookie, cursor);
					}
				});
			}
		});
	}

	/**
	 * This method begins an asynchronous update. When the update operation is
	 * done {@link #onUpdateComplete} is called.
	 *
	 * @param token A token passed into {@link #onUpdateComplete} to identify
	 *  the update operation.
	 * @param cookie An object that gets passed into {@link #onUpdateComplete}
	 * @param uri the Uri passed to the update operation.
	 * @param values the ContentValues parameter passed to the update operation.
	 */
	public final void startUpdate(final int token, final Object cookie, INSERT_ID uri,
	                              final ContentValues values, final String selection, final String[] selectionArgs) {
		asynchronousDbHelper.scheduleCustomOperation(new AsynchronousDbOperation<E, INSERT_ID>() {
			@Override
			public void runInMemoryDbOperation(AsynchronousDbHelper<E, INSERT_ID> db) {
				int cursor1;
				try {
					cursor1 = dataSource.update(selection, selectionArgs, values);
				} catch (Exception e) {
					cursor1 = 0;
				}

				final int cursor = cursor1;
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						onUpdateComplete(token, cookie, cursor);
					}
				});
			}
		});
	}

	/**
	 * This method begins an asynchronous delete. When the delete operation is
	 * done {@link #onDeleteComplete} is called.
	 *
	 * @param token A token passed into {@link #onDeleteComplete} to identify
	 *  the delete operation.
	 * @param cookie An object that gets passed into {@link #onDeleteComplete}
	 * @param uri the Uri passed to the delete operation.
	 * @param selection the where clause.
	 */
	public final void startDelete(final int token, final Object cookie, INSERT_ID uri,
	                              final String selection, final String[] selectionArgs) {
		asynchronousDbHelper.scheduleCustomOperation(new AsynchronousDbOperation<E, INSERT_ID>() {
			@Override
			public void runInMemoryDbOperation(AsynchronousDbHelper<E, INSERT_ID> db) {
				int cursor1;
				try {
					cursor1 = dataSource.delete(selection, selectionArgs);
				} catch (Exception e) {
					cursor1 = 0;
				}

				final int cursor = cursor1;
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						onDeleteComplete(token, cookie, cursor);
					}
				});
			}
		});
	}

	/**
	 * Called when an asynchronous query is completed.
	 *
	 * @param token the token to identify the query, passed in from
	 *            {@link #startQuery}.
	 * @param cookie the cookie object passed in from {@link #startQuery}.
	 * @param cursor The cursor holding the results from the query.
	 */
	protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
		// Empty
	}

	/**
	 * Called when an asynchronous insert is completed.
	 *
	 * @param token the token to identify the query, passed in from
	 *        {@link #startInsert}.
	 * @param cookie the cookie object that's passed in from
	 *        {@link #startInsert}.
	 * @param uri the uri returned from the insert operation.
	 */
	protected void onInsertComplete(int token, Object cookie, INSERT_ID uri) {
		// Empty
	}

	/**
	 * Called when an asynchronous update is completed.
	 *
	 * @param token the token to identify the query, passed in from
	 *        {@link #startUpdate}.
	 * @param cookie the cookie object that's passed in from
	 *        {@link #startUpdate}.
	 * @param result the result returned from the update operation
	 */
	protected void onUpdateComplete(int token, Object cookie, int result) {
		// Empty
	}

	/**
	 * Called when an asynchronous delete is completed.
	 *
	 * @param token the token to identify the query, passed in from
	 *        {@link #startDelete}.
	 * @param cookie the cookie object that's passed in from
	 *        {@link #startDelete}.
	 * @param result the result returned from the delete operation
	 */
	protected void onDeleteComplete(int token, Object cookie, int result) {
		// Empty
	}
}
