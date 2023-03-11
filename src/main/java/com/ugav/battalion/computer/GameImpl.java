package com.ugav.battalion.computer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.ugav.battalion.computer.GameTreeAlg.IGame;
import com.ugav.battalion.core.Action;
import com.ugav.battalion.core.Building;
import com.ugav.battalion.core.Cell;
import com.ugav.battalion.core.Game;
import com.ugav.battalion.core.Team;
import com.ugav.battalion.core.Unit;
import com.ugav.battalion.util.Iter;

class GameImpl implements IGame<Action, GameImpl.Node> {

	@Override
	public int getNumberOfPlayers() {
		return Team.values().length;
	}

	@Override
	public Node getModifiedPosition(Node position, Action action) {
		Node child = new Node(Game.copyOf(position.game));
		child.game.performAction(action);
		return child;
	}

	static int turnObjToInt(Team team) {
		return team.ordinal();
	}

	static Team turnIntToObj(int team) {
		return Team.values()[team];
	}

	static class Node implements IGame.IPosition<Action> {

		final Game game;

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

			unitAvailableActionsAttack(unit, actions);
			unitAvailableActionsMove(unit, actions);
			unitAvailableActionsTransport(unit, actions);
			if (unit.canRepair())
				actions.add(new Action.UnitRepair(unit.getPos()));
		}

		private static void factoryAvailableActions(Building factory, List<Action> actions) {
			for (Unit.Type type : Unit.Type.values())
				if (factory.canBuildUnit(type))
					actions.add(new Action.UnitBuild(factory.getPos(), type));
		}

		private static void unitAvailableActionsMove(Unit unit, List<Action> actions) {
			int unitPos = unit.getPos();
			Cell.Bitmap reachable = unit.getReachableMap();

			for (Iter.Int it = reachable.cells(); it.hasNext();) {
				int destination = it.next();
				if (destination != unitPos)
					actions.add(new Action.UnitMove(unitPos, unit.calcPath(destination)));
			}
		}

		private static void unitAvailableActionsAttack(Unit unit, List<Action> actions) {
			switch (unit.type.weapon.type) {
			case CloseRange:
				unitAvailableActionsAttackAndMove(unit, actions);
				break;
			case LongRange:
				unitAvailableActionsAttackRange(unit, actions);
				break;
			case None:
				break;
			default:
				throw new IllegalArgumentException("Unexpected value: " + unit.type.weapon.type);
			}
		}

		private static void unitAvailableActionsTransport(Unit unit, List<Action> actions) {
			for (Unit.Type transport : List.of(Unit.Type.LandingCraft, Unit.Type.TransportPlane))
				if (unit.canTransport(transport))
					actions.add(new Action.UnitTransport(unit.getPos(), transport));
			if (unit.canFinishTransport())
				actions.add(new Action.UnitTransportFinish(unit.getPos()));
		}

		private static void unitAvailableActionsAttackAndMove(Unit unit, List<Action> actions) {
			int attackerPos = unit.getPos();
			Cell.Bitmap attackable = unit.getAttackableMap();
			Cell.Bitmap reachable = unit.getReachableMap();
			for (Iter.Int it = attackable.cells(); it.hasNext();) {
				int target = it.next();
				for (Iter.Int nit = Cell.neighbors(target); nit.hasNext();) {
					int destination = nit.next();
					if (reachable.contains(destination))
						actions.add(new Action.UnitMoveAndAttack(attackerPos, unit.calcPath(destination), target));
				}
			}
		}

		private static void unitAvailableActionsAttackRange(Unit unit, List<Action> actions) {
			int attackerPos = unit.getPos();
			Cell.Bitmap attackable = unit.getAttackableMap();
			for (Iter.Int it = attackable.cells(); it.hasNext();) {
				int target = it.next();
				actions.add(new Action.UnitAttackLongRange(attackerPos, target));
			}
		}
	}

}
