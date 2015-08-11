package com.bombardier.plugin.testingplugin.misc;

import java.util.Map;

/**
 * Used to create a custom {@link Entry}
 * 
 * @author <a href="mailto:samuil.dragnev@gmail.com">Samuil Dragnev</a>
 * @since 1.0
 * 
 * @param <K>
 *            the key
 * @param <V>
 *            the value
 */

public final class GenericEntry<K, V> implements Map.Entry<K, V> {
	private final K key;
	private V value;

	public GenericEntry(K key, V value) {
		this.key = key;
		this.value = value;
	}

	@Override
	public V setValue(V arg0) {
		return value = arg0;
	}

	@Override
	public V getValue() {
		return value;
	}

	@Override
	public K getKey() {
		return key;
	}
}