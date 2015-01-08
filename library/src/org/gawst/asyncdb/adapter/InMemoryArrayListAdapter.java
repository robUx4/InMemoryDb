package org.gawst.asyncdb.adapter;

import org.gawst.asyncdb.InMemoryDbArrayList;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;

/**
 * Basic {@link android.widget.ListAdapter ListAdapter} using a {@link InMemoryDbArrayList} data source
 *
 * @param <E> Type of the elements stored in the {@link InMemoryDbArrayList}
 */
public class InMemoryArrayListAdapter<E> extends InMemoryFilteredListAdapter<E> {

	/**
	 * Constructor for the adapter.
	 *
	 * @param context          {@code Context} used to get the layout inflater.
	 * @param array            The data source, monitored for changes.
	 * @param layoutResourceId The layout to inflate to display an element. It must contain a {@code TextView} element with id
	 *                         {@code android.R.id.text1} unless you inflate the {@code View} yourself.
	 */
	public InMemoryArrayListAdapter(@NonNull Context context, @NonNull InMemoryDbArrayList<E, ?> array, @LayoutRes int layoutResourceId) {
		super(context, array, layoutResourceId, null);
	}
}
