package org.gawst.asyncdb.source;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;

import org.gawst.asyncdb.source.typed.TypedContentProviderDataSource;

/**
 * A {@link org.gawst.asyncdb.DataSource} reading/writing data using a {@link android.content.ContentProvider ContentProvider}
 * @param <E> Type of the elements read from the {@code Cursor}
 */
public class ContentProviderDataSource<E> extends TypedContentProviderDataSource<E, Cursor> implements DatabaseSource<Uri, Uri> {
	/**
	 * Contructor.
	 * @param contentResolver ContentResolver used to access the {@link android.content.ContentProvider ContentProvider}
	 * @param contentProviderUri {@link android.net.Uri Uri} to access the data from the {@link android.content.ContentProvider ContentProvider}
	 * @param databaseElementHandler Handler to transform {@link E} elements to queries and {@code Cursor} to {@link E} elements.
	 */
	public ContentProviderDataSource(@NonNull ContentResolver contentResolver, @NonNull Uri contentProviderUri, @NonNull DatabaseElementHandler<E> databaseElementHandler) {
		super(contentResolver, contentProviderUri, databaseElementHandler);
	}

	/**
	 * Contructor.
	 * @param context Context used to get the {@link android.content.ContentResolver ContentResolver} used to access the {@link android.content.ContentProvider ContentProvider}
	 * @param contentProviderUri {@link android.net.Uri Uri} to access the data from the {@link android.content.ContentProvider ContentProvider}
	 * @param databaseElementHandler Handler to transform {@link E} elements to queries and {@code Cursor} to {@link E} elements.
	 */
	public ContentProviderDataSource(@NonNull Context context, @NonNull Uri contentProviderUri, @NonNull DatabaseElementHandler<E> databaseElementHandler) {
		super(context, contentProviderUri, databaseElementHandler);
	}

	@Override
	public Cursor wrapCursor(Cursor cursor) {
		return cursor;
	}
}
