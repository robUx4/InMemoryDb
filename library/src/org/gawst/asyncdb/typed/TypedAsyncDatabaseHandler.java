package org.gawst.asyncdb.typed;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.gawst.asyncdb.AsynchronousDbHelper;
import org.gawst.asyncdb.AsynchronousDbOperation;
import org.gawst.asyncdb.source.typed.TypedDatabaseSource;

import java.util.concurrent.Callable;

/**
 * Class similar to Android's {@link android.content.AsyncQueryHandler AsyncQueryHandler} to work with an
 * {@link org.gawst.asyncdb.AsynchronousDbHelper} instead of a ContentProvider source and dealing with items of type {@link E}.
 *
 * @param <CURSOR>      the type of {@link android.database.Cursor} returned by a query, see {@link #onQueryComplete(int, Object, android.database.Cursor)}
 * @param <E>           the type of elements written to the database.
 * @param <INSERT_ID>   the type of {@code id} returned from a {@link #startInsert(int, Object, Object)}, see {@link #onInsertComplete(int, Object, Object)}
 * @param <DATABASE_ID> the database internal ID, used to check everything is okay internally
 */
public class TypedAsyncDatabaseHandler<E, CURSOR extends Cursor, INSERT_ID, DATABASE_ID> {

	private final AsynchronousDbHelper<E, INSERT_ID> asynchronousDbHelper;
	private static final Handler mHandler = new Handler(Looper.getMainLooper());
	protected final TypedDatabaseSource<INSERT_ID, DATABASE_ID, CURSOR> dataSource;

	/**
	 * Constructor.
	 *
	 * @param asynchronousDbHelper The {@link AsynchronousDbHelper} database to work with.
	 * @param dataSource           The {@link org.gawst.asyncdb.source.typed.TypedDatabaseSource} source used by the {@code asynchronousDbHelper}.
	 */
	public TypedAsyncDatabaseHandler(AsynchronousDbHelper<E, INSERT_ID> asynchronousDbHelper, TypedDatabaseSource<INSERT_ID, DATABASE_ID, CURSOR> dataSource) {
		this.asynchronousDbHelper = asynchronousDbHelper;
		this.dataSource = dataSource;
	}

	private void checkDatabaseId(DATABASE_ID databaseId) {
		if (databaseId != null && databaseId != dataSource.getDatabaseId())
			throw new IllegalArgumentException("wrong database id " + databaseId + " expected " + dataSource.getDatabaseId() + " try startRunnable()");
	}

	public AsynchronousDbHelper<E, INSERT_ID> getAsynchronousDbHelper() {
		return asynchronousDbHelper;
	}

