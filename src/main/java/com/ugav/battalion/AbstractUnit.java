package com.ugav.battalion;

import java.util.Objects;

public abstract class AbstractUnit extends AbstractEntity implements Unit {

    private ExpolredMap expolredMap;
    private int health;

    AbstractUnit(Team team) {
	super(team);
	if (getTeam() == Team.None)
	    throw new IllegalArgumentException();
	expolredMap = null;
	health = getMaxHealth();
    }

    @Override
    public final void setTeam(Team team) {
	throw new UnsupportedOperationException();
    }

    @Override
    public final int getHealth() {
	return health;
    }

    @Override
    public ExpolredMap getExploredMap() {
	if (expolredMap == null)
	    throw new IllegalStateException();
	return expolredMap;
    }

    @Override
    public void setExploredMap(ExpolredMap expolredMap) {
	this.expolredMap = Objects.requireNonNull(expolredMap);
    }

    @Override
    public void invalidateExploredMap() {
	expolredMap = null;
    }

}
