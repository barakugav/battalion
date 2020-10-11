package com.ugav.battalion;

public abstract class Building extends AbstractEntity {

    public Building(Team team) {
	super(team);
    }

    @Override
    public abstract Building deepCopy();

    public static class OilRefinery extends Building {

	public OilRefinery(Team team) {
	    super(team);
	}

	@Override
	public OilRefinery deepCopy() {
	    return new OilRefinery(getTeam());
	}

    }

    public static class Factory extends Building {

	public Factory(Team team) {
	    super(team);
	}

	@Override
	public Factory deepCopy() {
	    return new Factory(getTeam());
	}

    }

}
