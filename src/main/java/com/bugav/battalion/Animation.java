package com.bugav.battalion;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import javax.swing.SwingUtilities;

import com.bugav.battalion.ArenaPanelAbstract.ArenaComp;
import com.bugav.battalion.ArenaPanelGameAbstract.EntityLayer.BuildingComp;
import com.bugav.battalion.ArenaPanelGameAbstract.EntityLayer.UnitComp;
import com.bugav.battalion.core.Cell;
import com.bugav.battalion.core.Direction;
import com.bugav.battalion.util.Event;
import com.bugav.battalion.util.ListInt;
import com.bugav.battalion.util.Logger;

@FunctionalInterface
interface Animation {

	boolean animationStep();

	default void beforeFirst() {
	};

	default void afterLast() {
	};

	static class UnitMove implements Animation {

		final UnitComp comp;
		private final ListInt path;
		private int cursor;
		private static final int StepSize = 16;

		UnitMove(UnitComp comp, ListInt path) {
			if (path.isEmpty())
				throw new IllegalArgumentException();
			this.comp = Objects.requireNonNull(comp);
			this.path = path.copy().unmodifiableView();
		}

		@Override
		public void beforeFirst() {
			comp.isAnimated = comp.isMoving = true;
			comp.arena.mapMove.mapMoveStart();
		}

		@Override
		public boolean animationStep() {
			int length = path.size() - 1;
			if (length == 0)
				return false;
			if (cursor >= length * StepSize)
				throw new NoSuchElementException();

			int idx = cursor / StepSize;
			int p1 = path.get(idx);
			int p2 = path.get(idx + 1);
			comp.orientation = Cell.diffDir(p1, p2);
			double frac = (cursor % StepSize + 1) / (double) StepSize;
			int x1 = Cell.x(p1), x2 = Cell.x(p2);
			int y1 = Cell.y(p1), y2 = Cell.y(p2);
			double x = x1 + (x2 - x1) * frac;
			double y = y1 + (y2 - y1) * frac;
			comp.pos = Position.of(x, y);

			if (isMapFollow())
				comp.arena.mapMove.setPos(comp.arena.mapMove.calcMapPosCentered(comp.pos));

			return ++cursor < length * StepSize;
		}

		@Override
		public void afterLast() {
			comp.pos = Position.fromCell(path.last());
			if (isMapFollow())
				comp.arena.mapMove.setPos(comp.arena.mapMove.calcMapPosCentered(comp.pos));
			comp.isAnimated = comp.isMoving = false;
			comp.arena.mapMove.mapMoveEnd();
		}

		private boolean isMapFollow() {
			return !comp.unit().type.invisible;
		}

		@Override
		public String toString() {
			return "UnitMove(" + Cell.toString(path) + ")";
		}

	}

	static Direction calcOrientation(Position source, Position target) {
		Direction orientation = null;
		for (Direction dir : Direction.values())
			if (orientation == null || Position.dist(source, target, orientation) < Position.dist(source, target, dir))
				orientation = dir;
		return orientation;
	}

	static class Attack implements Animation, ArenaComp {
		private final ArenaPanelGameAbstract arena;
		private final UnitComp comp;
		private final int target;
		private Position basePos;
		private int cursor = 0;
		private static final int Duration = 20;

		Attack(ArenaPanelGameAbstract arena, UnitComp comp, int target) {
			this.arena = arena;
			this.comp = comp;
			this.target = target;
		}

		@Override
		public void beforeFirst() {
			comp.isAnimated = true;
			basePos = comp.pos;
			comp.orientation = calcOrientation(comp.pos, Position.fromCell(target));
			arena.entityLayer.comps.put(this, this);
		}

		@Override
		public boolean animationStep() {
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
			int gestureNum = Images.Ect.AttackGestureNum;
			int gestureIdx = (int) (cursor / ((double) Duration / gestureNum));
			BufferedImage img = Images.Ect.attack(gestureIdx);

			int x = arena.displayedXCell(Cell.x(target));
			int y = arena.displayedYCell(Cell.y(target));
			g.drawImage(img, x, y, null);
		}

		@Override
		public int getZOrder() {
			return ZOrderAnimation;
		}

		@Override
		public Position pos() {
			return Position.fromCell(target);
		}

		@Override
		public String toString() {
			return "Attack(" + Cell.toString(comp.unit().getPos()) + ", " + Cell.toString(target) + ")";
		}
	}

	static class Conquer implements Animation {
		private final UnitComp comp;
		private Position basePos;
		private int cursor = 0;
		private static final int Duration = 20;

		Conquer(UnitComp comp) {
			this.comp = comp;
		}

