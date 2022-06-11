package com.ugav.battalion;

import java.util.Objects;

public abstract class AbstractEntity implements Entity {

	private Team team;
	private boolean canMove;

	AbstractEntity(Team team) {
		this.team = Objects.requireNonNull(team);
		canMove = false;
	}

	@Override
	public final Team getTeam() {
		return team;
	}

	@Override
	public void setTeam(Team team) {
		this.team = Objects.requireNonNull(team);
	}

	@Override
	public final boolean canAct() {
		return canMove;
	}

	@Override
	public final void setCanAct(boolean canMove) {
		this.canMove = canMove;
	}

}
