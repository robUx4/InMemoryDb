package org.gawst.asyncdb.adapter;

import java.util.List;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

/**
 * A {@link android.widget.ListAdapter ListAdapter} that only shows a subset of elements using the {@link org.gawst.asyncdb.adapter.InMemoryFilteredAdapter.InMemoryFilter}
 * @see org.gawst.asyncdb.adapter.InMemoryFilteredListAdapter
 * @see org.gawst.asyncdb.adapter.InMemoryFilteredTreeAdapter
 *
 * @author Created by robUx4 on 08/01/2015.
 */
public class InMemoryFilteredAdapter<E> extends BaseAdapter {

	protected final LayoutInflater mInflater;
	@LayoutRes
	protected final int layoutId;
	@Nullable
	protected final InMemoryFilter<E> filter;
	@NonNull
	private List<E> mData;
	private UIHandler uiHandler;

	public InMemoryFilteredAdapter(@NonNull Context context, @NonNull List<E> initialData, @LayoutRes int layoutResourceId, @Nullable InMemoryFilter<E> filter) {
		this.mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.layoutId = layoutResourceId;
		this.filter = filter;
		mData = initialData;
	}

	@Override
	public int getCount() {
		return mData.size();
	}

	@Override
	public E getItem(int position) {
		return mData.get(position);
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public long getItemId(int position) {
		if (position<mData.size())
			return mData.get(position).hashCode();
		return -1;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView==null)
			convertView = mInflater.inflate(layoutId, parent, false);

		TextView vt = (TextView) convertView.findViewById(android.R.id.text1);
		vt.setText(mData.get(position).toString());

		return convertView;
	}

	/**
	 * Helper function to filter the data from the {@link org.gawst.asyncdb.adapter.InMemoryFilteredAdapter.InMemoryFilter.InMemoryFilterable} source
	 * @param source The data source.
	 * @param filter The filter to apply to the source. May be {@code null}.
	 * @param <E> Type of elements in the source.
	 * @return A filtered read-only {@link List} of elements coming from the source.
	 */
	protected static <E> List<E> getFilteredData(@NonNull InMemoryFilter.InMemoryFilterable<E> source, @Nullable InMemoryFilter<E> filter) {
		if (null == filter)
			return source.getListCopy();
		else
			return filter.getFilteredData(source.getListCopy());
	}

	protected void setFilteredData(final List<E> filteredData) {
		Runnable runner = new Runnable() {
			@Override
			public void run() {
				mData = filteredData;
				notifyDataSetChanged();
			}
		};

		if (null==uiHandler)
			uiHandler = new UIHandler();
		uiHandler.runOnUiThread(runner);
	}

	/**
	 * Interface to filter the whole data stored in an {@link org.gawst.asyncdb.AsynchronousDbHelper}
	 */
	public static interface InMemoryFilter<E> {

		/**
		 * Describes a data source that can produce a {@link java.util.List} object from its data.
		 *
		 * @param <E> Type of elements in the generated {@link java.util.List}.
		 */
		public interface InMemoryFilterable<E> {
			/**
			 * @return A read-only {@link java.util.List} version of the source data.
			 */
			List<E> getListCopy();
		}

		/**
		 * Filter data coming from a {@link org.gawst.asyncdb.adapter.InMemoryFilteredAdapter.InMemoryFilter.InMemoryFilterable InMemoryFilterable} source
		 * @param data List of elements to filter.
		 * @return Filtered List of elements.
		 */
		@NonNull
		List<E> getFilteredData(List<E> data);
	}
}
