package com.ugav.battalion.computer;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntPredicate;

import com.ugav.battalion.computer.GameImpl.Node;
import com.ugav.battalion.core.Action;
import com.ugav.battalion.core.Building;
import com.ugav.battalion.core.Cell;
import com.ugav.battalion.core.Game;
import com.ugav.battalion.core.Team;
import com.ugav.battalion.core.Terrain;
import com.ugav.battalion.core.Unit;
import com.ugav.battalion.util.Cache;
import com.ugav.battalion.util.Graph;
import com.ugav.battalion.util.GraphArrayDirected;
import com.ugav.battalion.util.Iter;
import com.ugav.battalion.util.SSSP;
import com.ugav.battalion.util.SSSPDial1969;

class ValueFunctionImpl implements GameTreeAlg.ValueFunction<Action, GameImpl.Node, GameImpl> {

	private final Cache<Game, AttackPlans> attackPlansCache = new Cache.FixSize<>(100);

	private static final double Aggression = 0.95;

	ValueFunctionImpl() {
		checkParams();
	}

	@Override
	public double evaluate(Node position, int us) {
		return evaluate(null, null, position, us);
	}

	@Override
	public double evaluate(Node history, Action action, Node position, int us0) {
		Team us = GameImpl.turnIntToObj(us0);
		if (position.game.isFinished())
			return position.game.getWinner() == us ? Double.MAX_VALUE : -Double.MAX_VALUE;
		Game plansPosition = (history != null ? history : position).game;
		AttackPlans attackPlans = attackPlansCache.getOrCompute(plansPosition, AttackPlans::new);
		double actionEval = new ActionEvaluator(attackPlans, position, us).evaluate();
		double[] positionEvals = new PositionEvaluator(position).evaluate();
		return actionEval + evalFromTeamEvals(us, positionEvals);
	}

	private static double evalFromTeamEvals(Team us, double[]... evals0) {
		double[] evals = new double[Team.values().length];
		for (double[] e : evals0)
			for (int i = 0; i < Team.values().length; i++)
				evals[i] += e[i];

		double maxEnemyEval = 0;
		for (Team team : Team.values())
			if (team != us)
				maxEnemyEval = Math.max(maxEnemyEval, evals[team.ordinal()]);
		double eval = (1 - Aggression) * evals[us.ordinal()];
		for (Team team : Team.values())
			if (team != us)
				eval -= Aggression * Math.pow(evals[team.ordinal()], 2) / maxEnemyEval;
		return eval;
	}

	@SuppressWarnings("unused")
	private static void checkParams() {
		if (!(0 <= Aggression && Aggression <= 1))
			throw new IllegalArgumentException();
	}

	private static class PositionEvaluator {

		private final Game game;

		private final Units Units;

		PositionEvaluator(Node position) {
			this.game = position.game;
			Units = new Units();
		}

		double[] evaluate() {
			double[] evals = new double[Team.values().length];
			for (Unit unit : game.units().forEach())
				evals[unit.getTeam().ordinal()] += Units.eval(unit);

			for (Building building : game.buildings().forEach()) {
				double buildingEval = Buildings.eval(building);
				if (building.getTeam() != null)
					evals[building.getTeam().ordinal()] += buildingEval;
				Team conquerTeam = building.getConquerTeam();
				if (conquerTeam != null)
					evals[conquerTeam.ordinal()] += buildingEval * building.getConquerProgress();
			}

			for (Team team : Team.values()) {
				int money = game.getMoney(team);
				evals[team.ordinal()] += 0.2 * Math.pow(money, 4.0 / 5.0);
			}

			for (Team team : Team.values())
				assert evals[team.ordinal()] >= 0;
			return evals;
		}

		private class Units {

			private static class Weight {
				private static final double Alive = 10;
				private static final double Health = 0.8;
				private static final double VulnerablePenalty = 5;
				private static final double VulnerableRepairPenalty = 10;
			}

			private final Map<Team, Cell.Bitmap> vulnerable = new EnumMap<>(Team.class);

			Units() {
				for (Team team : Team.values()) {
					List<Cell.Bitmap> attackable = game.enemiesSeenBy(team).map(Unit::getAttackableMap).toList();
					IntPredicate isVulnerable = cell -> Iter.of(attackable).mapBool(m -> m.contains(cell)).any();
					vulnerable.put(team, Cell.Bitmap.fromPredicate(game.width(), game.height(), isVulnerable));
				}
			}

