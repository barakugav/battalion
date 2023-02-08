package com.ugav.battalion.core;

import java.util.Objects;

import com.ugav.battalion.DataChangeNotifier;
import com.ugav.battalion.DataEvent.EntityChange;

public abstract class Entity {

	final Arena arena;
	private Team team;
	private boolean active;

	Entity(Arena arena, Team team) {
		this.arena = Objects.requireNonNull(arena);
		this.team = Objects.requireNonNull(team);
		active = false;
	}

	public Team getTeam() {
		return team;
	}

	void setTeam(Team team) {
		this.team = Objects.requireNonNull(team);
		onChange().notify(new EntityChange(this));
	}

	public boolean isActive() {
		return active;
	}

	void setActive(boolean active) {
		this.active = active;
		onChange().notify(new EntityChange(this));
	}

	DataChangeNotifier<EntityChange> onChange() {
		return arena.onChange;
	}

}
