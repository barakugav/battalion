package com.bugav.battalion.computer;

import java.util.Objects;

import com.bugav.battalion.computer.GameImpl.Node;
import com.bugav.battalion.computer.GameTreeAlg.IGame;
import com.bugav.battalion.computer.GameTreeAlg.ValueFunction;
import com.bugav.battalion.computer.GameTreeAlg.IGame.IPosition;

public class Greedy<Action, Position extends IPosition<Action>, Game extends IGame<Action, Position>> {

	private final Game game;
	private final ValueFunction<Action, Position, Game> valueFunc;

	Greedy(Game game, ValueFunction<Action, Position, Game> valueFunc) {
		this.game = Objects.requireNonNull(game);
		this.valueFunc = Objects.requireNonNull(valueFunc);
	}

	Action chooseAction(Position position) {
		final int us = position.getTurn();

		Action bestAction = null;
		double bestEval = valueFunc.evaluate(position, us);

		for (Action action : position.availableActions().forEach()) {
			Position child = game.getModifiedPosition(position, action);
			double val = valueFunc.evaluate(position, action, child, us);
			if (val > bestEval) {
				bestEval = val;
				bestAction = action;
			}
		}
		return bestAction;
	}

	public static class Player implements com.bugav.battalion.computer.Player {

		private final Greedy<com.bugav.battalion.core.Action, GameImpl.Node, GameImpl> algo;

		public Player() {
			algo = new Greedy<>(new GameImpl(), new ValueFunctionImpl());
		}

		@Override
		public com.bugav.battalion.core.Action chooseAction(com.bugav.battalion.core.Game game) {
			return algo.chooseAction(new Node(game));
		}

	}

}
