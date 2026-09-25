package dev.munchaholic.core;

import java.util.Objects;

/**
 * Extra legality for one candidate step, on top of the cap range: a vetoed candidate is treated exactly like a capped
 * one (Random mode rolls a different attribute, Recipes mode returns {@link RollOutcome.Capped}). The core always adds
 * {@link Mobility#guard}; the server adds world checks (e.g. whether a bigger player still fits where they stand).
 */
@FunctionalInterface
public interface RollVeto {
	/** Vetoes nothing. */
	RollVeto NONE = candidate -> false;

	/** True if this candidate step must not be applied. */
	boolean vetoes(RollOutcome.Applied candidate);

	/** Vetoes what either of the two vetoes. */
	default RollVeto or(RollVeto other) {
		Objects.requireNonNull(other, "other");
		return candidate -> vetoes(candidate) || other.vetoes(candidate);
	}
}
