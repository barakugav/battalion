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
import com.ugav.battalion.core.Cell;
import com.ugav.battalion.core.Direction;

@FunctionalInterface
interface Animation {

	boolean advanceAnimationStep();

	default void beforeFirst() {
	};

	default void afterLast() {
	};

	static class UnitMove implements Animation {

		final UnitComp comp;
		private final List<Cell> path;
		private int cursor;
		private static final int StepSize = 16;

		UnitMove(UnitComp comp, List<Cell> path) {
			if (path.isEmpty())
				throw new IllegalArgumentException();
			this.comp = Objects.requireNonNull(comp);
			this.path = Collections.unmodifiableList(new ArrayList<>(path));
		}

		@Override
		public void beforeFirst() {
			comp.isAnimated = comp.isMoving = true;
		}

		@Override
		public boolean advanceAnimationStep() {
			int length = path.size() - 1;
			if (length == 0)
				return false;
			if (cursor >= length * StepSize)
				throw new NoSuchElementException();

			int idx = cursor / StepSize;
			Cell p1 = path.get(idx);
			Cell p2 = path.get(idx + 1);
			comp.orientation = Cell.diffDir(p1, p2);
			double frac = (cursor % StepSize + 1) / (double) StepSize;
			double x = p1.x + (p2.x - p1.x) * frac;
			double y = p1.y + (p2.y - p1.y) * frac;
			comp.pos = Position.of(x, y);

			return ++cursor < length * StepSize;
		}

		@Override
		public void afterLast() {
			comp.isAnimated = comp.isMoving = false;
		}

	}

	static class Attack implements Animation, ArenaComp {
		private final ArenaPanelAbstract<?, ?, ?> arena;
		private final UnitComp comp;
		private final Cell target;
		private Position basePos;
		private int cursor = 0;
		private static final int Duration = 20;

		Attack(ArenaPanelAbstract<?, ?, ?> arena, UnitComp comp, Cell target) {
			this.arena = arena;
			this.comp = comp;
			this.target = target;
		}

		@Override
		public void beforeFirst() {
			comp.isAnimated = true;

			basePos = comp.pos;

			Direction orientation = null;
			Position targetPos = Position.of(target);
			for (Direction dir : Direction.values())
				if (orientation == null
						|| Position.dist(comp.pos, targetPos, orientation) < Position.dist(comp.pos, targetPos, dir))
					orientation = dir;
			comp.orientation = orientation;

			arena.entityLayer.comps.put(this, this);
		}

		@Override
		public boolean advanceAnimationStep() {
			if (cursor >= Duration)
				throw new NoSuchElementException();
			double yOffset = (Math.abs(Duration / 2 - cursor) - Duration / 2) / 30.0;
			comp.pos = Position.of(basePos.x, basePos.y + yOffset);
			return ++cursor < Duration;
		}

		@Override
		public void afterLast() {
			comp.isAnimated = false;
			arena.entityLayer.comps.remove(this);
			comp.pos = basePos;
		}

		@Override
		public void clear() {
		}

		@Override
		public void paintComponent(Graphics g) {
			int gestureNum = Images.getGestureNum("Attack");
			int gestureIdx = (int) (cursor / ((double) Duration / gestureNum));
			BufferedImage img = Images.getAttackImg(gestureIdx);

			int x = arena.displayedXCell(target.x);
			int y = arena.displayedYCell(target.y);
			g.drawImage(img, x, y, null);
		}

		@Override
		public int getZOrder() {
			return 200;
		}

		@Override
		public Position pos() {
			return Position.of(target);
		}
	}

	static class UnitAppear implements Animation {
		private final UnitComp comp;
		private int cursor = 0;
		private static final int Duration = 30;

		UnitAppear(UnitComp comp) {
			this.comp = comp;
		}

		@Override
		public void beforeFirst() {
			comp.isAnimated = true;
			comp.baseAlphaMax = 0;
		}

		@Override
		public boolean advanceAnimationStep() {
			if (cursor >= Duration)
				throw new NoSuchElementException();
			comp.alpha = (float) (cursor + 1) / Duration;
			return ++cursor < Duration;
		}

		@Override
		public void afterLast() {
			comp.baseAlphaMax = 1;
			comp.alpha = 1;
			comp.isAnimated = false;
		}
	}

	static class UnitAppearDisappear implements Animation {
		private final UnitComp comp;
		private int cursor = 0;
		private static final int Duration = 60;

		UnitAppearDisappear(UnitComp comp) {
			this.comp = comp;
		}

		@Override
		public void beforeFirst() {
			comp.isAnimated = true;
		}

		@Override
		public boolean advanceAnimationStep() {
			if (cursor >= Duration)
				throw new NoSuchElementException();
			if (cursor < Duration / 2)
				comp.alpha = (float) (cursor + 1) / (Duration / 2);
			else
				comp.alpha = (float) (Duration - cursor) / (Duration / 2);
			return ++cursor < Duration;
		}

