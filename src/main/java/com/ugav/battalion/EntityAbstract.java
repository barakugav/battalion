package com.ugav.battalion;

import java.util.Objects;

abstract class EntityAbstract implements Entity {

	private Team team;
	private boolean active;

	EntityAbstract(Team team) {
		this.team = Objects.requireNonNull(team);
		active = false;
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
	public final boolean isActive() {
		return active;
	}

	@Override
	public final void setActive(boolean active) {
		this.active = active;
	}

}
