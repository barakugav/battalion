package com.ugav.battalion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

abstract class Building extends Entity {

	enum Type {
		OilRefinery(Terrain.Category.FlatLand),

		OilRefineryBig(Terrain.Category.FlatLand),

		OilRig(Terrain.Category.Water),

		Factory(Terrain.Category.FlatLand);

		final Set<Terrain.Category> canBuildOn;

		Type(Terrain.Category... canBuildOn) {
			this.canBuildOn = Collections.unmodifiableSet(EnumSet.copyOf(List.of(canBuildOn)));
		}
	}

	final Type type;
	private Position pos;
	private Arena arena;
	private Team conquerTeam;
	private int conquerProgress;

	private static final int CONQUER_DURATION_FROM_NONE = 2;
	private static final int CONQUER_DURATION_FROM_OTHER = 3;

	Building(Type type, Team team) {
		super(team);
		this.type = type;
	}

	Position getPos() {
		return pos;
	}

	void setPos(Position pos) {
		this.pos = pos;
	}

	void setArena(Arena arena) {
		this.arena = arena;
	}

	Arena getArena() {
		return arena;
	}

	void tryConquer(Team conquerer) {
		if (conquerer != conquerTeam) {
			conquerTeam = null;
			conquerProgress = 0;
		}
		if (conquerer != null && conquerer != getTeam()) {
			conquerTeam = conquerer;
			int conquer_duration = getTeam() == Team.None ? CONQUER_DURATION_FROM_NONE : CONQUER_DURATION_FROM_OTHER;
			if (++conquerProgress == conquer_duration)
				setTeam(conquerer);
		}
	}

	abstract int getMoneyGain();

	static class Factory extends Building {

		Factory(Team team) {
			super(Type.Factory, team);
		}

		@Override
		int getMoneyGain() {
			return 0;
		}

		List<UnitSale> getAvailableUnits() {
			List<UnitSale> l = new ArrayList<>();
			l.add(UnitSale.of(Unit.Type.Soldier, 100));
			l.add(UnitSale.of(Unit.Type.Tank, 300));
			return l;
		}

		static class UnitSale {
			final Unit.Type type;
			final int price;

			UnitSale(Unit.Type type, int price) {
				this.type = type;
				this.price = price;
			}

			private static UnitSale of(Unit.Type type, int price) {
				return new UnitSale(type, price);
			}
		}

	}

	private static abstract class BuildingNonActive extends Building {

		BuildingNonActive(Type type, Team team) {
			super(type, team);
			super.setActive(false);
		}

		@Override
		final boolean isActive() {
			return false;
		}

		@Override
		final void setActive(boolean active) {
		}

	}

	static class OilRefinery extends BuildingNonActive {

		OilRefinery(Team team) {
			super(Type.OilRefinery, team);
		}

		@Override
		int getMoneyGain() {
			return 100;
		}

	}

	static class OilRefineryBig extends BuildingNonActive {

		OilRefineryBig(Team team) {
			super(Type.OilRefineryBig, team);
		}

		@Override
		int getMoneyGain() {
			return 200;
		}

	}

	static class OilRig extends BuildingNonActive {

		OilRig(Team team) {
			super(Type.OilRig, team);
		}

		@Override
		int getMoneyGain() {
			return 400;
		}

	}

	@Override
	public String toString() {
		return (getTeam() == Team.Red ? "R" : "B") + type;
	}

}
