package st.gaw.db;

import java.util.concurrent.CopyOnWriteArrayList;

import android.content.Context;

/**
 * a basic helper class to keep the content of a flat database in an {@link CopyOnWriteArrayList}
 * @author Steve Lhomme
 *
 * @param <E> the type of items stored in memory by the {@link InMemoryDbCopyOnWriteArrayList}
 */
public abstract class InMemoryDbCopyOnWriteArrayList<E> extends InMemoryDbList<E, CopyOnWriteArrayList<E>> {

	/**
	 * the array where the data are stored, locked when writing on it
	 */
	private final CopyOnWriteArrayList<E> mData = new CopyOnWriteArrayList<E>();

	/**
	 * @param context to use to open or create the database
	 * @param name of the database file, or null for an in-memory database
	 * @param version number of the database (starting at 1); if the database is older,
	 *     {@link #onUpgrade} will be used to upgrade the database; if the database is
	 *     newer, {@link #onDowngrade} will be used to downgrade the database
	 */
	protected InMemoryDbCopyOnWriteArrayList(Context context, String name, int version) {
		super(context, name, version);
	}
	
	@Override
	protected CopyOnWriteArrayList<E> getList() {
		return mData;
	}
}
