package org.gawst.asyncdb;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;

/**
 * Created by robUx4 on 12/31/2014.
 */
public class ContentProviderDataSource<E> extends CursorDataSource<E> {
	private final Uri contentProviderUri;
	private final Context context;

	public ContentProviderDataSource(@NonNull Context context, @NonNull Uri contentProviderUri, @NonNull CursorSourceHandler<E> cursorSourceHandler) {
		super(cursorSourceHandler);
		this.context = context.getApplicationContext();
		this.contentProviderUri = contentProviderUri;
	}

	@Override
	protected Cursor readAll() {
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
		return context.getContentResolver().update(contentProviderUri, updateValues,
				cursorSourceHandler.getItemSelectClause(itemToUpdate),
				cursorSourceHandler.getItemSelectArgs(itemToUpdate))!=0;
	}

	@Override
	public boolean delete(E itemToDelete) {
		return context.getContentResolver().delete(contentProviderUri,
				cursorSourceHandler.getItemSelectClause(itemToDelete),
				cursorSourceHandler.getItemSelectArgs(itemToDelete))!=0;
	}

	@Override
	public boolean deleteInvalidEntry(InvalidEntry invalidEntry) {
		// TODO
		return false;
	}

	@Override
	public void eraseSource() {
		// TODO
	}
}