			double eval(Unit unit) {
				double eval = Weight.Alive;

				double health = unit.getHealth() + (unit.isRepairing() ? unit.repairAmount() / 2 : 0);
				eval += health * Weight.Health;

				if (isVulnerable(unit)) {
					eval -= Weight.VulnerablePenalty;
					if (unit.isRepairing())
						eval -= Weight.VulnerableRepairPenalty;
				}

				return eval;
			}

			private boolean isVulnerable(Unit unit) {
				return vulnerable.get(unit.getTeam()).contains(unit.getPos());
			}

		}

		private final Buildings Buildings = new Buildings();

		private class Buildings {

			private static class Weight {
				static final double Factory = 50;
				static final double FactoryBlocked = 25;
				static final double AllowUnitBuildLand = 20;
				static final double AllowUnitBuildWater = 20;
				static final double AllowUnitBuildAir = 20;
			}

			double eval(Building building) {
				double eval = 0;
				eval += building.getMoneyGain();
				if (building.type.canBuildUnits) {
					boolean isBlocked = game.unit(building.getPos()) != null;
					eval += isBlocked ? Weight.FactoryBlocked : Weight.Factory;
				}
				if (building.type.allowUnitBuildLand)
					eval += Weight.AllowUnitBuildLand;
				if (building.type.allowUnitBuildWater)
					eval += Weight.AllowUnitBuildWater;
				if (building.type.allowUnitBuildAir)
					eval += Weight.AllowUnitBuildAir;
				return eval;
			}
		}

	}

	private static class ActionEvaluator {

		private final AttackPlans attackPlans;
		private final Game game;
		private final Team us;

		ActionEvaluator(AttackPlans attackPlans, GameImpl.Node game, Team us) {
			this.attackPlans = Objects.requireNonNull(attackPlans);
			this.game = Objects.requireNonNull(game.game);
			this.us = Objects.requireNonNull(us);
		}

		double evaluate() {
			double overallAttackEval = 0;
			List<Unit> enemies = game.enemiesSeenBy(us).toList();
			for (Unit attacker0 : game.units(us).forEach()) {
				Unit attacker = attacker0.type.transportUnits ? attacker0.getTransportedUnit() : attacker0;

				/* Find the best attack */
				double bestAttackEval = 0;
				for (Unit target : enemies) {
					if (!attacker.canAttack(target.type))
						continue;
					double dis = attackPlans.getAttackDistance(attacker0, target);
					if (dis < 0)
						continue;
					double attackEval = (1 / (dis + 1)) * attacker.getDamge(target);
					if (attackEval > bestAttackEval)
						bestAttackEval = attackEval;
				}
				overallAttackEval += bestAttackEval;
			}

			return overallAttackEval;
		}
	}

	private static class AttackPlans {
		private final Game game;

		AttackPlans(Game game) {
			this.game = Objects.requireNonNull(game);
		}

		private final Map<PlanKey, Plan> plans = new HashMap<>();
		private final SSSP sssp = new SSSPDial1969();

		private int cellToVIdx(int cell, Unit.Type transportType) {
			Plan.Layer layer;
			if (transportType == null) {
				layer = Plan.Layer.Regular;

			} else {
				switch (transportType) {
				case LandingCraft:
					layer = Plan.Layer.Water;
					break;
				case TransportPlane:
					layer = Plan.Layer.Air;
					break;
				default:
					throw new IllegalArgumentException("Unexpected value: " + transportType);
				}
			}

			int layerSize = game.width() * game.height();
			return layer.ordinal() * layerSize + Cell.x(cell) * game.height() + Cell.y(cell);
		}

		double getAttackDistance(Unit attacker, Unit target) {
			boolean useTransportWater = game.canBuildUnitType(attacker.getTeam(), Unit.Type.LandingCraft);
			boolean useTransportAir = game.canBuildUnitType(attacker.getTeam(), Unit.Type.TransportPlane);

			Unit.Type transportType;
			Unit attacker0;
			if (!attacker.type.transportUnits) {
				transportType = null;
				attacker0 = attacker;
			} else {
				transportType = attacker.type;
				useTransportWater = useTransportWater || transportType == Unit.Type.LandingCraft;
				useTransportAir = useTransportWater || transportType == Unit.Type.TransportPlane;
				attacker0 = attacker.getTransportedUnit();
			}

			PlanKey key = new PlanKey(attacker0, target, useTransportWater, useTransportAir);
			Plan plan = plans.computeIfAbsent(key, k -> new Plan(k.attackerCanStandOn, k.attackerTeam, k.target,
					k.useTransportWater, k.useTransportAir));
			return plan.getAttackDistance(attacker0.getPos(), transportType);
		}

