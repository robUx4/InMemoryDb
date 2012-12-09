package st.gaw.db;

public interface InMemoryDbErrorHandler<E> {

	void onAddItemFailed(InMemoryDbHelper<E> db, E item, Throwable cause);
	void onRemoveItemFailed(InMemoryDbHelper<E> db, E item, Throwable cause);
	
}
