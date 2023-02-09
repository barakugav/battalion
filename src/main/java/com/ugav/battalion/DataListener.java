package com.ugav.battalion;

public interface DataListener<E extends DataEvent> {

	public void onEvent(E event);

}
