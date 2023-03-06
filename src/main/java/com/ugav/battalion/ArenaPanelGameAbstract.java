package com.ugav.battalion;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import com.ugav.battalion.Animation.MapMove.Manager.MapPosRange;
import com.ugav.battalion.core.Building;
import com.ugav.battalion.core.Cell;
import com.ugav.battalion.core.Game;
import com.ugav.battalion.core.Team;
import com.ugav.battalion.core.Terrain;
import com.ugav.battalion.core.Unit;
import com.ugav.battalion.util.Event;
import com.ugav.battalion.util.Iter;
import com.ugav.battalion.util.ListInt;
import com.ugav.battalion.util.Utils;

class ArenaPanelGameAbstract extends
		ArenaPanelAbstract<ArenaPanelAbstract.TerrainComp, ArenaPanelGameAbstract.EntityLayer.BuildingComp, ArenaPanelGameAbstract.EntityLayer.UnitComp>
		implements Clearable {

	final Game game;
	final Event.Register register = new Event.Register();

	private static final long serialVersionUID = 1L;

	ArenaPanelGameAbstract(Game game, Globals globals) {
		super(globals);
		this.game = game;

		register.register(game.beforeUnitMove, e -> {
			ListInt animationPath = new ListInt.Array(e.path.size() + 1);
			animationPath.add(e.unit.getPos());
			animationPath.addAll(e.path);
			animateUnitMove(e.unit, animationPath);
		});
		register.register(game.beforeUnitAttack, e -> animateUnitAttack(e.attacker, e.target.getPos()));

		updateArenaSize(game.width(), game.height());
	}

	@Override
	EntityLayer createEntityLayer() {
		return new EntityLayer();
	}

	EntityLayer entityLayer() {
		return (EntityLayer) entityLayer;
	}

	void initGame() {
		tickTaskManager.start();
		entityLayer().reset();
		mapMove.setPos(Position.of(0, 0));
	}

	@Override
	public void clear() {
		register.unregisterAll();
		super.clear();
	}

	private void animateUnitAdd(Unit unit) {
		EntityLayer.UnitComp comp = (EntityLayer.UnitComp) entityLayer().comps.get(unit);
		Animation animation;
		if (!unit.type.invisible) {
			animation = new Animation.UnitAppear(comp);
		} else {
			animation = new Animation.UnitAppearDisappear(comp);
		}
		animation = centerMapBeforeAnimation(unit, animation);
		runAnimationAndWait(animation);
	}

	private void animateUnitMove(Unit unit, ListInt path) {
		EntityLayer.UnitComp comp = (EntityLayer.UnitComp) entityLayer().comps.get(unit);
		Animation animation = new Animation.UnitMove(comp, path);
		animation = appearDisappearAnimationWrap(comp, animation);
		animation = centerMapBeforeAnimation(unit, animation);
		runAnimationAndWait(animation);
	}

	private void animateUnitAttack(Unit unit, int target) {
		EntityLayer.UnitComp comp = (EntityLayer.UnitComp) entityLayer().comps.get(unit);
		Animation animation = new Animation.Attack(this, comp, target);
		animation = appearDisappearAnimationWrap(comp, animation);
		animation = makeSureAnimationIsVisible(animation, ListInt.of(unit.getPos(), target));
		runAnimationAndWait(animation);
	}

	private static Animation appearDisappearAnimationWrap(EntityLayer.UnitComp unitComp, Animation animation) {
		if (unitComp.unit().type.invisible)
			animation = Animation.of(new Animation.UnitAppearDisappear(unitComp), animation);
		return animation;
	}

	private Animation centerMapBeforeAnimation(Unit unit, Animation animation) {
		Position mapMovePos = mapMove.calcMapPosCentered(Position.fromCell(unit.getPos()));
		Animation mapMoveAnimation = new Animation.MapMove(this, mapMovePos);
		return Animation.of(mapMoveAnimation, animation);
	}

	private Animation makeSureAnimationIsVisible(Animation animation, ListInt cells) {
		if (cells.isEmpty())
			throw new IllegalArgumentException();
		int p0 = cells.get(0);
		int xmin = Cell.x(p0), xmax = Cell.x(p0);
		int ymin = Cell.y(p0), ymax = Cell.y(p0);
		for (Iter.Int it = cells.iterator(); it.hasNext();) {
			int cell = it.next();
			xmin = Math.min(xmin, Cell.x(cell));
			xmax = Math.max(xmax, Cell.x(cell));
			ymin = Math.min(ymin, Cell.y(cell));
			ymax = Math.max(ymax, Cell.y(cell));
		}
		int topLeft = Cell.of(xmin, ymin);
		int bottomRight = Cell.of(xmax, ymax);
		MapPosRange displayedRange = mapMove.getDisplayedRange();
		boolean topLeftVisible = displayedRange.contains(Position.fromCell(topLeft));
		boolean bottomRightVisible = displayedRange.contains(Position.fromCell(bottomRight));
		if (topLeftVisible && bottomRightVisible)
			return animation; /* already visible */

		if (xmin > xmax + displayedArenaWidth() || ymin > ymax + displayedArenaHeight())
			throw new IllegalArgumentException("can't display rect [" + topLeft + ", " + bottomRight + "]");
		int x = (int) (xmin + (xmax - xmin) / 2 - displayedArenaWidth() / 2);
		int y = (int) (ymin + (ymax - ymin) / 2 - displayedArenaHeight() / 2);
		Position pos = mapMove.getDisplayedRange().closestContainedPoint(Position.of(x, y));

		for (Iter.Int it = cells.iterator(); it.hasNext();) {
			int cell = it.next();
			if (!Cell.isInRect(cell, pos.x, pos.y, pos.x + displayedArenaWidth() - 1,
					pos.y + displayedArenaHeight() - 1))
				throw new IllegalStateException();
		}

		Animation mapMoveAnimation = new Animation.MapMove(this, pos);
		return Animation.of(mapMoveAnimation, animation);
	}

	class EntityLayer extends
			ArenaPanelAbstract.EntityLayer<ArenaPanelAbstract.TerrainComp, EntityLayer.BuildingComp, EntityLayer.UnitComp> {

		private static final long serialVersionUID = 1L;

		private final GestureTask gestureTask = new GestureTask();

		private final Event.Register register = new Event.Register();

		EntityLayer() {
			super(ArenaPanelGameAbstract.this);
		}

		void initUI() {
			register.register(game.onUnitAdd, e -> {
				Utils.swingRun(() -> new UnitComp(e.unit));
				animateUnitAdd(e.unit);
			});
			register.register(game.onUnitDeath, e -> {
				UnitComp unitComp = (UnitComp) comps.get(e.unit);
				Animation animation = new Animation.UnitDeath(ArenaPanelGameAbstract.this, unitComp);
				runAnimationAndWait(animation);
			});
			register.register(game.onUnitRemove, Utils.swingListener(e -> {
				UnitComp unitComp = (UnitComp) comps.remove(e.unit);
				if (unitComp != null)
					unitComp.clear();
			}));
			register.register(game.onEntityChange, e -> {
				if (e.source instanceof Unit unit) {
					UnitComp comp = (UnitComp) comps.get(unit);
					if (comp != null)
						comp.pos = Position.fromCell(unit.getPos());
				}
			});
			register.register(game.onConquer, e -> {
				UnitComp unitComp = (UnitComp) comps.get(e.conquerer);
				Animation animation = new Animation.Conquer(unitComp);
				animation = centerMapBeforeAnimation(e.conquerer, animation);
				runAnimationAndWait(animation);
			});

			tickTaskManager.addTask(100, gestureTask);
		}

		@Override
		public void clear() {
			register.unregisterAll();
			gestureTask.clear();
			super.clear();
		}

		void reset() {
			removeAllArenaComps();

			for (Iter.Int it = game.cells(); it.hasNext();) {
				int cell = it.next();
				TerrainComp terrainComp = new TerrainComp(ArenaPanelGameAbstract.this, cell);
				comps.put("Terrain " + cell, terrainComp); // TODO bug, comps is identity map

				Unit unit = game.unit(cell);
				if (unit != null)
					comps.put(unit, new UnitComp(unit));

				Building building = game.building(cell);
				if (building != null)
					comps.put(building, new BuildingComp(cell, building));

			}
		}

		class BuildingComp extends ArenaPanelAbstract.BuildingComp {

			BuildingComp(int pos, Building building) {
				super(ArenaPanelGameAbstract.this, pos, building);
			}

			@Override
			Building building() {
				return (Building) super.building();
			}

			@Override
			public void paintComponent(Graphics g) {
				super.paintComponent(g);

				/* Draw flag glow */
				Team conquerTeam = building().getConquerTeam();
				if (conquerTeam != null) {
					BufferedImage flagImg = Images.getFlagGlowImg(conquerTeam, getFlagGesture());
					int x = arena.displayedXCell(pos().xInt()) + 39;
					int y = arena.displayedYCell(pos().yInt()) - 6;
					g.drawImage(flagImg, x, y, arena);
				}
			}

			@Override
			int getGasture() {
				return gestureTask.getGesture() % Images.getGestureNum(building().getType());
			}

			@Override
			int getFlagGesture() {
				return gestureTask.getGesture() % Images.getGestureNum("Flag");
			}

		}

		class UnitComp extends ArenaPanelAbstract.UnitComp {

			volatile boolean isAnimated;
			float alpha = 0.0f;
			float baseAlphaMax = 1.0f;

			private static final Color HealthColorHigh = new Color(0, 206, 0);
			private static final Color HealthColorMed = new Color(255, 130, 4);
			private static final Color HealthColorLow = new Color(242, 0, 0);

			UnitComp(Unit unit) {
				super(ArenaPanelGameAbstract.this, unit.getPos(), unit);
			}

			@Override
			Unit unit() {
				return (Unit) super.unit();
			}

			@Override
			int getGasture() {
				if (unit().getTeam() == game.getTurn() && !unit().isActive())
					return 0;
				int gestureNum = isMoving ? Images.getGestureNumUnitMove(unit().type)
						: Images.getGestureNumUnitStand(unit().type);
				return gestureTask.getGesture() % gestureNum;
			}

			@Override
			public void paintComponent(Graphics g) {
				final Team playerTeam = Team.Red;
				if (!isAnimated && !game.isUnitVisible(unit().getPos(), playerTeam))
					return;

				/* Draw unit */
				super.paintComponent(g);

				/* Draw health bar */
				if (unit().getHealth() != 0 && unit().getHealth() != unit().type.health) {
					double health = (double) unit().getHealth() / unit().type.health;

					BufferedImage healthBar = new BufferedImage(17, 9, BufferedImage.TYPE_INT_RGB);
					Graphics2D healthBarG = healthBar.createGraphics();
					healthBarG.setColor(Color.BLACK);
					healthBarG.fillRect(0, 0, 17, 9);
					Color healthColor;
					if (health > 2.0 / 3.0)
						healthColor = HealthColorHigh;
					else if (health > 1.0 / 3.0)
						healthColor = HealthColorMed;
					else
						healthColor = HealthColorLow;
					healthBarG.setColor(healthColor);
					for (int bar = 0; bar < 4; bar++) {
						double barPrec = Math.min(Math.max(0, health - bar * 0.25) * 4, 1);
						int barHeight = (int) (7 * barPrec);
						healthBarG.fillRect(1 + bar * 4, 8 - barHeight, 3, barHeight);
					}
					int x = arena.displayedXCell(pos.x) + 32;
					int y = arena.displayedYCell(pos.y) + 42;
					g.drawImage(healthBar, x, y, null);
				}
			}

			@Override
			BufferedImage getUnitImg() {
				BufferedImage img = super.getUnitImg();

				if (unit().getTeam() == game.getTurn() && !unit().isActive())
					img = Utils.imgDarken(img, .7f);

				float alpha0 = Math.max(calcBaseAlpha(), alpha);
				if (alpha0 != 1.0)
					img = Utils.imgTransparent(img, alpha0);

				return img;
			}

			float calcBaseAlpha() {
				final Team playerTeam = Team.Red;

				float baseAlpha;
				if (unit().isDead())
					baseAlpha = 0f;
				else if (!unit().type.invisible)
					baseAlpha = 1f;
				else if (unit().getTeam() == playerTeam || game.isUnitVisible(unit().getPos(), playerTeam))
					baseAlpha = .5f;
				else
					baseAlpha = 0f;
				return Math.min(baseAlpha, baseAlphaMax);
			}

			@Override
			public int getZOrder() {
				return isAnimated ? 100 : 0;
			}

		}

	}

	@Override
	Terrain getTerrain(int cell) {
		return game.terrain(cell);
	}

}
