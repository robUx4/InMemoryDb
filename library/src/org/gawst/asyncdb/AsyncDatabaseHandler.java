package org.gawst.asyncdb;

import org.gawst.asyncdb.source.DatabaseSource;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;

/**
 * Class similar to Android's {@link android.content.AsyncQueryHandler AsyncQueryHandler} to work with an
 * {@link org.gawst.asyncdb.AsynchronousDbHelper} instead of a ContentProvider source
 *
 * @see org.gawst.asyncdb.AsynchronousDbHelper.AsyncHandler AsyncHandler for a ready to use version
 */
public class AsyncDatabaseHandler<INSERT_ID, DATABASE_ID> {

	private final AsynchronousDbHelper<?, INSERT_ID> asynchronousDbHelper;
	private static final Handler mHandler = new Handler(Looper.getMainLooper());
	protected final DatabaseSource<INSERT_ID, DATABASE_ID> dataSource;

	/**
	 * Constructor.
	 *
	 * @param asynchronousDbHelper The {@link org.gawst.asyncdb.AsynchronousDbHelper} database to work with.
	 * @param dataSource           The {@link org.gawst.asyncdb.source.DatabaseSource} source used by the {@code asynchronousDbHelper}.
	 */
	public AsyncDatabaseHandler(AsynchronousDbHelper<?, INSERT_ID> asynchronousDbHelper, DatabaseSource<INSERT_ID, DATABASE_ID> dataSource) {
		this.asynchronousDbHelper = asynchronousDbHelper;
		this.dataSource = dataSource;
	}

	private void checkDatabaseId(DATABASE_ID databaseId) {
		if (databaseId != null && databaseId != dataSource.getDatabaseId())
			throw new IllegalArgumentException("wrong database id " + databaseId + " expected " + dataSource.getDatabaseId() + " try startRunnable()");
	}

	public AsynchronousDbHelper<?, INSERT_ID> getAsynchronousDbHelper() {
		return asynchronousDbHelper;
	}

	public DatabaseSource<INSERT_ID, DATABASE_ID> getDataSource() {
		return dataSource;
	}

	/**
	 * This method begins an asynchronous query. When the query is done
	 * {@link #onQueryComplete} is called.
	 *
	 * @param token         A token passed into {@link #onQueryComplete} to identify
	 *                      the query.
	 * @param cookie        An object that gets passed into {@link #onQueryComplete}
	 * @param projection    A list of which columns to return. Passing null will
	 *                      return all columns, which is discouraged to prevent reading data
	 *                      from storage that isn't going to be used.
	 * @param selection     A filter declaring which rows to return, formatted as an
	 *                      SQL WHERE clause (excluding the WHERE itself). Passing null will
	 *                      return all rows for the given URI.
	 * @param selectionArgs You may include ?s in selection, which will be
	 *                      replaced by the values from selectionArgs, in the order that they
	 *                      appear in the selection. The values will be bound as Strings.
	 * @param orderBy       How to order the rows, formatted as an SQL ORDER BY
	 */
	public void startQuery(final int token, final Object cookie,
	                       final String[] projection, final String selection, final String[] selectionArgs,
	                       final String orderBy) {
		startQuery(token, cookie, projection, selection, selectionArgs, orderBy, null);
	}

	/**
	 * <b>Deprecated, the {@code uri} field will be ignored, the one from {@link org.gawst.asyncdb.source.DatabaseSource} will be used.</b>
	 * <p/>
	 * This method begins an asynchronous query. When the query is done
	 * {@link #onQueryComplete} is called.
	 *
	 * @param token         A token passed into {@link #onQueryComplete} to identify
	 *                      the query.
	 * @param cookie        An object that gets passed into {@link #onQueryComplete}
	 * @param databaseId
	 * @param projection    A list of which columns to return. Passing null will
	 *                      return all columns, which is discouraged to prevent reading data
	 *                      from storage that isn't going to be used.
	 * @param selection     A filter declaring which rows to return, formatted as an
	 *                      SQL WHERE clause (excluding the WHERE itself). Passing null will
	 *                      return all rows for the given URI.
	 * @param selectionArgs You may include ?s in selection, which will be
	 *                      replaced by the values from selectionArgs, in the order that they
	 *                      appear in the selection. The values will be bound as Strings.
	 * @param orderBy       How to order the rows, formatted as an SQL ORDER BY
	 * @see #startQuery(int, Object, String[], String, String[], String)
	 */
	@Deprecated
	public void startQuery(final int token, final Object cookie, DATABASE_ID databaseId,
	                       final String[] projection, final String selection, final String[] selectionArgs,
	                       final String orderBy) {
		startQuery(token, cookie, databaseId, projection, selection, selectionArgs, orderBy, null);
	}

