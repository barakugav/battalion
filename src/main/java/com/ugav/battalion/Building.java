package com.ugav.battalion;

abstract class Building extends Entity {

	enum Type {
		OilRefinery, Factory
	}

	final Type type;

	Building(Type type, Team team) {
		super(team);
		this.type = type;
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

}
