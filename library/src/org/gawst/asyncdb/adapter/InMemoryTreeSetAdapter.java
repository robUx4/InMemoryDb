package org.gawst.asyncdb.adapter;

import org.gawst.asyncdb.InMemoryDbTreeSet;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;

/**
 * Basic {@link android.widget.ListAdapter ListAdapter} using a {@link InMemoryDbTreeSet} data source
 *
 * @param <E> Type of the elements stored in the {@link InMemoryDbTreeSet}
 */
public class InMemoryTreeSetAdapter<E> extends InMemoryFilteredTreeAdapter<E> {

	/**
	 * Constructor for the adapter.
	 *
	 * @param context          {@code Context} used to get the layout inflater.
	 * @param array            The data source, monitored for changes.
	 * @param layoutResourceId The layout to inflate to display an element. It must contain a {@code TextView} element with id
	 *                         {@code android.R.id.text1} unless you inflate the {@code View} yourself.
	 */
	public InMemoryTreeSetAdapter(@NonNull Context context, @NonNull InMemoryDbTreeSet<E, ?> array, @LayoutRes int layoutResourceId) {
		super(context, array, layoutResourceId, null);
	}
}
