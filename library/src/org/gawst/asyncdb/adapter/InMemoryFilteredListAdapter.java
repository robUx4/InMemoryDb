package org.gawst.asyncdb.adapter;

import org.gawst.asyncdb.AsynchronousDbHelper;
import org.gawst.asyncdb.InMemoryDbArrayList;
import org.gawst.asyncdb.InMemoryDbListener;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * A {@link android.widget.ListAdapter ListAdapter} that only shows a subset of elements in a {@link InMemoryDbArrayList}
 * using the {@link org.gawst.asyncdb.adapter.InMemoryFilteredAdapter.InMemoryFilter}
 *
 * @param <E> Type of the elements stored in the {@link InMemoryDbArrayList}
 */
public class InMemoryFilteredListAdapter<E> extends InMemoryFilteredAdapter<E> implements InMemoryDbListener<E> {

	@NonNull
	private final InMemoryDbArrayList<E, ?> mArray;

	/**
	 * Constructor for the adapter.
	 *
	 * @param context          {@code Context} used to get the layout inflater.
	 * @param array            The data source, monitored for changes.
	 * @param layoutResourceId The layout to inflate to display an element. It must contain a {@code TextView} element with id
	 *                         {@code android.R.id.text1} unless you inflate the {@code View} yourself.
	 * @param filter           Filter to apply to the data source before it's displayed.
	 */
	public InMemoryFilteredListAdapter(@NonNull Context context, @NonNull InMemoryDbArrayList<E, ?> array, @LayoutRes int layoutResourceId, @Nullable InMemoryFilter<E> filter) {
		super(context, getFilteredData(array, filter), layoutResourceId, filter);
		this.mArray = array;
		array.addListener(this);
	}

	@NonNull
	protected InMemoryDbArrayList<E, ?> getDataSource() {
        return mArray;
    }

	@Override
	public void onMemoryDbChanged(AsynchronousDbHelper<E, ?> db) {
		setFilteredData(getFilteredData(mArray, filter));
	}
}