	/**
	 * This method begins an asynchronous query. When the query is done
	 * {@link #onQueryComplete} is called.
	 *
	 * @param token         A token passed into {@link #onQueryComplete} to identify
	 *                      the query.
	 * @param cookie        An object that gets passed into {@link #onQueryComplete}
	 * @param projection    A list of which columns to return. Passing null will
	 *                      return all columns, which is discouraged to prevent reading data
	 *                      from storage that isn't going to be used.
	 * @param selection     A filter declaring which rows to return, formatted as an
	 *                      SQL WHERE clause (excluding the WHERE itself). Passing null will
	 *                      return all rows for the given URI.
	 * @param selectionArgs You may include ?s in selection, which will be
	 *                      replaced by the values from selectionArgs, in the order that they
	 *                      appear in the selection. The values will be bound as Strings.
	 * @param orderBy       How to order the rows, formatted as an SQL ORDER BY
	 * @param limit         Limits the number of rows returned by the query,
	 *                      formatted as LIMIT clause. Passing null denotes no LIMIT clause.
	 */
	public void startQuery(final int token, final Object cookie,
	                       final String[] projection, final String selection, final String[] selectionArgs,
	                       final String orderBy, final String limit) {
		startQuery(token, cookie, dataSource.getDatabaseId(), projection, selection, selectionArgs, orderBy, limit);
	}

