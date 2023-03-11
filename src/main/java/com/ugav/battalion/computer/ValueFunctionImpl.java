package com.ugav.battalion.computer;

import com.ugav.battalion.computer.GameImpl.Node;
import com.ugav.battalion.core.Action;
import com.ugav.battalion.core.Building;
import com.ugav.battalion.core.Cell;
import com.ugav.battalion.core.Game;
import com.ugav.battalion.core.Team;
import com.ugav.battalion.core.Unit;
import com.ugav.battalion.util.Iter;

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
				evals[unit.getTeam().ordinal()] += Units.eval(unit);

			for (Building building : game.buildings().forEach()) {
				if (building.getTeam() == null)
					continue;
				double buildingEval = Buildings.eval(building);
				evals[building.getTeam().ordinal()] += buildingEval;
				Team conquerTeam = building.getConquerTeam();
				if (conquerTeam != null)
					evals[conquerTeam.ordinal()] += buildingEval * building.getConquerProgress();
			}

			for (Team team : Team.values()) {
				int money = game.getMoney(team);
				evals[team.ordinal()] += 0.2 * Math.pow(money, 4.0 / 5.0);
			}

			for (Team team : Team.values())
				assert evals[team.ordinal()] >= 0;

			double maxEnemyEval = 0;
			for (Team team : Team.values())
				if (team != us)
					maxEnemyEval = Math.max(maxEnemyEval, evals[team.ordinal()]);
			double eval = (1 - Aggression) * evals[us.ordinal()];
			for (Team team : Team.values())
				if (team != us)
					eval -= Aggression * Math.pow(evals[team.ordinal()], 2) / maxEnemyEval;
			return eval;
		}

		private final Units Units = new Units();

		private class Units {

			private static class Weight {
				private static final double Health = 0.8;
				private static final double Damage = 0.5;
			}

			double eval(Unit unit) {
				double eval = 0;

				double health = unit.getHealth() + (unit.isRepairing() ? unit.repairAmount() / 2 : 0);
				eval += health * Weight.Health;
				eval += evalAttack(unit);

				return eval;
			}

			double evalAttack(Unit unit) {
				double attackingEval;
				if (unit.isEnemyInRange()) {
					attackingEval = 1;
				} else {
					final Team us = unit.getTeam();
					int minDist = Integer.MAX_VALUE;
					// TODO switch weapon type
					for (Unit enemy : game.enemiesSeenBy(us).forEach()) {
						if (!unit.canAttack(enemy.type))
							continue;
						for (Iter.Int nit = Cell.neighbors(enemy.getPos()); nit.hasNext();) {
							int neighbor = nit.next();
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
				return attackingEval * unit.type.damage * Weight.Damage;
			}

		}

		private final Buildings Buildings = new Buildings();

		private class Buildings {

			private static class Weight {
				static final double Factory = 50;
				static final double FactoryBlocked = 25;
				static final double AllowUnitBuildLand = 20;
				static final double AllowUnitBuildWater = 20;
				static final double AllowUnitBuildAir = 20;
			}

			double eval(Building building) {
				double eval = 0;
				eval += building.getMoneyGain();
				if (building.type.canBuildUnits) {
					boolean isBlocked = game.unit(building.getPos()) != null;
					eval += isBlocked ? Weight.FactoryBlocked : Weight.Factory;
				}
				if (building.type.allowUnitBuildLand)
					eval += Weight.AllowUnitBuildLand;
				if (building.type.allowUnitBuildWater)
					eval += Weight.AllowUnitBuildWater;
				if (building.type.allowUnitBuildAir)
					eval += Weight.AllowUnitBuildAir;
				return eval;
			}
		}

	}

}
