package dev.munchaholic.core;

/** Locale-independent number text ({@code .} decimal point, ASCII signs). No units, no translation. */
public final class Numbers {
	private Numbers() {}

	/** Rounds to {@code maxDecimals}, using more decimals (up to 3) only if rounding would show 0. */
	public static String format(double value, int maxDecimals) {
		throw new UnsupportedOperationException("TODO");
	}

	/** {@link #format} with a {@code +} for positive non-zero results. */
	public static String signed(double value, int maxDecimals) {
		throw new UnsupportedOperationException("TODO");
	}
}
