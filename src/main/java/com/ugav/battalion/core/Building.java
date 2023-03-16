package com.ugav.battalion.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.ugav.battalion.core.Level.BuildingDesc;
import com.ugav.battalion.util.Event;

public class Building extends Entity implements IBuilding {

	enum Tech {
		BuildOnLandFlat(TypeBuilder::canBuildOn, Terrain.Category.FlatLand),

		BuildOnShore(TypeBuilder::canBuildOn, Terrain.Category.Shore),

		BuildOnWater(TypeBuilder::canBuildOn, Terrain.Category.Water),

		UnitBuilder, Capital, AllowUnitBuildLand, AllowUnitBuildWater, AllowUnitBuildAir;

		final Consumer<TypeBuilder> op;

		Tech() {
			op = null;
		}

		Tech(Consumer<TypeBuilder> op) {
			this.op = Objects.requireNonNull(op);
		}

		@SuppressWarnings("unchecked")
		<T> Tech(BiConsumer<TypeBuilder, T[]> op, T... args) {
			this(t -> op.accept(t, args));
		}
	}

	private static class TypeBuilder {
		final Set<Terrain.Category> canBuildOn = EnumSet.noneOf(Terrain.Category.class);
		final Set<Tech> tech;

		TypeBuilder(Tech... techs) {
			this.tech = techs.length > 0 ? EnumSet.copyOf(List.of(techs)) : EnumSet.noneOf(Tech.class);
			for (Tech tech : techs)
				if (tech.op != null)
					tech.op.accept(this);
		}

		void canBuildOn(Terrain.Category... categories) {
			canBuildOn.addAll(List.of(categories));
		}

	}

	public enum Type {
		OilRefinery(20, Tech.BuildOnLandFlat),

		OilProcessingPlant(35, Tech.BuildOnLandFlat),

		OilRig(50, Tech.BuildOnWater),

		Factory(0, Tech.BuildOnLandFlat, Tech.BuildOnShore, Tech.UnitBuilder),

		Capital(0, Tech.BuildOnLandFlat, Tech.Capital),

		LandResearchFacility(0, Tech.BuildOnLandFlat, Tech.AllowUnitBuildLand),
		NavalControlCenter(0, Tech.BuildOnLandFlat, Tech.AllowUnitBuildWater),
		SkyOperationsHub(0, Tech.BuildOnLandFlat, Tech.AllowUnitBuildAir);

		private final Set<Terrain.Category> canBuildOn;
		public final int moneyGain;
		public final boolean canBuildUnits;
		public final boolean allowUnitBuildLand;
		public final boolean allowUnitBuildWater;
		public final boolean allowUnitBuildAir;

		Type(int moneyGain, Tech... techs) {
			TypeBuilder builder = new TypeBuilder(techs);

			if (moneyGain < 0)
				throw new IllegalArgumentException();
			this.moneyGain = moneyGain;

			this.canBuildOn = Collections.unmodifiableSet(EnumSet.copyOf(builder.canBuildOn));
			this.canBuildUnits = builder.tech.contains(Tech.UnitBuilder);
			this.allowUnitBuildLand = builder.tech.contains(Tech.AllowUnitBuildLand);
			this.allowUnitBuildWater = builder.tech.contains(Tech.AllowUnitBuildWater);
			this.allowUnitBuildAir = builder.tech.contains(Tech.AllowUnitBuildAir);
		}

		public boolean canBuildOn(Terrain terrain) {
			return canBuildOn.contains(terrain.category);
		}
	}

	public final Type type;
	private final int pos;
	private Team conquerTeam;
	private int conquerProgress;

	private static final int CONQUER_DURATION_FROM_NONE = 3;
	private static final int CONQUER_DURATION_FROM_OTHER = 4;

	private Building(Game game, Type type, Team team, int pos, boolean active) {
		super(game, team);
		this.type = Objects.requireNonNull(type);
		this.pos = pos;

		setActive(canBeActive());
	}

	public static Building valueOf(Game game, BuildingDesc desc, int pos) {
		return new Building(game, desc.type, desc.team, pos, desc.active);
	}

	public static Building copyOf(Game game, Building building) {
		Building copy = new Building(game, building.type, building.getTeam(), building.pos, building.isActive());
		copy.conquerTeam = building.conquerTeam;
		copy.conquerProgress = building.conquerProgress;
		return copy;
	}

	@Override
	public int getPos() {
		return pos;
	}

