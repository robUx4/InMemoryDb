package st.gaw.db;

/**
 * interface used to receive errors from the {@link InMemoryDbHelper}
 * @see InMemoryDbHelper#setDbListener(InMemoryDbErrorHandler)
 * @author Steve Lhomme
 *
 * @param <E> the type of items stored in memory by the {@link InMemoryDbHelper}
 */
public interface InMemoryDbErrorHandler<E> {

	/**
	 * called when the database failed to store the item
	 * @param db the database that tried to store
	 * @param item the data that failed to store
	 * @param cause an exception explaining why it failed
	 */
	void onAddItemFailed(InMemoryDbHelper<E> db, E item, Throwable cause);
	
	/**
	 * called when the database failed to delete an item
	 * @param db the database that tried to store
	 * @param item the data that failed to store
	 * @param cause an exception explaining why it failed
	 */
	void onRemoveItemFailed(InMemoryDbHelper<E> db, E item, Throwable cause);
	
}
