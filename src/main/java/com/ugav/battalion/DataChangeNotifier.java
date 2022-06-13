package com.ugav.battalion;

import java.util.ArrayList;
import java.util.List;

class DataChangeNotifier<E extends DataEvent> {

	private final List<DataListener<? super E>> listeners;

	DataChangeNotifier() {
		listeners = new ArrayList<>();
	}

	void addListener(DataListener<? super E> listener) {
		listeners.add(listener);
	}

	void removeListener(DataListener<? super E> listener) {
		listeners.remove(listener);
	}

	void notify(E event) {
		for (DataListener<? super E> listener : listeners)
			listener.onChange(event);
	}

	void clear() {
		listeners.clear();
	}

}
