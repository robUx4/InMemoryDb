package org.gawst.asyncdb.adapter;

import org.gawst.asyncdb.AsynchronousDbHelper;
import org.gawst.asyncdb.InMemoryDbListener;
import org.gawst.asyncdb.InMemoryDbSet;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * A {@link android.widget.ListAdapter ListAdapter} that shows only a subset of elements in a {@link InMemoryDbSet}
 * using the {@link org.gawst.asyncdb.adapter.InMemoryFilteredAdapter.InMemoryFilter}
 *
 * @param <E> Type of the elements stored in the {@link InMemoryDbSet}
 */
public class InMemoryFilteredTreeAdapter<E> extends InMemoryFilteredAdapter<E> implements InMemoryDbListener<E> {

	@NonNull
	private final InMemoryDbSet<E, ?, ?> mArray;

	/**
	 * Constructor for the adapter.
	 *
	 * @param context          {@code Context} used to get the layout inflater.
	 * @param array            The data source, monitored for changes.
	 * @param layoutResourceId The layout to inflate to display an element. It must contain a {@code TextView} element with id
	 *                         {@code android.R.id.text1} unless you inflate the {@code View} yourself.
	 * @param filter           Filter to apply to the data source before it's displayed.
	 */
	public InMemoryFilteredTreeAdapter(@NonNull Context context, @NonNull InMemoryDbSet<E, ?, ?> array, @LayoutRes int layoutResourceId, @Nullable InMemoryFilter<E> filter) {
		super(context, getFilteredData(array, filter), layoutResourceId, filter);
		this.mArray = array;
		mArray.addListener(this);
	}

	@NonNull
	protected InMemoryDbSet<E, ?, ?> getDataSource() {
		return mArray;
	}

	@Override
	public void onMemoryDbChanged(AsynchronousDbHelper<E, ?> db) {
		setFilteredData(getFilteredData(mArray, filter));
	}
}
