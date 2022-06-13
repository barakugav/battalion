package com.ugav.battalion;

import java.util.ArrayList;
import java.util.List;

abstract class Building extends Entity {

	enum Type {
		OilRefinery, Factory
	}

	final Type type;
	private Position pos;
	private Arena arena;

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

	@Override
	public String toString() {
		return (getTeam() == Team.Red ? "R" : "B") + type;
	}

}
