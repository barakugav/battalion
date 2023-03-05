package com.ugav.battalion;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.ugav.battalion.core.Level;
import com.ugav.battalion.util.Pair;

class Levels {

	private final Map<String, Supplier<Level>> levels;
	private final List<String> campaign;

	Levels(LevelSerializer levelSerializer) {
		levels = new HashMap<>();
		BiConsumer<String, String> load = (name, path) -> levels.put(name, () -> levelSerializer.levelRead(path));
		load.accept("Level 01", "level/level01.xml");
		load.accept("Level 02", "level/level02.xml");
		load.accept("Level 03", "level/level03.xml");
		load.accept("Level 04", "level/level04.xml");
		load.accept("Level 05", "level/level05.xml");
		load.accept("Level 06", "level/level06.xml");
		load.accept("Level 07", "level/level07.xml");
		load.accept("Level 08", "level/level08.xml");
		load.accept("Level 09", "level/level09.xml");
		load.accept("Level 10", "level/level10.xml");
		load.accept("Bonus Level", "level/bonus_level.xml");

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
		return levels.get(name).get();
	}

	List<Pair<String, Supplier<Level>>> getCampaign() {
		List<Pair<String, Supplier<Level>>> lvls = new ArrayList<>();
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
