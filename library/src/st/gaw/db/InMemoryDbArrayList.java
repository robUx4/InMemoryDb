package st.gaw.db;

import java.util.ArrayList;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.util.Log;

/**
 * a basic helper class to keep the content of a flat database in an {@link ArrayList}
 * @author Steve Lhomme
 *
 * @param <E> the type of items stored in memory by the {@link InMemoryDbArrayList}
 */
public abstract class InMemoryDbArrayList<E> extends InMemoryDbHelper<E> {

	/**
	 * the array where the data are stored, locked when writing on it
	 */
	protected final ArrayList<E> mData = new ArrayList<E>();

	/**
	 * @param context to use to open or create the database
	 * @param name of the database file, or null for an in-memory database
	 * @param version number of the database (starting at 1); if the database is older,
	 *     {@link #onUpgrade} will be used to upgrade the database; if the database is
	 *     newer, {@link #onDowngrade} will be used to downgrade the database
	 */
	protected InMemoryDbArrayList(Context context, String name, int version) {
		super(context, name, version);
	}

	@Override
	protected void loadDataInMemory() {
		synchronized (mData) {
			mData.clear();
			try {
				Cursor c = getReadableDatabase().query(getMainTableName(), null, null, null, null, null, null);
				if (c!=null)
					try {
						if (c.moveToFirst()) {
							mData.ensureCapacity(c.getCount());
							do {
								E item = getDataFromCursor(c);
								if (item!=null)
									mData.add(item);
							} while (c.moveToNext());
						}

					} finally {
						c.close();
					}
			} catch (SQLException e) {
				Log.w(TAG,"Can't query table "+getMainTableName()+" in "+this, e);
			}
		}
	}
}