	public static class ConquerEvent extends Event {

		public final Building building;
		public final Unit conquerer;

		public ConquerEvent(Game source, Building building, Unit conquerer) {
			super(source);
			this.building = building;
			this.conquerer = conquerer;
		}

	}

	void tryConquer(Unit conquerer) {
		Team conquererTeam = conquerer != null ? conquerer.getTeam() : null;
		if (conquererTeam != conquerTeam) {
			conquerTeam = null;
			conquerProgress = 0;
		}
		if (conquerer != null && conquererTeam != getTeam()) {
			conquerTeam = conquererTeam;
			conquerProgress++;
			game.onConquerProgress.notify(new ConquerEvent(game, this, conquerer));

			if (conquerProgress >= getConquerDuration()) {
				Team conqueredTeam = getTeam();
				setTeam(conquererTeam);
				conquerTeam = null;
				conquerProgress = 0;

				game.buildingsCache.invalidate();
				game.onConquerFinish.notify(new ConquerEvent(game, this, conquerer));

				if (type == Type.Capital)
					game.eliminateTeam(conqueredTeam);
			}
		}
	}

	private int getConquerDuration() {
		return getTeam() == null ? CONQUER_DURATION_FROM_NONE : CONQUER_DURATION_FROM_OTHER;
	}

	public Team getConquerTeam() {
		return conquerTeam;
	}

	public double getConquerProgress() {
		return (double) conquerProgress / getConquerDuration();
	}

	public int getMoneyGain() {
		return type.moneyGain;
	}

	boolean canBeActive() {
		return type.canBuildUnits;
	}

	@Override
	void setActive(boolean active) {
		if (active && !canBeActive())
			throw new IllegalStateException();
		super.setActive(active);
	}

	public boolean canBuildUnit(Unit.Type unitType) {
		boolean enable = isActive();
		enable = enable && type.canBuildUnits;
		enable = enable && game.unit(getPos()) == null;
		enable = enable && unitType.canStandOn(game.terrain(getPos()));
		if (!enable)
			return false;
		UnitSale sale = getAvailableUnits().get(unitType);
		return sale != null && game.getMoney(getTeam()) >= sale.price;
	}

	public Map<Unit.Type, UnitSale> getAvailableUnits() {
		if (!type.canBuildUnits)
			throw new IllegalStateException();
		Team team = getTeam();
		List<UnitSale> sales = new ArrayList<>();
		if (game.canBuildLandUnits(team)) {
			sales.add(UnitSale.of(Unit.Type.Rifleman));
			sales.add(UnitSale.of(Unit.Type.RocketSpecialist));
			sales.add(UnitSale.of(Unit.Type.AATank));
			sales.add(UnitSale.of(Unit.Type.BattleTank));
			sales.add(UnitSale.of(Unit.Type.Mortar));
			sales.add(UnitSale.of(Unit.Type.Artillery));
			sales.add(UnitSale.of(Unit.Type.TitanTank));
		}
		if (game.canBuildWaterUnits(team)) {
			sales.add(UnitSale.of(Unit.Type.SpeedBoat));
			sales.add(UnitSale.of(Unit.Type.AACruiser));
			sales.add(UnitSale.of(Unit.Type.Corvette));
			sales.add(UnitSale.of(Unit.Type.Battleship));
			sales.add(UnitSale.of(Unit.Type.Submarine));
		}
		if (game.canBuildAirUnits(team)) {
			sales.add(UnitSale.of(Unit.Type.FighterPlane));
			sales.add(UnitSale.of(Unit.Type.ZeppelinBomber));
		}

		Terrain terrain = game.terrain(getPos());
		Map<Unit.Type, UnitSale> salesMap = new HashMap<>();
		for (UnitSale sale : sales)
			if (sale.type.canStandOn(terrain))
				salesMap.put(sale.type, sale);
		return salesMap;
	}

	public static class UnitSale {
		public final Unit.Type type;
		public final int price;

		UnitSale(Unit.Type type, int price) {
			this.type = type;
			this.price = price;
		}

		@Override
		public String toString() {
			return "<" + type + ", " + price + ">";
		}

		private static UnitSale of(Unit.Type type) {
			return new UnitSale(type, type.price);
		}
	}

	@Override
	public String toString() {
		return "" + getTeam().toString().charAt(0) + type + Cell.toString(getPos());
	}

	@Override
	public Type getType() {
		return type;
	}

}
