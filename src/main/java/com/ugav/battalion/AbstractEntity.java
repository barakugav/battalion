package com.ugav.battalion;

import java.util.Objects;

public abstract class AbstractEntity implements Entity {

    private Team team;
    private boolean canMove;

    AbstractEntity(Team team) {
	this.team = Objects.requireNonNull(team);
	canMove = false;
    }

    public final Team getTeam() {
	return team;
    }

    public void setTeam(Team team) {
	this.team = Objects.requireNonNull(team);
    }

    public final boolean canAct() {
	return canMove;
    }

    public final void setCanAct(boolean canMove) {
	this.canMove = canMove;
    }

}
