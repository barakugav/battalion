package com.ugav.battalion.computer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import com.ugav.battalion.core.Action;
import com.ugav.battalion.core.Building;
import com.ugav.battalion.core.Building.UnitSale;
import com.ugav.battalion.core.Cell;
import com.ugav.battalion.core.Game;
import com.ugav.battalion.core.Team;
import com.ugav.battalion.core.Unit;
import com.ugav.battalion.util.Iter;
import com.ugav.battalion.util.Logger;

public class PlayerMiniMaxAlphaBeta implements Player {

	private final MiniMaxAlphaBeta<Action, Node, GameImpl> algo;

	private final int DepthLimit = 1;
	private final Logger logger = new Logger(true); // TODO

	public PlayerMiniMaxAlphaBeta() {
		algo = new MiniMaxAlphaBeta<>(new GameImpl(), DepthLimit);
	}

	@Override
	public void playTurn(Game game, Consumer<Action> actionsHandler) {
		final Team us = game.getTurn();
		for (;;) {
			long t0 = System.currentTimeMillis();
			Action action = algo.chooseAction(new Node(Game.copyOf(game)));
			long t1 = System.currentTimeMillis();
			logger.dbgln("Engine action computed in " + (t1 - t0) + "ms");
			if (action == null) {
				actionsHandler.accept(new Action.TurnEnd());
				return;
			}
			actionsHandler.accept(action);
			assert game.getTurn() == us;
		}
	}

	private static class GameImpl implements MiniMaxAlphaBeta.IGame<Action, Node> {

		@Override
		public int getNumberOfPlayers() {
			return Team.realTeams.size();
		}

		@Override
		public Node getModifiedPosition(Node position, Action action) {
			Node child = new Node(Game.copyOf(position.game));
			action.apply(child.game);
			return child;
		}

		@Override
		public double evaluate(Node position, int us) {
			return new Evaluator(position, us).evaluate();
		}

		private static int turnObjToInt(Team team) {
			return team.ordinal();
		}

		private static Team turnIntToObj(int team) {
			return Team.values()[team];
		}

	}

	private static class Evaluator {

		private final Game game;
		private final Team us;

		private static final double Aggression = 0.95;

		Evaluator(Node position, int us) {
			this.game = position.game;
			this.us = GameImpl.turnIntToObj(us);

			checkParams();
		}

		@SuppressWarnings("unused")
		private static void checkParams() {
			if (!(0 <= Aggression && Aggression <= 1))
				throw new IllegalArgumentException();
		}

		double evaluate() {
			if (game.isFinished())
				return game.getWinner() == us ? Double.MAX_VALUE : -Double.MAX_VALUE;
			double[] evals = new double[Team.values().length];

			for (Unit unit : game.units().forEach())
				evals[unit.getTeam().ordinal()] += evalUnit(unit);

			for (Building building : game.buildings().forEach()) {
				double buildingEval = evalBuilding(building);
				evals[building.getTeam().ordinal()] += buildingEval;
				Team conquerTeam = building.getConquerTeam();
				if (conquerTeam != null)
					evals[conquerTeam.ordinal()] += buildingEval * building.getConquerProgress();
			}

			for (Team team : Team.realTeams) {
				final double MoneyWeight = 0.2;
				int money = game.getMoney(team);
				evals[team.ordinal()] += MoneyWeight * Math.pow(money, 4.0 / 5.0);
			}

			double maxEnemyEval = 0;
			for (Team team : Team.realTeams)
				if (team != us)
					maxEnemyEval = Math.max(maxEnemyEval, evals[team.ordinal()]);
			double eval = (1 - Aggression) * evals[us.ordinal()] - Aggression * maxEnemyEval;

			return eval;
		}

		private double evalUnit(Unit unit) {
			double eval = 0;

			eval += unit.getHealth();

			double attackingEval;
			if (unit.isEnemyInRange()) {
				attackingEval = 1;
			} else {
				final Team us = unit.getTeam();
				int minDist = Integer.MAX_VALUE;
				for (Unit enemy : game.enemiesSeenBy(us).forEach()) {
					if (!unit.canAttack(enemy))
						continue;
					for (int neighbor : Cell.neighbors(enemy.getPos())) {
						if (!game.isValidCell(neighbor))
							continue;
						int d = unit.getDistanceTo(neighbor);
						if (d < 0)
							continue; /* unreachable */
						if (d < minDist)
							minDist = d;
					}
				}

				attackingEval = Math.min((double) unit.type.moveLimit / minDist, 0.9);
			}
			eval += attackingEval * 20;

			return eval;
		}

