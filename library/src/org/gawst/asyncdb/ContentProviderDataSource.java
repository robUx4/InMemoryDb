package org.gawst.asyncdb;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

/**
 * A {@link org.gawst.asyncdb.DataSource} reading/writing data using a {@link android.content.ContentProvider ContentProvider}
 * @param <E> Type of the elements read from the {@code Cursor}
 */
public class ContentProviderDataSource<E> extends CursorDataSource<E, Uri, Uri> {
	protected final Uri contentProviderUri;
	private final Context context;
	private final ContentResolver contentResolver;

	public ContentProviderDataSource(@NonNull ContentResolver contentResolver, @NonNull Uri contentProviderUri, @NonNull DatabaseElementHandler<E> databaseElementHandler) {
		super(databaseElementHandler);
		this.context = null;
		this.contentResolver = contentResolver;
		this.contentProviderUri = contentProviderUri;
	}

	/**
	 * Contructor.
	 * @param context Context used to get the {@link android.content.ContentResolver ContentResolver} used to access the {@link android.content.ContentProvider ContentProvider}
	 * @param contentProviderUri {@link android.net.Uri Uri} to access the data from the {@link android.content.ContentProvider ContentProvider}
	 * @param databaseElementHandler Handler to transform {@link E} elements to queries and {@code Cursor} to {@link E} elements.
	 */
	public ContentProviderDataSource(@NonNull Context context, @NonNull Uri contentProviderUri, @NonNull DatabaseElementHandler<E> databaseElementHandler) {
		super(databaseElementHandler);
		this.context = context.getApplicationContext();
		this.contentResolver = null;
		this.contentProviderUri = contentProviderUri;
	}

	@Override
	public Uri getDatabaseId() {
		return contentProviderUri;
	}

	private ContentResolver getContentResolver() {
		if (null != context)
			return context.getContentResolver();
		return contentResolver;
	}

	@Override
	public Cursor query(String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit) {
		if (!TextUtils.isEmpty(limit)) {
			if (TextUtils.isEmpty(orderBy)) {
				orderBy = "LIMIT "+limit;
			} else {
				orderBy += " LIMIT "+limit;
			}
		}
		return getContentResolver().query(contentProviderUri, columns, selection, selectionArgs, orderBy);
	}

	@Override
	public int clearAllData() {
		return getContentResolver().delete(contentProviderUri, "1", null);
	}

	@Override
	public Uri insert(ContentValues values) throws RuntimeException {
		return getContentResolver().insert(contentProviderUri, values);
	}

	@Override
	public int update(ContentValues updateValues, String where, String[] selectionArgs) {
		return getContentResolver().update(contentProviderUri, updateValues, where, selectionArgs);
	}

	@Override
	public int delete(String selection, String[] selectionArgs) {
		return getContentResolver().delete(contentProviderUri, selection, selectionArgs);
	}

	/**
	 * Does nothing for a {@link android.content.ContentProvider}
	 */
	@Override
	public void eraseSource() {
		// TODO
	}
}
