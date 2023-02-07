package com.ugav.battalion;

import java.util.Objects;

class Globals {

	final GameFrame frame;
	final LevelSerializer levelSerializer;

	Globals(GameFrame frame) {
		this.frame = Objects.requireNonNull(frame);
		levelSerializer = new LevelSerializerXML();
	}

}
