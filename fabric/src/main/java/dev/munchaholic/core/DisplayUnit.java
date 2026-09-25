package dev.munchaholic.core;

/**
 * How a value and a change are shown. {@code maxDecimals} feeds {@link Numbers}; the translation key and its English
 * fallback wrap the formatted number ({@code "%s%%"}), so units stay localizable.
 */
public enum DisplayUnit {
	PERCENT_OF_BASE(0, "munchaholic.unit.percent", "%s%%"),
	PERCENT_POINTS(0, "munchaholic.unit.percent", "%s%%"),
	POINTS(2, "munchaholic.unit.points", "%s");

	private final int maxDecimals;
	private final String translationKey;
	private final String fallback;

	DisplayUnit(int maxDecimals, String translationKey, String fallback) {
		this.maxDecimals = maxDecimals;
		this.translationKey = translationKey;
		this.fallback = fallback;
	}

	public int maxDecimals() {
		return maxDecimals;
	}

	public String translationKey() {
		return translationKey;
	}

	/** The en_us value of {@link #translationKey()}. */
	public String fallback() {
		return fallback;
	}
}
