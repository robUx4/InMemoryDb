package org.gawst.asyncdb.adapter;

import java.util.List;

import org.gawst.asyncdb.AsynchronousDbHelper;
import org.gawst.asyncdb.InMemoryDbArrayList;
import org.gawst.asyncdb.InMemoryDbList;
import org.gawst.asyncdb.InMemoryDbListener;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class InMemoryFilteredListAdapter<E> extends BaseAdapter implements InMemoryDbListener<E> {

	public interface DbFilter<E> {
		List<E> getFilteredData(InMemoryDbList<E,?> source);
	}

	private final InMemoryDbArrayList<E> mArray;
	private final LayoutInflater mInflater;
	private final int layoutId;
	private final DbFilter<E> filter;
	private UIHandler uiHandler;
	private List<E> mData;

	public InMemoryFilteredListAdapter(Context context, InMemoryDbArrayList<E> array, int layoutResourceId, DbFilter<E> filter) {
		this.mArray = array;
		this.mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.layoutId = layoutResourceId;
		this.filter = filter;
		mData = filter.getFilteredData(mArray);
		mArray.addListener(this);
	}

    protected InMemoryDbArrayList<E> getDataSource() {
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
