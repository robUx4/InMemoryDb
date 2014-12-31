package org.gawst.asyncdb;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

/**
 * Created by robUx4 on 12/31/2014.
 */
public abstract class ContentProviderDataSource<E> implements DataSource<E> {
	private final Uri contentProviderUri;
	private final Context context;

	public ContentProviderDataSource(Context context, Uri contentProviderUri) {
		this.context = context.getApplicationContext();
		this.contentProviderUri = contentProviderUri;
	}

	protected abstract String getItemSelectClause(E itemToSelect);
	protected abstract String[] getItemSelectArgs(E itemToSelect);

	@Override
	public Cursor queryAll() {
		return context.getContentResolver().query(contentProviderUri, null, null, null, null);
	}

	@Override
	public int clearAllData() {
		return context.getContentResolver().delete(contentProviderUri, "1", null);
	}

	@Override
	public Object insert(ContentValues element) throws RuntimeException {
		return context.getContentResolver().insert(contentProviderUri, element);
	}

	@Override
	public boolean update(E itemToUpdate, ContentValues updateValues) {
		return context.getContentResolver().update(contentProviderUri, updateValues, getItemSelectClause(itemToUpdate), getItemSelectArgs(itemToUpdate))!=0;
	}

	@Override
	public boolean delete(E itemToDelete) {
		return context.getContentResolver().delete(contentProviderUri, getItemSelectClause(itemToDelete), getItemSelectArgs(itemToDelete))!=0;
	}

	@Override
	public void eraseSource() {
		// TODO
	}
}