	/**
	 * <b>Deprecated, the {@code uri} field will be ignored, the one from {@link org.gawst.asyncdb.source.DatabaseSource} will be used.</b>
	 * <p/>
	 * This method begins an asynchronous query. When the query is done
	 * {@link #onQueryComplete} is called.
	 *
	 * @param token         A token passed into {@link #onQueryComplete} to identify
	 *                      the query.
	 * @param cookie        An object that gets passed into {@link #onQueryComplete}
	 * @param databaseId
	 * @param projection    A list of which columns to return. Passing null will
	 *                      return all columns, which is discouraged to prevent reading data
	 *                      from storage that isn't going to be used.
	 * @param selection     A filter declaring which rows to return, formatted as an
	 *                      SQL WHERE clause (excluding the WHERE itself). Passing null will
	 *                      return all rows for the given URI.
	 * @param selectionArgs You may include ?s in selection, which will be
	 *                      replaced by the values from selectionArgs, in the order that they
	 *                      appear in the selection. The values will be bound as Strings.
	 * @param orderBy       How to order the rows, formatted as an SQL ORDER BY
	 * @param limit         Limits the number of rows returned by the query,
	 *                      formatted as LIMIT clause. Passing null denotes no LIMIT clause.
	 * @see #startQuery(int, Object, String[], String, String[], String, String)
	 */
	@Deprecated
	public void startQuery(final int token, final Object cookie, DATABASE_ID databaseId,
	                       final String[] projection, final String selection, final String[] selectionArgs,
	                       final String orderBy, final String limit) {
		checkDatabaseId(databaseId);
		asynchronousDbHelper.scheduleCustomOperation(new AsynchronousDbOperation() {
			@Override
			public void runInMemoryDbOperation(AsynchronousDbHelper<?, ?> db) {
				Cursor cursor1;
				try {
					cursor1 = dataSource.query(projection, selection, selectionArgs, null, null, orderBy, limit);
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
	 * <b>Deprecated, the {@code uri} field will be ignored, the one from {@link org.gawst.asyncdb.source.DatabaseSource} will be used.</b>
	 * <p/>
	 * This method begins an asynchronous insert. When the insert operation is
	 * done {@link #onInsertComplete} is called.
	 *
	 * @param token         A token passed into {@link #onInsertComplete} to identify
	 *                      the insert operation.
	 * @param cookie        An object that gets passed into {@link #onInsertComplete}
	 * @param databaseId    the Uri passed to the insert operation.
	 * @param initialValues the ContentValues parameter passed to the insert operation.
	 * @see #startInsert(int, Object, android.content.ContentValues)
	 */
	@Deprecated
	public void startInsert(final int token, final Object cookie, DATABASE_ID databaseId,
	                        final ContentValues initialValues) {
		checkDatabaseId(databaseId);
		startInsert(token, cookie, initialValues);
	}

	/**
	 * This method begins an asynchronous insert. When the insert operation is
	 * done {@link #onInsertComplete} is called.
	 *
	 * @param token         A token passed into {@link #onInsertComplete} to identify
	 *                      the insert operation.
	 * @param cookie        An object that gets passed into {@link #onInsertComplete}
	 * @param initialValues the ContentValues parameter passed to the insert operation.
	 */
	public void startInsert(final int token, final Object cookie,
	                        final ContentValues initialValues) {
		asynchronousDbHelper.scheduleCustomOperation(new AsynchronousDbOperation() {
			@Override
			public void runInMemoryDbOperation(AsynchronousDbHelper<?, ?> db) {
				INSERT_ID inserted1 = null;
				try {
					inserted1 = dataSource.insert(initialValues);
				} catch (Exception e) {
					inserted1 = null;
				} finally {
					if (inserted1 != null) {
						asynchronousDbHelper.triggerPurgeHandler();
					}
				}

				final INSERT_ID insertId = inserted1;
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						onInsertComplete(token, cookie, insertId);
					}
				});
			}
		});
	}

	/**
	 * <b>Deprecated, the {@code uri} field will be ignored, the one from {@link org.gawst.asyncdb.source.DatabaseSource} will be used.</b>
	 * <p/>
	 * This method begins an asynchronous update. When the update operation is
	 * done {@link #onUpdateComplete} is called.
	 *
	 * @param token      A token passed into {@link #onUpdateComplete} to identify
	 *                   the update operation.
	 * @param cookie     An object that gets passed into {@link #onUpdateComplete}
	 * @param databaseId the Uri passed to the update operation.
	 * @param values     the ContentValues parameter passed to the update operation.
	 * @see #startUpdate(int, Object, android.content.ContentValues, String, String[])
	 */
	@Deprecated
	public void startUpdate(final int token, final Object cookie, DATABASE_ID databaseId,
	                        final ContentValues values, final String selection, final String[] selectionArgs) {
		checkDatabaseId(databaseId);
		startUpdate(token, cookie, values, selection, selectionArgs);
	}

	/**
	 * This method begins an asynchronous update. When the update operation is
	 * done {@link #onUpdateComplete} is called.
	 *
	 * @param token  A token passed into {@link #onUpdateComplete} to identify
	 *               the update operation.
	 * @param cookie An object that gets passed into {@link #onUpdateComplete}
	 * @param values the ContentValues parameter passed to the update operation.
	 */
	public void startUpdate(final int token, final Object cookie,
	                        final ContentValues values, final String selection, final String[] selectionArgs) {
		asynchronousDbHelper.scheduleCustomOperation(new AsynchronousDbOperation() {
			@Override
			public void runInMemoryDbOperation(AsynchronousDbHelper<?, ?> db) {
				int cursor1;
				try {
					cursor1 = dataSource.update(values, selection, selectionArgs);
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
	 * <b>Deprecated, the {@code uri} field will be ignored, the one from {@link org.gawst.asyncdb.source.DatabaseSource} will be used.</b>
	 * <p/>
	 * This method begins an asynchronous delete. When the delete operation is
	 * done {@link #onDeleteComplete} is called.
	 *
	 * @param token      A token passed into {@link #onDeleteComplete} to identify
	 *                   the delete operation.
	 * @param cookie     An object that gets passed into {@link #onDeleteComplete}
	 * @param databaseId the Uri passed to the delete operation.
	 * @param selection  the where clause.
	 * @see #startDelete(int, Object, String, String[])
	 */
	@Deprecated
	public void startDelete(final int token, final Object cookie, DATABASE_ID databaseId,
	                        final String selection, final String[] selectionArgs) {
		checkDatabaseId(databaseId);
		startDelete(token, cookie, selection, selectionArgs);
	}

	/**
	 * This method begins an asynchronous delete. When the delete operation is
	 * done {@link #onDeleteComplete} is called.
	 *
	 * @param token     A token passed into {@link #onDeleteComplete} to identify
	 *                  the delete operation.
	 * @param cookie    An object that gets passed into {@link #onDeleteComplete}
	 * @param selection the where clause.
	 */
	public void startDelete(final int token, final Object cookie,
	                        final String selection, final String[] selectionArgs) {
		asynchronousDbHelper.scheduleCustomOperation(new AsynchronousDbOperation() {
			@Override
			public void runInMemoryDbOperation(AsynchronousDbHelper<?, ?> db) {
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
	 * This method begins an asynchronous processing of the {@code Runnable}. When the operation is
	 * done {@link #onRunnableCompleted} is called.
	 *
	 * @param token  A token passed into {@link #onRunnableCompleted} to identify the operation.
	 * @param cookie An object that gets passed into {@link #onRunnableCompleted}
	 * @param job    The {@code Runnable} to run.
	 */
	public final void startRunnable(final int token, final Object cookie, final Runnable job) {
		asynchronousDbHelper.scheduleCustomOperation(new AsynchronousDbOperation() {
			@Override
			public void runInMemoryDbOperation(AsynchronousDbHelper<?, ?> db) {
				job.run();

				mHandler.post(new Runnable() {
					@Override
					public void run() {
						onRunnableCompleted(token, cookie);
					}
				});
			}
		});
	}

	/**
	 * Called when an asynchronous query is completed. The receiver is responsible to close the Cursor.
	 * </p>Called in the UI thread
	 *
	 * @param token  the token to identify the query, passed in from
	 *               {@link #startQuery}.
	 * @param cookie the cookie object passed in from {@link #startQuery}.
	 * @param cursor The cursor holding the results from the query.
	 */
	protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
		// Empty
	}

	/**
	 * Called when an asynchronous insert is completed.
	 * </p>Called in the UI thread
	 *
	 * @param token    the token to identify the query, passed in from
	 *                 {@link #startInsert}.
	 * @param cookie   the cookie object that's passed in from
	 *                 {@link #startInsert}.
	 * @param insertId the uri returned from the insert operation.
	 */
	protected void onInsertComplete(int token, Object cookie, INSERT_ID insertId) {
		// Empty
	}

	/**
	 * Called when an asynchronous update is completed.
	 * </p>Called in the UI thread
	 *
	 * @param token  the token to identify the query, passed in from
	 *               {@link #startUpdate}.
	 * @param cookie the cookie object that's passed in from
	 *               {@link #startUpdate}.
	 * @param result the result returned from the update operation
	 */
	protected void onUpdateComplete(int token, Object cookie, int result) {
		// Empty
	}

	/**
	 * Called when an asynchronous delete is completed.
	 * </p>Called in the UI thread
	 *
	 * @param token  the token to identify the query, passed in from
	 *               {@link #startDelete}.
	 * @param cookie the cookie object that's passed in from
	 *               {@link #startDelete}.
	 * @param result the result returned from the delete operation
	 */
	protected void onDeleteComplete(int token, Object cookie, int result) {
		// Empty
	}

	/**
	 * Called when an asynchronous {@code Runnable} is completed.
	 * </p>Called in the UI thread
	 *
	 * @param token  the token to identify the query, passed in from
	 *               {@link #startRunnable}.
	 * @param cookie the cookie object that's passed in from
	 *               {@link #startRunnable}.
	 */
	protected void onRunnableCompleted(int token, Object cookie) {
		// Empty
	}
}
