package com.ugav.battalion.core;

public interface IUnit {

	Unit.Type getType();

	Team getTeam();

	IUnit getTransportedUnit();

}
