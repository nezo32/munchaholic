package dev.munchaholic.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/** Zero entries dropped, withSteps / after, saturating bites, immutability, isEmpty. */
class PlayerStacksTest {
	@Test
	void zeroAndNullEntriesAreDropped() {
		Map<String, Integer> raw = new HashMap<>();
		raw.put("scale", 2);
		raw.put("luck", 0);
		raw.put(null, 3);
		raw.put("armor", null);
		PlayerStacks stacks = new PlayerStacks(raw, 5);
		assertEquals(Map.of("scale", 2), stacks.steps());
		assertEquals(5, stacks.bites());
		assertEquals(PlayerStacks.EMPTY, new PlayerStacks(null, 0));
		assertEquals(0, new PlayerStacks(Map.of(), -4).bites(), "negative bites clamp to 0");
	}

	@Test
	void stepsDefaultsToZero() {
		PlayerStacks stacks = PlayerStacks.EMPTY.withSteps(Caps.SCALE, -3);
		assertEquals(-3, stacks.steps(Caps.SCALE));
		assertEquals(0, stacks.steps(Caps.LUCK));
		// unknown keys survive (another version's table) and count as a change
		PlayerStacks unknown = new PlayerStacks(Map.of("removed_attribute", 4), 0);
		assertEquals(4, unknown.steps().get("removed_attribute"));
		assertFalse(unknown.isEmpty());
	}

	@Test
	void withStepsReplacesAndZeroRemoves() {
		PlayerStacks a = PlayerStacks.EMPTY.withSteps(Caps.SCALE, 2).withSteps(Caps.LUCK, -1);
		assertEquals(Map.of("scale", 2, "luck", -1), a.steps());
		PlayerStacks b = a.withSteps(Caps.SCALE, 5);
		assertEquals(Map.of("scale", 5, "luck", -1), b.steps());
		PlayerStacks c = b.withSteps(Caps.SCALE, 0);
		assertEquals(Map.of("luck", -1), c.steps());
		assertEquals(Map.of("scale", 2, "luck", -1), a.steps(), "withSteps returns a copy");
		assertEquals(0, c.bites());
		assertEquals(3, new PlayerStacks(Map.of(), 3).withSteps(Caps.ARMOR, 1).bites(), "bites are kept");
	}

	@Test
	void afterApplied() {
		PlayerStacks start = new PlayerStacks(Map.of("scale", 1), 7);
		PlayerStacks next = start.after(new RollOutcome.Applied(Caps.SCALE, Direction.UP, 1, 2, 1.0));
		assertEquals(2, next.steps(Caps.SCALE));
		assertEquals(8, next.bites());
		PlayerStacks back = next.after(new RollOutcome.Applied(Caps.SCALE, Direction.DOWN, 2, 1, 1.0))
				.after(new RollOutcome.Applied(Caps.SCALE, Direction.DOWN, 1, 0, 1.0));
		assertTrue(back.isEmpty(), "opposite rolls cancel out");
		assertEquals(10, back.bites());
		assertEquals(Map.of(), back.steps());
	}

	@Test
	void afterCappedOrNothingOnlyCountsTheBite() {
		PlayerStacks start = new PlayerStacks(Map.of("armor", 20), 3);
		PlayerStacks capped = start.after(new RollOutcome.Capped(Caps.ARMOR, Direction.UP, 20, 0.0));
		assertEquals(start.steps(), capped.steps());
		assertEquals(4, capped.bites());
		PlayerStacks nothing = capped.after(RollOutcome.Nothing.INSTANCE);
		assertEquals(start.steps(), nothing.steps());
		assertEquals(5, nothing.bites());
		assertEquals(3, start.bites(), "after returns a copy");
	}

	@Test
	void bitesSaturate() {
		PlayerStacks almost = new PlayerStacks(Map.of(), Integer.MAX_VALUE - 1);
		assertEquals(Integer.MAX_VALUE, almost.withBite().bites());
		assertEquals(Integer.MAX_VALUE, almost.withBite().withBite().bites());
		assertEquals(Integer.MAX_VALUE, almost.withBite().after(RollOutcome.Nothing.INSTANCE).bites());
		PlayerStacks full = new PlayerStacks(Map.of(), Integer.MAX_VALUE);
		assertEquals(Integer.MAX_VALUE,
				full.after(new RollOutcome.Applied(Caps.LUCK, Direction.UP, 0, 1, 0.0)).bites());
		assertEquals(1, full.after(new RollOutcome.Applied(Caps.LUCK, Direction.UP, 0, 1, 0.0)).steps(Caps.LUCK));
		assertEquals(1, PlayerStacks.EMPTY.withBite().bites());
		assertEquals(0, PlayerStacks.EMPTY.bites());
	}

	@Test
	void immutability() {
		Map<String, Integer> source = new HashMap<>(Map.of("scale", 1));
		PlayerStacks stacks = new PlayerStacks(source, 0);
		source.put("scale", 9);
		source.put("luck", 3);
		assertEquals(Map.of("scale", 1), stacks.steps(), "defensive copy of the input map");
		assertThrows(UnsupportedOperationException.class, () -> stacks.steps().put("luck", 1));
		assertThrows(UnsupportedOperationException.class, () -> stacks.steps().remove("scale"));
		assertThrows(UnsupportedOperationException.class, () -> PlayerStacks.EMPTY.steps().put("x", 1));
		PlayerStacks before = stacks;
		stacks.withSteps(Caps.SCALE, 4).withBite();
		assertEquals(new PlayerStacks(Map.of("scale", 1), 0), before);
	}

	@Test
	void isEmptyIgnoresBites() {
		assertTrue(PlayerStacks.EMPTY.isEmpty());
		assertTrue(new PlayerStacks(Map.of(), 42).isEmpty());
		assertTrue(new PlayerStacks(Map.of("scale", 0), 42).isEmpty());
		assertFalse(PlayerStacks.EMPTY.withSteps(Caps.LUCK, -1).isEmpty());
		assertEquals(PlayerStacks.EMPTY, new PlayerStacks(Map.of("luck", 0), 0));
	}
}
