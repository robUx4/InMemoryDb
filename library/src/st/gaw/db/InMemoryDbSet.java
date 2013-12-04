package st.gaw.db;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * a basic helper class to keep the content of a flat database in an {@link List}
 * @author Steve Lhomme
 *
 * @param <E> the type of items stored in memory by the {@link InMemoryDbSet}
 * @param <S> the type of in memory storage that will be used
 */
public abstract class InMemoryDbSet<E, S extends Set<E>> extends AsynchronousDbHelper<E> implements InMemoryDbErrorHandler<E>/*, Set<E>*/ {

	private WeakReference<InMemoryDbErrorHandler<E>> mListener;

	/**
	 * @param context to use to open or create the database
	 * @param name of the database file, or null for an in-memory database
	 * @param version number of the database (starting at 1); if the database is older,
	 *     {@link #onUpgrade} will be used to upgrade the database; if the database is
	 *     newer, {@link #onDowngrade} will be used to downgrade the database
	 * @param logger the {@link Logger} to use for all logs (can be null for the default Android logs)
	 */
	protected InMemoryDbSet(Context context, String name, int version, Logger logger) {
		super(context, name, version, logger, null);
		super.setDbErrorHandler(this);
	}

	/**
	 * return the object used to the in-memory elements
	 * @return
	 */
	abstract protected S getSet();

	/**
	 * transform the {@link Cursor} into an element that can be used in memory
	 * @param c the Cursor to transform
	 * @return a formated element used in memory
	 * @see #getValuesFromData(Object, SQLiteDatabase)
	 */
	protected abstract E getDataFromCursor(Cursor c);

	/**
	 * transform the element in memory into {@link ContentValues} that can be saved in the database
	 * @param data the data to transform
	 * @return a ContentValues element with all data that can be used to restore the data later from the database
	 * @see #addCursorInMemory(Cursor)
	 */
	protected abstract ContentValues getValuesFromData(E data);

	protected void onDataCleared() {}

	@Override
	protected final ContentValues getValuesFromData(E data, SQLiteDatabase dbToFill) throws RuntimeException {
		return getValuesFromData(data); // we don't need to know the database in Lists
	};

	@Override
	public void setDbErrorHandler(InMemoryDbErrorHandler<E> listener) {
		if (listener==null)
			mListener = null;
		else
			mListener = new WeakReference<InMemoryDbErrorHandler<E>>(listener);
	}

	@Override
	protected final void addCursorInMemory(Cursor c) {
		E item = getDataFromCursor(c);
		getSet().add(item);
	}

	/**
	 * add a new element in memory (synchronous) and in the database (asynchronous)
	 * @param item to add
	 */
	public boolean add(E item) {
		if (getSet().add(item)) {
			scheduleAddOperation(item);
			return true;
		}
		return false;
	}
	
	/**
	 * replace an existing element by the same element with other values not included in {@link E#equals()}
	 * @param item
	 * @return true if the element was replaced
	 */
	public boolean replace(E item) {
		if (!getSet().contains(item))
			return false;
		getSet().remove(item);
		getSet().add(item);
		scheduleUpdateOperation(item);
		return true;
	}
	
	/**
	 * remove and item from memory (synchronous) and from the database (asynchronous)
	 * @param item
	 * @return true if the element was removed
	 */
	public boolean remove(E item) {
		if (!getSet().remove(item))
			return false;

		scheduleRemoveOperation(item);
		return true;
	}

	public boolean removeAll(Collection<E> collection) {
		if (!getSet().removeAll(collection))
			return false;
		pushModifyingTransaction();
		for (E e : collection)
			scheduleRemoveOperation(e);
		popModifyingTransaction();
		return true;
	}

	public boolean contains(E object) {
		return getSet().contains(object);
	}

	public E get(int position) {
		if (position >= getSet().size())
			return null;
		Iterator<E> it = getSet().iterator();
		while (position-- > 0 && it.hasNext())
			it.next();
		return it.next();
	}

	@Override
	protected void clearDataInMemory() {
		getSet().clear();
		super.clearDataInMemory();
		onDataCleared();
	}

	public void onAddItemFailed(AsynchronousDbHelper<E> db, E item, ContentValues values, Throwable cause) {
		// revert the failed change in memory
		remove(item);

		if (mListener!=null) {
			final InMemoryDbErrorHandler<E> listener = mListener.get(); 
			if (listener==null)
				mListener = null;
			else
				listener.onAddItemFailed(db, item, values, cause);
		}
	};

	public void onRemoveItemFailed(AsynchronousDbHelper<E> db, E item, Throwable cause) {
		// revert the failed change in memory
		add(item);

		if (mListener!=null) {
			final InMemoryDbErrorHandler<E> listener = mListener.get(); 
			if (listener==null)
				mListener = null;
			else
				listener.onRemoveItemFailed(db, item, cause);
		}
	};

	public void onUpdateItemFailed(AsynchronousDbHelper<E> db, E item, Throwable cause) {
		if (mListener!=null) {
			final InMemoryDbErrorHandler<E> listener = mListener.get(); 
			if (listener==null)
				mListener = null;
			else
				listener.onUpdateItemFailed(db, item, cause);
		}
	};

	public void onReplaceItemFailed(AsynchronousDbHelper<E> db, E original, E replacement, Throwable cause) {
		// do nothing
	};
}
