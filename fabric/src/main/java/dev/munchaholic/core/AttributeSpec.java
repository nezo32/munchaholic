package dev.munchaholic.core;

import java.util.Objects;

/**
 * One row of the attribute table ({@link Caps}): which attribute, how our modifier combines, the step per bite, the
 * absolute limits of the munch value (base + our modifier only) and how it is shown. No validation beyond non-null,
 * so tests can build degenerate specs; the table invariants live in CapsTest.
 */
public record AttributeSpec(String key, ModifierOp op, double step, double min, double max, double playerBase,
		DisplayUnit unit, Direction buff) {
	public static final double EPS = 1e-9;

	public AttributeSpec {
		Objects.requireNonNull(key, "key");
		Objects.requireNonNull(op, "op");
		Objects.requireNonNull(unit, "unit");
		Objects.requireNonNull(buff, "buff");
	}

	/** {@code "attribute.name." + key}, shipped by vanilla in every language. */
	public String translationKey() {
		throw new UnsupportedOperationException("TODO");
	}

	/** The munch value after {@code steps} steps: base + steps*step (ADD_VALUE) or base*(1 + steps*step). */
	public double valueAt(double base, int steps) {
		throw new UnsupportedOperationException("TODO");
	}

	/** The amount of our single modifier: {@code steps * step}. */
	public double modifierAmount(int steps) {
		throw new UnsupportedOperationException("TODO");
	}

	public boolean inRange(double value) {
		throw new UnsupportedOperationException("TODO");
	}

	/** Whether one step in {@code direction} is allowed (moving back into the range is always allowed). */
	public boolean canStep(double base, int steps, Direction direction) {
		throw new UnsupportedOperationException("TODO");
	}

	/** {@code direction == buff}. */
	public boolean isBuff(Direction direction) {
		throw new UnsupportedOperationException("TODO");
	}

	/** The value as shown to players, in this spec's {@link DisplayUnit}. */
	public double displayValue(double base, int steps) {
		throw new UnsupportedOperationException("TODO");
	}

	/** A change of {@code stepsDelta} steps as shown to players. */
	public double displayDelta(int stepsDelta) {
		throw new UnsupportedOperationException("TODO");
	}

	/** Lowest steps reachable from 0 by legal DOWN steps (0 if none). */
	public int minSteps(double base) {
		throw new UnsupportedOperationException("TODO");
	}

	/** Highest steps reachable from 0 by legal UP steps (0 if none). */
	public int maxSteps(double base) {
		throw new UnsupportedOperationException("TODO");
	}
}
