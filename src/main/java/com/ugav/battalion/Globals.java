package com.ugav.battalion;

import java.util.Objects;

import com.ugav.battalion.util.Logger;

class Globals {

	final GameFrame frame;
	final LevelSerializer levelSerializer;
	final Levels levels;
	final Debug debug = new Debug();
	final Logger logger = Logger.createDefault();

	Globals(GameFrame frame) {
		this.frame = Objects.requireNonNull(frame);
		levelSerializer = new LevelSerializerXML();
		levels = new Levels(levelSerializer);
	}

}
