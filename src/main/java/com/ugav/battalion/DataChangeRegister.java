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

	<E extends DataEvent> void registerListener(DataChangeNotifier<E> notifier, DataListener<? super E> listener) {
		notifier.addListener(listener);
		listeners.computeIfAbsent(notifier, k -> new ArrayList<>()).add(listener);
	}

	<E extends DataEvent> void unregisterListener(DataChangeNotifier<E> notifier, DataListener<? super E> listener) {
		notifier.removeListener(listener);

		List<DataListener<?>> l = listeners.get(notifier);
		if (l == null || !l.remove(listener))
			throw new IllegalStateException();
		if (l.isEmpty())
			listeners.remove(notifier);
	}

	<E extends DataEvent> void unregisterAllListeners(DataChangeNotifier<E> notifier) {
		@SuppressWarnings({ "unchecked", "rawtypes" })
		List<DataListener<? super E>> l = (List) listeners.get(notifier);
		for (DataListener<? super E> listener : l)
			notifier.removeListener(listener);
		l.clear();
		listeners.remove(notifier);
	}

}
