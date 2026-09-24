package dev.munchaholic.core;

/** What one bite did. */
public sealed interface RollOutcome permits RollOutcome.Applied, RollOutcome.Capped, RollOutcome.Nothing {
	/** One step was applied: {@code oldSteps -> newSteps} at the given live base value. */
	record Applied(AttributeSpec spec, Direction direction, int oldSteps, int newSteps, double base) implements RollOutcome {
		/** {@code spec.isBuff(direction)}. */
		public boolean isBuff() {
			throw new UnsupportedOperationException("TODO");
		}

		/** {@code spec.displayDelta(newSteps - oldSteps)}. */
		public double displayDelta() {
			throw new UnsupportedOperationException("TODO");
		}

		/** {@code spec.displayValue(base, newSteps)}. */
		public double displayNow() {
			throw new UnsupportedOperationException("TODO");
		}
	}

	/** Recipes mode: the food's fixed effect would leave the cap range. Nothing changed. */
	record Capped(AttributeSpec spec, Direction direction, int steps, double base) implements RollOutcome {
		/** {@code spec.displayDelta(direction.sign())}. */
		public double displayDelta() {
			throw new UnsupportedOperationException("TODO");
		}

		/** {@code spec.displayValue(base, steps)}. */
		public double displayNow() {
			throw new UnsupportedOperationException("TODO");
		}
	}

	/** No legal change exists at all (unreachable with the shipped table). */
	record Nothing() implements RollOutcome {
		public static final Nothing INSTANCE = new Nothing();
	}
}
