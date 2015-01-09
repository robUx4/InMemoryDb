package org.gawst.asyncdb;

import android.net.Uri;

/**
 * Class to mimick exactly the API of Android's {@link android.content.AsyncQueryHandler AsyncQueryHandler} with a {@link org.gawst.asyncdb.AsynchronousDbHelper} source
 * @see org.gawst.asyncdb.AsynchronousDbHelper.AsyncHandler AsyncHandler for a cleaner version
 * @author Created by robUx4 on 09/01/2015.
 */
public class AsyncQueryHandler extends AsyncDatabaseHandler<Object, Uri> {

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
