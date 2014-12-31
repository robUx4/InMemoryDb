package org.gawst.asyncdb;

import android.content.ContentValues;

/**
* Created by robUx4 on 12/31/2014.
*/
public interface MapDataSource<K, V> extends DataSource<MapEntry<K, V>> {

	boolean updateByKey(K key, ContentValues updateValues);

	boolean deleteByKey(K key);
}
