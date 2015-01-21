package org.gawst.asyncdb;

import org.gawst.asyncdb.source.typed.TypedDatabaseSource;

import android.net.Uri;

/**
 * A class similar to {@link android.content.AsyncQueryHandler} to do simple calls asynchronously with a callback when it's done.
 * Only works with {@link org.gawst.asyncdb.AsynchronousDbHelper} classes that use a {@link org.gawst.asyncdb.source.DatabaseSource} source.
 * <p>When using different instances for the same {@link org.gawst.asyncdb.AsynchronousDbHelper} source, make sure the IDs you pass to the startXXX() methods are unique!
 */
public class AsyncDbHelperHandler<INSERT_ID> extends AsyncDatabaseHandler<INSERT_ID, Uri> {
	/**
	 * Constructor.
	 *
	 * @param asynchronousDbHelper
	 */
	public AsyncDbHelperHandler(AsynchronousDbHelper<?, INSERT_ID> asynchronousDbHelper) {
		super(asynchronousDbHelper, (TypedDatabaseSource<INSERT_ID, Uri, ?>) asynchronousDbHelper.dataSource);
	}
}
