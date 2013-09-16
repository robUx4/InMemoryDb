package st.gaw.db.adapter;

import java.util.List;

import st.gaw.db.AsynchronousDbHelper;
import st.gaw.db.InMemoryDbArrayList;
import st.gaw.db.InMemoryDbList;
import st.gaw.db.InMemoryDbListener;
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
	private final UIHandler uiHandler;
	private List<E> mData;

	public InMemoryFilteredListAdapter(Context context, UIHandler uiHandler, InMemoryDbArrayList<E> array, int layoutResourceId, DbFilter<E> filter) {
		this.mArray = array;
		this.mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.layoutId = layoutResourceId;
		this.filter = filter;
		this.uiHandler = uiHandler;
		mData = filter.getFilteredData(mArray);
		mArray.addListener(this);
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

		if (uiHandler!=null)
			uiHandler.runOnUiThread(runner);
		else
			runner.run();
	}
}
