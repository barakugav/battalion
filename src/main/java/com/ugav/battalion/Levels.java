package com.ugav.battalion;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.ugav.battalion.core.Level;

class Levels {

	private final Map<String, Level> levels;
	private final List<String> campaign;

	Levels(LevelSerializer levelSerializer) {
		levels = new HashMap<>();
		levels.put("Level 01", levelSerializer.levelRead("level/level01.xml"));
		levels.put("Level 02", levelSerializer.levelRead("level/level02.xml"));
		levels.put("Level 03", levelSerializer.levelRead("level/level03.xml"));
		levels.put("Level 04", levelSerializer.levelRead("level/level04.xml"));
		levels.put("Level 05", levelSerializer.levelRead("level/level05.xml"));
		levels.put("Level 06", levelSerializer.levelRead("level/level06.xml"));
		levels.put("Level 07", levelSerializer.levelRead("level/level07.xml"));
		levels.put("Level 08", levelSerializer.levelRead("level/level08.xml"));
		levels.put("Level 09", levelSerializer.levelRead("level/level09.xml"));
		levels.put("Level 10", levelSerializer.levelRead("level/level10.xml"));
		levels.put("Bonus Level", levelSerializer.levelRead("level/bonus_level.xml"));

		campaign = new ArrayList<>();
		campaign.add("Level 01");
		campaign.add("Level 02");
		campaign.add("Level 03");
		campaign.add("Level 04");
		campaign.add("Level 05");
		campaign.add("Level 06");
		campaign.add("Level 07");
		campaign.add("Level 08");
		campaign.add("Level 09");
		campaign.add("Level 10");
	}

	Level getLevel(String name) {
		return Objects.requireNonNull(levels.get(name));
	}

	List<Pair<String, Level>> getLevels() {
		List<Pair<String, Level>> lvls = new ArrayList<>();
		for (String lvl : campaign)
			lvls.add(Pair.of(lvl.toString(), levels.get(lvl)));
		return lvls;
	}

	static JFileChooser createFileChooser(String fileType, String dirPath) {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileFilter(new FileNameExtensionFilter("Level file (*." + fileType + ")", fileType));
		String dialogDir = (dirPath != null && (new File(dirPath).isDirectory())) ? dirPath
				: System.getProperty("user.home");
		fileChooser.setCurrentDirectory(new File(dialogDir));
		return fileChooser;
	}

}
