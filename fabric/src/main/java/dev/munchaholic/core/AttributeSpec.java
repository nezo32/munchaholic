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
	/** Safety bound for {@link #minSteps}/{@link #maxSteps} on degenerate specs (e.g. a zero base or step). */
	private static final int WALK_LIMIT = 1_000_000;

	public AttributeSpec {
		Objects.requireNonNull(key, "key");
		Objects.requireNonNull(op, "op");
		Objects.requireNonNull(unit, "unit");
		Objects.requireNonNull(buff, "buff");
	}

	/** {@code "attribute.name." + key}, shipped by vanilla in every language. */
	public String translationKey() {
		return "attribute.name." + key;
	}

	/** The munch value after {@code steps} steps: base + steps*step (ADD_VALUE) or base*(1 + steps*step). */
	public double valueAt(double base, int steps) {
		return op == ModifierOp.ADD_VALUE ? base + steps * step : base * (1.0 + steps * step);
	}

	/** The amount of our single modifier: {@code steps * step}. */
	public double modifierAmount(int steps) {
		return steps * step;
	}

	public boolean inRange(double value) {
		return value >= min - EPS && value <= max + EPS;
	}

	/** Whether one step in {@code direction} is allowed (moving back into the range is always allowed). */
	public boolean canStep(double base, int steps, Direction direction) {
		long next = (long) steps + direction.sign();
		if (next != (int) next) return false;
		if (inRange(valueAt(base, (int) next))) return true;
		double now = valueAt(base, steps);
		return (now < min - EPS && direction == Direction.UP) || (now > max + EPS && direction == Direction.DOWN);
	}

	/** {@code direction == buff}. */
	public boolean isBuff(Direction direction) {
		return direction == buff;
	}

	/** The value as shown to players, in this spec's {@link DisplayUnit}. */
	public double displayValue(double base, int steps) {
		return switch (unit) {
			case PERCENT_OF_BASE -> base == 0 ? 100 * (1 + steps * step) : 100 * valueAt(base, steps) / base;
			case PERCENT_POINTS -> 100 * valueAt(base, steps);
			case POINTS -> valueAt(base, steps);
		};
	}

	/** A change of {@code stepsDelta} steps as shown to players. */
	public double displayDelta(int stepsDelta) {
		return switch (unit) {
			case PERCENT_OF_BASE, PERCENT_POINTS -> 100.0 * stepsDelta * step;
			case POINTS -> stepsDelta * step;
		};
	}

	/** Lowest steps reachable from 0 by legal DOWN steps (0 if none). */
	public int minSteps(double base) {
		int s = 0;
		while (s > -WALK_LIMIT && canStep(base, s, Direction.DOWN)) s--;
		return s;
	}

	/** Highest steps reachable from 0 by legal UP steps (0 if none). */
	public int maxSteps(double base) {
		int s = 0;
		while (s < WALK_LIMIT && canStep(base, s, Direction.UP)) s++;
		return s;
	}
}
