package com.ugav.battalion;

abstract class Building extends Entity {

	enum Type {
		OilRefinery, Factory
	}

	final Type type;
	private Position pos;

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

	static class OilRefinery extends Building {

		OilRefinery(Team team) {
			super(Type.OilRefinery, team);
		}

	}

	static class Factory extends Building {

		Factory(Team team) {
			super(Type.Factory, team);
		}

	}

	@Override
	public String toString() {
		return (getTeam() == Team.Red ? "R" : "B") + type;
	}

}
