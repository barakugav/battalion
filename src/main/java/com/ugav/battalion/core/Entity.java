package com.ugav.battalion.core;

import java.util.Objects;

import com.ugav.battalion.core.Game.EntityChange;
import com.ugav.battalion.util.Event;

public abstract class Entity {

	final Game game;
	private Team team;
	private boolean active;

	Entity(Game game, Team team) {
		this.game = Objects.requireNonNull(game);
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

	Event.Notifier<EntityChange> onChange() {
		return game.onEntityChange;
	}

}
