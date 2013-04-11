package st.gaw.db;

/**
 * interface for operations that can be run via {@link AsynchronousDbHelper#scheduleCustomOperation(InMemoryDbOperation)}
 * <p> said operation is run in the internal thread of the {@link AsynchronousDbHelper}
 */
public interface AsynchronousDbOperation<E> {
	/**
	 * callback of the operation to run in the internal thread of db
	 * @param db the database on which this operation is processed
	 */
	void runInMemoryDbOperation(AsynchronousDbHelper<E> db);
}
