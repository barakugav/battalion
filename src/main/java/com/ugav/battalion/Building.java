package com.ugav.battalion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.ugav.battalion.Level.BuildingDesc;

class Building extends Entity {

	enum Tech {
		BuildOnLandFlat(TypeBuilder::canBuildOn, Terrain.Category.FlatLand),

		BuildOnShore(TypeBuilder::canBuildOn, Terrain.Category.Shore),

		BuildOnWater(TypeBuilder::canBuildOn, Terrain.Category.Water),

		UnitBuilder(type -> type.canBuildUnits = true);

		final Consumer<TypeBuilder> op;

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
		boolean canBuildUnits = false;

		void canBuildOn(Terrain.Category... categories) {
			canBuildOn.addAll(List.of(categories));
		}

	}

	enum Type {
		OilRefinery(100, Tech.BuildOnLandFlat),

		OilRefineryBig(200, Tech.BuildOnLandFlat),

		OilRig(400, Tech.BuildOnWater),

		Factory(0, Tech.BuildOnLandFlat, Tech.BuildOnShore, Tech.UnitBuilder);

		final Set<Terrain.Category> canBuildOn;
		final int moneyGain;
		final boolean canBuildUnits;

		Type(int moneyGain, Tech... techs) {
			TypeBuilder builder = new TypeBuilder();
			for (Tech tech : techs)
				tech.op.accept(builder);

			if (moneyGain < 0)
				throw new IllegalArgumentException();
			this.moneyGain = moneyGain;

			this.canBuildOn = Collections.unmodifiableSet(EnumSet.copyOf(builder.canBuildOn));
			this.canBuildUnits = builder.canBuildUnits;
		}
	}

	final Type type;
	private Position pos;
	private Arena arena;
	private Team conquerTeam;
	private int conquerProgress;

	private static final int CONQUER_DURATION_FROM_NONE = 2;
	private static final int CONQUER_DURATION_FROM_OTHER = 3;

	Building(Type type, Team team) {
		super(team);
		this.type = type;

		setActive(canBeActive());
	}

	static Building valueOf(BuildingDesc desc) {
		return new Building(desc.type, desc.team);
	}

	Position getPos() {
		return pos;
	}

	void setPos(Position pos) {
		this.pos = pos;
	}

	void setArena(Arena arena) {
		this.arena = arena;
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
			int conquer_duration = getTeam() == Team.None ? CONQUER_DURATION_FROM_NONE : CONQUER_DURATION_FROM_OTHER;
			if (++conquerProgress == conquer_duration)
				setTeam(conquerer);
		}
	}

	int getMoneyGain() {
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

	List<UnitSale> getAvailableUnits() {
		if (!type.canBuildUnits)
			throw new IllegalStateException();
		List<UnitSale> l = new ArrayList<>();
		l.add(UnitSale.of(Unit.Type.Soldier, 100));
		l.add(UnitSale.of(Unit.Type.Tank, 300));
		return l;
	}

	static class UnitSale {
		final Unit.Type type;
		final int price;

		UnitSale(Unit.Type type, int price) {
			this.type = type;
			this.price = price;
		}

		private static UnitSale of(Unit.Type type, int price) {
			return new UnitSale(type, price);
		}
	}

	@Override
	public String toString() {
		return (getTeam() == Team.Red ? "R" : "B") + type;
	}

}
