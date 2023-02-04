package com.ugav.battalion;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Cookies {

	static final String LEVEL_DISK_LAST_DIR = "LEVEL_DISK_LAST_DIR";

	private Cookies() {
	}

	private static final String cookiesFilename = Utils.buildPath(System.getProperty("user.home"), ".battalion",
			"cookies.txt");
	private static Map<String, String> cookies = readCookies();
	static {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> writeCookies(cookies)));
	}

	private static Map<String, String> readCookies() {
		if (new File(cookiesFilename).isFile()) {
			try {
				Map<String, String> cookies = new HashMap<>();
				List<String> lines = Utils.readLines(cookiesFilename);
				if (lines.size() % 2 != 0)
					throw new IllegalStateException("corrupted cookies file");
				for (int i = 0; i < lines.size() / 2; i++)
					cookies.put(lines.get(i * 2), lines.get(i * 2 + 1));
				return cookies;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return new HashMap<>();
	}

	private static void writeCookies(Map<String, String> cookies) {
		for (Map.Entry<String, String> cookie : cookies.entrySet()) {
			checkString(cookie.getKey());
			checkString(cookie.getValue());
		}
		StringBuilder txt = new StringBuilder();
		for (Map.Entry<String, String> cookie : cookies.entrySet()) {
			txt.append(cookie.getKey()).append(System.lineSeparator());
			txt.append(cookie.getValue()).append(System.lineSeparator());
		}
		File cookiesFile = new File(cookiesFilename);
		try {
			if (!cookiesFile.isFile()) {
				new File(cookiesFile.getParent()).mkdirs();
				cookiesFile.createNewFile();
			}
			try (FileWriter writer = new FileWriter(cookiesFilename)) {
				writer.write(txt.toString());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static synchronized String getCookieValue(String key) {
		checkString(key);
		return cookies.get(key);
	}

	static synchronized void setCookieValue(String key, String value) {
		checkString(key);
		checkString(value);
		cookies.put(key, value);
	}

	private static void checkString(String v) {
		if (v.contains("\n") || v.contains("\r") || v.contains(System.lineSeparator()))
			throw new IllegalArgumentException("invalid value: '" + v + "'");
	}

}
