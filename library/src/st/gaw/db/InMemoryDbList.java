package st.gaw.db;

import java.lang.ref.WeakReference;
import java.util.List;

import android.content.Context;
import android.database.Cursor;

/**
 * a basic helper class to keep the content of a flat database in an {@link List}
 * @author Steve Lhomme
 *
 * @param <E> the type of items stored in memory by the {@link InMemoryDbList}
 * @param <L> the type of in memory storage that will be used
 */
public abstract class InMemoryDbList<E, L extends List<E>> extends InMemoryDbHelper<E> implements InMemoryDbErrorHandler<E> {

	private WeakReference<InMemoryDbErrorHandler<E>> mListener;

	/**
	 * @param context to use to open or create the database
	 * @param name of the database file, or null for an in-memory database
	 * @param version number of the database (starting at 1); if the database is older,
	 *     {@link #onUpgrade} will be used to upgrade the database; if the database is
	 *     newer, {@link #onDowngrade} will be used to downgrade the database
	 */
	protected InMemoryDbList(Context context, String name, int version) {
		super(context, name, version);
		super.setDbListener(this);
	}

	/**
	 * return the object used to the in-memory elements
	 * @return
	 */
	abstract protected L getList();

	/**
	 * transform the {@link Cursor} into an element that can be used in memory
	 * @param c the Cursor to transform
	 * @return a formated element used in memory
	 * @see #getValuesFromData(Object)
	 */
	protected abstract E getDataFromCursor(Cursor c);

	
	@Override
	public void setDbListener(InMemoryDbErrorHandler<E> listener) {
		if (listener==null)
			mListener = null;
		else
			mListener = new WeakReference<InMemoryDbErrorHandler<E>>(listener);
	}
	
	@Override
	protected final void addCursorInMemory(Cursor c) {
		E item = getDataFromCursor(c);
		getList().add(item);
	}

	@Override
	protected void startLoadingInMemory() {
		getList().clear();
		super.startLoadingInMemory();
	}

	/**
	 * add a new element in memory (synchronous) and in the database (asynchronous)
	 * @param item to add
	 */
	public void addItem(E item) {
		if (!getList().contains(item)) {
			getList().add(item);
			scheduleAddOperation(item);
		}
	}

	/**
	 * remove and item from memory (synchronous) and from the database (asynchronous)
	 * @param item
	 * @return true if the element was removed
	 */
	public boolean removeItem(E item) {
		if (!getList().remove(item))
			return false;

		scheduleRemoveOperation(item);
		return true;
	}

	public void onAddItemFailed(InMemoryDbHelper<E> db, E item, Throwable cause) {
		removeItem(item);
		if (mListener!=null) {
			final InMemoryDbErrorHandler<E> listener = mListener.get(); 
			if (listener==null)
				mListener = null;
			else
				listener.onAddItemFailed(db, item, cause);
		}
	};

	public void onRemoveItemFailed(InMemoryDbHelper<E> db, E item, Throwable cause) {
		addItem(item);
		if (mListener!=null) {
			final InMemoryDbErrorHandler<E> listener = mListener.get(); 
			if (listener==null)
				mListener = null;
			else
				listener.onRemoveItemFailed(db, item, cause);
		}
	};

}
