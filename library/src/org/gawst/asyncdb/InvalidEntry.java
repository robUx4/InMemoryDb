package org.gawst.asyncdb;

/**
 * Interface to access the invalid data detected and thrown with {@link org.gawst.asyncdb.InvalidDbEntry}
 *
 * @author Created by Steve Lhomme on 05/01/2015.
 */
public interface InvalidEntry {
	/**
	 * @return the list of arguments to pass to the {@link org.gawst.asyncdb.DatabaseSource#delete(String, String[])
	 * DatabaseSource.delete()} when removing this invalid element.
	 * @see org.gawst.asyncdb.DatabaseElementHandler#getItemSelectArgs(Object)
	 */
	String[] getSelectArgs();
}
