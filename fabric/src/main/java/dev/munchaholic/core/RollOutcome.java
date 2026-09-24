package dev.munchaholic.core;

/** What one bite did. */
public sealed interface RollOutcome permits RollOutcome.Applied, RollOutcome.Capped, RollOutcome.Nothing {
	/** One step was applied: {@code oldSteps -> newSteps} at the given live base value. */
	record Applied(AttributeSpec spec, Direction direction, int oldSteps, int newSteps, double base) implements RollOutcome {
		/** {@code spec.isBuff(direction)}. */
		public boolean isBuff() {
			return spec.isBuff(direction);
		}

		/** {@code spec.displayDelta(newSteps - oldSteps)}. */
		public double displayDelta() {
			return spec.displayDelta(newSteps - oldSteps);
		}

		/** {@code spec.displayValue(base, newSteps)}. */
		public double displayNow() {
			return spec.displayValue(base, newSteps);
		}
	}

	/** Recipes mode: the food's fixed effect would leave the cap range, or is vetoed ({@link RollVeto}). Nothing changed. */
	record Capped(AttributeSpec spec, Direction direction, int steps, double base) implements RollOutcome {
		/** {@code spec.displayDelta(direction.sign())}. */
		public double displayDelta() {
			return spec.displayDelta(direction.sign());
		}

		/** {@code spec.displayValue(base, steps)}. */
		public double displayNow() {
			return spec.displayValue(base, steps);
		}
	}

	/** No legal change exists at all (unreachable with the shipped table). */
	record Nothing() implements RollOutcome {
		public static final Nothing INSTANCE = new Nothing();
	}
}
