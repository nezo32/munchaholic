package dev.munchaholic.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;

import org.junit.jupiter.api.Test;

/** The formatting cases of ARCHITECTURE.md §4.4. */
class NumbersTest {
	@Test
	void frozenFormatCases() {
		assertEquals("92", Numbers.format(92.00000000000001, 0));
		assertEquals("92", Numbers.format(91.6, 0));
		assertEquals("0.4", Numbers.format(0.4, 0));
		assertEquals("0.04", Numbers.format(0.04, 0));
		assertEquals("0", Numbers.format(-0.0, 0));
		assertEquals("0.8", Numbers.format(0.8, 2));
		assertEquals("2.6", Numbers.format(2.6000000000000005, 2));
		assertEquals("1.35", Numbers.format(1.35, 2));
		assertEquals("0", Numbers.format(1e-5, 0));
		assertEquals("1234567", Numbers.format(1234567, 0));
	}

	@Test
	void frozenSignedCases() {
		assertEquals("+8", Numbers.signed(8, 0));
		assertEquals("-8", Numbers.signed(-8, 0));
		assertEquals("+13", Numbers.signed(12.5, 0));
		assertEquals("0", Numbers.signed(0, 0));
		// §4.4 lists signed(-0.5, 0) = "-0.5", but its own algorithm (HALF_UP, more decimals only when rounding shows
		// 0) gives "-1", consistent with signed(12.5, 0) = "+13". The -0.5 that is actually shown is attack_damage's
		// POINTS delta, formatted with 2 decimals:
		assertEquals("-0.5", Numbers.signed(-0.5, 2));
		assertEquals("-1", Numbers.signed(-0.5, 0));
	}

	@Test
	void roundingAndExtraDecimals() {
		assertEquals("1", Numbers.format(0.5, 0), "HALF_UP");
		assertEquals("-1", Numbers.format(-0.5, 0), "HALF_UP rounds away from zero");
		assertEquals("0.01", Numbers.format(0.005, 0), "the first non-zero rounding wins");
		assertEquals("0.001", Numbers.format(0.0005, 0), "HALF_UP at the third decimal");
		assertEquals("0", Numbers.format(0.0004, 0), "at most 3 decimals");
		assertEquals("0.01", Numbers.format(0.0149, 2));
		assertEquals("0.003", Numbers.format(0.003, 2));
		assertEquals("1.23", Numbers.format(1.2345, 2));
		assertEquals("1.24", Numbers.format(1.235, 2), "BigDecimal.valueOf uses the shortest decimal, 1.235");
		assertEquals("10", Numbers.format(10.004, 2));
		assertEquals("-0.2", Numbers.format(-0.2, 2));
		assertEquals("-0.4", Numbers.format(-0.36, 0));
		assertEquals("300", Numbers.format(300.00000447034836, 0));
		assertEquals("596", Numbers.format(596.0000000000001, 0));
	}

	@Test
	void tinyValuesAreZero() {
		assertEquals("0", Numbers.format(1e-10, 0));
		assertEquals("0", Numbers.format(-1e-10, 2));
		assertEquals("0", Numbers.format(-1e-5, 2));
		assertEquals("0", Numbers.signed(1e-5, 0), "no sign on a shown 0");
		assertEquals("0", Numbers.signed(-1e-5, 0));
		assertEquals("0", Numbers.signed(-0.0, 0));
	}

	@Test
	void neverExponentNotation() {
		assertEquals("100000000", Numbers.format(1e8, 0));
		assertEquals("1200", Numbers.format(1200.0, 2));
		assertEquals("12345678901234567000", Numbers.format(1.2345678901234567e19, 0));
		assertEquals("+1000", Numbers.signed(1000, 0));
	}

	@Test
	void moreDecimalsThanThreeAreHonoured() {
		assertEquals("1.23457", Numbers.format(1.234567, 5));
		assertEquals("0.00001", Numbers.format(1e-5, 5));
	}

	@Test
	void nonFiniteDoesNotThrow() {
		assertEquals("NaN", Numbers.format(Double.NaN, 0));
		assertEquals("Infinity", Numbers.format(Double.POSITIVE_INFINITY, 0));
		assertEquals("-Infinity", Numbers.signed(Double.NEGATIVE_INFINITY, 0));
	}

	@Test
	void everyShownTableValueFormatsCleanly() {
		for (AttributeSpec spec : Caps.ALL) {
			double base = spec.playerBase();
			int d = spec.unit().maxDecimals();
			for (int s = spec.minSteps(base); s <= spec.maxSteps(base); s++) {
				String text = Numbers.format(spec.displayValue(base, s), d);
				// no float noise: at most 2 decimals (percents are whole, points have 1-decimal steps)
				int dot = text.indexOf('.');
				int decimals = dot < 0 ? 0 : text.length() - dot - 1;
				assertEquals(true, decimals <= 2, spec.key() + " " + s + " -> " + text);
			}
			String delta = Numbers.signed(spec.displayDelta(1), d);
			assertEquals('+', delta.charAt(0), spec.key() + " " + delta);
		}
		assertEquals("-8", Numbers.signed(Caps.SCALE.displayDelta(-1), 0));
		assertEquals("92", Numbers.format(Caps.SCALE.displayValue(1.0, -1), 0));
		assertEquals("300", Numbers.format(Caps.MOVEMENT_SPEED.displayValue((double) 0.1F, 25), 0));
		assertEquals("+0.2", Numbers.signed(Caps.STEP_HEIGHT.displayDelta(1), 2));
		assertEquals("2.6", Numbers.format(Caps.STEP_HEIGHT.displayValue(0.6, 10), 2));
		assertEquals("30", Numbers.format(Caps.KNOCKBACK_RESISTANCE.displayValue(0.0, 3), 0));
	}

	@Test
	void localeIndependent() {
		Locale previous = Locale.getDefault();
		try {
			Locale.setDefault(Locale.GERMANY);
			assertEquals("1.35", Numbers.format(1.35, 2));
			assertEquals("-0.04", Numbers.signed(-0.04, 0));
			Locale.setDefault(Locale.forLanguageTag("ar-EG"));
			assertEquals("1234567", Numbers.format(1234567, 0));
		} finally {
			Locale.setDefault(previous);
		}
	}
}
