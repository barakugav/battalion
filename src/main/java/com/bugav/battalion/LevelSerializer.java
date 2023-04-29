package com.bugav.battalion;

import com.bugav.battalion.core.Level;

interface LevelSerializer {

	void levelWrite(Level level, String outpath);

	Level levelRead(String path);

	String getFileType();

}
