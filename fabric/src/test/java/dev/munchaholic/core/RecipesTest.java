package dev.munchaholic.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

/** The frozen vectors of ARCHITECTURE.md §4.3, determinism, seed sensitivity, uniformity, order independence. */
class RecipesTest {
	private record Vector(long seed, String food, String attribute, Direction direction) {}

	private static final List<Vector> FROZEN = List.of(
			new Vector(0L, "minecraft:carrot", "movement_speed", Direction.UP),
			new Vector(0L, "minecraft:cake", "movement_speed", Direction.UP),
			new Vector(0L, "minecraft:golden_apple", "attack_speed", Direction.DOWN),
			new Vector(0L, "minecraft:bread", "safe_fall_distance", Direction.UP),
			new Vector(12345L, "minecraft:carrot", "attack_speed", Direction.DOWN),
			new Vector(12345L, "minecraft:cake", "gravity", Direction.DOWN),
			new Vector(12345L, "minecraft:golden_apple", "attack_damage", Direction.UP),
			new Vector(12345L, "minecraft:bread", "jump_strength", Direction.UP),
			new Vector(-4172144997902289642L, "minecraft:carrot", "sneaking_speed", Direction.UP),
			new Vector(-4172144997902289642L, "minecraft:cake", "knockback_resistance", Direction.DOWN),
			new Vector(-4172144997902289642L, "minecraft:golden_apple", "attack_damage", Direction.DOWN),
			new Vector(-4172144997902289642L, "minecraft:bread", "step_height", Direction.UP));

	@Test
	void frozenHashVectors() {
		assertEquals(0x535d57d51d90cc95L, Recipes.fnv1a64("minecraft:carrot"));
		assertEquals(0x5692161d100b05e5L, Recipes.mix64(1L));
		assertEquals(0xcbf29ce484222325L, Recipes.fnv1a64(""), "FNV offset basis");
		assertEquals(0L, Recipes.mix64(0L));
		assertEquals(0x6D756E6368L, Recipes.DIRECTION_SALT);
	}

	@Test
	void fnvHashesUtf8Bytes() {
		// "é" is two UTF-8 bytes (0xC3 0xA9); hashing chars instead would differ
		long manual = 0xcbf29ce484222325L;
		for (int b : new int[] {0xC3, 0xA9}) {
			manual ^= b;
			manual *= 0x100000001b3L;
		}
		assertEquals(manual, Recipes.fnv1a64("é"));
	}

	@Test
	void frozenRecipeVectors() {
		for (Vector v : FROZEN) {
			Recipe r = Recipes.of(v.seed(), v.food());
			assertEquals(v.attribute(), r.spec().key(), v.toString());
			assertEquals(v.direction(), r.direction(), v.toString());
			assertSame(Caps.byKey(v.attribute()).orElseThrow(), r.spec());
		}
	}

	@Test
	void deterministic() {
		for (int i = 0; i < 200; i++) {
			String food = "test:food_" + i;
			assertEquals(Recipes.of(99L, food), Recipes.of(99L, food));
			assertEquals(Recipes.of(99L, food), Recipes.of(99L, food, Caps.ALL));
		}
	}

	@Test
	void independentOfListOrder() {
		List<AttributeSpec> shuffled = new ArrayList<>(Caps.ALL);
		Random random = new Random(3);
		for (int round = 0; round < 5; round++) {
			Collections.shuffle(shuffled, random);
			for (int i = 0; i < 500; i++) {
				String food = "test:food_" + i;
				assertEquals(Recipes.of(7L, food), Recipes.of(7L, food, shuffled), food);
			}
			for (Vector v : FROZEN) assertEquals(v.attribute(), Recipes.of(v.seed(), v.food(), shuffled).spec().key());
		}
	}

	@Test
	void addingAnAttributeOnlyMovesFoodsToIt() {
		AttributeSpec extra = new AttributeSpec("new_attribute", ModifierOp.ADD_VALUE, 1, 0, 10, 0, DisplayUnit.POINTS,
				Direction.UP);
		List<AttributeSpec> more = new ArrayList<>(Caps.ALL);
		more.add(extra);
		int moved = 0;
		for (int i = 0; i < 2000; i++) {
			String food = "test:food_" + i;
			Recipe before = Recipes.of(5L, food);
			Recipe after = Recipes.of(5L, food, more);
			if (after.spec() == extra) moved++;
			else assertEquals(before, after, food);
		}
		assertTrue(moved > 50 && moved < 150, "moved " + moved);
	}

	@Test
	void seedSensitivity() {
		int differ = 0;
		for (int i = 0; i < 1000; i++) {
			String food = "test:food_" + i;
			if (!Recipes.of(1L, food).equals(Recipes.of(2L, food))) differ++;
		}
		assertTrue(differ >= 900, "only " + differ + " of 1000 foods differ between seeds 1 and 2");
	}

	@Test
	void uniformOverAttributesAndDirections() {
		Map<String, Integer> counts = new HashMap<>();
		int up = 0;
		int n = 20_000;
		for (int i = 0; i < n; i++) {
			Recipe r = Recipes.of(42L, "test:food_" + i);
			counts.merge(r.spec().key(), 1, Integer::sum);
			if (r.direction() == Direction.UP) up++;
		}
		for (AttributeSpec spec : Caps.ALL) {
			int c = counts.getOrDefault(spec.key(), 0);
			assertTrue(c >= 900 && c <= 1100, spec.key() + " got " + c);
		}
		double upShare = up / (double) n;
		assertTrue(upShare >= 0.45 && upShare <= 0.55, "UP share " + upShare);
	}

	@Test
	void tieBreaksToTheSmallerKey() {
		// two specs with the same key hash identically, so the tie-break decides; equal keys -> the first stays
		AttributeSpec a = new AttributeSpec("same", ModifierOp.ADD_VALUE, 1, 0, 1, 0, DisplayUnit.POINTS, Direction.UP);
		AttributeSpec b = new AttributeSpec("same", ModifierOp.ADD_VALUE, 2, 0, 1, 0, DisplayUnit.POINTS, Direction.UP);
		assertSame(a, Recipes.of(1L, "x", List.of(a, b)).spec());
		assertSame(b, Recipes.of(1L, "x", List.of(b, a)).spec());
	}

	@Test
	void singleSpecAlwaysWinsAndEmptyThrows() {
		assertSame(Caps.LUCK, Recipes.of(1L, "minecraft:carrot", List.of(Caps.LUCK)).spec());
		assertThrows(IllegalArgumentException.class, () -> Recipes.of(1L, "minecraft:carrot", List.of()));
	}
}
