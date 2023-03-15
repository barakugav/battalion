package com.ugav.battalion;

import java.util.EnumMap;
import java.util.Map;

import com.ugav.battalion.core.Game;
import com.ugav.battalion.core.Team;
import com.ugav.battalion.util.Event;

class GameStats implements Clearable {

	private static class PerTeam {
		private int turnsPlayed;
		private int unitsBuilt;
		private int enemiesTerminated;
		private int unitsCasualties;
		private int buildingsConquered;
		private int moneyGained;
		private int moneySpent;
	}

	private final Map<Team, PerTeam> stats = new EnumMap<>(Team.class);

	private final Event.Register register = new Event.Register();

	GameStats(Game game) {
		register.register(game.onTurnBegin, e -> perTeam(game.getTurn()).turnsPlayed++);
		register.register(game.onUnitBuy, e -> perTeam(e.unit.getTeam()).unitsBuilt++);
		register.register(game.onUnitDeath, e -> {
			perTeam(e.attacker.getTeam()).enemiesTerminated++;
			perTeam(e.unit.getTeam()).unitsCasualties++;
		});
		register.register(game.onConquerFinish, e -> perTeam(e.conquerer.getTeam()).buildingsConquered++);
		register.register(game.onMoneyChange, e -> {
			if (e.delta >= 0) {
				perTeam(e.team).moneyGained += e.delta;
			} else {
				perTeam(e.team).moneySpent += -e.delta;
			}
		});
	}

	private PerTeam perTeam(Team team) {
		return stats.computeIfAbsent(team, t -> new PerTeam());
	}

	@Override
	public void clear() {
		register.unregisterAll();
	}

	int getTurnsPlayed(Team team) {
		return perTeam(team).turnsPlayed;
	}

	int getUnitsBuilt(Team team) {
		return perTeam(team).unitsBuilt;
	}

	int getEnemiesTerminated(Team team) {
		return perTeam(team).enemiesTerminated;
	}

	int getUnitsCasualties(Team team) {
		return perTeam(team).unitsCasualties;
	}

	int getBuildingsConquered(Team team) {
		return perTeam(team).buildingsConquered;
	}

	int getMoneyGained(Team team) {
		return perTeam(team).moneyGained;
	}

	int getMoneySpent(Team team) {
		return perTeam(team).moneySpent;
	}

}
