package com.ugav.battalion.core;

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

		OilRefineryBig(35, Tech.BuildOnLandFlat),

		OilRig(50, Tech.BuildOnWater),

		Factory(0, Tech.BuildOnLandFlat, Tech.BuildOnShore, Tech.UnitBuilder),

		Capital(0, Tech.BuildOnLandFlat, Tech.Capital),

		ControllerLand(0, Tech.BuildOnLandFlat, Tech.AllowUnitBuildLand),
		ControllerWater(0, Tech.BuildOnLandFlat, Tech.AllowUnitBuildWater),
		ControllerAir(0, Tech.BuildOnLandFlat, Tech.AllowUnitBuildAir);

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
	private int pos;
	private Team conquerTeam;
	private int conquerProgress;

	private static final int CONQUER_DURATION_FROM_NONE = 3;
	private static final int CONQUER_DURATION_FROM_OTHER = 2;

	Building(Arena arena, Type type, Team team) {
		super(arena, team);
		this.type = Objects.requireNonNull(type);

		setActive(canBeActive());
	}

	public static Building valueOf(Arena arena, BuildingDesc desc) {
		return new Building(arena, desc.type, desc.team);
	}

	public static Building copyOf(Arena arena, Building building) {
		Building copy = new Building(arena, building.type, building.getTeam());
		copy.setActive(building.isActive());
		copy.pos = building.pos;
		copy.conquerTeam = building.conquerTeam;
		copy.conquerProgress = building.conquerProgress;
		return copy;
	}

	public int getPos() {
		return pos;
	}

	void setPos(int pos) {
		this.pos = pos;
	}

	Arena getArena() {
		return arena;
	}

	void tryConquer(Team conquerer) {
		if (conquerer != conquerTeam) {
			conquerTeam = null;
			conquerProgress = 0;
		}
		if (conquerer != null && conquerer != getTeam()) {
			conquerTeam = conquerer;
			if (++conquerProgress >= getConquerDuration()) {
				setTeam(conquerer);
				conquerTeam = null;
				conquerProgress = 0;
			}
		}
	}

	private int getConquerDuration() {
		return getTeam() == Team.None ? CONQUER_DURATION_FROM_NONE : CONQUER_DURATION_FROM_OTHER;
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

	public Map<Unit.Type, UnitSale> getAvailableUnits() {
		if (!type.canBuildUnits)
			throw new IllegalStateException();
		Map<Unit.Type, UnitSale> sales = new HashMap<>();
		Consumer<UnitSale> addSale = sale -> sales.put(sale.type, sale);

		Team team = getTeam();
		boolean canBuildLandUnits = arena.buildings().filter(b -> team == b.getTeam() && b.type.allowUnitBuildLand)
				.hasNext();
		boolean canBuildWaterUnits = arena.buildings().filter(b -> team == b.getTeam() && b.type.allowUnitBuildWater)
				.hasNext()
				&& EnumSet.of(Terrain.Category.Water, Terrain.Category.Shore).contains(arena.terrain(pos).category);
		boolean canBuildAirUnits = arena.buildings().filter(b -> team == b.getTeam() && b.type.allowUnitBuildAir)
				.hasNext();

		if (canBuildLandUnits) {
			addSale.accept(UnitSale.of(Unit.Type.Soldier, 75));
			addSale.accept(UnitSale.of(Unit.Type.Bazooka, 100));
			addSale.accept(UnitSale.of(Unit.Type.TankAntiAir, 230));
			addSale.accept(UnitSale.of(Unit.Type.Tank, 270));
			addSale.accept(UnitSale.of(Unit.Type.Mortar, 300));
			addSale.accept(UnitSale.of(Unit.Type.Artillery, 470));
			addSale.accept(UnitSale.of(Unit.Type.TankBig, 470));
		}
		if (canBuildWaterUnits) {
			addSale.accept(UnitSale.of(Unit.Type.SpeedBoat, 200));
			addSale.accept(UnitSale.of(Unit.Type.ShipAntiAir, 450));
			addSale.accept(UnitSale.of(Unit.Type.Ship, 500));
			addSale.accept(UnitSale.of(Unit.Type.ShipArtillery, 800));
			addSale.accept(UnitSale.of(Unit.Type.Submarine, 475));
		}
		if (canBuildAirUnits) {
			addSale.accept(UnitSale.of(Unit.Type.Airplane, 340));
			addSale.accept(UnitSale.of(Unit.Type.Zeppelin, 650));
		}

		return sales;
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

		private static UnitSale of(Unit.Type type, int price) {
			return new UnitSale(type, price);
		}
	}

	@Override
	public String toString() {
		return "" + getTeam().toString().charAt(0) + type;
	}

	@Override
	public Type getType() {
		return type;
	}

}
