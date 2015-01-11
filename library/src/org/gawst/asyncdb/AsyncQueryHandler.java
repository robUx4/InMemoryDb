package org.gawst.asyncdb;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Class to mimick exactly the API of Android's {@link android.content.AsyncQueryHandler AsyncQueryHandler} with a {@link org.gawst.asyncdb.AsynchronousDbHelper} source
 *
 * @author Created by robUx4 on 09/01/2015.
 * @see org.gawst.asyncdb.AsynchronousDbHelper.AsyncHandler AsyncHandler for a cleaner version
 */
public class AsyncQueryHandler extends AsyncDatabaseHandler<Object, Uri> {

	/**
	 * Constructor similar to Android's {@link android.content.AsyncQueryHandler#AsyncQueryHandler(android.content.ContentResolver) AsyncQueryHandler()}
	 * using a fixed {@link android.net.Uri} to access data.
	 *
	 * @param contentResolver
	 * @param contentProviderUri
	 */
	public AsyncQueryHandler(ContentResolver contentResolver, Uri contentProviderUri) {
		this(new ContentProviderDataSource<Object>(contentResolver, contentProviderUri, new DatabaseElementHandler<Object>() {
			@Override
			public String getItemSelectClause(@Nullable Object itemToSelect) {
				throw new AssertionError("not supported");
			}

			@Override
			public String[] getItemSelectArgs(@NonNull Object itemToSelect) {
				throw new AssertionError("not supported");
			}

			@NonNull
			@Override
			public Object cursorToItem(@NonNull Cursor cursor) throws InvalidDbEntry {
				throw new AssertionError("not supported");
			}
		}));
	}

	/**
	 * Constructor.
	 *
	 * @param dataSource Custom {@link org.gawst.asyncdb.ContentProviderDataSource} source.
	 */
	private AsyncQueryHandler(ContentProviderDataSource<Object> dataSource) {
		this(new AsynchronousDatabase<Object, Uri>(dataSource, dataSource.toString(), null) {
			@Override
			protected ContentValues getValuesFromData(Object data) throws RuntimeException {
				throw new AssertionError("not supported");
			}
		}, dataSource);
	}

	/**
	 * Constructor.
	 *
	 * @param asynchronousDbHelper The {@link org.gawst.asyncdb.AsynchronousDbHelper} database to work with.
	 * @param dataSource           The {@link org.gawst.asyncdb.DatabaseSource} source used by the {@code asynchronousDbHelper}.
	 */
	public AsyncQueryHandler(AsynchronousDbHelper<?, ?> asynchronousDbHelper, DatabaseSource<?, ?> dataSource) {
		super((AsynchronousDbHelper<?, Object>) asynchronousDbHelper, (DatabaseSource<Object, Uri>) dataSource);
	}
}
