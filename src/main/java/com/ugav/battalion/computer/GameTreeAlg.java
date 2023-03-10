package com.ugav.battalion.computer;

import com.ugav.battalion.util.Iter;

class GameTreeAlg {

	interface IGame<Action, Position extends IGame.IPosition<Action>> {

		int getNumberOfPlayers();

		Position getModifiedPosition(Position position, Action action);

		static interface IPosition<Action> {

			boolean isTerminated();

			int getTurn();

			Iter<Action> availableActions();

		}

	}

	@FunctionalInterface
	interface ValueFunction<Actions, Position extends IGame.IPosition<Actions>, Games extends IGame<Actions, Position>> {

		double evaluate(Position position, int us);

	}

}
