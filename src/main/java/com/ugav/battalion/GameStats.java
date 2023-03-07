package com.ugav.battalion;

import com.ugav.battalion.core.Game;
import com.ugav.battalion.core.Team;
import com.ugav.battalion.util.Event;

class GameStats implements Clearable {

	private int turnsPlayed;
	private int unitsBuilt;
	private int enemiesTerminated;
	private int unitsCasualties;
	private int buildingsConquered;
	private int moneyGained;
	private int moneySpent;

	private final Event.Register register = new Event.Register();

	GameStats(Game game) {
		final Team player = Team.Red;
		register.register(game.onTurnBegin, e -> {
			if (game.getTurn() == player)
				turnsPlayed++;
		});
		register.register(game.onUnitBuy, e -> {
			if (e.unit.getTeam() == player)
				unitsBuilt++;
		});
		register.register(game.onUnitDeath, e -> {
			if (e.unit.getTeam() == player) {
				unitsCasualties++;
			} else {
				enemiesTerminated++;
			}
		});
		register.register(game.onConquerFinish, e -> {
			if (e.conquerer.getTeam() == player)
				buildingsConquered++;
		});
		register.register(game.onMoneyChange, e -> {
			if (e.team == player) {
				if (e.delta >= 0) {
					moneyGained += e.delta;
				} else {
					moneySpent += -e.delta;
				}
			}
		});
	}

	@Override
	public void clear() {
		register.unregisterAll();
	}

	int getTurnsPlayed() {
		return turnsPlayed;
	}

	int getUnitsBuilt() {
		return unitsBuilt;
	}

	int getEnemiesTerminated() {
		return enemiesTerminated;
	}

	int getUnitsCasualties() {
		return unitsCasualties;
	}

	int getBuildingsConquered() {
		return buildingsConquered;
	}

	int getMoneyGained() {
		return moneyGained;
	}

	int getMoneySpent() {
		return moneySpent;
	}

}
