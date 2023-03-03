package com.ugav.battalion.core;

import java.util.Objects;

import com.ugav.battalion.util.ListInt;

public abstract class Action {

	public abstract void apply(Game game);

	public static class UnitMove extends Action {
		private final int source;
		private final int destination;

		public UnitMove(int source, int destination) {
			this.source = source;
			this.destination = destination;
		}

		@Override
		public void apply(Game game) {
			Unit unit = game.unit(source);
			game.move(unit, unit.calcPath(destination));
		}

		@Override
		public String toString() {
			return "UnitMove(" + Cell.toString(source) + ", " + Cell.toString(destination) + ")";
		}

	}

	public static class UnitMoveAndAttack extends Action {
		private final int attacker;
		private final int destination;
		private final int target;

		public UnitMoveAndAttack(int attacker, int destination, int target) {
			this.attacker = attacker;
			this.destination = destination;
			this.target = target;
		}

		@Override
		public void apply(Game game) {
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

	public static class UnitAttackLongRange extends Action {
		private final int attacker;
		private final int target;

		public UnitAttackLongRange(int attacker, int target) {
			this.attacker = attacker;
			this.target = target;
		}

		@Override
		public void apply(Game game) {
			game.attackRange(game.unit(attacker), game.unit(target));
		}

		@Override
		public String toString() {
			return "UnitAttackLongRange(" + Cell.toString(attacker) + ", " + Cell.toString(target) + ")";
		}

	}

	public static class UnitBuild extends Action {

		private final int factory;
		private final Unit.Type unit;

		public UnitBuild(int factory, Unit.Type unit) {
			this.factory = factory;
			this.unit = Objects.requireNonNull(unit);
		}

		@Override
		public void apply(Game game) {
			game.buildUnit(game.building(factory), unit);
		}

		@Override
		public String toString() {
			return "UnitBuild(" + Cell.toString(factory) + ", " + unit + ")";
		}

	}

}
