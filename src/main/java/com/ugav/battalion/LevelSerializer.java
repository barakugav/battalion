package com.ugav.battalion;

import com.ugav.battalion.core.Level;

interface LevelSerializer {

	void levelWrite(Level level, String outpath);

	Level levelRead(String path);

	String getFileType();

}
