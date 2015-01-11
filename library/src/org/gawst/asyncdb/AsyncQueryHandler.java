package org.gawst.asyncdb;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.gawst.asyncdb.source.ContentProviderDataSource;
import org.gawst.asyncdb.source.DatabaseElementHandler;
import org.gawst.asyncdb.source.DatabaseSource;

/**
 * Class to mimick exactly the API of Android's {@link android.content.AsyncQueryHandler AsyncQueryHandler} with a {@link org.gawst.asyncdb.AsynchronousDbHelper} source
 *
 * @author Created by robUx4 on 09/01/2015.
 * @see org.gawst.asyncdb.AsynchronousDbHelper.AsyncHandler AsyncHandler for a cleaner version
 */
public class AsyncQueryHandler extends AsyncDatabaseHandler<Uri, Uri> {

	/**
	 * Constructor similar to Android's {@link android.content.AsyncQueryHandler#AsyncQueryHandler(android.content.ContentResolver) AsyncQueryHandler()}
	 * using a fixed {@link android.net.Uri} to access data.
	 *
	 * @param contentResolver
	 * @param contentProviderUri
	 */
	public AsyncQueryHandler(ContentResolver contentResolver, Uri contentProviderUri) {
		this(new ContentProviderDataSource<Uri>(contentResolver, contentProviderUri, new DatabaseElementHandler<Uri>() {
			@Override
			public String getItemSelectClause(@Nullable Uri itemToSelect) {
				throw new AssertionError("not supported");
			}

			@Override
			public String[] getItemSelectArgs(@NonNull Uri itemToSelect) {
				throw new AssertionError("not supported");
			}

			@NonNull
			@Override
			public Uri cursorToItem(@NonNull Cursor cursor) throws InvalidDbEntry {
				throw new AssertionError("not supported");
			}
		}));
	}

	/**
	 * Constructor.
	 *
	 * @param dataSource Custom {@link org.gawst.asyncdb.source.ContentProviderDataSource} source.
	 */
	public AsyncQueryHandler(ContentProviderDataSource<Uri> dataSource) {
		this(new AsynchronousDatabase<Uri, Uri>(dataSource, dataSource.contentProviderUri.getAuthority() + dataSource.contentProviderUri.getPath(), null) {
			@Override
			protected ContentValues getValuesFromData(Uri data) throws RuntimeException {
				throw new AssertionError("not supported");
			}
		}, dataSource);
	}

	/**
	 * Constructor.
	 *
	 * @param asynchronousDbHelper The {@link org.gawst.asyncdb.AsynchronousDbHelper} database to work with.
	 * @param dataSource           The {@link org.gawst.asyncdb.source.DatabaseSource} source used by the {@code asynchronousDbHelper}.
	 */
	public AsyncQueryHandler(AsynchronousDbHelper<?, Uri> asynchronousDbHelper, DatabaseSource<Uri, Uri> dataSource) {
		super(asynchronousDbHelper, dataSource);
	}
}
