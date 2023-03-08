package com.ugav.battalion.util;

public class Func {

	private Func() {
	}

	@FunctionalInterface
	public static interface IntToInt {
		int apply(int val);
	}

}
