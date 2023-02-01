package com.ugav.battalion;

class DataEvent {
	final Object source;

	DataEvent(Object source) {
		this.source = source;
	}

	static class UnitAdd extends DataEvent {

		final Unit unit;

		UnitAdd(Game source, Unit unit) {
			super(source);
			this.unit = unit;
		}

	}

	static class UnitRemove extends DataEvent {

		final Unit unit;

		UnitRemove(Game source, Unit unit) {
			super(source);
			this.unit = unit;
		}

	}

	static class MoneyChange extends DataEvent {

		final Team team;
		final int newAmount;

		MoneyChange(Game source, Team team, int newAmount) {
			super(source);
			this.team = team;
			this.newAmount = newAmount;
		}

	}

	static class TileChange extends DataEvent {

		final Position pos;

		TileChange(LevelBuilder source, Position pos) {
			super(source);
			this.pos = pos;
		}

	}

}
