package org.gawst.asyncdb.adapter;

import org.gawst.asyncdb.AsynchronousDbHelper;
import org.gawst.asyncdb.InMemoryDbListener;
import org.gawst.asyncdb.InMemoryDbTreeSet;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class InMemoryTreeSetAdapter<E> extends BaseAdapter implements InMemoryDbListener<E> {

	private final InMemoryDbTreeSet<E, ?> mArray;
	private final LayoutInflater mInflater;
	private final int layoutId;
	private UIHandler uiHandler;
	
	public InMemoryTreeSetAdapter(Context context, InMemoryDbTreeSet<E, ?> array, int layoutResourceId) {
		this.mArray = array;
		this.mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.layoutId = layoutResourceId;
		mArray.addListener(this);
	}

	@Override
	public int getCount() {
		return mArray.size();
	}

	@Override
	public Object getItem(int position) {
		return mArray.get(position);
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}
	
	@Override
	public long getItemId(int position) {
		return mArray.get(position).hashCode();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView==null)
			convertView = mInflater.inflate(layoutId, parent, false);

		TextView vt = (TextView) convertView.findViewById(android.R.id.text1);
		vt.setText(mArray.get(position).toString());
		
		return convertView;
	}

	@Override
	public void onMemoryDbChanged(AsynchronousDbHelper<E, ?> db) {
		Runnable runner = new Runnable() {
			@Override
			public void run() {
				notifyDataSetChanged();
			}
		};

		if (null==uiHandler)
			uiHandler = new UIHandler();
		uiHandler.runOnUiThread(runner);
	}
}
