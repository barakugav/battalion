package com.bugav.battalion;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.bugav.battalion.core.Level;

class Levels {

	private final Map<String, LevelHandle> levels;
	private final List<String> campaign;

	Levels(LevelSerializer levelSerializer) {
		levels = new HashMap<>();
		campaign = new ArrayList<>();
		BiConsumer<String, String> loadCampaign = (name, path) -> {
			int campaignIdx = campaign.size();
			levels.put(name, new LevelHandle(name, () -> levelSerializer.levelRead(path), campaignIdx));
			campaign.add(name);
		};
		BiConsumer<String, String> loadNonCampaign = (name, path) -> {
			levels.put(name, new LevelHandle(name, () -> levelSerializer.levelRead(path), -1));
		};
		loadCampaign.accept("Level 01", "level/level01.xml");
		loadCampaign.accept("Level 02", "level/level02.xml");
		loadCampaign.accept("Level 03", "level/level03.xml");
		loadCampaign.accept("Level 04", "level/level04.xml");
		loadCampaign.accept("Level 05", "level/level05.xml");
		loadCampaign.accept("Level 06", "level/level06.xml");
		loadCampaign.accept("Level 07", "level/level07.xml");
		loadCampaign.accept("Level 08", "level/level08.xml");
		loadCampaign.accept("Level 09", "level/level09.xml");
		loadCampaign.accept("Level 10", "level/level10.xml");
		loadNonCampaign.accept("Bonus Level", "level/bonus_level.xml");

//		List<String> p = List.of("animated01.xml", "bonus_level.xml", "level01.xml", "level02.xml", "level03.xml",
//				"level04.xml", "level05.xml", "level06.xml", "level07.xml", "level08.xml", "level09.xml", "level10.xml",
//				"test_conquer.xml", "test_game_end.xml", "test_unit_menu.xml", "test_units.xml");
//		for (String name : p) {
//			String path = "level/" + name;
//			new LevelSerializerXML().levelWrite(new LevelSerializerXML2().levelRead(path), path);
//		}
//		System.out.println("done");
	}

	LevelHandle getLevel(String name) {
		return Objects.requireNonNull(levels.get(name));
	}

	List<LevelHandle> getCampaign() {
		List<LevelHandle> lvls = new ArrayList<>(campaign.size());
		for (String lvl : campaign)
			lvls.add(levels.get(lvl));
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

	static class LevelHandle {
		final String name;
		final Supplier<Level> level;
		final int campaignIdx;

		LevelHandle(String name, Supplier<Level> level, int campaignIdx) {
			this.name = name;
			this.level = level;
			this.campaignIdx = campaignIdx;
		}

		boolean isCampaignLevel() {
			return campaignIdx >= 0;
		}
	}

}
