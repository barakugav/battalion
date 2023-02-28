package com.ugav.battalion.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class Event {

	public final Object source;

	public Event(Object source) {
		this.source = source;
	}

	public static interface Listener<E extends Event> {

		public void onEvent(E event);

	}

	public static class Notifier<E extends Event> {

		private final List<Listener<? super E>> listeners;

		public Notifier() {
			listeners = new CopyOnWriteArrayList<>();
		}

		public void addListener(Listener<? super E> listener) {
			listeners.add(listener);
		}

		public void removeListener(Listener<? super E> listener) {
			listeners.remove(listener);
		}

		public void notify(E event) {
			for (Listener<? super E> listener : listeners)
				listener.onEvent(event);
		}

		public void clear() {
			listeners.clear();
		}

	}

	public static class Register {

		private final Map<Notifier<?>, List<Listener<?>>> listeners;

		public Register() {
			listeners = new HashMap<>();
		}

		public <E extends Event> void register(Notifier<E> notifier, Listener<? super E> listener) {
			notifier.addListener(listener);
			listeners.computeIfAbsent(notifier, k -> new ArrayList<>()).add(listener);
		}

		public <E extends Event> void unregister(Notifier<E> notifier, Listener<? super E> listener) {
			notifier.removeListener(listener);

			List<Listener<?>> l = listeners.get(notifier);
			if (l == null || !l.remove(listener))
				throw new IllegalStateException();
			if (l.isEmpty())
				listeners.remove(notifier);
		}

		public <E extends Event> void unregisterAll(Notifier<E> notifier) {
			@SuppressWarnings({ "unchecked", "rawtypes" })
			List<Listener<? super E>> l = (List) listeners.get(notifier);
			for (Listener<? super E> listener : l)
				notifier.removeListener(listener);
			l.clear();
			listeners.remove(notifier);
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		public void unregisterAll() {
			for (Map.Entry<Notifier<?>, List<Listener<?>>> entry : listeners.entrySet()) {
				for (Listener listener : entry.getValue())
					entry.getKey().removeListener(listener);
				entry.getValue().clear();
			}
			listeners.clear();
		}

	}

}
