package dev.munchaholic.core;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Food ids (e.g. {@code "minecraft:carrot"}) the player has eaten in Recipes mode. Player knowledge: survives death. */
public record Discoveries(Set<String> foods) {
	public static final Discoveries EMPTY = new Discoveries(Set.of());

	public Discoveries {
		foods = Set.copyOf(foods);
	}

	public boolean has(String foodId) {
		return foodId != null && foods.contains(foodId);
	}

	public Discoveries with(String foodId) {
		if (has(foodId)) return this;
		Set<String> next = new HashSet<>(foods);
		next.add(foodId);
		return new Discoveries(next);
	}

	public int size() {
		return foods.size();
	}

	/** Natural order, for NBT and payloads. */
	public List<String> sorted() {
		return foods.stream().sorted().toList();
	}
}
