package com.ugav.battalion;

class DataEvent {
	final Object source;

	DataEvent(Object source) {
		this.source = source;
	}

	static class NewUnit extends DataEvent {

		final Unit unit;

		NewUnit(Game source, Unit unit) {
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

}
