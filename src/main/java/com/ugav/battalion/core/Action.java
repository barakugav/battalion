package com.ugav.battalion.core;

import java.util.Objects;

import com.ugav.battalion.util.ListInt;

public abstract class Action {

	public abstract void apply(Game game);

	public static class Start extends Action {

		@Override
		public void apply(Game game) {
			game.start();
		}

		@Override
		public String toString() {
			return "Start";
		}

	}

	public static class TurnEnd extends Action {

		@Override
		public void apply(Game game) {
			game.turnEnd();
		}

		@Override
		public String toString() {
			return "TurnEnd";
		}

	}

	public static class UnitMove extends Action {
		private final int source;
		private final ListInt path;

		public UnitMove(int source, ListInt path) {
			this.source = source;
			this.path = path.copy().unmodifiableView();
		}

		@Override
		public void apply(Game game) {
			Unit unit = Objects.requireNonNull(game.unit(source), toString());
			game.move(unit, path);
		}

		@Override
		public String toString() {
			return "UnitMove(" + Cell.toString(source) + ", " + Cell.toString(path) + ")";
		}

	}

	public static class UnitMoveAndAttack extends Action {
		private final int attacker;
		private final ListInt path;
		private final int target;

		public UnitMoveAndAttack(int attacker, ListInt path, int target) {
			this.attacker = attacker;
			this.path = path.copy().unmodifiableView();
			this.target = target;
		}

		@Override
		public void apply(Game game) {
			game.moveAndAttack(game.unit(attacker), path, game.unit(target));
		}

		@Override
		public String toString() {
			return "UnitMoveAndAttack(" + Cell.toString(attacker) + ", " + Cell.toString(path) + ", "
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

	public static class UnitTransport extends Action {

		private final int unit;
		private final Unit.Type transport;

		public UnitTransport(int unit, Unit.Type transport) {
			this.unit = unit;
			this.transport = Objects.requireNonNull(transport);
		}

		@Override
		public void apply(Game game) {
			game.unitTransport(game.unit(unit), transport);
		}

		@Override
		public String toString() {
			return "UnitTransport(" + Cell.toString(unit) + ", " + transport + ")";
		}

	}

	public static class UnitTransportFinish extends Action {

		private final int unit;

		public UnitTransportFinish(int unit) {
			this.unit = unit;
		}

		@Override
		public void apply(Game game) {
			game.transportFinish(game.unit(unit));
		}

		@Override
		public String toString() {
			return "UnitTransportFinish(" + Cell.toString(unit) + ")";
		}

	}

	public static class UnitRepair extends Action {

		private final int unit;

		public UnitRepair(int unit) {
			this.unit = unit;
		}

		@Override
		public void apply(Game game) {
			game.unitRepair(game.unit(unit));
		}

		@Override
		public String toString() {
			return "UnitRepair(" + Cell.toString(unit) + ")";
		}

	}

}
