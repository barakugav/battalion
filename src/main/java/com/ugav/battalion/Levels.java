package com.ugav.battalion;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

class Levels {

	private final Globals globals;
	private final Map<Label, Level> levels;
	private final List<Label> campaign;

	static enum Label {
		GraphicTest, TeamTest,

		BonusLevel,
	}

	Levels(Globals globals) {
		this.globals = Objects.requireNonNull(globals);
		levels = new HashMap<>();
		levels.put(Label.BonusLevel, this.globals.levelSerializer.levelRead("level/bonus_level.xml"));

		campaign = new ArrayList<>();
//		campaign.add(Label.GraphicTest);
//		campaign.add(Label.TeamTest);
//		campaign.add(Label.Level1);
		campaign.add(Label.BonusLevel);
	}

	List<Pair<String, Level>> getLevels() {
		List<Pair<String, Level>> lvls = new ArrayList<>();
		for (Label lvl : campaign)
			lvls.add(Pair.of(lvl.toString(), levels.get(lvl)));
		return lvls;
	}

	static JFileChooser createFileChooser(String fileType) {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileFilter(new FileNameExtensionFilter("Level file (*." + fileType + ")", fileType));
		String dialogDir = Cookies.getCookieValue(Cookies.LEVEL_DISK_LAST_DIR);
		if (dialogDir == null || !(new File(dialogDir).isDirectory()))
			dialogDir = System.getProperty("user.home");
		fileChooser.setCurrentDirectory(new File(dialogDir));
		return fileChooser;
	}

}
