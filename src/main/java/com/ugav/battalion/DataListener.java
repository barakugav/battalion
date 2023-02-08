package com.ugav.battalion;

public interface DataListener<E extends DataEvent> {

	public void onChange(E event);

}
