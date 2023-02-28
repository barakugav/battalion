package com.ugav.battalion.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.ugav.battalion.util.Utils.Holder;

public class IdentityWeakHashMap<K, V> extends AbstractMap<K, V> {

	private final Map<Integer, Node<K, V>> map = new HashMap<>();
	private final ReferenceQueue<Object> queue = new ReferenceQueue<>();

	private Map<Integer, Node<K, V>> map() {
		expungeStaleEntries();
		return map;
	}

	@Override
	public int size() {
		return map().size();
	}

	@Override
	public boolean containsKey(Object key) {
		return map().containsKey(keyOf(key));
	}

	@Override
	public V get(Object key) {
		Node<K, V> n = map().get(keyOf(key));
		return n != null ? n.val : null;
	}

	@Override
	public V put(K key, V value) {
		Holder<V> oldVal = new Holder<>();
		map().compute(keyOf(key), (k, n) -> {
			if (n == null) {
				n = new Node<>(key, value, queue);
			} else {
				oldVal.val = n.val;
				n.val = value;
			}
			return n;
		});
		return oldVal.val;
	}

	@Override
	public V remove(Object key) {
		Node<K, V> n = map().remove(keyOf(key));
		return n != null ? n.val : null;
	}

	@Override
	public void clear() {
		map.clear();
	}

	private static Integer keyOf(Object key) {
		return Integer.valueOf(System.identityHashCode(key));
	}

	private void expungeStaleEntries() {
		for (Object x; (x = queue.poll()) != null;) {
			synchronized (queue) {
				@SuppressWarnings("unchecked")
				Node<K, V> e = (Node<K, V>) x;
				map.remove(e.identityKey);
			}
		}
	}

	private static class Node<K, V> extends WeakReference<K> implements Map.Entry<K, V> {

		final Integer identityKey;
		V val;

		public Node(K key, V value, ReferenceQueue<Object> queue) {
			super(key, queue);
			identityKey = keyOf(key);
			val = value;
		}

		@Override
		public K getKey() {
			return get();
		}

		@Override
		public V getValue() {
			return val;
		}

		@Override
		public V setValue(V value) {
			V old = val;
			val = value;
			return old;
		}

	}

	private EntrySet entrySet;

	@Override
	public Set<Entry<K, V>> entrySet() {
		if (entrySet == null)
			entrySet = new EntrySet();
		return entrySet;
	}

	private class EntrySet extends AbstractSet<Entry<K, V>> {

		private final Set<Entry<Integer, Node<K, V>>> set = map.entrySet();

		private Set<Entry<Integer, Node<K, V>>> set() {
			expungeStaleEntries();
			return set;
		}

		@Override
		public int size() {
			return set().size();
		}

		@Override
		public boolean contains(Object o) {
			if (!(o instanceof Map.Entry<?, ?>))
				return false;
			Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;

			Node<K, V> n = map().get(keyOf(e.getKey()));
			return n != null && Objects.equals(e.getValue(), n.val);
		}

		@Override
		public Iter<Entry<K, V>> iterator() {
			return new Iter<>() {

				final Iterator<Entry<Integer, Node<K, V>>> it = set().iterator();

				@Override
				public boolean hasNext() {
					return it.hasNext();
				}

				@Override
				public Entry<K, V> next() {
					return it.next().getValue();
				}

			};
		}

		@Override
		public void clear() {
			IdentityWeakHashMap.this.clear();
		}

	}

}
