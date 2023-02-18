package com.ugav.battalion;

import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.SwingUtilities;

@FunctionalInterface
interface Animation {

	boolean advanceAnimationStep();

	static class Task implements TickTask {

		private final Collection<AnimationEntry> queue = new ConcurrentLinkedQueue<>();

		private boolean isAnimationRunning = false;

		final DataChangeNotifier<DataEvent> onAnimationBegin = new DataChangeNotifier<>();
		final DataChangeNotifier<DataEvent> onAnimationEnd = new DataChangeNotifier<>();
//		final DataChangeNotifier<DataEvent> onAnimationStep = new DataChangeNotifier<>();

		Task() {
		}

		@Override
		public void run() {
			boolean animated = false;

			for (Iterator<AnimationEntry> it = queue.iterator(); it.hasNext();) {
				AnimationEntry entry = it.next();
				animated = true;
				if (!entry.animation.advanceAnimationStep()) {
					it.remove();
					if (entry.future != null)
						entry.future.run();
					synchronized (entry) {
						entry.notify();
					}
				}
			}

			if (animated && !isAnimationRunning) {
				isAnimationRunning = true;
				onAnimationBegin.notify(new DataEvent(Task.this));
			}
//			if (animated) {
//				onAnimationStep.notify(new DataEvent(Animation.this));
//			}
			if (!animated && isAnimationRunning) {
				isAnimationRunning = false;
				onAnimationEnd.notify(new DataEvent(Task.this));
			}
		}

		void animate(Animation animation) {
			animate(animation, false, null);
		}

		void animateAndWait(Animation animation, Runnable future) {
			animate(animation, true, future);
		}

		private void animate(Animation animation, boolean wait, Runnable future) {
			AnimationEntry entry = new AnimationEntry(animation, future);
			if (wait) {
				if (SwingUtilities.isEventDispatchThread())
					throw new IllegalStateException("Can't wait for animation from GUI thread");

				try {
					synchronized (entry) {
						queue.add(entry);
						entry.wait();
					}
				} catch (InterruptedException e) {
					throw new RuntimeException("Thread interrupted while waiting for animation", e);
				}
			} else {

				queue.add(entry);
			}
		}

		private static class AnimationEntry {
			final Animation animation;
			final Runnable future;

			AnimationEntry(Animation animation, Runnable future) {
				this.animation = Objects.requireNonNull(animation);
				this.future = future;
			}
		}

	}

}