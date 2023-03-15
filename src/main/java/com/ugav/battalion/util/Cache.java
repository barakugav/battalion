package com.ugav.battalion.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public interface Cache<K, V> {

	V getOrCompute(K key, Function<? super K, ? extends V> compute);

	public static class FixSize<K, V> implements Cache<K, V> {

		private final Map<K, V> map;

		public FixSize(int maxSize) {
			map = new LinkedHashMap<>() {
				private static final long serialVersionUID = 1L;

				protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
					return map.size() > maxSize;
				}
			};
		}

		@Override
		public V getOrCompute(K key, Function<? super K, ? extends V> compute) {
			return map.computeIfAbsent(key, compute);
		}

	}

}
