package com.bugav.battalion.computer;

import com.bugav.battalion.util.Iter;

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

	interface ValueFunction<Action_, Position extends IGame.IPosition<Action_>, Game_ extends IGame<Action_, Position>> {

		double evaluate(Position position, int us);

		double evaluate(Position history, Action_ action, Position position, int us);

	}

}
