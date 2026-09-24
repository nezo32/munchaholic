package dev.munchaholic.core;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Locale-independent number text ({@code .} decimal point, ASCII signs). No units, no translation. */
public final class Numbers {
	/** More decimals than asked are used, up to this many, only when rounding would otherwise show 0. */
	private static final int MAX_EXTRA_DECIMALS = 3;

	private Numbers() {}

	/** Rounds to {@code maxDecimals}, using more decimals (up to 3) only if rounding would show 0. */
	public static String format(double value, int maxDecimals) {
		if (!Double.isFinite(value)) return Double.toString(value);
		if (Math.abs(value) < AttributeSpec.EPS) return "0";
		BigDecimal exact = BigDecimal.valueOf(value);
		for (int k = maxDecimals; k <= Math.max(maxDecimals, MAX_EXTRA_DECIMALS); k++) {
			BigDecimal rounded = exact.setScale(k, RoundingMode.HALF_UP);
			if (rounded.signum() != 0) return rounded.stripTrailingZeros().toPlainString();
		}
		return "0";
	}

	/** {@link #format} with a {@code +} for positive non-zero results. */
	public static String signed(double value, int maxDecimals) {
		String text = format(value, maxDecimals);
		return value > 0 && !"0".equals(text) ? "+" + text : text;
	}
}
