package st.gaw.db;

public interface InMemoryDbListener<E> {
	void onMemoryDbChanged(InMemoryDbHelper<E> db);
}
