package com.ugav.battalion;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.SwingUtilities;

import com.ugav.battalion.ArenaPanelAbstract.ArenaComp;
import com.ugav.battalion.ArenaPanelGameAbstract.EntityLayer.UnitComp;
import com.ugav.battalion.core.Cell;
import com.ugav.battalion.core.Direction;
import com.ugav.battalion.util.Event;
import com.ugav.battalion.util.ListInt;

@FunctionalInterface
interface Animation {

	boolean advanceAnimationStep();

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
		public boolean advanceAnimationStep() {
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

			Direction orientation = null;
			Position targetPos = Position.fromCell(target);
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

			int x = arena.displayedXCell(Cell.x(target));
			int y = arena.displayedYCell(Cell.y(target));
			g.drawImage(img, x, y, null);
		}

		@Override
		public int getZOrder() {
			return 200;
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

		@Override
		public String toString() {
			return "UnitDeath(" + Cell.toString(comp.unit().getPos()) + ")";
		}
	}

	static class MapMove implements Animation {

		private final ArenaPanelGameAbstract arena;
		private final Position target0;
		private static final double StepSize = 0.1;

		MapMove(ArenaPanelGameAbstract arena, Position target) {
			this.arena = arena;
			this.target0 = target;
		}

		private Position target() {
			return arena.mapMove.getMapPosRange().closestContainedPoint(target0);
		}

		@Override
		public void beforeFirst() {
			arena.mapMove.mapMoveStart();
		}

		@Override
		public boolean advanceAnimationStep() {
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

			private boolean userChosenPosValid;
			private int userChosenPos;
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

			}

			MapPosRange getMapPosRange() {
				double xmin, xmax, ymin, ymax;
				double shownWidth = arena.displayedArenaWidth();
				if (shownWidth >= arena.arenaWidth()) {
					xmin = xmax = (arena.arenaWidth() - shownWidth) / 2;
				} else {
					xmin = 0;
					xmax = Math.ceil(arena.arenaWidth() - shownWidth);
				}
				double shownHeight = arena.displayedArenaHeight();
				if (shownHeight >= arena.arenaHeight()) {
					ymin = ymax = (arena.arenaHeight() - shownHeight) / 2;
				} else {
					ymin = 0;
					ymax = Math.ceil(arena.arenaHeight() - shownHeight);
				}

				return new MapPosRange(xmin, xmax, ymin, ymax);
			}

			boolean isValidMapPos(Position pos) {
				return getMapPosRange().contains(pos);
			}

			MapPosRange getDisplayedRange() {
				double x = currentPos.x, y = currentPos.y;
				return new MapPosRange(x, x + arena.displayedArenaWidth() - 1, y, y + arena.displayedArenaHeight() - 1);
			}

			synchronized void userMapMove(Direction dir) {
				if (isMapMoving.get())
					return; /* User input is ignored during animation */

				Position userChoosenPosNew = (!userChosenPosValid ? currentPos : Position.fromCell(userChosenPos))
						.add(dir);
				userChoosenPosNew = getMapPosRange().closestContainedPoint(userChoosenPosNew);
				if (userChoosenPosNew.dist(currentPos) >= 3)
					return;

				userChosenPos = Cell.of((int) userChoosenPosNew.x, (int) userChoosenPosNew.y);
				userChosenPosValid = true;
				onMapMove.notify(new Event(this));
			}

			private void mapMoveStart() {
				synchronized (this) {
					if (!isMapMoving.compareAndSet(false, true))
						throw new IllegalStateException();
					userChosenPosValid = false;
				}
				onMapMove.notify(new Event(this));
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
			public synchronized void run() {
				if (!userChosenPosValid)
					return;
				double dx = Cell.x(userChosenPos) - currentPos.x;
				double dy = Cell.y(userChosenPos) - currentPos.y;
				if (dx == 0 && dy == 0)
					return;
				Position position = Position.fromCell(userChosenPos);
				double l = Math.sqrt(dx * dx + dy * dy);
				double stepSize = StepSize * Math.sqrt(l);
				if (currentPos.dist(position) > stepSize) {
					double x = currentPos.x + dx / l * stepSize;
					double y = currentPos.y + dy / l * stepSize;
					arena.mapMove.setPos(Position.of(x, y));
				} else {
					arena.mapMove.setPos(position);
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

			@Override
			public String toString() {
				return Arrays.toString(animations);
			}
		};
	}

	static class Task implements TickTask {

		private final Collection<AnimationEntry> queue = new ConcurrentLinkedQueue<>();

		private boolean isAnimationRunning = false;

		final Event.Notifier<Event> onAnimationBegin = new Event.Notifier<>();
		final Event.Notifier<Event> onAnimationEnd = new Event.Notifier<>();

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
				onAnimationBegin.notify(new Event(Task.this));
			}
			if (!animated && isAnimationRunning) {
				isAnimationRunning = false;
				onAnimationEnd.notify(new Event(Task.this));
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