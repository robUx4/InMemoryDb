package org.gawst.asyncdb;

import android.content.ContentValues;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.List;

/**
 * a basic helper class to keep the content of a flat database in an {@link List}
 * @author Steve Lhomme
 *
 * @param <E> the type of items stored in memory by the {@link InMemoryDbList}
 * @param <L> the type of in memory storage that will be used
 */
public abstract class InMemoryDbList<E, L extends List<E>, INSERT_ID> extends AsynchronousDbHelper<E, INSERT_ID> implements AsynchronousDbErrorHandler<E>/*, List<E>*/ {

	private WeakReference<AsynchronousDbErrorHandler<E>> mListener;

	/**
	 * @param db The already created {@link android.database.sqlite.SQLiteOpenHelper} to use as storage
	 * @param name Database name for logs
	 * @param logger The {@link org.gawst.asyncdb.Logger} to use for all logs (can be null for the default Android logs)
	 * @param initCookie Cookie to pass to {@link AsynchronousDbHelper#preloadInit(Object)}
	 */
	protected InMemoryDbList(DataSource<E, INSERT_ID> db, String name, Logger logger, Object initCookie) {
		super(db, name, logger, initCookie);
		super.setDbErrorHandler(this);
	}

	/**
	 * return the object used to the in-memory elements
	 * @return
	 */
	abstract protected L getList();

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
		getList().add(item);
	}

	/**
	 * add a new element in memory (synchronous) and in the database (asynchronous)
	 * @param item to add
	 */
	public boolean add(E item) {
		if (!getList().contains(item)) {
			if (getList().add(item)) {
				scheduleAddOperation(item);
				return true;
			}
		}
		return false;
	}
	
	/**
	 * add new elements in memory (synchronous) and in the database (asynchronous)
	 * @param items to add
	 */
	public boolean addAll(Collection<? extends E> items) {
		if (getList().addAll(items)) {
			scheduleAddOperation(items);
			return true;
		}
		return false;
	}

	/**
	 * remove and item from memory (synchronous) and from the database (asynchronous)
	 * @param item
	 * @return true if the element was removed
	 */
	public boolean remove(E item) {
		if (!getList().remove(item))
			return false;

		scheduleRemoveOperation(item);
		return true;
	}

	public boolean remove(int index) {
		if (index < 0 || index >= getList().size())
			return false;
		E removedItem = getList().remove(index);
		if (removedItem==null)
			return false;

		scheduleRemoveOperation(removedItem);
		return true;
	}

	public E get(int position) {
		if (position >= getList().size())
			return null;
		return getList().get(position);
	}

	public E findItem(E similar) {
		int found = getList().indexOf(similar);
		if (found<0)
			return null;

		return getList().get(found);
	}

	public int indexOf(E similar) {
		return getList().indexOf(similar);
	}

	public void notifyItemChanged(E item) {
		int itemPos = getList().indexOf(item);
		if (itemPos>=0) {
			getList().set(itemPos, item);
			scheduleUpdateOperation(item);
		}
	}

	@Override
	protected void clearDataInMemory() {
		getList().clear();
		super.clearDataInMemory();
		onDataCleared();
	}

	public boolean replace(int index, E newData) {
		if (index < 0 || index >= getList().size())
			return false;

		E prevValue = getList().get(index); 
		getList().set(index, newData);
		scheduleReplaceOperation(prevValue, newData);
		return true;
	}

	public boolean swap(int positionA, int positionB) {
		if (positionA < 0 || positionA >= getList().size())
			return false;
		if (positionB < 0 || positionB >= getList().size())
			return false;

		E aa = getList().get(positionA);
		E bb = getList().get(positionB);
		getList().set(positionB, aa);
		getList().set(positionA, bb);

		scheduleSwapOperation(aa, bb);
		return true;
	}

	public void onAddItemFailed(AsynchronousDbHelper<E, ?> db, E item, ContentValues values, Throwable cause) {
		// revert the failed change in memory
		remove(item);

		if (mListener!=null) {
			final AsynchronousDbErrorHandler<E> listener = mListener.get();
			if (listener==null)
				mListener = null;
			else if (listener != this)
				listener.onAddItemFailed(db, item, values, cause);
		}
	}

	public void onRemoveItemFailed(AsynchronousDbHelper<E, ?> db, E item, Throwable cause) {
		// revert the failed change in memory
		add(item);

		if (mListener!=null) {
			final AsynchronousDbErrorHandler<E> listener = mListener.get();
			if (listener==null)
				mListener = null;
			else if (listener != this)
				listener.onRemoveItemFailed(db, item, cause);
		}
	}

	public void onUpdateItemFailed(AsynchronousDbHelper<E, ?> db, E item, Throwable cause) {
		if (mListener!=null) {
			final AsynchronousDbErrorHandler<E> listener = mListener.get();
			if (listener==null)
				mListener = null;
			else if (listener != this)
				listener.onUpdateItemFailed(db, item, cause);
		}
	}

	public void onReplaceItemFailed(AsynchronousDbHelper<E, ?> db, E original, E replacement, Throwable cause) {
		// revert the failed change in memory
		int prevIndex = getList().indexOf(replacement); // TODO: we may store the position somewhere
		if (prevIndex>=0)
			getList().set(prevIndex, original);

		if (mListener!=null) {
			final AsynchronousDbErrorHandler<E> listener = mListener.get();
			if (listener==null)
				mListener = null;
			else if (listener != this)
				listener.onReplaceItemFailed(db, original, replacement, cause);
		}
	}

	@Override
	public void onCorruption(AsynchronousDbHelper<E, ?> db) {
		if (mListener!=null) {
			final AsynchronousDbErrorHandler<E> listener = mListener.get();
			if (listener==null)
				mListener = null;
			else if (listener != this)
				listener.onCorruption(db);
		}
	}
}
