package dev.munchaholic.core;

import java.util.List;
import java.util.Set;

/** Food ids (e.g. {@code "minecraft:carrot"}) the player has eaten in Recipes mode. Player knowledge: survives death. */
public record Discoveries(Set<String> foods) {
	public static final Discoveries EMPTY = new Discoveries(Set.of());

	public Discoveries {
		foods = Set.copyOf(foods);
	}

	public boolean has(String foodId) {
		throw new UnsupportedOperationException("TODO");
	}

	public Discoveries with(String foodId) {
		throw new UnsupportedOperationException("TODO");
	}

	public int size() {
		throw new UnsupportedOperationException("TODO");
	}

	/** Natural order, for NBT and payloads. */
	public List<String> sorted() {
		throw new UnsupportedOperationException("TODO");
	}
}
