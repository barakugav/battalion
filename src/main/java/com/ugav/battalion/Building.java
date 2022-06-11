package com.ugav.battalion;

abstract class Building extends EntityAbstract {

	Building(Team team) {
		super(team);
	}

	@Override
	public abstract Building deepCopy();

	static class OilRefinery extends Building {

		OilRefinery(Team team) {
			super(team);
		}

		@Override
		public OilRefinery deepCopy() {
			return new OilRefinery(getTeam());
		}

	}

	static class Factory extends Building {

		Factory(Team team) {
			super(team);
		}

		@Override
		public Factory deepCopy() {
			return new Factory(getTeam());
		}

	}

}