		@Override
		public void afterLast() {
			comp.alpha = 0;
			comp.isAnimated = false;
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
		public void beforeFirst() {
			comp.isAnimated = true;
			arena.entityLayer.comps.put(this, this);
		}

		@Override
		public boolean advanceAnimationStep() {
			if (cursor >= Duration)
				throw new NoSuchElementException();
			comp.alpha = (float) (Duration - cursor - 1) / Duration;
			return ++cursor < Duration;
		}

		@Override
		public void afterLast() {
			comp.alpha = 0;
			arena.entityLayer.comps.remove(this);
			comp.isAnimated = false;
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

			int x = arena.displayedXCell(pos.x);
			int y = arena.displayedYCell(pos.y);
			g.drawImage(img, x, y, null);
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

	static class MapMove implements Animation {

		private final ArenaPanelAbstract<?, ?, ?> arena;
		private final Position target;
		private static final double StepSize = 0.1;

		MapMove(ArenaPanelAbstract<?, ?, ?> arena, Position target) {
			this.arena = arena;
			this.target = target;
		}

		@Override
		public void beforeFirst() {
		}

		@Override
		public boolean advanceAnimationStep() {
			if (arena.mapPos.equals(target))
				return false;

			double dx = target.x - arena.mapPos.x;
			double dy = target.y - arena.mapPos.y;
			double l = Math.sqrt(dx * dx + dy * dy);
			double stepSize = StepSize * Math.sqrt(l);
			if (arena.mapPos.dist(target) > stepSize) {
				double x = arena.mapPos.x + dx / l * stepSize;
				double y = arena.mapPos.y + dy / l * stepSize;
				arena.mapPos = Position.of(x, y);
				return true;
			} else {
				arena.mapPos = target;
				return false;
			}
		}

		@Override
		public void afterLast() {
			arena.mapPos = target;
		}

		static class Manager implements TickTask {
			private final ArenaPanelAbstract<?, ?, ?> arena;
			private Animation.MapMove animation;
			private Position userChoosenPos;

			Manager(ArenaPanelAbstract<?, ?, ?> arena) {
				this.arena = Objects.requireNonNull(arena);
			}

			synchronized void userMapMove(Direction dir) {
				if (animation != null)
					return; /* User input is ignored during animation */

				Position userChoosenPosNew = (userChoosenPos == null ? arena.mapPos : userChoosenPos).add(dir);
				if (userChoosenPosNew.dist(arena.mapPos) >= 3)
					return;
				if (!userChoosenPosNew.isInRect(arena.arenaWidth() - ArenaPanelAbstract.DISPLAYED_ARENA_WIDTH,
						arena.arenaHeight() - ArenaPanelAbstract.DISPLAYED_ARENA_HEIGHT))
					return;

				userChoosenPos = userChoosenPosNew;
			}

			synchronized Animation createAnimation(Position target) {
				return new MapMove(arena, target) {

					@Override
					public void beforeFirst() {
						synchronized (Manager.this) {
							if (animation != null)
								throw new IllegalStateException();
							userChoosenPos = null;
							animation = this;
						}
						super.beforeFirst();
					}

					@Override
					public void afterLast() {
						super.afterLast();
						synchronized (Manager.this) {
							if (animation != this)
								throw new IllegalStateException();
							animation = null;
						}
					}
				};
			}

			@Override
			public synchronized void run() {
				if (userChoosenPos == null)
					return;
				double dx = userChoosenPos.x - arena.mapPos.x;
				double dy = userChoosenPos.y - arena.mapPos.y;
				if (dx == 0 && dy == 0)
					return;
				double l = Math.sqrt(dx * dx + dy * dy);
				double stepSize = StepSize * Math.sqrt(l);
				if (arena.mapPos.dist(userChoosenPos) > stepSize) {
					double x = arena.mapPos.x + dx / l * stepSize;
					double y = arena.mapPos.y + dy / l * stepSize;
					arena.mapPos = Position.of(x, y);
				} else {
					arena.mapPos = userChoosenPos;
				}
			}

		}

	}

	static Animation of(Animation... animations) {
		if (animations.length < 2)
			throw new IllegalArgumentException();
		return new Animation() {

			int index = 0;
			int currentAnimationStepCount;

			@Override
			public boolean advanceAnimationStep() {
				if (index >= animations.length)
					throw new NoSuchElementException();
				if (++currentAnimationStepCount == 1)
					animations[index].beforeFirst();
				if (!animations[index].advanceAnimationStep()) {
					animations[index].afterLast();
					index++;
					currentAnimationStepCount = 0;
				}
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
				if (++entry.animationCount == 1)
					entry.animation.beforeFirst();
				if (!entry.animation.advanceAnimationStep()) {
					it.remove();
					entry.animation.afterLast();
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
			int animationCount;

			AnimationEntry(Animation animation, Runnable future) {
				this.animation = Objects.requireNonNull(animation);
				this.future = future;
			}
		}

	}

}