package com.ugav.battalion.computer;

import java.util.Objects;

import com.ugav.battalion.computer.GameImpl.Node;
import com.ugav.battalion.computer.GameTreeAlg.IGame;
import com.ugav.battalion.computer.GameTreeAlg.IGame.IPosition;
import com.ugav.battalion.computer.GameTreeAlg.ValueFunction;

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

	public static class Player implements com.ugav.battalion.computer.Player {

		private final Greedy<com.ugav.battalion.core.Action, GameImpl.Node, GameImpl> algo;

		public Player() {
			algo = new Greedy<>(new GameImpl(), new ValueFunctionImpl());
		}

		@Override
		public com.ugav.battalion.core.Action chooseAction(com.ugav.battalion.core.Game game) {
			return algo.chooseAction(new Node(game));
		}

	}

}