		private class Plan {

			private static enum Layer {
				Regular, Water, Air
			}

			private final SSSP.Result<Integer> distances;
			private final Set<Terrain.Category> attackerCanStandOn;
			private final Team attackerTeam;

			private static final Integer MoveWeight = Integer.valueOf(3);
			private static final Integer TransportWaterWeight = Integer.valueOf(10);
			private static final Integer TransportAirhWeight = Integer.valueOf(10);
			private static final Integer TransportFinishWeight = Integer.valueOf(1);

			Plan(Set<Terrain.Category> attackerCanStandOn, Team attackerTeam, int target, boolean useTransportWater,
					boolean useTransportAir) {
				this.attackerCanStandOn = attackerCanStandOn;
				this.attackerTeam = attackerTeam;

				/**
				 * We want to create a graph that will represent all move path of a unit. We
				 * create a graph with (layerNum * width * height) vertices, were Layer.Regular
				 * is the layer containing vertices and edges the unit can stand on by itself.
				 * Layer.Water and Layer.Air are containing the vertices and edges the unit can
				 * move while transported by either LandingCraft or TransportPlane respectively.
				 * In addition, there are edges connecting the layers representing the action of
				 * wrapping a unit by a transporter or finishing a transportation extracting the
				 * original unit from a wrapper transported unit. Within each layer all the
				 * edges have a weight of 1. Edges between layers have different weights as
				 * penalty for the action and cost.
				 */
				Graph<Integer> graph = new GraphArrayDirected<>(Layer.values().length * game.width() * game.height());

				/* Add vertices and edges of Layer.Regular */
				for (Iter.Int it = game.cells(); it.hasNext();) {
					int cell = it.next();
					if (!canStandOn(cell))
						continue;
					int u = cellToVIdx(cell, null);

					/* Add edges to 4 neighbors */
					for (Iter.Int nit = Cell.neighbors(cell); nit.hasNext();) {
						int neighbor = nit.next();
						if (!game.isValidCell(neighbor) || !canStandOn(neighbor))
							continue;
						int v = cellToVIdx(neighbor, null);
						graph.addEdge(u, v).setData(MoveWeight);
					}
				}

				/* Add vertices and edges of Layer.Water */
				if (useTransportWater) {
					for (Iter.Int it = game.cells(); it.hasNext();) {
						int cell = it.next();
						if (!canTransportOnWater(cell))
							continue;
						int u = cellToVIdx(cell, Unit.Type.LandingCraft);

						/* Add edges to 4 neighbors */
						for (Iter.Int nit = Cell.neighbors(cell); nit.hasNext();) {
							int neighbor = nit.next();
							if (!game.isValidCell(neighbor) || !canTransportOnWater(neighbor))
								continue;
							int v = cellToVIdx(neighbor, Unit.Type.LandingCraft);
							graph.addEdge(u, v).setData(MoveWeight);
						}

						/* Add edges to Layer.Regular */
						if (canStandOn(cell)) {
							int v = cellToVIdx(cell, null);
							graph.addEdge(u, v).setData(TransportWaterWeight);
							graph.addEdge(v, u).setData(TransportFinishWeight);
						}
					}
				}

				/* Add vertices and edges of Layer.Air */
				if (useTransportAir) {
					for (Iter.Int it = game.cells(); it.hasNext();) {
						int cell = it.next();
						if (!canTransportOnAir(cell))
							continue;
						int u = cellToVIdx(cell, Unit.Type.TransportPlane);

						/* Add edges to 4 neighbors */
						for (Iter.Int nit = Cell.neighbors(cell); nit.hasNext();) {
							int neighbor = nit.next();
							if (!game.isValidCell(neighbor) || !canTransportOnAir(neighbor))
								continue;
							int v = cellToVIdx(neighbor, Unit.Type.TransportPlane);
							graph.addEdge(u, v).setData(MoveWeight);
						}

						/* Add edges to Layer.Regular */
						if (canStandOn(cell)) {
							int v = cellToVIdx(cell, null);
							graph.addEdge(u, v).setData(TransportAirhWeight);
							graph.addEdge(v, u).setData(TransportFinishWeight);
						}
					}
				}

				/*
				 * Add artificial edges from target from the SSSP to find a path to it, as the
				 * unit can't stand on it
				 */
				for (Iter.Int nit = Cell.neighbors(target); nit.hasNext();) {
					int neighbor = nit.next();
					if (!game.isValidCell(neighbor) || !canStandOn(neighbor))
						continue;
					int v = cellToVIdx(neighbor, null);
					graph.addEdge(cellToVIdx(target, null), v).setData(MoveWeight);
				}

				/* Calculate all distances to the target using SSSP */
				Graph.WeightFunctionInt<Integer> w = e -> e.data().intValue();
				distances = sssp.calcDistances(graph, w, cellToVIdx(target, null));
			}

