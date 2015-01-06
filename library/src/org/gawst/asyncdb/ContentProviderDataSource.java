package org.gawst.asyncdb;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;

/**
 * Created by robUx4 on 12/31/2014.
 */
public class ContentProviderDataSource<E> extends CursorDataSource<E, Uri> {
	private final Uri contentProviderUri;
	private final Context context;

	public ContentProviderDataSource(@NonNull Context context, @NonNull Uri contentProviderUri, @NonNull CursorSourceHandler<E> cursorSourceHandler) {
		super(cursorSourceHandler);
		this.context = context.getApplicationContext();
		this.contentProviderUri = contentProviderUri;
	}

	@Override
	public Cursor query(String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit) {
		return context.getContentResolver().query(contentProviderUri, columns, selection, selectionArgs, orderBy);
	}

	@Override
	public int clearAllData() {
		return context.getContentResolver().delete(contentProviderUri, "1", null);
	}

	@Override
	public Uri insert(ContentValues element) throws RuntimeException {
		return context.getContentResolver().insert(contentProviderUri, element);
	}

	@Override
	public int update(String where, String[] selectionArgs, ContentValues updateValues) {
		return context.getContentResolver().update(contentProviderUri, updateValues, where, selectionArgs);
	}

	@Override
	public int delete(String selection, String[] selectionArgs) {
		return context.getContentResolver().delete(contentProviderUri, selection, selectionArgs);
	}

	@Override
	public void eraseSource() {
		// TODO
	}
}
