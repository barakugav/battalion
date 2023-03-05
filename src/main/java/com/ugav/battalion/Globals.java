package com.ugav.battalion;

import java.util.Objects;

class Globals {

	final GameFrame frame;
	final LevelSerializer levelSerializer;
	final Levels levels;
	final Debug debug = new Debug();

	Globals(GameFrame frame) {
		this.frame = Objects.requireNonNull(frame);
		levelSerializer = new LevelSerializerXML();
		levels = new Levels(levelSerializer);
	}

}
