package com.ugav.battalion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

	static class UnitMove extends DataEvent {

		final Unit unit;
		final List<Position> path;

		UnitMove(Game source, Unit unit, List<Position> path) {
			super(source);
			this.unit = unit;
			List<Position> tempPath = new ArrayList<>(path.size() + 1);
			tempPath.add(unit.getPos());
			tempPath.addAll(path);
			this.path = Collections.unmodifiableList(tempPath);
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

	static class LevelReset extends DataEvent {

		LevelReset(LevelBuilder source) {
			super(source);
		}

	}

}
