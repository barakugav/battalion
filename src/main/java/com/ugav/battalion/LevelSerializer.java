package com.ugav.battalion;

interface LevelSerializer {

	void levelWrite(Level level, String outpath);

	Level levelRead(String path);

	String getFileType();

}
