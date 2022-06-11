package com.ugav.battalion;

import com.ugav.battalion.Map.Neighbor;
import com.ugav.battalion.Map.Position;
import com.ugav.battalion.Unit.Category;

class Game {

	private final Map map;
	private Team turn;
	private Team winner;

	Game(Level level) {
		map = new Map(level);
		turn = Team.Blue;
		winner = Team.None;

		int xLen = map.getXLen(), yLen = map.getYLen();
		for (int x = 0; x < xLen; x++) {
			for (int y = 0; y < yLen; y++) {
				Tile tile = map.at(x, y);
				if (!tile.hasUnit())
					continue;
				Unit u = tile.getUnit();
				u.setMap(map);
				u.setPos(x, y);
			}
		}
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
		for (Tile tile : map.tiles()) {
			if (tile.hasBuilding()) {
				Building building = tile.getBuilding();
				building.setActive(building.getTeam() == turn);
			}
			if (tile.hasUnit()) {
				Unit unit = tile.getUnit();
				unit.setActive(unit.getTeam() == turn);
			}
		}
	}

	void turnEnd() {
		boolean blueDead = true;
		boolean redDead = true;
		for (Tile tile : map.tiles()) {
			if (tile.hasUnit()) {
				Team unitTeam = tile.getUnit().getTeam();
				if (unitTeam == Team.Blue)
					blueDead = false;
				else if (unitTeam == Team.Red)
					redDead = false;
				else
					throw new InternalError();
			}
		}

		if (blueDead) {
			winner = Team.Red;
			return;
		}
		if (redDead) {
			winner = Team.Blue;
			return;
		}

		/* TODO add money */
		turn = turn == Team.Blue ? Team.Red : Team.Blue;
	}

	Team getWinner() {
		return winner;
	}

	void move(int fromX, int fromY, int toX, int toY) {
		if (!isMoveValid(fromX, fromY, toX, toY))
			throw new IllegalStateException();
		move0(fromX, fromY, toX, toY);
		map.at(toX, toY).getUnit().setActive(false);
	}

	private void move0(int fromX, int fromY, int toX, int toY) {
		Tile from = map.at(fromX, fromY);
		Unit unit = from.getUnit();
		from.removeUnit();
		map.at(toX, toY).setUnit(unit);
	}

	boolean isMoveValid(int fromX, int fromY, int toX, int toY) {
		Tile from = map.at(fromX, fromY);
		Tile to = map.at(toX, toY);
		if (!from.hasUnit() || to.hasUnit())
			return false;
		Unit unit = from.getUnit();
		return unit.getTeam() == turn && unit.isActive() && unit.isMoveValid(toX, toY);
	}

	void moveAndAttack(int attackerX, int attackerY, int moveToX, int moveToY, int targetX, int targetY) {
		Unit attacker = map.at(attackerX, attackerY).getUnit();
		Unit target = map.at(targetX, targetY).getUnit();
		if (!(attacker.type.category == Category.Land))
			throw new UnsupportedOperationException();

		if (!isAttackValid(attackerX, attackerY, targetX, targetY))
			throw new IllegalStateException();

		boolean moveNearTarget = false;
		for (Position neighbor : Neighbor.of(targetX, targetY)) {
			if (map.isInMap(neighbor) && neighbor.x == moveToX && neighbor.y == moveToY) {
				moveNearTarget = true;
				break;
			}
		}
		if (!moveNearTarget)
			throw new UnsupportedOperationException();

		move0(attackerX, attackerY, moveToX, moveToY);
		doDamage(attacker, target);
		attacker.setActive(false);
	}

	void attackRange(int attackerX, int attackerY, int targetX, int targetY) {
		Unit attacker = map.at(attackerX, attackerY).getUnit();
		Unit target = map.at(targetX, targetY).getUnit();
//		if (!(attacker instanceof LongRangeUnit))
//			throw new UnsupportedOperationException();
		if (!isAttackValid(attackerX, attackerY, targetX, targetY))
			throw new IllegalStateException();
		doDamage(attacker, target);
		attacker.setActive(false);
	}

	boolean isAttackValid(int attackerX, int attackerY, int tagertX, int targetY) {
		Tile attackerTile = map.at(attackerX, attackerY);
		Tile targetTile = map.at(tagertX, targetY);
		if (!attackerTile.hasUnit() || !targetTile.hasUnit())
			return false;
		Unit attacker = attackerTile.getUnit();
		return attacker.getTeam() == turn && attacker.isActive() && attacker.isAttackValid(tagertX, targetY)
				&& targetTile.getUnit().getTeam() != attacker.getTeam();
	}

	private void doDamage(Unit attacker, Unit target) {
		/* TODO */
	}

}
