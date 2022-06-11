package com.ugav.battalion;

public interface Entity extends Drawable {

	Team getTeam();

	void setTeam(Team team);

	boolean canAct();

	void setCanAct(boolean canMove);

	Entity deepCopy();

}
