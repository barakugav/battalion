package com.ugav.battalion.computer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import com.ugav.battalion.Utils;
import com.ugav.battalion.core.Game;
import com.ugav.battalion.core.Position;
import com.ugav.battalion.core.Team;
import com.ugav.battalion.core.Unit;

public interface Player {

	void playTurn(Game game);

	static class Random implements Player {

		private final java.util.Random rand = new java.util.Random();

		@Override
		public void playTurn(Game game) {
			Team me = game.getTurn();
			Set<Unit> failedToMove = Collections.newSetFromMap(new IdentityHashMap<>());

			for (;;) {
				Collection<Unit> units = game.arena().units(me);
				units.removeIf(u -> !u.isActive() || failedToMove.contains(u));
				if (units.isEmpty())
					break;
				Unit unit = new ArrayList<>(units).get(rand.nextInt(units.size()));
				List<Position> reachable = Utils.listCollect(unit.getReachableMap());
				reachable.remove(unit.getPos());
				if (reachable.isEmpty()) {
					failedToMove.add(unit);
				} else {
					Position destination = reachable.get(rand.nextInt(reachable.size()));
					game.move(unit, game.calcRealPath(unit, unit.calcPath(destination)));
				}
			}

		}
	}

}
