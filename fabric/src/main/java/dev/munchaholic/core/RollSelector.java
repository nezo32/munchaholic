package dev.munchaholic.core;

import java.util.List;

/** Picks what one bite does, inside the caps (ARCHITECTURE.md §4.2). Never mutates its inputs. */
public final class RollSelector {
	private RollSelector() {}

	/** Random mode over {@link Caps#ALL}. */
	public static RollOutcome random(PlayerStacks stacks, BaseLookup bases, RandomIndex random) {
		throw new UnsupportedOperationException("TODO");
	}

	/**
	 * Random mode: a uniform attribute, then a 50/50 direction; a capped pick removes that attribute and rolls a
	 * different one. Once all were tried, a uniform pick among the still-allowed (attribute, direction) pairs.
	 */
	public static RollOutcome random(List<AttributeSpec> specs, PlayerStacks stacks, BaseLookup bases, RandomIndex random) {
		throw new UnsupportedOperationException("TODO");
	}

	/** Recipes mode: {@link RollOutcome.Applied} if the recipe's step is allowed, else {@link RollOutcome.Capped}. */
	public static RollOutcome fixed(PlayerStacks stacks, BaseLookup bases, Recipe recipe) {
		throw new UnsupportedOperationException("TODO");
	}
}
