package com.ugav.battalion.computer;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import com.ugav.battalion.core.Cell;
import com.ugav.battalion.core.Game;
import com.ugav.battalion.core.Team;
import com.ugav.battalion.core.Unit;
import com.ugav.battalion.util.Utils;

public interface Player {

	void playTurn(Game game);

	static class Random implements Player {

		private final java.util.Random rand = new java.util.Random();

		@Override
		public void playTurn(Game game) {
			Team me = game.getTurn();
			Set<Unit> failedToMove = Collections.newSetFromMap(new IdentityHashMap<>());

			for (;;) {
				List<Unit> units = game.arena().units(me).filter(u -> u.isActive() && !failedToMove.contains(u))
						.collectList();
				if (units.isEmpty())
					break;
				Unit unit = units.get(rand.nextInt(units.size()));
				List<Cell> reachable = Utils.listCollect(unit.getReachableMap());
				reachable.remove(unit.getPos());
				if (reachable.isEmpty()) {
					failedToMove.add(unit);
				} else {
					Cell destination = reachable.get(rand.nextInt(reachable.size()));
					game.move(unit, game.calcRealPath(unit, unit.calcPath(destination)));
				}
			}

		}
	}

}
