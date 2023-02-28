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

public class IdentityWeakHashMap<K, V> extends AbstractMap<K, V> {

	private final Map<Key<K>, Node<K, V>> map = new HashMap<>();
	private final ReferenceQueue<Object> queue = new ReferenceQueue<>();

	private Map<Key<K>, Node<K, V>> map() {
		expungeStaleEntries();
		return map;
	}

	@Override
	public int size() {
		return map().size();
	}

	@Override
	public boolean containsKey(Object key) {
		return map().containsKey(queryKey(key));
	}

	@Override
	public V get(Object key) {
		Node<K, V> n = map().get(queryKey(key));
		return n != null ? n.val : null;
	}

	@Override
	public V put(K key, V value) {
		Node<K, V> n = map().get(queryKey(key));
		if (n != null) {
			return n.setValue(value);
		} else {
			n = new Node<>(key, value, queue);
			map().put(new NodeKey<>(n), n);
			return null;
		}
	}

	@Override
	public V remove(Object key) {
		Node<K, V> n = map().remove(queryKey(key));
		return n != null ? n.val : null;
	}

	@Override
	public void clear() {
		map.clear();
	}

	@SuppressWarnings("unchecked")
	private static <K> Key<K> queryKey(Object key) {
		return new Query<>((K) key);
	}

	private void expungeStaleEntries() {
		for (Object x; (x = queue.poll()) != null;) {
			synchronized (queue) {
				@SuppressWarnings("unchecked")
				Node<K, V> e = (Node<K, V>) x;
				map.remove(e.key);
			}
		}
	}

	private static class Node<K, V> extends WeakReference<K> implements Map.Entry<K, V> {

		final NodeKey<K, V> key;
		V val;

		public Node(K key, V value, ReferenceQueue<Object> queue) {
			super(key, queue);
			this.key = new NodeKey<>(this);
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

	private static interface Key<K> {
		K get();
	}

	private static class Query<K> implements Key<K> {

		private final K key;

		Query(K key) {
			this.key = Objects.requireNonNull(key);
		}

		@Override
		public K get() {
			return key;
		}

		@Override
		public int hashCode() {
			return System.identityHashCode(key);
		}

		@Override
		public boolean equals(Object other) {
			return other == this || (other instanceof Key<?> k && get() == k.get());
		}

	}

	private static class NodeKey<K, V> implements Key<K> {

		private final Node<K, V> node;
		private final int hash;

		NodeKey(Node<K, V> node) {
			this.node = node;
			hash = System.identityHashCode(node.getKey());
		}

		@Override
		public K get() {
			return node.getKey();
		}

		@Override
		public int hashCode() {
			return hash;
		}

		@Override
		public boolean equals(Object other) {
			return other == this || (other instanceof Key<?> k && get() == k.get());
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

		private final Set<Entry<Key<K>, Node<K, V>>> set = map.entrySet();

		private Set<Entry<Key<K>, Node<K, V>>> set() {
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

			Node<K, V> n = map().get(queryKey(e.getKey()));
			return n != null && Objects.equals(e.getValue(), n.val);
		}

		@Override
		public Iter<Entry<K, V>> iterator() {
			return new Iter<>() {

				final Iterator<Entry<Key<K>, Node<K, V>>> it = set().iterator();

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
