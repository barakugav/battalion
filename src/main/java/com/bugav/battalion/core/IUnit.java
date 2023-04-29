package com.bugav.battalion.core;

public interface IUnit {

	Unit.Type getType();

	Team getTeam();

	int getHealth();

	IUnit getTransportedUnit();

}