		@Override
		public void beforeFirst() {
			comp.isAnimated = true;
			basePos = comp.pos;
		}

		@Override
		public boolean animationStep() {
			if (cursor >= Duration)
				throw new NoSuchElementException();
			double yOffset = (Math.abs(Duration / 2 - cursor) - Duration / 2) / 30.0;
			comp.pos = Position.of(basePos.x, basePos.y + yOffset);
			return ++cursor < Duration;
		}

		@Override
		public void afterLast() {
			comp.isAnimated = false;
			comp.pos = basePos;
		}

		@Override
		public String toString() {
			return "Conquer(" + Cell.toString(comp.unit().getPos()) + ")";
		}
	}

	static class UnitAppear implements Animation {
		private final UnitComp comp;
		private int cursor = 0;
		private static final int Duration = 30;

		UnitAppear(UnitComp comp) {
			this.comp = Objects.requireNonNull(comp);
		}

		@Override
		public void beforeFirst() {
			comp.isAnimated = true;
			comp.baseAlphaMax = 0;
		}

		@Override
		public boolean animationStep() {
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

		@Override
		public String toString() {
			return "UnitAppear(" + Cell.toString(comp.unit().getPos()) + ")";
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
		public boolean animationStep() {
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

		@Override
		public String toString() {
			return "UnitAppearDisappear(" + Cell.toString(comp.unit().getPos()) + ")";
		}
	}

	static class UnitDeath implements Animation, ArenaComp {
		private final ArenaPanelGameAbstract arena;
		private final UnitComp comp;
		private int cursor = 0;
		private static final int Duration = 90;

		UnitDeath(ArenaPanelGameAbstract arena, UnitComp comp) {
			this.arena = arena;
			this.comp = comp;
		}

		@Override
		public void beforeFirst() {
			comp.isAnimated = true;
			arena.entityLayer.comps.put(this, this);
		}

		@Override
		public boolean animationStep() {
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
			int gestureNum = Images.Ect.ExplosionGestureNum;
			int gestureIdx = (int) (cursor / ((double) Duration / gestureNum));
			BufferedImage img = Images.Ect.explosion(gestureIdx);

			int x = arena.displayedXCell(pos.x);
			int y = arena.displayedYCell(pos.y);
			g.drawImage(img, x, y, null);
		}

		@Override
		public int getZOrder() {
			return ZOrderAnimation;
		}

		@Override
		public Position pos() {
			return comp.pos;
		}

		@Override
		public String toString() {
			return "UnitDeath(" + Cell.toString(comp.unit().getPos()) + ")";
		}
	}

	static class BuildingExplosion implements Animation, ArenaComp {
		private final ArenaPanelGameAbstract arena;
		private final BuildingComp comp;
		private int cursor = 0;
		private static final int Duration = 90;

		BuildingExplosion(ArenaPanelGameAbstract arena, BuildingComp comp) {
			this.arena = arena;
			this.comp = comp;
		}

		@Override
		public void beforeFirst() {
			arena.entityLayer.comps.put(this, this);
		}

		@Override
		public boolean animationStep() {
			if (cursor >= Duration)
				throw new NoSuchElementException();
			return ++cursor < Duration;
		}

		@Override
		public void afterLast() {
			arena.entityLayer.comps.remove(this);
		}

		@Override
		public void clear() {
		}

		@Override
		public void paintComponent(Graphics g) {
			Position pos = comp.pos();
			int gestureNum = Images.Ect.ExplosionGestureNum;
			int gestureIdx = (int) (cursor / ((double) Duration / gestureNum));
			BufferedImage img = Images.Ect.explosion(gestureIdx);

			int x = arena.displayedXCell(pos.x);
			int y = arena.displayedYCell(pos.y);
			g.drawImage(img, x, y, null);
		}

		@Override
		public int getZOrder() {
			return ZOrderAnimation;
		}

		@Override
		public Position pos() {
			return comp.pos();
		}

		@Override
		public String toString() {
			return "BuildingExplosion(" + comp.pos() + ")";
		}
	}

	static class Delay implements Animation {

		int duration;

		Delay(int duration) {
			if (duration < 1)
				throw new IllegalArgumentException();
			this.duration = duration;
		}

		@Override
		public boolean animationStep() {
			if (duration <= 0)
				throw new NoSuchElementException();
			return --duration > 0;
		}

	}

	static class MapMove implements Animation {

		private final ArenaPanelGameAbstract arena;
		private final Supplier<Position> target0;
		private static final double StepSize = 0.1;

		MapMove(ArenaPanelGameAbstract arena, Supplier<Position> target) {
			this.arena = arena;
			this.target0 = target;
		}

		private Position target() {
			return arena.mapMove.getMapPosRange().closestContainedPoint(target0.get());
		}

		@Override
		public void beforeFirst() {
			arena.mapMove.mapMoveStart();
		}

		@Override
		public boolean animationStep() {
			Position current = arena.mapMove.currentPos;
			Position target = target();
			if (current.equals(target))
				return false;

			double dx = target.x - current.x;
			double dy = target.y - current.y;
			double l = Math.sqrt(dx * dx + dy * dy);
			double stepSize = StepSize * Math.sqrt(l);
			if (current.dist(target) > stepSize) {
				double x = current.x + dx / l * stepSize;
				double y = current.y + dy / l * stepSize;
				arena.mapMove.setPos(Position.of(x, y));
				return true;
			} else {
				arena.mapMove.setPos(target);
				return false;
			}
		}

		@Override
		public void afterLast() {
			arena.mapMove.setPos(target());
			arena.mapMove.mapMoveEnd();
		}

		@Override
		public String toString() {
			return "MapMove(" + target() + ")";
		}

		static class Manager implements TickTask {
			private final ArenaPanelAbstract<?, ?, ?> arena;
			private final AtomicBoolean isMapMoving = new AtomicBoolean();

			private double userChosenDx;
			private double userChosenDy;
			private boolean userChosenDirValid;
			private Position currentPos;

			final Event.Notifier<Event> onMapMove = new Event.Notifier<>();

			Manager(ArenaPanelAbstract<?, ?, ?> arena) {
				this.arena = Objects.requireNonNull(arena);
			}

			Position getCurrent() {
				return currentPos;
			}

			void setPos(Position pos) {
				currentPos = getMapPosRange().closestContainedPoint(pos);
				onMapMove.notify(new Event(this));
			}

			static class MapPosRange {
				final double xmin, xmax, ymin, ymax;

				MapPosRange(double xmin, double xmax, double ymin, double ymax) {
					this.xmin = xmin;
					this.xmax = xmax;
					this.ymin = ymin;
					this.ymax = ymax;
				}

				boolean contains(Position pos) {
					return pos.isInRect(xmin, ymin, xmax, ymax);
				}

				Position closestContainedPoint(Position pos) {
					double x = Math.max(xmin, Math.min(pos.x, xmax));
					double y = Math.max(ymin, Math.min(pos.y, ymax));
					return Position.of(x, y);
				}

				@Override
				public String toString() {
					return "(" + xmin + " <= x <= " + xmax + ", " + ymin + " <= x <= " + ymax + ")";
				}

			}

			MapPosRange getMapPosRange() {
				double xmin, xmax, ymin, ymax;
				double shownWidth = arena.displayedArenaWidth();
				if (shownWidth >= arena.arenaWidth()) {
					xmin = xmax = (arena.arenaWidth() - shownWidth) / 2;
				} else {
					xmin = 0;
					xmax = arena.arenaWidth() - shownWidth;
				}
				double shownHeight = arena.displayedArenaHeight();
				if (shownHeight >= arena.arenaHeight()) {
					ymin = ymax = (arena.arenaHeight() - shownHeight) / 2;
				} else {
					ymin = 0;
					ymax = arena.arenaHeight() - shownHeight;
				}

				return new MapPosRange(xmin, xmax, ymin, ymax);
			}

			boolean isValidMapPos(Position pos) {
				return getMapPosRange().contains(pos);
			}

			MapPosRange getDisplayedRange() {
				double x = currentPos.x, y = currentPos.y;
				return new MapPosRange(x, x + arena.displayedArenaWidth(), y, y + arena.displayedArenaHeight());
			}

			MapPosRange getDisplayedRangeFully() {
				MapPosRange d = getDisplayedRange();
				return new MapPosRange(d.xmin, d.xmax - 1, d.ymin, d.ymax - 1);
			}

			synchronized void userMapMove(double dx, double dy) {
				if (isMapMoving.get())
					return; /* User input is ignored during animation */
				userChosenDx = dx;
				userChosenDy = dy;
				userChosenDirValid = true;
			}

			synchronized void userMapMoveCancel() {
				userChosenDirValid = false;
			}

			private void mapMoveStart() {
				synchronized (this) {
					if (!isMapMoving.compareAndSet(false, true))
						throw new IllegalStateException();
					userChosenDirValid = false;
				}
			}

			private void mapMoveEnd() {
				synchronized (this) {
					if (!isMapMoving.compareAndSet(true, false))
						throw new IllegalStateException();
				}
			}

			Position calcMapPosCentered(Position center) {
				double mapx = center.x + 0.5 - arena.displayedArenaWidth() / 2.0;
				double mapy = center.y + 0.5 - arena.displayedArenaHeight() / 2.0;
				return getMapPosRange().closestContainedPoint(Position.of(mapx, mapy));
			}

			@Override
			public synchronized void onTick() {
				if (!userChosenDirValid)
					return;
				double x = currentPos.x + userChosenDx * StepSize;
				double y = currentPos.y + userChosenDy * StepSize;
				arena.mapMove.setPos(Position.of(x, y));
			}

			@Override
			public void clear() {
			}

		}

	}

	static Animation sequence(Animation... animations) {
		return new Animation() {

			int index = 0;
			int currentAnimationStepCount;

			@Override
			public boolean animationStep() {
				if (index >= animations.length)
					throw new NoSuchElementException();
				if (++currentAnimationStepCount == 1)
					animations[index].beforeFirst();
				if (!animations[index].animationStep()) {
					animations[index].afterLast();
					index++;
					currentAnimationStepCount = 0;
				}
				return index < animations.length;
			}

			@Override
			public String toString() {
				return Arrays.toString(animations);
			}
		};
	}

	static Animation parallel(Animation... animations) {
		return new Animation() {

			boolean[] done = new boolean[animations.length];

			@Override
			public void beforeFirst() {
				for (Animation animation : animations)
					animation.beforeFirst();
			}

			@Override
			public boolean animationStep() {
				boolean needMore = false;
				for (int i = 0; i < animations.length; i++) {
					if (done[i])
						continue;
					if (animations[i].animationStep()) {
						needMore = true;
					} else {
						done[i] = true;
						animations[i].afterLast();
					}
				}
				return needMore;
			}

			@Override
			public String toString() {
				return Arrays.toString(animations);
			}
		};
	}

	static Animation ofSingleStep(Runnable runnable) {
		return new Animation() {

			@Override
			public boolean animationStep() {
				runnable.run();
				return false;
			}
		};
	}

	static class Task implements TickTask {

		private volatile boolean isTaskRunning;
		private final Collection<AnimationEntry> queue = new ConcurrentLinkedQueue<>();
		private boolean isAnimationRunning = false;

		final Event.Notifier<Event> onAnimationBegin = new Event.Notifier<>();
		final Event.Notifier<Event> onAnimationEnd = new Event.Notifier<>();
		private final Globals globals;
		private final Logger logger;

		Task(Globals globals, Logger logger) {
			this.globals = Objects.requireNonNull(globals);
			this.logger = Objects.requireNonNull(logger);
		}

		@Override
		public void onTick() {
			if (!isTaskRunning)
				return;
			boolean animated = false;

			for (Iterator<AnimationEntry> it = queue.iterator(); it.hasNext();) {
				AnimationEntry entry = it.next();
				animated = true;
				if (entry.isFirstRunStep) {
					entry.isFirstRunStep = false;
					logger.dbgln("animation begin ", entry.animation);
					entry.animation.beforeFirst();
				}
				boolean done = !entry.animation.animationStep();
				if (!done && globals.debug.skipAnimations)
					while (!done)
						done = !entry.animation.animationStep();
				if (done) {
					it.remove();
					endAnimation(entry);
					logger.dbgln("animation end ", entry.animation);
				}
			}
			setAnimationRunning(animated);
		}

		void runAndWait(Animation animation) {
			animate(animation, false);
		}

		void runAsync(Animation animation) {
			animate(animation, true);
		}

		private void animate(Animation animation, boolean async) {
			if (!isTaskRunning)
				return;
			AnimationEntry entry = new AnimationEntry(animation);
			if (async) {
				queue.add(entry);
			} else {
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
			}
		}

		private static class AnimationEntry {
			final Animation animation;
			boolean isFirstRunStep = true;

			AnimationEntry(Animation animation) {
				this.animation = Objects.requireNonNull(animation);
			}
		}

		void setRunning(boolean running) {
			isTaskRunning = running;
		}

		@Override
		public void clear() {
			isTaskRunning = false;
			while (!queue.isEmpty()) {
				for (Iterator<AnimationEntry> it = queue.iterator(); it.hasNext();) {
					AnimationEntry entry = it.next();
					it.remove();
					endAnimation(entry);
				}
			}
			setAnimationRunning(false);
		}

		private void setAnimationRunning(boolean animated) {
			if (animated == isAnimationRunning)
				return;
			if (isAnimationRunning = animated) {
				onAnimationBegin.notify(new Event(Task.this));
			} else {
				onAnimationEnd.notify(new Event(Task.this));
			}
		}

		private static void endAnimation(AnimationEntry entry) {
			entry.animation.afterLast();
			synchronized (entry) {
				entry.notify();
			}
		}

	}

}