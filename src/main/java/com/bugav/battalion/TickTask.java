package com.bugav.battalion;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.swing.Timer;

import com.bugav.battalion.util.Pair;

interface TickTask extends Clearable {

	void onTick();

	static class Manager {

		private final Timer tickTimer;
		private final List<Pair<TickTask, Integer>> tasks = new ArrayList<>();
		private static final int TickFPS = 120;

		Manager() {
			tickTimer = new Timer(1000 / TickFPS, e -> {
				for (Pair<TickTask, Integer> task : tasks)
					task.e1.onTick();
			});
			tickTimer.setRepeats(true);
		}

		void addTask(int priorary, TickTask task) {
			if (tickTimer.isRunning())
				throw new IllegalStateException();
			tasks.add(Pair.of(Objects.requireNonNull(task), Integer.valueOf(priorary)));
		}

		void start() {
			if (tickTimer.isRunning())
				return;
			tasks.sort((p1, p2) -> {
				int priorary1 = p1.e2.intValue(), priorary2 = p2.e2.intValue();
				return Integer.compare(priorary1, priorary2);
			});
			tickTimer.start();
		}

		void stop() {
			for (Pair<TickTask, Integer> task : tasks)
				task.e1.clear();
			tickTimer.stop();
		}
	}

}
