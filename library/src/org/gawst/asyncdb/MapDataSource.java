package org.gawst.asyncdb;

/**
 * Interface that defines sources that handle a Key/Value pair to store in a regular {@link org.gawst.asyncdb.DataSource}
 *
 * @param <K> Type of the Key handled the {@link org.gawst.asyncdb.DataSource}.
 * @param <V> Type of the Value handled the {@link org.gawst.asyncdb.DataSource}.
 * @author Created by robUx4 on 12/31/2014.
 */
public interface MapDataSource<K, V, INSERT_ID> extends DataSource<MapEntry<K, V>, INSERT_ID> {
}
