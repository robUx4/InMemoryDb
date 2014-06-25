package org.gawst.asyncdb.adapter;

import java.util.List;

import org.gawst.asyncdb.AsynchronousDbHelper;
import org.gawst.asyncdb.InMemoryDbListener;
import org.gawst.asyncdb.InMemoryDbSet;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

/**
 * adapter that presents only a subset of elements in a {@link InMemoryDbSet} using a {@link DbFilter}
 * 
 * @param <E>
 */
public class InMemoryFilteredTreeAdapter<E> extends BaseAdapter implements InMemoryDbListener<E> {

	public interface DbFilter<E> {
		List<E> getFilteredData(InMemoryDbSet<E,?> source);
	}

	private final InMemoryDbSet<E, ?> mArray;
	private final LayoutInflater mInflater;
	private final int layoutId;
	private final DbFilter<E> filter;
	private UIHandler uiHandler;
	private List<E> mData;

	public InMemoryFilteredTreeAdapter(Context context, InMemoryDbSet<E, ?> array, int layoutResourceId, DbFilter<E> filter) {
		this.mArray = array;
		this.mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.layoutId = layoutResourceId;
		this.filter = filter;
		mData = filter.getFilteredData(mArray);
		mArray.addListener(this);
	}
	
	protected InMemoryDbSet<E, ?> getDataSource() {
		return mArray;
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

	@Override
	public void onMemoryDbChanged(AsynchronousDbHelper<E> db) {
		final List<E> newData = filter.getFilteredData(mArray);
		Runnable runner = new Runnable() {
			@Override
			public void run() {
				mData = newData;
				notifyDataSetChanged();
			}
		};

		if (null==uiHandler)
			uiHandler = new UIHandler();
		uiHandler.runOnUiThread(runner);
	}
}
