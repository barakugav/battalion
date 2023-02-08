package com.ugav.battalion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ugav.battalion.core.Game;
import com.ugav.battalion.core.LevelBuilder;
import com.ugav.battalion.core.Position;
import com.ugav.battalion.core.Team;
import com.ugav.battalion.core.Unit;

public class DataEvent {
	public final Object source;

	public DataEvent(Object source) {
		this.source = source;
	}

	public static class UnitAdd extends DataEvent {

		public final Unit unit;

		public UnitAdd(Game source, Unit unit) {
			super(source);
			this.unit = unit;
		}

	}

	public static class UnitRemove extends DataEvent {

		public final Unit unit;

		public UnitRemove(Game source, Unit unit) {
			super(source);
			this.unit = unit;
		}

	}

	public static class UnitMove extends DataEvent {

		public final Unit unit;
		public final List<Position> path;

		public UnitMove(Game source, Unit unit, List<Position> path) {
			super(source);
			this.unit = unit;
			List<Position> tempPath = new ArrayList<>(path.size() + 1);
			tempPath.add(unit.getPos());
			tempPath.addAll(path);
			this.path = Collections.unmodifiableList(tempPath);
		}

	}

	public static class MoneyChange extends DataEvent {

		public final Team team;
		public final int newAmount;

		public MoneyChange(Game source, Team team, int newAmount) {
			super(source);
			this.team = team;
			this.newAmount = newAmount;
		}

	}

	public static class TileChange extends DataEvent {

		public final Position pos;

		public TileChange(LevelBuilder source, Position pos) {
			super(source);
			this.pos = pos;
		}

	}

	public static class LevelReset extends DataEvent {

		public LevelReset(LevelBuilder source) {
			super(source);
		}

	}

}
