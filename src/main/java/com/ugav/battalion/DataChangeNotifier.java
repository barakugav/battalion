package com.ugav.battalion;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class DataChangeNotifier<E extends DataEvent> {

	private final List<DataListener<? super E>> listeners;

	public DataChangeNotifier() {
		listeners = new CopyOnWriteArrayList<>();
	}

	public void addListener(DataListener<? super E> listener) {
		listeners.add(listener);
	}

	public void removeListener(DataListener<? super E> listener) {
		listeners.remove(listener);
	}

	public void notify(E event) {
		for (DataListener<? super E> listener : listeners)
			listener.onChange(event);
	}

	public void clear() {
		listeners.clear();
	}

}
