package com.ugav.battalion.computer;

import java.util.Objects;

import com.ugav.battalion.Utils;
import com.ugav.battalion.computer.MiniMaxAlphaBeta.IGame;
import com.ugav.battalion.computer.MiniMaxAlphaBeta.IMove;
import com.ugav.battalion.computer.MiniMaxAlphaBeta.IPosition;
import com.ugav.battalion.core.Iter;

class MiniMaxAlphaBeta<Move extends IMove, Position extends IPosition<Move>, Game extends IGame<Move, Position>> {

	private final Game game;
	private final int maxDepth;

	MiniMaxAlphaBeta(Game game, int maxDepth) {
		this.game = Objects.requireNonNull(game);
		this.maxDepth = maxDepth;
	}

	Move chooseMove(Position position) {
		final int us = position.getTurn();
		double alpha = Double.MIN_VALUE, beta = Double.MAX_VALUE;

		Move bestMove = null;
		double bestEval = Double.MIN_VALUE;
//		double bestEval = game.evaluate(position, us);
//		alpha = Math.max(alpha, bestEval);

		for (Move move : Utils.iterable(position.availableMoves())) {
			Position child = game.getMovedPosition(position, move);
			double val = evaluate(child, 1, alpha, beta, us);
			if (val > bestEval) {
				bestEval = val;
				bestMove = move;
			}
			alpha = Math.max(alpha, val);
		}
		return bestMove;
	}

	private double evaluate(Position position, int depth, double alpha, double beta, final int us) {
		if (depth == maxDepth || position.isTerminated())
			return game.evaluate(position, us);
		if (position.getTurn() == us) {
			double val = Double.MIN_VALUE;
			for (Move move : Utils.iterable(position.availableMoves())) {
				Position child = game.getMovedPosition(position, move);
				val = Math.max(val, evaluate(child, depth + 1, alpha, beta, us));
				if (val > beta)
					break;
				alpha = Math.max(alpha, val);
			}
			return val;
		} else {
			double val = Double.MAX_VALUE;
			for (Move move : Utils.iterable(position.availableMoves())) {
				Position child = game.getMovedPosition(position, move);
				val = Math.min(val, evaluate(child, depth + 1, alpha, beta, us));
				if (val < alpha)
					break;
				beta = Math.min(beta, val);
			}
			return val;
		}
	}

	static interface IGame<Move extends IMove, Position extends IPosition<Move>> {

		int getNumberOfPlayers();

		Position getMovedPosition(Position position, Move move);

		double evaluate(Position position, int us);

	}

	static interface IPosition<Move extends IMove> {

		boolean isTerminated();

		int getTurn();

		Iter<Move> availableMoves();

	}

	static interface IMove {

	}

}
