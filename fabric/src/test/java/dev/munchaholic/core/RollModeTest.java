package dev.munchaholic.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

/** byId, ids and translation keys. */
class RollModeTest {
	@Test
	void ids() {
		assertEquals("random", RollMode.RANDOM.id());
		assertEquals("recipes", RollMode.RECIPES.id());
		assertEquals(2, RollMode.values().length);
		assertEquals(0, RollMode.RANDOM.ordinal(), "the /munchaholic mode return value");
		assertEquals(1, RollMode.RECIPES.ordinal());
	}

	@Test
	void translationKeys() {
		assertEquals("munchaholic.rollMode.random", RollMode.RANDOM.translationKey());
		assertEquals("munchaholic.rollMode.recipes", RollMode.RECIPES.translationKey());
	}

	@Test
	void byId() {
		for (RollMode mode : RollMode.values()) assertSame(mode, RollMode.byId(mode.id(), null));
		assertSame(RollMode.RANDOM, RollMode.byId(null, RollMode.RANDOM));
		assertSame(RollMode.RECIPES, RollMode.byId(null, RollMode.RECIPES));
		assertSame(RollMode.RECIPES, RollMode.byId("nope", RollMode.RECIPES));
		assertSame(RollMode.RANDOM, RollMode.byId("", RollMode.RANDOM));
		assertSame(RollMode.RANDOM, RollMode.byId("RECIPES", RollMode.RANDOM), "case-sensitive");
		assertSame(RollMode.RANDOM, RollMode.byId("Recipes", RollMode.RANDOM));
		assertSame(RollMode.RANDOM, RollMode.byId(" recipes", RollMode.RANDOM));
		assertNull(RollMode.byId("unknown", null));
	}
}
