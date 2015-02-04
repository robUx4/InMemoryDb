package org.gawst.asyncdb.source.typed;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

/**
 * A {@link org.gawst.asyncdb.DataSource} reading/writing data using a {@link android.content.ContentProvider ContentProvider}
 *
 * @param <E>      Type of the elements read from the {@code CURSOR}
 * @param <CURSOR> Wrapper around the raw {@code Cursor} read
 * @author Created by robUx4 on 11/01/2015.
 */
public abstract class TypedContentProviderDataSource<E, CURSOR extends Cursor> extends TypedCursorDataSource<E, Uri, Uri, CURSOR> {
	public final Uri contentProviderUri;
	private final Context context;
	private final ContentResolver contentResolver;

	/**
	 * Contructor.
	 *
	 * @param contentResolver        ContentResolver used to access the {@link android.content.ContentProvider ContentProvider}
	 * @param contentProviderUri     {@link android.net.Uri Uri} to access the data from the {@link android.content.ContentProvider ContentProvider}
	 * @param databaseElementHandler Handler to transform {@link E} elements to queries and {@code Cursor} to {@link E} elements.
	 */
	public TypedContentProviderDataSource(@NonNull ContentResolver contentResolver, @NonNull Uri contentProviderUri, @NonNull TypedDatabaseElementHandler<E, CURSOR> databaseElementHandler) {
		super(databaseElementHandler);
		this.context = null;
		this.contentResolver = contentResolver;
		this.contentProviderUri = contentProviderUri;
	}

	/**
	 * Contructor.
	 *
	 * @param context                Context used to get the {@link android.content.ContentResolver ContentResolver} used to access the {@link android.content.ContentProvider ContentProvider}
	 * @param contentProviderUri     {@link android.net.Uri Uri} to access the data from the {@link android.content.ContentProvider ContentProvider}
	 * @param databaseElementHandler Handler to transform {@link E} elements to queries and {@code Cursor} to {@link E} elements.
	 */
	public TypedContentProviderDataSource(@NonNull Context context, @NonNull Uri contentProviderUri, @NonNull TypedDatabaseElementHandler<E, CURSOR> databaseElementHandler) {
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
	public CURSOR query(String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit) {
		Uri uri = contentProviderUri;
		if (!TextUtils.isEmpty(groupBy)) {
			orderBy = getGroupOrderBy(orderBy, groupBy);
			uri = getGroupUri(uri, groupBy);
		}
		if (!TextUtils.isEmpty(having)) {
			orderBy = getHavingOrderBy(orderBy, having);
			uri = getHavingUri(uri, having);
		}
		if (!TextUtils.isEmpty(limit)) {
			orderBy = getLimitOrderBy(orderBy, limit);
			uri = getLimitUri(uri, limit);
		}
		return wrapCursor(getContentResolver().query(uri, columns, selection, selectionArgs, orderBy));
	}

	/**
	 * Allow modifications on the {@code ORDER_BY} field to handle the {@code GROUP BY} field missing in Content-Provider query().
	 * @return modified {@code ORDER_BY} clause
	 */
	protected String getGroupOrderBy(@Nullable String orderBy, @NonNull String groupBy) {
		if (TextUtils.isEmpty(orderBy)) {
			return "GROUP BY " + groupBy;
		}

		return orderBy + " GROUP BY " + groupBy;
	}

	/**
	 * Allow modifications on the Content-Provider {@code Uri} to handle the {@code GROUP BY} field missing in Content-Provider query().
	 * @return modified {@code Uri}
	 */
	protected Uri getGroupUri(@NonNull Uri uri, @NonNull String groupBy) {
		return uri;
	}

	/**
	 * Allow modifications on the {@code ORDER_BY} field to handle the {@code HAVING} field missing in Content-Provider query().
	 * @return modified {@code ORDER_BY} clause
	 */
	protected String getHavingOrderBy(@Nullable String orderBy, @NonNull String having) {
		if (TextUtils.isEmpty(orderBy)) {
			return "HAVING " + having;
		}

		return orderBy + " HAVING " + having;
	}

	/**
	 * Allow modifications on the Content-Provider {@code Uri} to handle the {@code HAVING} field missing in Content-Provider query().
	 * @return modified {@code Uri}
	 */
	protected Uri getHavingUri(@NonNull Uri uri, @NonNull String having) {
		return uri;
	}

	/**
	 * Allow modifications on the {@code ORDER_BY} field to handle the {@code LIMIT} field missing in Content-Provider query().
	 * @return modified {@code ORDER_BY} clause
	 */
	protected String getLimitOrderBy(@Nullable String orderBy, @NonNull String limit) {
		if (TextUtils.isEmpty(orderBy)) {
			return "LIMIT " + limit;
		}

		return orderBy + " LIMIT " + limit;
	}

	/**
	 * Allow modifications on the Content-Provider {@code Uri} to handle the {@code LIMIT} field missing in Content-Provider query().
	 * @return modified {@code Uri}
	 */
	protected Uri getLimitUri(@NonNull Uri uri, @NonNull String limit) {
		return uri;
	}

	@Override
	public int clearAllData() {
		return getContentResolver().delete(contentProviderUri, "1", null);
	}

	@Override
	public Uri insert(@NonNull ContentValues values) throws RuntimeException {
		return getContentResolver().insert(contentProviderUri, values);
	}

	@Override
	public int update(@NonNull ContentValues updateValues, String where, String[] selectionArgs) {
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

	@Override
	protected String getToStringHeader() {
		return "TypedContentProvider:"+contentProviderUri;
	}
}
