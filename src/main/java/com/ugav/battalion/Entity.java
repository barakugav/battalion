package com.ugav.battalion;

interface Entity extends Drawable {

	Team getTeam();

	void setTeam(Team team);

	boolean isActive();

	void setActive(boolean active);

	Entity deepCopy();

}