			@SuppressWarnings("unused")
			void debug() {
				System.out.println();
				for (int y = 0; y < game.height(); y++) {
					for (int x = 0; x < game.width(); x++) {
						int d = (int) getAttackDistance(Cell.of(x, y), null);
						System.out.print(String.format("%04d ", Integer.valueOf(d)));
					}
					System.out.println();
				}
				System.out.println("********");
				for (int y = 0; y < game.height(); y++) {
					for (int x = 0; x < game.width(); x++) {
						int d = (int) getAttackDistance(Cell.of(x, y), Unit.Type.TransportPlane);
						System.out.print(String.format("%04d ", Integer.valueOf(d)));
					}
					System.out.println();
				}
				System.out.println();
			}

			double getAttackDistance(int source, Unit.Type transportType) {
				double d = distances.distance(cellToVIdx(source, transportType));
				return d != Double.POSITIVE_INFINITY ? d : -1;
			}

			private boolean canStandOn(int cell) {
				if (!attackerCanStandOn.contains(game.terrain(cell).category))
					return false;
				Unit u = game.unit(cell);
				return u == null || u.getTeam() == attackerTeam;
			}

			private boolean canTransportOnWater(int cell) {
				if (!Unit.Type.LandingCraft.canStandOn(game.terrain(cell)))
					return false;
				Unit u = game.unit(cell);
				return u == null || u.getTeam() == attackerTeam;
			}

			private boolean canTransportOnAir(int cell) {
				if (!Unit.Type.TransportPlane.canStandOn(game.terrain(cell)))
					return false;
				Unit u = game.unit(cell);
				return u == null || u.getTeam() == attackerTeam;
			}

		}

		private static class PlanKey {
			final Set<Terrain.Category> attackerCanStandOn;
			final Team attackerTeam;
			final int target;
			final boolean useTransportWater;
			final boolean useTransportAir;

			PlanKey(Unit attacker, Unit target, boolean useTransportWater, boolean useTransportAir) {
				this.attackerCanStandOn = attacker.type.canStandOn;
				this.attackerTeam = attacker.getTeam();
				this.target = target.getPos();
				this.useTransportWater = useTransportWater;
				this.useTransportAir = useTransportAir;
			}

			@Override
			public int hashCode() {
				int h = 1;
				h = h * 31 + attackerCanStandOn.hashCode();
				h = h * 31 + attackerTeam.hashCode();
				h = h * 31 + target;
				h = h * 31 + Boolean.hashCode(useTransportWater);
				h = h * 31 + Boolean.hashCode(useTransportAir);
				return h;
			}

			@Override
			public boolean equals(Object other) {
				if (other == this)
					return true;
				if (!(other instanceof PlanKey))
					return false;
				PlanKey o = (PlanKey) other;
				return target == o.target && attackerTeam == o.attackerTeam && useTransportWater == o.useTransportWater
						&& useTransportAir == o.useTransportAir && attackerCanStandOn.equals(o.attackerCanStandOn);
			}

			@Override
			public String toString() {
				return "[" + attackerCanStandOn + ", " + attackerTeam + ", " + Cell.toString(target)
						+ ", useTransportWater=" + useTransportWater + ", useTransportAir=" + useTransportAir + "]";
			}
		}
	}

}