		private double evalBuilding(Building building) {
			double eval = 0;
			eval += building.getMoneyGain();
			if (building.type.canBuildUnits) {
				eval += 50;
				if (game.unit(building.getPos()) != null)
					eval -= 25; /* Factory is blocked */
			}
			if (building.type.allowUnitBuildLand)
				eval += 20;
			if (building.type.allowUnitBuildWater)
				eval += 20;
			if (building.type.allowUnitBuildAir)
				eval += 20;
			return eval;
		}
	}

	private static class Node implements MiniMaxAlphaBeta.IPosition<Action> {

		private final Game game;

		Node(Game game) {
			this.game = Objects.requireNonNull(game);
		}

		@Override
		public boolean isTerminated() {
			return game.isFinished();
		}

		@Override
		public int getTurn() {
			return GameImpl.turnObjToInt(game.getTurn());
		}

		@Override
		public Iter<Action> availableActions() {
			if (isTerminated())
				return Iter.empty();
			List<Action> actions = new ArrayList<>();
			final Team us = game.getTurn();

			for (Unit unit : game.units().filter(u -> us == u.getTeam() && u.isActive()).forEach())
				unitAvailableActions(unit, actions);

			for (Building factory : game.buildings()
					.filter(b -> us == b.getTeam() && b.isActive() && b.type.canBuildUnits).forEach())
				factoryAvailableActions(factory, actions);

			return Iter.of(actions);
		}

		private void unitAvailableActions(Unit unit, List<Action> actions) {
			if (unit.getTeam() != game.getTurn() || !unit.isActive())
				return;

			Cell.Bitmap attackable = unit.getAttackableMap();
			Cell.Bitmap reachable = unit.getReachableMap();

			unitAvailableActionsAttack(unit, reachable, attackable, actions);
			unitAvailableActionsMove(unit, reachable, actions);

			// TODO transport unit actions
		}

		private void factoryAvailableActions(Building factory, List<Action> actions) {
			if (factory.getTeam() != game.getTurn() || !factory.isActive() || game.unit(factory.getPos()) != null)
				return;
			final int money = game.getMoney(factory.getTeam());

			for (UnitSale sale : factory.getAvailableUnits().values())
				if (sale != null && sale.price <= money)
					actions.add(new Action.UnitBuild(factory.getPos(), sale.type));
		}

		private static void unitAvailableActionsMove(Unit unit, Cell.Bitmap reachable, List<Action> actions) {
			int unitPos = unit.getPos();

			for (Iter.Int it = reachable.cells(); it.hasNext();) {
				int destination = it.next();
				if (destination != unitPos)
					actions.add(new Action.UnitMove(unitPos, unit.calcPath(destination)));
			}
		}

		private static void unitAvailableActionsAttack(Unit unit, Cell.Bitmap reachable, Cell.Bitmap attackable,
				List<Action> actions) {
			switch (unit.type.weapon.type) {
			case CloseRange:
				unitAvailableActionsAttackAndMove(unit, reachable, attackable, actions);
				break;
			case LongRange:
				unitAvailableActionsAttackRange(unit, attackable, actions);
				break;
			case None:
				break;
			default:
				throw new IllegalArgumentException("Unexpected value: " + unit.type.weapon.type);
			}
		}

		private static void unitAvailableActionsAttackAndMove(Unit unit, Cell.Bitmap reachable, Cell.Bitmap attackable,
				List<Action> actions) {
			int attackerPos = unit.getPos();
			for (Iter.Int it = attackable.cells(); it.hasNext();) {
				int target = it.next();
				for (int destination : Cell.neighbors(target))
					if (reachable.contains(destination))
						actions.add(new Action.UnitMoveAndAttack(attackerPos, unit.calcPath(destination), target));
			}
		}

		private static void unitAvailableActionsAttackRange(Unit unit, Cell.Bitmap attackable, List<Action> actions) {
			int attackerPos = unit.getPos();
			for (Iter.Int it = attackable.cells(); it.hasNext();) {
				int target = it.next();
				actions.add(new Action.UnitAttackLongRange(attackerPos, target));
			}
		}
	}

}
