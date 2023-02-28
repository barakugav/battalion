package com.ugav.battalion.core;

import java.util.Objects;

import com.ugav.battalion.core.Game.EntityChange;
import com.ugav.battalion.util.Event;

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
		if (Objects.equals(this.team, team))
			return;
		this.team = Objects.requireNonNull(team);
		onChange().notify(new EntityChange(this));
	}

	public boolean isActive() {
		return active;
	}

	void setActive(boolean active) {
		if (this.active == active)
			return;
		this.active = active;
		onChange().notify(new EntityChange(this));
	}

	public Event.Notifier<EntityChange> onChange() {
		return arena.onEntityChange;
	}

}
