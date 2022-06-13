package com.ugav.battalion;

interface DataListener<E extends DataEvent> {

	void onChange(E event);

}
