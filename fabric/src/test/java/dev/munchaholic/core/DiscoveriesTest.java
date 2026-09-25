package dev.munchaholic.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** with is idempotent, sorted order, immutability. */
class DiscoveriesTest {
	@Test
	void withAddsAndIsIdempotent() {
		Discoveries one = Discoveries.EMPTY.with("minecraft:carrot");
		assertTrue(one.has("minecraft:carrot"));
		assertFalse(one.has("minecraft:bread"));
		assertEquals(1, one.size());
		Discoveries again = one.with("minecraft:carrot");
		assertSame(one, again, "re-adding a known food returns the same value");
		assertEquals(1, again.size());
		Discoveries two = one.with("minecraft:bread");
		assertEquals(2, two.size());
		assertEquals(1, one.size(), "with returns a copy");
		assertEquals(0, Discoveries.EMPTY.size());
		assertFalse(Discoveries.EMPTY.has("minecraft:carrot"));
	}

	@Test
	void hasNullIsFalse() {
		assertFalse(Discoveries.EMPTY.has(null));
		assertFalse(Discoveries.EMPTY.with("a").has(null));
		assertFalse(Discoveries.EMPTY.has(""));
	}

	@Test
	void sortedIsNaturalOrder() {
		Discoveries d = Discoveries.EMPTY.with("minecraft:golden_apple").with("minecraft:bread").with("mymod:apple")
				.with("minecraft:cake").with("minecraft:apple");
		assertEquals(List.of("minecraft:apple", "minecraft:bread", "minecraft:cake", "minecraft:golden_apple", "mymod:apple"),
				d.sorted());
		assertEquals(List.of(), Discoveries.EMPTY.sorted());
		assertThrows(UnsupportedOperationException.class, () -> d.sorted().add("x"));
	}

	@Test
	void immutability() {
		Set<String> source = new HashSet<>(Set.of("minecraft:carrot"));
		Discoveries d = new Discoveries(source);
		source.add("minecraft:bread");
		assertEquals(Set.of("minecraft:carrot"), d.foods(), "defensive copy of the input set");
		assertThrows(UnsupportedOperationException.class, () -> d.foods().add("minecraft:bread"));
		assertThrows(UnsupportedOperationException.class, () -> Discoveries.EMPTY.foods().add("x"));
		assertEquals(new Discoveries(Set.of("a", "b")), Discoveries.EMPTY.with("b").with("a"));
	}
}
