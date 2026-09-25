package dev.munchaholic.core;

import java.util.HashMap;
import java.util.Map;

/**
 * A player's accumulated changes: attribute key -> signed steps (zero entries dropped), plus a bite counter.
 * Integer steps are the source of truth; the attribute modifiers are always derived from them.
 */
public record PlayerStacks(Map<String, Integer> steps, int bites) {
	public static final PlayerStacks EMPTY = new PlayerStacks(Map.of(), 0);

	public PlayerStacks {
		Map<String, Integer> clean = new HashMap<>();
		if (steps != null) {
			steps.forEach((key, value) -> {
				if (key != null && value != null && value != 0) clean.put(key, value);
			});
		}
		steps = Map.copyOf(clean);
		bites = Math.max(0, bites);
	}

	/** Steps for this spec; 0 if absent. */
	public int steps(AttributeSpec spec) {
		return steps.getOrDefault(spec.key(), 0);
	}

	public PlayerStacks withSteps(AttributeSpec spec, int steps) {
		Map<String, Integer> next = new HashMap<>(this.steps);
		next.put(spec.key(), steps);
		return new PlayerStacks(next, bites);
	}

	/** bites + 1, saturating at Integer.MAX_VALUE. */
	public PlayerStacks withBite() {
		return bites == Integer.MAX_VALUE ? this : new PlayerStacks(steps, bites + 1);
	}

	/** {@link #withBite()}, plus the new steps for an {@link RollOutcome.Applied}. */
	public PlayerStacks after(RollOutcome outcome) {
		PlayerStacks bitten = withBite();
		if (outcome instanceof RollOutcome.Applied applied) return bitten.withSteps(applied.spec(), applied.newSteps());
		return bitten;
	}

	/** No non-zero steps (bites are ignored). */
	public boolean isEmpty() {
		return steps.isEmpty();
	}
}