	public TypedDatabaseSource<INSERT_ID, DATABASE_ID, CURSOR> getDataSource() {
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
	 * <b>Deprecated, the {@code uri} field will be ignored, the one from {@link org.gawst.asyncdb.source.typed.TypedDatabaseSource} will be used.</b>
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
	 * <b>Deprecated, the {@code uri} field will be ignored, the one from {@link org.gawst.asyncdb.source.typed.TypedDatabaseSource} will be used.</b>
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
				CURSOR cursor1;
				try {
					cursor1 = dataSource.query(projection, selection, selectionArgs, null, null, orderBy, limit);
				} catch (Exception e) {
					cursor1 = null;
				}

				final CURSOR cursor = cursor1;
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
	 * @param token  A token passed into {@link #onInsertComplete} to identify
	 *               the insert operation.
	 * @param cookie An object that gets passed into {@link #onInsertComplete}
	 * @param item   to the insert in the database.
	 */
	public void startInsert(final int token, final Object cookie, E item) {
		final ContentValues insertValues = asynchronousDbHelper.getValuesFromData(item, false);
		if (null != insertValues) {
			asynchronousDbHelper.scheduleCustomOperation(new AsynchronousDbOperation() {
				@Override
				public void runInMemoryDbOperation(AsynchronousDbHelper<?, ?> db) {
					INSERT_ID inserted1 = null;
					try {
						inserted1 = dataSource.insert(insertValues);
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
	}

	/**
	 * This method begins an asynchronous update. When the update operation is
	 * done {@link #onUpdateComplete} is called.
	 *
	 * @param token  A token passed into {@link #onUpdateComplete} to identify
	 *               the update operation.
	 * @param cookie An object that gets passed into {@link #onUpdateComplete}
	 * @param item   to update in the database.
	 */
	public void startUpdate(final int token, final Object cookie,
	                        final E item, final String selection, final String[] selectionArgs) {
		final ContentValues updateValues = asynchronousDbHelper.getValuesFromData(item, true);
		if (null != updateValues) {
			asynchronousDbHelper.scheduleCustomOperation(new AsynchronousDbOperation() {
				@Override
				public void runInMemoryDbOperation(AsynchronousDbHelper<?, ?> db) {
					int updatedRows;
					try {
						updatedRows = dataSource.update(updateValues, selection, selectionArgs);
					} catch (Exception e) {
						updatedRows = 0;
					}

					final int finalUpdatedRows = updatedRows;
					mHandler.post(new Runnable() {
						@Override
						public void run() {
							onUpdateComplete(token, cookie, finalUpdatedRows);
						}
					});
				}
			});
		}
	}

	/**
	 * This method begins an asynchronous delete. When the delete operation is
	 * done {@link #onDeleteComplete} is called.
	 *
	 * @param token  A token passed into {@link #onDeleteComplete} to identify
	 *               the delete operation.
	 * @param cookie An object that gets passed into {@link #onDeleteComplete}
	 * @param item   to delete from the database.
	 */
	public void startDelete(final int token, final Object cookie, final E item) {
		asynchronousDbHelper.scheduleCustomOperation(new AsynchronousDbOperation() {
			@Override
			public void runInMemoryDbOperation(AsynchronousDbHelper<?, ?> db) {
				int deleted;
				try {
					deleted = asynchronousDbHelper.getDataSource().delete(item);
				} catch (Exception e) {
					deleted = 0;
				}

				final int finalDeleted = deleted;
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						onDeleteComplete(token, cookie, finalDeleted);
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
     * Callback called when the {@link java.util.concurrent.Callable} passed to {@link #startCallable(int, Object,
     * java.util.concurrent.Callable, org.gawst.asyncdb.typed.TypedAsyncDatabaseHandler.CallableCallback) startCallable()} is finished.
     *
     * @param <V> type of the data returned by the {@code Callable}
     */
    public interface CallableCallback<V> {
        /**
         * Callback called when the {@link java.util.concurrent.Callable} passed to {@link #startCallable(int, Object,
         * java.util.concurrent.Callable, org.gawst.asyncdb.typed.TypedAsyncDatabaseHandler.CallableCallback) startCallable()} is finished.
         *
         * @param token     A token passed into {@link #startCallable(int, Object, java.util.concurrent.Callable,
         *                  org.gawst.asyncdb.typed.TypedAsyncDatabaseHandler.CallableCallback) startCallable()} to
         *                  identify the operation.
         * @param cookie    An object that gets passed into {@link #startCallable(int, Object, java.util.concurrent.Callable,
         *                  org.gawst.asyncdb.typed.TypedAsyncDatabaseHandler.CallableCallback) startCallable()} .
         * @param result    of the {@code Callable} if there was no exception.
         * @param exception thrown by the {@code Callable}.
         */
        void onCallableResult(int token, Object cookie, @Nullable V result, @Nullable Exception exception);
    }

    /**
     * This method begins an asynchronous processing of the {@code Callable}. When the operation is
     * done the {@code callback} is called.
     *
     * @param token    A token passed into {@link CallableCallback#onCallableResult(int, Object, Object, Exception)
     *                 CallableCallback.onCallableResult()} to identify the operation.
     * @param cookie   An object that gets passed into {@link CallableCallback#onCallableResult(int, Object, Object, Exception)
     *                 CallableCallback.onCallableResult()}.
     * @param callable The {@code Callable} to run.
     * @param callback The callback call when the {@code Callable} is finished with a result or an exception.
     * @param <V>      type of the data returned by the {@code Callable}
     */
    public final <V> void startCallable(final int token, final Object cookie, @NonNull final Callable<V> callable, @NonNull final CallableCallback<V> callback) {
        asynchronousDbHelper.scheduleCustomOperation(new AsynchronousDbOperation() {
            @Override
            public void runInMemoryDbOperation(AsynchronousDbHelper<?, ?> db) {
                try {
                    final V result = callable.call();
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onCallableResult(token, cookie, result, null);
                        }
                    });
                } catch (final Exception e) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onCallableResult(token, cookie, null, e);
                        }
                    });
                }
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
	protected void onQueryComplete(int token, Object cookie, CURSOR cursor) {
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
	 * @param result {@code 1} if the element was deleted, {@code 0} otherwise.
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
