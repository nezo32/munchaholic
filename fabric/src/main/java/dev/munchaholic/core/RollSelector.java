package dev.munchaholic.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Picks what one bite does, inside the caps (ARCHITECTURE.md §4.2). A step is legal if it stays in the cap range and
 * no veto rejects it: always the {@link Mobility#guard}, plus the caller's {@link RollVeto}. Never mutates its inputs.
 */
public final class RollSelector {
	private static final Direction[] DIRECTIONS = {Direction.UP, Direction.DOWN};

	private RollSelector() {}

	/** Random mode over {@link Caps#ALL}, no extra veto. */
	public static RollOutcome random(PlayerStacks stacks, BaseLookup bases, RandomIndex random) {
		return random(Caps.ALL, stacks, bases, random, RollVeto.NONE);
	}

	/** Random mode over {@link Caps#ALL}. */
	public static RollOutcome random(PlayerStacks stacks, BaseLookup bases, RandomIndex random, RollVeto veto) {
		return random(Caps.ALL, stacks, bases, random, veto);
	}

	/** Random mode, no extra veto. */
	public static RollOutcome random(List<AttributeSpec> specs, PlayerStacks stacks, BaseLookup bases, RandomIndex random) {
		return random(specs, stacks, bases, random, RollVeto.NONE);
	}

	/**
	 * Random mode: a uniform attribute, then a 50/50 direction; a capped (or vetoed) pick removes that attribute and
	 * rolls a different one. Once all were tried, a uniform pick among the still-allowed (attribute, direction) pairs.
	 */
	public static RollOutcome random(List<AttributeSpec> specs, PlayerStacks stacks, BaseLookup bases, RandomIndex random,
			RollVeto veto) {
		RollVeto legality = Mobility.guard(stacks, bases).or(veto);
		List<AttributeSpec> candidates = new ArrayList<>(specs);
		while (!candidates.isEmpty()) {
			AttributeSpec spec = candidates.remove(random.nextInt(candidates.size()));
			Direction direction = random.nextInt(2) == 0 ? Direction.UP : Direction.DOWN;
			RollOutcome.Applied applied = tryStep(spec, direction, stacks, bases, legality);
			if (applied != null) return applied;
		}
		List<RollOutcome.Applied> pairs = new ArrayList<>();
		for (AttributeSpec spec : specs) {
			for (Direction direction : DIRECTIONS) {
				RollOutcome.Applied applied = tryStep(spec, direction, stacks, bases, legality);
				if (applied != null) pairs.add(applied);
			}
		}
		if (pairs.isEmpty()) return RollOutcome.Nothing.INSTANCE;
		return pairs.get(random.nextInt(pairs.size()));
	}

	/** Recipes mode, no extra veto. */
	public static RollOutcome fixed(PlayerStacks stacks, BaseLookup bases, Recipe recipe) {
		return fixed(stacks, bases, recipe, RollVeto.NONE);
	}

	/**
	 * Recipes mode: {@link RollOutcome.Applied} if the recipe's step is allowed, else {@link RollOutcome.Capped} (out of
	 * range or vetoed; both show the "at the limit" message).
	 */
	public static RollOutcome fixed(PlayerStacks stacks, BaseLookup bases, Recipe recipe, RollVeto veto) {
		RollVeto legality = Mobility.guard(stacks, bases).or(veto);
		RollOutcome.Applied applied = tryStep(recipe.spec(), recipe.direction(), stacks, bases, legality);
		if (applied != null) return applied;
		return new RollOutcome.Capped(recipe.spec(), recipe.direction(), stacks.steps(recipe.spec()), bases.base(recipe.spec()));
	}

	/** The one-step outcome, or null if that step would leave the cap range or is vetoed. */
	private static RollOutcome.Applied tryStep(AttributeSpec spec, Direction direction, PlayerStacks stacks, BaseLookup bases,
			RollVeto legality) {
		int steps = stacks.steps(spec);
		double base = bases.base(spec);
		if (!spec.canStep(base, steps, direction)) return null;
		RollOutcome.Applied candidate = new RollOutcome.Applied(spec, direction, steps, steps + direction.sign(), base);
		return legality.vetoes(candidate) ? null : candidate;
	}
}
