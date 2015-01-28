package org.gawst.asyncdb;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.Nullable;

import org.gawst.asyncdb.adapter.InMemoryFilteredAdapter;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * a basic helper class to keep the content of a flat database in an {@link List}
 * @author Steve Lhomme
 *
 * @param <E> the type of items stored in memory by the {@link InMemoryDbSet}
 * @param <S> the type of in memory storage that will be used
 */
public abstract class InMemoryDbSet<E, S extends Set<E>, INSERT_ID> extends AsynchronousDbHelper<E, INSERT_ID> implements AsynchronousDbErrorHandler<E>, InMemoryFilteredAdapter.InMemoryFilter.InMemoryFilterable<E>/*, Set<E>*/ {

	private WeakReference<AsynchronousDbErrorHandler<E>> mListener;

	/**
	 * @param db The already created {@link android.database.sqlite.SQLiteOpenHelper} to use as storage
	 * @param name Database name for logs
	 * @param logger The {@link org.gawst.asyncdb.Logger} to use for all logs (can be null for the default Android logs)
	 * @param initCookie Cookie to pass to {@link AsynchronousDbHelper#preloadInit(Object)}
	 */
	protected InMemoryDbSet(DataSource<E, INSERT_ID> db, String name, Logger logger, Object initCookie) {
		super(db, name, logger, initCookie);
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
	 * @see AsynchronousDbHelper#getValuesFromData(Object, boolean)
	 */
	protected abstract E getDataFromCursor(Cursor c);

	/**
	 * Called when data in memory have been cleared.
	 *
	 * @see #clear()
	 */
	protected void onDataCleared() {}

	@Override
	public void setDbErrorHandler(AsynchronousDbErrorHandler<E> listener) {
		if (listener==null)
			mListener = null;
		else
			mListener = new WeakReference<AsynchronousDbErrorHandler<E>>(listener);
	}

	@Override
	public final void addItemInMemory(E item) {
		getSet().add(item);
	}

	/**
	 * Add a new element in memory (synchronous) and in the database (asynchronous).
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
	 * Replace an existing element by the same element with other values not included in {@link E#equals(Object)}.
	 * The value is replaced in memory (synchronous) and in the database (asynchronous).
	 * @param item
	 * @return {@code true} if the element was replaced.
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
	 * Remove and item from memory (synchronous) and in the database (asynchronous).
	 *
	 * @param item
	 * @return {@code true} if the element was removed, {@code false} if the element could not be found or was {@code null}.
	 */
	public boolean remove(E item) {
		if (!getSet().remove(item))
			return false;

		scheduleRemoveOperation(item);
		return true;
	}

	/**
	 * Removes all objects in the specified collection from this set in memory (synchronous) and in the database (asynchronous).
	 *
	 * @see java.util.Set#removeAll(java.util.Collection)
	 */
	public boolean removeAll(Collection<E> collection) {
		if (!getSet().removeAll(collection))
			return false;
		pushModifyingTransaction();
		for (E e : collection)
			scheduleRemoveOperation(e);
		popModifyingTransaction();
		return true;
	}

	/**
	 * @see java.util.Set#contains(Object)
	 */
	public boolean contains(E object) {
		return getSet().contains(object);
	}

	/**
	 * Get the item at the specified position or {@code null}. It may be useful to iterate through the Set.
	 */
	@Nullable
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

	@Override
	public void onAddItemFailed(AsynchronousDbHelper<E, ?> db, E item, ContentValues values, Throwable cause) {
		// revert the failed change in memory
		remove(item);

		if (mListener!=null) {
			final AsynchronousDbErrorHandler<E> listener = mListener.get();
			if (listener==null)
				mListener = null;
			else
				listener.onAddItemFailed(db, item, values, cause);
		}
	}

	@Override
	public void onRemoveItemFailed(AsynchronousDbHelper<E, ?> db, E item, Throwable cause) {
		// revert the failed change in memory
		add(item);

		if (mListener!=null) {
			final AsynchronousDbErrorHandler<E> listener = mListener.get();
			if (listener==null)
				mListener = null;
			else
				listener.onRemoveItemFailed(db, item, cause);
		}
	}

	@Override
	public void onUpdateItemFailed(AsynchronousDbHelper<E, ?> db, E item, Throwable cause) {
		if (mListener!=null) {
			final AsynchronousDbErrorHandler<E> listener = mListener.get();
			if (listener==null)
				mListener = null;
			else
				listener.onUpdateItemFailed(db, item, cause);
		}
	}

	@Override
	public void onReplaceItemFailed(AsynchronousDbHelper<E, ?> db, E original, E replacement, Throwable cause) {
		// do nothing
	}
	
	@Override
	public void onCorruption(AsynchronousDbHelper<E, ?> db) {
		if (mListener!=null) {
			final AsynchronousDbErrorHandler<E> listener = mListener.get();
			if (listener==null)
				mListener = null;
			else
				listener.onCorruption(db);
		}
	}
}
