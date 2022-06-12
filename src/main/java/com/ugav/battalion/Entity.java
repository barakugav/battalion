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

	Team getTeam() {
		return team;
	}

	void setTeam(Team team) {
		this.team = Objects.requireNonNull(team);
	}

	boolean isActive() {
		return active;
	}

	void setActive(boolean active) {
		this.active = active;
	}

}
