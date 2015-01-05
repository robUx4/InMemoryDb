package org.gawst.asyncdb;

/**
 * Interface to access the invalid data detected and thrown with {@link org.gawst.asyncdb.InvalidDbEntry}
 * Created by Steve Lhomme on 05/01/2015.
 */
public interface InvalidEntry {
	String[] getSelectArgs();
}
