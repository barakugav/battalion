package com.ugav.battalion;

import com.ugav.battalion.Unit.Category;

class Game {

	final Arena arena;
	private Team turn;
	private Team winner;

	Game(Level level) {
		arena = new Arena(level);
		turn = Team.Blue;
		winner = Team.None;

		for (Position pos : arena.positions()) {
			Tile tile = arena.at(pos);
			if (!tile.hasUnit())
				continue;
			Unit u = tile.getUnit();
			u.setArena(arena);
			u.setPos(pos);
		}
	}

	int getWidth() {
		return arena.getWidth();
	}

	int getHeight() {
		return arena.getHeight();
	}

	Tile getTile(Position pos) {
		return arena.at(pos);
	}

	Team getTurn() {
		return turn;
	}

	void start() {
		turnBegin();
	}

	void turnBegin() {
		for (Tile tile : arena.tiles()) {
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
		for (Tile tile : arena.tiles()) {
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

	void move(Position source, Position target) {
		if (!isMoveValid(source, target))
			throw new IllegalStateException();
		move0(source, target);
		arena.at(target).getUnit().setActive(false);
	}

	private void move0(Position source, Position target) {
		Tile from = arena.at(source);
		Unit unit = from.getUnit();
		from.removeUnit();
		arena.at(target).setUnit(unit);
		unit.setPos(target);
	}

	boolean isMoveValid(Position source, Position target) {
		Tile from = arena.at(source);
		Tile to = arena.at(target);
		if (!from.hasUnit() || to.hasUnit())
			return false;
		Unit unit = from.getUnit();
		return unit.getTeam() == turn && unit.isActive() && unit.isMoveValid(target);
	}

	void moveAndAttack(Position attackerPos, /* Position moveTarget, */ Position attackedPos) {
		Unit attacker = arena.at(attackerPos).getUnit();
		Unit target = arena.at(attackedPos).getUnit();
		if (!(attacker.type.category == Category.Land))
			throw new UnsupportedOperationException();

		if (!isAttackValid(attackerPos, attackedPos))
			throw new IllegalStateException();
		Position moveTarget = attacker.getMovePositionToAttack(attackedPos);

		boolean moveNearTarget = false;
		for (Position neighbor : attackedPos.neighbors()) {
			if (arena.isValidPos(neighbor) && neighbor.equals(moveTarget)) {
				moveNearTarget = true;
				break;
			}
		}
		if (!moveNearTarget)
			throw new UnsupportedOperationException();

		move0(attackerPos, moveTarget);
		doDamage(attacker, target);
		attacker.setActive(false);
	}

	void attackRange(Position attackerPos, Position targetPos) {
		Unit attacker = arena.at(attackerPos).getUnit();
		Unit target = arena.at(targetPos).getUnit();
//		if (!(attacker instanceof LongRangeUnit))
//			throw new UnsupportedOperationException();
		if (!isAttackValid(attackerPos, targetPos))
			throw new IllegalStateException();
		doDamage(attacker, target);
		attacker.setActive(false);
	}

	boolean isAttackValid(Position attackerPos, Position targetPos) {
		Tile attackerTile = arena.at(attackerPos);
		Tile targetTile = arena.at(targetPos);
		if (!attackerTile.hasUnit() || !targetTile.hasUnit())
			return false;
		Unit attacker = attackerTile.getUnit();
		return attacker.getTeam() == turn && attacker.isActive() && attacker.getTeam() != targetTile.getUnit().getTeam()
				&& attacker.isAttackValid(targetPos);
	}

	private void doDamage(Unit attacker, Unit target) {
		int damage = attacker.getDamge(target);
		if (target.getHealth() <= damage) {
			target.setHealth(0);
		} else {
			target.setHealth(target.getHealth() - damage);
		}
	}

}
