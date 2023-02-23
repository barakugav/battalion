package com.ugav.battalion.computer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.ugav.battalion.core.Cell;
import com.ugav.battalion.core.Game;
import com.ugav.battalion.core.Team;
import com.ugav.battalion.core.Unit;
import com.ugav.battalion.util.DebugPrintsManager;
import com.ugav.battalion.util.Iter;

public class PlayerMiniMaxAlphaBeta implements Player {

	private final MiniMaxAlphaBeta<Move, Node, GameImpl> algo;

	private final int DepthLimit = 2;
	private final DebugPrintsManager debug = new DebugPrintsManager(true); // TODO

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
			debug.println("engine move time: " + (t1 - t0));
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
		public double evaluate(Node position, int us0) {
			final Team us = turnIntToObj(us0);
			double eval = 0;
			for (Unit unit : position.game.arena().units().forEach())
				eval += (unit.getTeam() == us ? 1 : -1) * evalUnit(unit);
			return eval;
		}

		private static double evalUnit(Unit unit) {
			return unit.getHealth();
		}

		private static int turnObjToInt(Team team) {
			return team.ordinal();
		}

		private static Team turnIntToObj(int team) {
			return Team.values()[team];
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
			for (Unit unit : game.arena().units().filter(u -> us == u.getTeam() && u.isActive()).forEach())
				unitAvailableMoves(unit, moves);
			return Iter.of(moves.iterator());
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

		private static void unitAvailableMovesChangePosition(Unit unit, Cell.Bitmap reachable, List<Move> moves) {
			int unitPos = unit.getPos();

			for (Iter.Int it = reachable.cells(); it.hasNext();) {
				int destination = it.next();
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
			Unit unit = game.getUnit(source);
			game.move(unit, game.calcRealPath(unit, unit.calcPath(destination)));
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
			Unit unit = game.getUnit(attacker);
			List<Integer> path = unit.calcPath(destination);
			List<Integer> realPath = game.calcRealPath(unit, path);
			if (path.size() == realPath.size())
				game.moveAndAttack(unit, realPath, game.getUnit(target));
			else
				game.move(unit, realPath);
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
			game.attackRange(game.getUnit(attacker), game.getUnit(target));
		}

		@Override
		public String toString() {
			return "UnitAttackLongRange(" + Cell.toString(attacker) + ", " + Cell.toString(target) + ")";
		}

	}

}
