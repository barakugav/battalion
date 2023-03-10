package com.ugav.battalion.core;

import java.util.Objects;

import com.ugav.battalion.util.ListInt;

public abstract class Action {

	public static final Start Start = new Start();

	public static class Start extends Action {
		private Start() {
		}

		@Override
		public String toString() {
			return "Start";
		}

	}

	public static final TurnEnd TurnEnd = new TurnEnd();

	public static class TurnEnd extends Action {
		private TurnEnd() {
		}

		@Override
		public String toString() {
			return "TurnEnd";
		}

	}

	public static class UnitMove extends Action {
		final int source;
		final ListInt path;

		public UnitMove(int source, ListInt path) {
			this.source = source;
			this.path = path.copy().unmodifiableView();
		}

		@Override
		public String toString() {
			return "UnitMove(" + Cell.toString(source) + ", " + Cell.toString(path) + ")";
		}

	}

	public static class UnitMoveAndAttack extends Action {
		final int attacker;
		final ListInt path;
		final int target;

		public UnitMoveAndAttack(int attacker, ListInt path, int target) {
			this.attacker = attacker;
			this.path = path.copy().unmodifiableView();
			this.target = target;
		}

		@Override
		public String toString() {
			return "UnitMoveAndAttack(" + Cell.toString(attacker) + ", " + Cell.toString(path) + ", "
					+ Cell.toString(target) + ")";
		}

	}

	public static class UnitAttackLongRange extends Action {

		final int attacker;
		final int target;

		public UnitAttackLongRange(int attacker, int target) {
			this.attacker = attacker;
			this.target = target;
		}

		@Override
		public String toString() {
			return "UnitAttackLongRange(" + Cell.toString(attacker) + ", " + Cell.toString(target) + ")";
		}

	}

	public static class UnitBuild extends Action {

		final int factory;
		final Unit.Type unit;

		public UnitBuild(int factory, Unit.Type unit) {
			this.factory = factory;
			this.unit = Objects.requireNonNull(unit);
		}

		@Override
		public String toString() {
			return "UnitBuild(" + Cell.toString(factory) + ", " + unit + ")";
		}

	}

	public static class UnitTransport extends Action {

		final int unit;
		final Unit.Type transport;

		public UnitTransport(int unit, Unit.Type transport) {
			this.unit = unit;
			this.transport = Objects.requireNonNull(transport);
		}

		@Override
		public String toString() {
			return "UnitTransport(" + Cell.toString(unit) + ", " + transport + ")";
		}

	}

	public static class UnitTransportFinish extends Action {

		final int unit;

		public UnitTransportFinish(int unit) {
			this.unit = unit;
		}

		@Override
		public String toString() {
			return "UnitTransportFinish(" + Cell.toString(unit) + ")";
		}

	}

	public static class UnitRepair extends Action {

		final int unit;

		public UnitRepair(int unit) {
			this.unit = unit;
		}

		@Override
		public String toString() {
			return "UnitRepair(" + Cell.toString(unit) + ")";
		}

	}

}
