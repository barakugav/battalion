package com.ugav.battalion;

import java.util.Objects;

import com.ugav.battalion.Images.Drawable;

abstract class Entity implements Drawable {

	private Team team;
	private boolean active;

	Entity(Team team) {
		this.team = Objects.requireNonNull(team);
		active = false;
	}

	public Team getTeam() {
		return team;
	}

	public void setTeam(Team team) {
		this.team = Objects.requireNonNull(team);
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

}
