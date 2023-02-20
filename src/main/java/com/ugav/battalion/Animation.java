package com.ugav.battalion;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.SwingUtilities;

import com.ugav.battalion.ArenaPanelAbstract.ArenaComp;
import com.ugav.battalion.GameArenaPanel.EntityLayer.UnitComp;
import com.ugav.battalion.core.Position;
import com.ugav.battalion.core.Position.Direction;

@FunctionalInterface
interface Animation {

	boolean advanceAnimationStep();

	static class UnitMove implements Animation {

		final UnitComp comp;
		private final List<Position> path;
		private int cursor;
		private static final int StepSize = 16;

		UnitMove(UnitComp comp, List<Position> path) {
			this.comp = comp;
			this.path = Collections.unmodifiableList(new ArrayList<>(path));
		}

		@Override
		public boolean advanceAnimationStep() {
			if (cursor >= path.size() * StepSize)
				throw new NoSuchElementException();

			if (cursor == 0)
				comp.isMoving = true;

			int idx = cursor / StepSize;
			double frac = (cursor % StepSize + 1) / (double) StepSize;
			Position p1 = path.get(idx);
			Position p2 = path.get(idx + 1);
			comp.orientation = Direction.calc(p1, p2);
			double x = p1.x + (p2.x - p1.x) * frac;
			double y = p1.y + (p2.y - p1.y) * frac;
			comp.pos = Position.of(x, y);

			boolean moreToAnimation = ++cursor < (path.size() - 1) * StepSize;
			if (!moreToAnimation)
				comp.isMoving = false;
			return moreToAnimation;
		}
	}

	static class Attack implements Animation, ArenaComp {
		private final ArenaPanelAbstract<?, ?, ?> arena;
		private final UnitComp comp;
		private final Position target;
		private Position basePos;
		private int cursor = 0;
		private static final int Duration = 20;

		Attack(ArenaPanelAbstract<?, ?, ?> arena, UnitComp comp, Position target) {
			this.arena = arena;
			this.comp = comp;
			this.target = target;
		}

		@Override
		public boolean advanceAnimationStep() {
			if (cursor >= Duration)
				throw new NoSuchElementException();

			if (cursor == 0) {
				basePos = comp.pos;

				Direction orientation = null;
				for (Direction dir : Direction.values())
					if (orientation == null || orientation.dist(comp.pos, target) < dir.dist(comp.pos, target))
						orientation = dir;
				comp.orientation = orientation;

				arena.entityLayer.comps.put(this, this);
			}

			double yOffset = (Math.abs(Duration / 2 - cursor) - Duration / 2) / 30.0;
			comp.pos = Position.of(basePos.x, basePos.y + yOffset);

			boolean moveToAnimate = ++cursor < Duration;
			if (!moveToAnimate) {
				arena.entityLayer.comps.remove(this);
				comp.pos = basePos;
			}
			return moveToAnimate;
		}

		@Override
		public void clear() {
		}

		@Override
		public void paintComponent(Graphics g) {
			int gestureNum = Images.getGestureNum("Attack");
			int gestureIdx = (int) (cursor / ((double) Duration / gestureNum));
			BufferedImage img = Images.getAttackImg(gestureIdx);

			Position drawPos = arena.displayedTile(target);
			g.drawImage(img, drawPos.xInt(), drawPos.yInt(), null);
		}

		@Override
		public int getZOrder() {
			return 200;
		}

		@Override
		public Position pos() {
			return comp.pos;
		}
	}

	static class UnitReappear implements Animation {
		private final UnitComp comp;
		private int cursor = 0;
		private static final int Duration = 30;

		UnitReappear(UnitComp comp) {
			this.comp = comp;
		}

		@Override
		public boolean advanceAnimationStep() {
			if (cursor >= Duration)
				throw new NoSuchElementException();
			comp.alpha = (float) cursor / Duration;
			return ++cursor < Duration;
		}
	}

	static class UnitDisappear implements Animation {
		private final UnitComp comp;
		private int cursor = 0;
		private static final int Duration = 30;

		UnitDisappear(UnitComp comp) {
			this.comp = comp;
		}

		@Override
		public boolean advanceAnimationStep() {
			if (cursor >= Duration)
				throw new NoSuchElementException();
			comp.alpha = (float) (Duration - cursor) / Duration;
			return ++cursor < Duration;
		}
	}

	static class UnitDeath implements Animation, ArenaComp {
		private final ArenaPanelAbstract<?, ?, ?> arena;
		private final UnitComp comp;
		private int cursor = 0;
		private static final int Duration = 90;

		UnitDeath(ArenaPanelAbstract<?, ?, ?> arena, UnitComp comp) {
			this.arena = arena;
			this.comp = comp;
		}

		@Override
		public boolean advanceAnimationStep() {
			if (cursor >= Duration)
				throw new NoSuchElementException();
			if (cursor == 0)
				arena.entityLayer.comps.put(this, this);

			comp.alpha = (float) (Duration - cursor) / Duration;

			boolean moreToAnimate = ++cursor < Duration;
			if (!moreToAnimate)
				arena.entityLayer.comps.remove(this);
			return moreToAnimate;
		}

		@Override
		public void clear() {
		}

		@Override
		public void paintComponent(Graphics g) {
			Position pos = comp.pos;
			int gestureNum = Images.getGestureNum("Explosion");
			int gestureIdx = (int) (cursor / ((double) Duration / gestureNum));
			BufferedImage img = Images.getExplosionImg(gestureIdx);

			Position drawPos = arena.displayedTile(pos);
			g.drawImage(img, drawPos.xInt(), drawPos.yInt(), null);
		}

		@Override
		public int getZOrder() {
			return 200;
		}

		@Override
		public Position pos() {
			return comp.pos;
		}
	}

	static Animation of(Animation... animations) {
		if (animations.length < 2)
			throw new IllegalArgumentException();
		return new Animation() {

			int index = 0;

			@Override
			public boolean advanceAnimationStep() {
				if (index >= animations.length)
					throw new NoSuchElementException();
				if (!animations[index].advanceAnimationStep())
					index++;
				return index < animations.length;
			}
		};
	}

	static class Task implements TickTask {

		private final Collection<AnimationEntry> queue = new ConcurrentLinkedQueue<>();

		private boolean isAnimationRunning = false;

		final DataChangeNotifier<DataEvent> onAnimationBegin = new DataChangeNotifier<>();
		final DataChangeNotifier<DataEvent> onAnimationEnd = new DataChangeNotifier<>();

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
			if (!animated && isAnimationRunning) {
				isAnimationRunning = false;
				onAnimationEnd.notify(new DataEvent(Task.this));
			}
		}

		void animate(Animation animation, Runnable future) {
			animate(animation, false, future);
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