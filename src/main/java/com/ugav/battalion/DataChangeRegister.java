package com.ugav.battalion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class DataChangeRegister {

	private final Map<DataChangeNotifier<?>, List<DataListener<?>>> listeners;

	DataChangeRegister() {
		listeners = new HashMap<>();
	}

	<E extends DataEvent> void register(DataChangeNotifier<E> notifier, DataListener<? super E> listener) {
		notifier.addListener(listener);
		listeners.computeIfAbsent(notifier, k -> new ArrayList<>()).add(listener);
	}

	<E extends DataEvent> void unregister(DataChangeNotifier<E> notifier, DataListener<? super E> listener) {
		notifier.removeListener(listener);

		List<DataListener<?>> l = listeners.get(notifier);
		if (l == null || !l.remove(listener))
			throw new IllegalStateException();
		if (l.isEmpty())
			listeners.remove(notifier);
	}

	<E extends DataEvent> void unregisterAll(DataChangeNotifier<E> notifier) {
		@SuppressWarnings({ "unchecked", "rawtypes" })
		List<DataListener<? super E>> l = (List) listeners.get(notifier);
		for (DataListener<? super E> listener : l)
			notifier.removeListener(listener);
		l.clear();
		listeners.remove(notifier);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	void unregisterAll() {
		for (Map.Entry<DataChangeNotifier<?>, List<DataListener<?>>> entry : listeners.entrySet()) {
			for (DataListener listener : entry.getValue())
				entry.getKey().removeListener(listener);
			entry.getValue().clear();
		}
		listeners.clear();
	}

}
