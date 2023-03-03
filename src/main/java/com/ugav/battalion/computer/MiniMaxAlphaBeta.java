package com.ugav.battalion.computer;

import java.util.Objects;

import com.ugav.battalion.computer.MiniMaxAlphaBeta.IGame;
import com.ugav.battalion.computer.MiniMaxAlphaBeta.IPosition;
import com.ugav.battalion.util.Iter;

class MiniMaxAlphaBeta<Action, Position extends IPosition<Action>, Game extends IGame<Action, Position>> {

	private final Game game;
	private final int maxDepth;

	MiniMaxAlphaBeta(Game game, int maxDepth) {
		this.game = Objects.requireNonNull(game);
		this.maxDepth = maxDepth;
	}

	Action chooseAction(Position position) {
		final int us = position.getTurn();
		double alpha = Double.MAX_VALUE, beta = Double.MAX_VALUE;

		Action bestAction = null;
		double bestEval = game.evaluate(position, us);
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
			return game.evaluate(position, us);
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

	static interface IGame<Action, Position extends IPosition<Action>> {

		int getNumberOfPlayers();

		Position getModifiedPosition(Position position, Action action);

		double evaluate(Position position, int us);

	}

	static interface IPosition<Action> {

		boolean isTerminated();

		int getTurn();

		Iter<Action> availableActions();

	}

}
