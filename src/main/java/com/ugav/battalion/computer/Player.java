package com.ugav.battalion.computer;

import java.util.List;

import com.ugav.battalion.core.Action;
import com.ugav.battalion.core.Game;
import com.ugav.battalion.core.Team;
import com.ugav.battalion.core.Unit;
import com.ugav.battalion.util.ListInt;

public interface Player {

	Action chooseAction(Game game);

	public static class Random implements Player {

		private final java.util.Random rand = new java.util.Random();

		@Override
		public Action chooseAction(Game game) {
			Team me = game.getTurn();

			List<Unit> units = game.units(me).filter(Unit::isActive).toList();
			while (!units.isEmpty()) {
				int idx = rand.nextInt(units.size());
				Unit unit = units.get(idx);
				ListInt reachable = unit.getReachableMap().cells().toList();
				reachable.remove(unit.getPos());
				if (reachable.isEmpty()) {
					units.remove(idx);
				} else {
					int destination = reachable.get(rand.nextInt(reachable.size()));
					game.performAction(new Action.UnitMove(unit.getPos(), unit.calcPath(destination)));
				}
			}
			return Action.TurnEnd;
		}
	}

}
