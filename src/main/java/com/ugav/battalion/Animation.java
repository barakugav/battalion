package com.ugav.battalion;

import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

class Animation {

	private final Collection<AnimateTask> tasks;
	private final Timer timer;

	private boolean isAnimationRunning = false;

	final DataChangeNotifier<DataEvent> onAnimationBegin = new DataChangeNotifier<>();
	final DataChangeNotifier<DataEvent> onAnimationEnd = new DataChangeNotifier<>();
	final DataChangeNotifier<DataEvent> onAnimationStep = new DataChangeNotifier<>();

	private static final int AnimationFPS = 120;

	Animation() {
		tasks = new ConcurrentLinkedQueue<>();

		timer = new Timer(1000 / AnimationFPS, null);
		timer.addActionListener(e -> {
			boolean animated = false;

			for (Iterator<AnimateTask> it = tasks.iterator(); it.hasNext();) {
				AnimateTask task = it.next();
				animated = true;
				if (!task.obj.advanceAnimationStep()) {
					it.remove();
					if (task.future != null)
						task.future.run();
					synchronized (task) {
						task.notify();
					}
				}
			}

			if (animated && !isAnimationRunning) {
				isAnimationRunning = true;
				onAnimationBegin.notify(new DataEvent(Animation.this));
			}
			if (animated) {
				onAnimationStep.notify(new DataEvent(Animation.this));
			}
			if (!animated && isAnimationRunning) {
				isAnimationRunning = false;
				onAnimationEnd.notify(new DataEvent(Animation.this));
			}
		});
		timer.setRepeats(true);

	}

	void start() {
		timer.start();
	}

	void stop() {
		timer.stop();
	}

	void animate(Animated animatedObj) {
		animate(animatedObj, false, null);
	}

	void animateAndWait(Animated animatedObj, Runnable future) {
		animate(animatedObj, true, future);
	}

	private void animate(Animated animatedObj, boolean wait, Runnable future) {
		AnimateTask task = new AnimateTask(animatedObj, future);
		if (wait) {
			if (SwingUtilities.isEventDispatchThread())
				throw new IllegalStateException("Can't wait for animation from GUI thread");

			try {
				synchronized (task) {
					tasks.add(task);
					task.wait();
				}
			} catch (InterruptedException e) {
				throw new RuntimeException("Thread interrupted while waiting for animation", e);
			}
		} else {

			tasks.add(task);
		}
	}

	static interface Animated {

		boolean advanceAnimationStep();

	}

	private static class AnimateTask {
		final Animated obj;
		final Runnable future;

		AnimateTask(Animated obj, Runnable future) {
			this.obj = Objects.requireNonNull(obj);
			this.future = future;
		}
	}

}