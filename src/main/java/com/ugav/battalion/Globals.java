package com.ugav.battalion;

import java.util.Objects;

class Globals {

	final GameFrame frame;
	final LevelSerializer serializer;

	Globals(GameFrame frame) {
		this.frame = Objects.requireNonNull(frame);
		serializer = new LevelSerializerXML();
	}

}
