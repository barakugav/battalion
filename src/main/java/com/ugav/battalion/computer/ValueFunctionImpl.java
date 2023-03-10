package com.ugav.battalion.computer;

import com.ugav.battalion.computer.GameImpl.Node;
import com.ugav.battalion.core.Action;
import com.ugav.battalion.core.Building;
import com.ugav.battalion.core.Cell;
import com.ugav.battalion.core.Game;
import com.ugav.battalion.core.Team;
import com.ugav.battalion.core.Unit;

class ValueFunctionImpl implements GameTreeAlg.ValueFunction<Action, GameImpl.Node, GameImpl> {

	@Override
	public double evaluate(Node position, int us) {
		return new Evaluator(position, us).evaluate();
	}

	private static class Evaluator {

		private final Game game;
		private final Team us;

		private static final double Aggression = 0.95;

		Evaluator(Node position, int us) {
			this.game = position.game;
			this.us = GameImpl.turnIntToObj(us);

			checkParams();
		}

		@SuppressWarnings("unused")
		private static void checkParams() {
			if (!(0 <= Aggression && Aggression <= 1))
				throw new IllegalArgumentException();
		}

		double evaluate() {
			if (game.isFinished())
				return game.getWinner() == us ? Double.MAX_VALUE : -Double.MAX_VALUE;
			double[] evals = new double[Team.values().length];

			for (Unit unit : game.units().forEach())
				evals[unit.getTeam().ordinal()] += evalUnit(unit);

			for (Building building : game.buildings().forEach()) {
				if (building.getTeam() == null)
					continue;
				double buildingEval = evalBuilding(building);
				evals[building.getTeam().ordinal()] += buildingEval;
				Team conquerTeam = building.getConquerTeam();
				if (conquerTeam != null)
					evals[conquerTeam.ordinal()] += buildingEval * building.getConquerProgress();
			}

			for (Team team : Team.values()) {
				final double MoneyWeight = 0.2;
				int money = game.getMoney(team);
				evals[team.ordinal()] += MoneyWeight * Math.pow(money, 4.0 / 5.0);
			}

			double maxEnemyEval = 0;
			for (Team team : Team.values())
				if (team != us)
					maxEnemyEval = Math.max(maxEnemyEval, evals[team.ordinal()]);
			double eval = (1 - Aggression) * evals[us.ordinal()] - Aggression * maxEnemyEval;

			return eval;
		}

		private double evalUnit(Unit unit) {
			double eval = 0;

			eval += unit.getHealth();

			double attackingEval;
			if (unit.isEnemyInRange()) {
				attackingEval = 1;
			} else {
				final Team us = unit.getTeam();
				int minDist = Integer.MAX_VALUE;
				for (Unit enemy : game.enemiesSeenBy(us).forEach()) {
					if (!unit.canAttack(enemy))
						continue;
					for (int neighbor : Cell.neighbors(enemy.getPos())) {
						if (!game.isValidCell(neighbor))
							continue;
						int d = unit.getDistanceTo(neighbor);
						if (d < 0)
							continue; /* unreachable */
						if (d < minDist)
							minDist = d;
					}
				}

				attackingEval = Math.min((double) unit.type.moveLimit / minDist, 0.9);
			}
			eval += attackingEval * 20;

			return eval;
		}

		private double evalBuilding(Building building) {
			double eval = 0;
			eval += building.getMoneyGain();
			if (building.type.canBuildUnits) {
				eval += 50;
				if (game.unit(building.getPos()) != null)
					eval -= 25; /* Factory is blocked */
			}
			if (building.type.allowUnitBuildLand)
				eval += 20;
			if (building.type.allowUnitBuildWater)
				eval += 20;
			if (building.type.allowUnitBuildAir)
				eval += 20;
			return eval;
		}

	}

}
