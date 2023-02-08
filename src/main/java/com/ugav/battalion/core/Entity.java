package com.ugav.battalion.core;

import java.util.Objects;

import com.ugav.battalion.DataChangeNotifier;
import com.ugav.battalion.DataEvent;

public abstract class Entity {

	private Team team;
	private boolean active;

	public final DataChangeNotifier<DataEvent> onChange = new DataChangeNotifier<>();

	Entity(Team team) {
		this.team = Objects.requireNonNull(team);
		active = false;
	}

	public Team getTeam() {
		return team;
	}

	void setTeam(Team team) {
		this.team = Objects.requireNonNull(team);
		onChange.notify(new DataEvent(this));
	}

	public boolean isActive() {
		return active;
	}

	void setActive(boolean active) {
		this.active = active;
		onChange.notify(new DataEvent(this));
	}

	void clear() {
		onChange.clear();
	}

}
