package com.bugav.battalion.computer;

import java.util.Objects;

import com.bugav.battalion.computer.GameImpl.Node;
import com.bugav.battalion.computer.GameTreeAlg.IGame;
import com.bugav.battalion.computer.GameTreeAlg.ValueFunction;
import com.bugav.battalion.computer.GameTreeAlg.IGame.IPosition;

public class MiniMaxAlphaBeta<Action, Position extends IPosition<Action>, Game extends IGame<Action, Position>> {

	private final Game game;
	private final int maxDepth;
	private final ValueFunction<Action, Position, Game> valueFunc;

	MiniMaxAlphaBeta(Game game, int maxDepth, ValueFunction<Action, Position, Game> valueFunc) {
		this.game = Objects.requireNonNull(game);
		this.maxDepth = maxDepth;
		this.valueFunc = Objects.requireNonNull(valueFunc);
	}

	Action chooseAction(Position position) {
		final int us = position.getTurn();
		double alpha = Double.MAX_VALUE, beta = Double.MAX_VALUE;

		Action bestAction = null;
		double bestEval = valueFunc.evaluate(position, us);
		alpha = Math.max(alpha, bestEval);

		for (Action action : position.availableActions().forEach()) {
			Position child = game.getModifiedPosition(position, action);
			double val = evaluate(child, 1, alpha, beta, us);
			if (val > bestEval) {
				bestEval = val;
				bestAction = action;
			}
			alpha = Math.max(alpha, val);
		}
		return bestAction;
	}

	private double evaluate(Position position, int depth, double alpha, double beta, final int us) {
		if (depth == maxDepth || position.isTerminated())
			return valueFunc.evaluate(position, us);
		if (position.getTurn() == us) {
			double val = -Double.MAX_VALUE;
			for (Action action : position.availableActions().forEach()) {
				Position child = game.getModifiedPosition(position, action);
				val = Math.max(val, evaluate(child, depth + 1, alpha, beta, us));
				if (val > beta)
					break;
				alpha = Math.max(alpha, val);
			}
			return val;
		} else {
			double val = Double.MAX_VALUE;
			for (Action action : position.availableActions().forEach()) {
				Position child = game.getModifiedPosition(position, action);
				val = Math.min(val, evaluate(child, depth + 1, alpha, beta, us));
				if (val < alpha)
					break;
				beta = Math.min(beta, val);
			}
			return val;
		}
	}

	public static class Player implements com.bugav.battalion.computer.Player {

		private final MiniMaxAlphaBeta<com.bugav.battalion.core.Action, GameImpl.Node, GameImpl> algo;

		private final int DepthLimit = 2;

		public Player() {
			algo = new MiniMaxAlphaBeta<>(new GameImpl(), DepthLimit, new ValueFunctionImpl());
		}

		@Override
		public com.bugav.battalion.core.Action chooseAction(com.bugav.battalion.core.Game game) {
			return algo.chooseAction(new Node(game));
		}

	}

}
