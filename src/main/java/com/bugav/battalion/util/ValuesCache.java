package com.bugav.battalion.util;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class ValuesCache {

	private volatile int modCount;

	public void invalidate() {
		modCount++;
	}

	public <T> Supplier<T> newVal(Supplier<? extends T> calc) {
		return new Value<>(calc);
	}

	public BooleanSupplier newValBool(BooleanSupplier calc) {
		Value<Boolean> v = new Value<>(() -> Boolean.valueOf(calc.getAsBoolean()));
		return () -> v.get().booleanValue();
	}

	private class Value<T> implements Supplier<T> {
		private volatile T val;
		private volatile int cachedModcount;
		private final Supplier<? extends T> calc;

		private Value(Supplier<? extends T> calc) {
			cachedModcount = -1;
			this.calc = Objects.requireNonNull(calc);
		}

		@Override
		public T get() {
			if (modCount != cachedModcount) {
				val = calc.get();
				cachedModcount = modCount;
			}
			return val;
		}

	}

}
