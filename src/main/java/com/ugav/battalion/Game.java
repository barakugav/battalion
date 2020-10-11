package com.ugav.battalion;

import com.barakugav.util.Holder;

public class Game {

    private final Map map;
    private Team turn;
    private Team winner;

    Game(Level level) {
	map = new Map(level);
	turn = Team.Blue;
	winner = Team.None;
    }

    int getXLen() {
	return map.getXLen();
    }

    int getYLen() {
	return map.getYLen();
    }

    Tile getTile(int x, int y) {
	return map.at(x, y);
    }

    void start() {
	turnBegin();
    }

    void turnBegin() {
	map.forEach(tile -> {
	    if (tile.hasBuilding()) {
		Building building = tile.getBuilding();
		building.setCanAct(building.getTeam() == turn);
	    }
	    if (tile.hasUnit()) {
		Unit unit = tile.getUnit();
		unit.setCanAct(unit.getTeam() == turn);
	    }
	});
	for (int x = 0; x < map.getXLen(); x++) {
	    for (int y = 0; y < map.getYLen(); y++) {
		Tile tile = map.at(x, y);
		if (!tile.hasUnit())
		    continue;
		Unit unit = tile.getUnit();
		if (unit.getTeam() != turn)
		    continue;
		unit.setExploredMap(Unit.getExploredMap(map, x, y));
	    }
	}
    }

    void turnEnd() {
	Holder.Bool blueDead = new Holder.Bool(true);
	Holder.Bool redDead = new Holder.Bool(true);
	map.forEach(tile -> {
	    if (tile.hasUnit()) {
		Team unitTeam = tile.getUnit().getTeam();
		if (unitTeam == Team.Blue)
		    blueDead.set(false);
		else if (unitTeam == Team.Red)
		    redDead.set(false);
		else
		    throw new InternalError();
	    }
	});

	if (blueDead.getBoolean()) {
	    winner = Team.Red;
	    return;
	}
	if (redDead.getBoolean()) {
	    winner = Team.Blue;
	    return;
	}

	map.forEach(tile -> {
	    if (tile.hasUnit()) {
		Unit unit = tile.getUnit();
		unit.invalidateExploredMap();
	    }
	});
	/* TODO add money */
	turn = turn == Team.Blue ? Team.Red : Team.Blue;
    }

    Team getWinner() {
	return winner;
    }

    void move(int fromX, int fromY, int toX, int toY) {
	if (!canMove(fromX, fromY, toX, toY))
	    throw new IllegalStateException();
	move0(fromX, fromY, toX, toY);
	map.at(toX, toY).getUnit().setCanAct(false);
    }

    private void move0(int fromX, int fromY, int toX, int toY) {
	Tile from = map.at(fromX, fromY);
	Unit unit = from.getUnit();
	from.removeUnit();
	map.at(toX, toY).setUnit(unit);
    }

    boolean canMove(int fromX, int fromY, int toX, int toY) {
	Tile from = map.at(fromX, fromY);
	Tile to = map.at(toX, toY);
	if (!from.hasUnit() || to.hasUnit())
	    return false;
	Unit unit = from.getUnit();
	return unit.getTeam() == turn && unit.canAct() && unit.getExploredMap().canMove(toX, toY);
    }

    void moveAndAttack(int attackerX, int attackerY, int moveToX, int moveToY, int targetX, int targetY) {
	Unit attacker = map.at(attackerX, attackerY).getUnit();
	Unit target = map.at(targetX, targetY).getUnit();
	if (!(attacker instanceof CloseRangeUnit))
	    throw new UnsupportedOperationException();

	if (!canAttak(attackerX, attackerY, targetX, targetY))
	    throw new IllegalStateException();

	boolean moveNearTarget = false;
	for (int neighbor = 0; neighbor < Map.neighbors.length; neighbor++) {
	    int x2 = targetX + Map.neighbors[neighbor][0];
	    int y2 = targetY + Map.neighbors[neighbor][1];
	    if (map.isInMap(x2, y2) && x2 == moveToX && y2 == moveToY) {
		moveNearTarget = true;
		break;
	    }
	}
	if (!moveNearTarget)
	    throw new UnsupportedOperationException();

	move0(attackerX, attackerY, moveToX, moveToY);
	doDamage(attacker, target);
	attacker.setCanAct(false);
    }

    void attackRange(int attackerX, int attackerY, int targetX, int targetY) {
	Unit attacker = map.at(attackerX, attackerY).getUnit();
	Unit target = map.at(targetX, targetY).getUnit();
	if (!(attacker instanceof LongRangeUnit))
	    throw new UnsupportedOperationException();
	if (!canAttak(attackerX, attackerY, targetX, targetY))
	    throw new IllegalStateException();
	doDamage(attacker, target);
	attacker.setCanAct(false);
    }

    boolean canAttak(int attackerX, int attackerY, int tagertX, int targetY) {
	Tile attackerTile = map.at(attackerX, attackerY);
	Tile targetTile = map.at(tagertX, targetY);
	if (!attackerTile.hasUnit() || !targetTile.hasUnit())
	    return false;
	Unit attacker = attackerTile.getUnit();
	return attacker.getTeam() == turn && attacker.canAct() && attacker.getExploredMap().canAttack(tagertX, targetY)
		&& targetTile.getUnit().getTeam() != attacker.getTeam();
    }

    private void doDamage(Unit attacker, Unit target) {
	/* TODO */
    }

}
