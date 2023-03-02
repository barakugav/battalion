package com.ugav.battalion.computer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.ugav.battalion.core.Building;
import com.ugav.battalion.core.Building.UnitSale;
import com.ugav.battalion.core.Cell;
import com.ugav.battalion.core.Game;
import com.ugav.battalion.core.Team;
import com.ugav.battalion.core.Unit;
import com.ugav.battalion.util.Iter;
import com.ugav.battalion.util.ListInt;
import com.ugav.battalion.util.Logger;

public class PlayerMiniMaxAlphaBeta implements Player {

	private final MiniMaxAlphaBeta<Move, Node, GameImpl> algo;

	private final int DepthLimit = 1;
	private final Logger logger = new Logger(true); // TODO

	public PlayerMiniMaxAlphaBeta() {
		algo = new MiniMaxAlphaBeta<>(new GameImpl(), DepthLimit);
	}

	@Override
	public void playTurn(Game game) {
		final Team us = game.getTurn();
		for (;;) {
			long t0 = System.currentTimeMillis();
			Move move = algo.chooseMove(new Node(Game.copyOf(game)));
			long t1 = System.currentTimeMillis();
			logger.dbgln("Engine move (" + (t1 - t0) + "ms): " + move);
			if (move == null)
				return;
			move.apply(game);
			assert game.getTurn() == us;
		}
	}

	private static class GameImpl implements MiniMaxAlphaBeta.IGame<Move, Node> {

		@Override
		public int getNumberOfPlayers() {
			return Team.realTeams.size();
		}

		@Override
		public Node getMovedPosition(Node position, Move move) {
			Node child = new Node(Game.copyOf(position.game));
			move.apply(child.game);
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

	private static class Node implements MiniMaxAlphaBeta.IPosition<Move> {

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
		public Iter<Move> availableMoves() {
			if (isTerminated())
				return Iter.empty();
			List<Move> moves = new ArrayList<>();
			final Team us = game.getTurn();

			for (Unit unit : game.units().filter(u -> us == u.getTeam() && u.isActive()).forEach())
				unitAvailableMoves(unit, moves);

			for (Building factory : game.buildings()
					.filter(b -> us == b.getTeam() && b.isActive() && b.type.canBuildUnits).forEach())
				factoryAvailableMoves(factory, moves);

			return Iter.of(moves);
		}

		private void unitAvailableMoves(Unit unit, List<Move> moves) {
			if (unit.getTeam() != game.getTurn() || !unit.isActive())
				return;

			Cell.Bitmap attackable = unit.getAttackableMap();
			Cell.Bitmap reachable = unit.getReachableMap();

			unitAvailableMovesAttack(unit, reachable, attackable, moves);
			unitAvailableMovesChangePosition(unit, reachable, moves);

			// TODO transport unit moves
		}

		private void factoryAvailableMoves(Building factory, List<Move> moves) {
			if (factory.getTeam() != game.getTurn() || !factory.isActive() || game.unit(factory.getPos()) != null)
				return;
			final int money = game.getMoney(factory.getTeam());

			for (UnitSale sale : factory.getAvailableUnits().values())
				if (sale != null && sale.price <= money)
					moves.add(new UnitBuild(factory, sale.type));
		}

		private static void unitAvailableMovesChangePosition(Unit unit, Cell.Bitmap reachable, List<Move> moves) {
			int unitPos = unit.getPos();

			for (Iter.Int it = reachable.cells(); it.hasNext();) {
				int destination = it.next();
				if (destination != unitPos)
					moves.add(new UnitMove(unitPos, destination));
			}
		}

		private static void unitAvailableMovesAttack(Unit unit, Cell.Bitmap reachable, Cell.Bitmap attackable,
				List<Move> moves) {
			switch (unit.type.weapon.type) {
			case CloseRange:
				unitAvailableMovesAttackAndMove(unit, reachable, attackable, moves);
				break;
			case LongRange:
				unitAvailableMovesAttackRange(unit, attackable, moves);
				break;
			case None:
				break;
			default:
				throw new IllegalArgumentException("Unexpected value: " + unit.type.weapon.type);
			}
		}

		private static void unitAvailableMovesAttackAndMove(Unit unit, Cell.Bitmap reachable, Cell.Bitmap attackable,
				List<Move> moves) {
			int attackerPos = unit.getPos();
			for (Iter.Int it = attackable.cells(); it.hasNext();) {
				int target = it.next();
				for (int destination : Cell.neighbors(target))
					if (reachable.contains(destination))
						moves.add(new UnitMoveAndAttack(attackerPos, destination, target));
			}
		}

		private static void unitAvailableMovesAttackRange(Unit unit, Cell.Bitmap attackable, List<Move> moves) {
			int attackerPos = unit.getPos();
			for (Iter.Int it = attackable.cells(); it.hasNext();) {
				int target = it.next();
				moves.add(new UnitAttackLongRange(attackerPos, target));
			}
		}
	}

	private abstract static class Move implements MiniMaxAlphaBeta.IMove {

		abstract void apply(Game game);

	}

	private static class UnitMove extends Move {
		private final int source;
		private final int destination;

		UnitMove(int source, int destination) {
			this.source = source;
			this.destination = destination;
		}

		@Override
		void apply(Game game) {
			Unit unit = game.unit(source);
			game.move(unit, unit.calcPath(destination));
		}

		@Override
		public String toString() {
			return "UnitMove(" + Cell.toString(source) + ", " + Cell.toString(destination) + ")";
		}

	}

	private static class UnitMoveAndAttack extends Move {
		private final int attacker;
		private final int destination;
		private final int target;

		UnitMoveAndAttack(int attacker, int destination, int target) {
			this.attacker = attacker;
			this.destination = destination;
			this.target = target;
		}

		@Override
		void apply(Game game) {
			Unit unit = game.unit(attacker);
			ListInt path = unit.calcPath(destination);
			game.moveAndAttack(unit, path, game.unit(target));
		}

		@Override
		public String toString() {
			return "UnitMoveAndAttack(" + Cell.toString(attacker) + ", " + Cell.toString(destination) + ", "
					+ Cell.toString(target) + ")";
		}

	}

	private static class UnitAttackLongRange extends Move {
		private final int attacker;
		private final int target;

		UnitAttackLongRange(int attacker, int target) {
			this.attacker = attacker;
			this.target = target;
		}

		@Override
		void apply(Game game) {
			game.attackRange(game.unit(attacker), game.unit(target));
		}

		@Override
		public String toString() {
			return "UnitAttackLongRange(" + Cell.toString(attacker) + ", " + Cell.toString(target) + ")";
		}

	}

	private static class UnitBuild extends Move {

		private final int factory;
		private final Unit.Type unit;

		UnitBuild(Building factory, Unit.Type unit) {
			this.factory = factory.getPos();
			this.unit = Objects.requireNonNull(unit);
		}

		@Override
		void apply(Game game) {
			game.buildUnit(game.building(factory), unit);
		}

		@Override
		public String toString() {
			return "UnitBuild(" + Cell.toString(factory) + ", " + unit + ")";
		}

	}

}
