package dev.munchaholic.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import dev.munchaholic.core.RollOutcome.Applied;
import dev.munchaholic.core.RollOutcome.Capped;
import org.junit.jupiter.api.Test;

/** Scripted draws for RollSelector.random (capped reroll, fallback, Nothing) and fixed; a long random walk stays in range. */
class RollSelectorTest {
	/** Top of an armor-like 0..2 range: UP is capped, DOWN is fine. */
	private static final AttributeSpec SMALL = new AttributeSpec("small", ModifierOp.ADD_VALUE, 1.0, 0.0, 2.0, 0.0,
			DisplayUnit.POINTS, Direction.UP);
	private static final AttributeSpec OTHER = new AttributeSpec("other", ModifierOp.ADD_VALUE, 1.0, 0.0, 2.0, 0.0,
			DisplayUnit.POINTS, Direction.UP);
	/** No legal step at all: min == max == base. */
	private static final AttributeSpec STUCK = new AttributeSpec("stuck", ModifierOp.ADD_VALUE, 1.0, 0.0, 0.0, 0.0,
			DisplayUnit.POINTS, Direction.UP);

	/** Returns the scripted values in order and fails on any extra or out-of-range call. */
	private static RandomIndex scripted(int... values) {
		Deque<Integer> queue = new ArrayDeque<>();
		for (int v : values) queue.add(v);
		return bound -> {
			assertTrue(bound > 0, "bound must be > 0, was " + bound);
			Integer v = queue.poll();
			if (v == null) fail("unexpected extra random draw (bound " + bound + ")");
			assertTrue(v < bound, "scripted value " + v + " out of bound " + bound);
			return v;
		};
	}

	/** Like {@link #scripted} but also records the bounds it was called with. */
	private static RandomIndex recording(List<Integer> bounds, int... values) {
		RandomIndex inner = scripted(values);
		return bound -> {
			bounds.add(bound);
			return inner.nextInt(bound);
		};
	}

	/** java.util.Random wrapper that asserts the bound is positive. */
	private static RandomIndex checked(Random random) {
		return bound -> {
			assertTrue(bound > 0, "bound must be > 0, was " + bound);
			return random.nextInt(bound);
		};
	}

	@Test
	void appliedPick() {
		// index 0 = scale, 1 = DOWN
		Applied a = assertInstanceOf(Applied.class,
				RollSelector.random(PlayerStacks.EMPTY, BaseLookup.VANILLA, scripted(0, 1)));
		assertSame(Caps.SCALE, a.spec());
		assertEquals(Direction.DOWN, a.direction());
		assertEquals(0, a.oldSteps());
		assertEquals(-1, a.newSteps());
		assertEquals(1.0, a.base());
		assertEquals(-8.0, a.displayDelta(), 1e-9);
		assertEquals(92.0, a.displayNow(), 1e-9);
		assertEquals(false, a.isBuff());

		Applied luck = assertInstanceOf(Applied.class,
				RollSelector.random(PlayerStacks.EMPTY, BaseLookup.VANILLA, scripted(19, 0)));
		assertSame(Caps.LUCK, luck.spec());
		assertEquals(Direction.UP, luck.direction());
		assertEquals(true, luck.isBuff());
	}

	@Test
	void appliedUsesExistingStepsAndLiveBase() {
		PlayerStacks stacks = PlayerStacks.EMPTY.withSteps(Caps.MAX_HEALTH, 3);
		BaseLookup bases = spec -> spec == Caps.MAX_HEALTH ? 30.0 : spec.playerBase();
		Applied a = assertInstanceOf(Applied.class, RollSelector.random(stacks, bases, scripted(8, 0)));
		assertSame(Caps.MAX_HEALTH, a.spec());
		assertEquals(3, a.oldSteps());
		assertEquals(4, a.newSteps());
		assertEquals(30.0, a.base());
		assertEquals(38.0, a.displayNow(), 1e-9);
		assertEquals(2.0, a.displayDelta(), 1e-9);
	}

	@Test
	void cappedPickRemovesThatAttributeAndRollsADifferentOne() {
		List<Integer> bounds = new ArrayList<>();
		// armor (index 9) DOWN at 0 is capped -> 19 candidates left, index 0 = scale, UP
		Applied a = assertInstanceOf(Applied.class,
				RollSelector.random(PlayerStacks.EMPTY, BaseLookup.VANILLA, recording(bounds, 9, 1, 0, 0)));
		assertSame(Caps.SCALE, a.spec());
		assertEquals(Direction.UP, a.direction());
		assertEquals(List.of(20, 2, 19, 2), bounds);

		// capped armor is never drawn again, even when the next index would have pointed at it
		for (int i = 0; i < 19; i++) {
			Applied next = assertInstanceOf(Applied.class,
					RollSelector.random(PlayerStacks.EMPTY, BaseLookup.VANILLA, scripted(9, 1, i, 0)));
			assertNotEquals(Caps.ARMOR, next.spec(), "index " + i);
			assertEquals(Direction.UP, next.direction());
		}
		// index 9 in the remaining list is knockback_resistance (armor was removed)
		Applied kb = assertInstanceOf(Applied.class,
				RollSelector.random(PlayerStacks.EMPTY, BaseLookup.VANILLA, scripted(9, 1, 9, 1)));
		assertSame(Caps.KNOCKBACK_RESISTANCE, kb.spec());
		assertEquals(Direction.DOWN, kb.direction());
	}

	@Test
	void severalCappedPicksInARow() {
		// armor DOWN (9), then mining_efficiency (index 14 after removing armor) DOWN, then scale UP
		Applied a = assertInstanceOf(Applied.class,
				RollSelector.random(PlayerStacks.EMPTY, BaseLookup.VANILLA, scripted(9, 1, 14, 1, 0, 0)));
		assertSame(Caps.SCALE, a.spec());
	}

	@Test
	void fallbackOnceEveryAttributeWasTried() {
		List<AttributeSpec> specs = List.of(SMALL, OTHER);
		PlayerStacks stacks = PlayerStacks.EMPTY.withSteps(OTHER, 2);
		List<Integer> bounds = new ArrayList<>();
		// SMALL DOWN at 0: capped; OTHER UP at 2: capped; allowed pairs = [SMALL UP, OTHER DOWN] -> pick 1
		Applied a = assertInstanceOf(Applied.class,
				RollSelector.random(specs, stacks, BaseLookup.VANILLA, recording(bounds, 0, 1, 0, 0, 1)));
		assertSame(OTHER, a.spec());
		assertEquals(Direction.DOWN, a.direction());
		assertEquals(2, a.oldSteps());
		assertEquals(1, a.newSteps());
		assertEquals(List.of(2, 2, 1, 2, 2), bounds);

		Applied first = assertInstanceOf(Applied.class,
				RollSelector.random(specs, stacks, BaseLookup.VANILLA, scripted(0, 1, 0, 0, 0)));
		assertSame(SMALL, first.spec());
		assertEquals(Direction.UP, first.direction());
	}

	@Test
	void fallbackPairsAreInSpecOrderUpThenDown() {
		AttributeSpec third = new AttributeSpec("third", ModifierOp.ADD_VALUE, 1.0, 0.0, 2.0, 0.0, DisplayUnit.POINTS,
				Direction.UP);
		List<AttributeSpec> specs = List.of(SMALL, OTHER, third);
		PlayerStacks stacks = PlayerStacks.EMPTY.withSteps(OTHER, 2);
		// loop: SMALL DOWN, OTHER UP, third DOWN are all capped; pairs = [SMALL UP, OTHER DOWN, third UP]
		List<Recipe> expected = List.of(new Recipe(SMALL, Direction.UP), new Recipe(OTHER, Direction.DOWN),
				new Recipe(third, Direction.UP));
		for (int i = 0; i < expected.size(); i++) {
			List<Integer> bounds = new ArrayList<>();
			Applied a = assertInstanceOf(Applied.class,
					RollSelector.random(specs, stacks, BaseLookup.VANILLA, recording(bounds, 0, 1, 0, 0, 0, 1, i)));
			assertEquals(expected.get(i), new Recipe(a.spec(), a.direction()), "pair " + i);
			assertEquals(List.of(3, 2, 2, 2, 1, 2, 3), bounds);
		}
	}

	@Test
	void nothingWhenNoStepIsLegal() {
		List<Integer> bounds = new ArrayList<>();
		assertSame(RollOutcome.Nothing.INSTANCE,
				RollSelector.random(List.of(STUCK), PlayerStacks.EMPTY, BaseLookup.VANILLA, recording(bounds, 0, 0)));
		assertEquals(List.of(1, 2), bounds, "no fallback draw when there is no pair");
		assertSame(RollOutcome.Nothing.INSTANCE,
				RollSelector.random(List.of(STUCK), PlayerStacks.EMPTY, BaseLookup.VANILLA, scripted(0, 1)));
		assertSame(RollOutcome.Nothing.INSTANCE,
				RollSelector.random(List.of(), PlayerStacks.EMPTY, BaseLookup.VANILLA, scripted()));
		for (int seed = 0; seed < 50; seed++) {
			assertSame(RollOutcome.Nothing.INSTANCE,
					RollSelector.random(List.of(STUCK, STUCK), PlayerStacks.EMPTY, BaseLookup.VANILLA, checked(new Random(seed))));
		}
	}

	@Test
	void fixedAppliedOrCapped() {
		Recipe up = new Recipe(Caps.ARMOR, Direction.UP);
		Recipe down = new Recipe(Caps.ARMOR, Direction.DOWN);
		Applied a = assertInstanceOf(Applied.class, RollSelector.fixed(PlayerStacks.EMPTY, BaseLookup.VANILLA, up));
		assertSame(Caps.ARMOR, a.spec());
		assertEquals(Direction.UP, a.direction());
		assertEquals(0, a.oldSteps());
		assertEquals(1, a.newSteps());

		Capped c = assertInstanceOf(Capped.class, RollSelector.fixed(PlayerStacks.EMPTY, BaseLookup.VANILLA, down));
		assertSame(Caps.ARMOR, c.spec());
		assertEquals(Direction.DOWN, c.direction());
		assertEquals(0, c.steps());
		assertEquals(0.0, c.base());
		assertEquals(-1.0, c.displayDelta(), 1e-9);
		assertEquals(0.0, c.displayNow(), 1e-9);

		PlayerStacks atFloor = PlayerStacks.EMPTY.withSteps(Caps.SCALE, -10);
		Capped scale = assertInstanceOf(Capped.class,
				RollSelector.fixed(atFloor, BaseLookup.VANILLA, new Recipe(Caps.SCALE, Direction.DOWN)));
		assertEquals(-10, scale.steps());
		assertEquals(-8.0, scale.displayDelta(), 1e-9);
		assertEquals(20.0, scale.displayNow(), 1e-9);
		Applied back = assertInstanceOf(Applied.class,
				RollSelector.fixed(atFloor, BaseLookup.VANILLA, new Recipe(Caps.SCALE, Direction.UP)));
		assertEquals(-9, back.newSteps());
		assertEquals(28.0, back.displayNow(), 1e-9);
	}

	@Test
	void fixedUsesLiveBaseForRecovery() {
		BaseLookup huge = spec -> 10.0;
		assertInstanceOf(Capped.class, RollSelector.fixed(PlayerStacks.EMPTY, huge, new Recipe(Caps.SCALE, Direction.UP)));
		Applied a = assertInstanceOf(Applied.class,
				RollSelector.fixed(PlayerStacks.EMPTY, huge, new Recipe(Caps.SCALE, Direction.DOWN)));
		assertEquals(10.0, a.base());
	}

	@Test
	void vetoedPickIsTreatedLikeACappedOne() {
		RollVeto noGrowing = c -> c.spec() == Caps.SCALE && c.direction() == Direction.UP;
		List<Integer> bounds = new ArrayList<>();
		// scale UP vetoed -> scale leaves the candidates; index 0 of the rest = gravity, UP
		Applied a = assertInstanceOf(Applied.class,
				RollSelector.random(PlayerStacks.EMPTY, BaseLookup.VANILLA, recording(bounds, 0, 0, 0, 0), noGrowing));
		assertSame(Caps.GRAVITY, a.spec());
		assertEquals(Direction.UP, a.direction());
		assertEquals(List.of(20, 2, 19, 2), bounds);
		// scale DOWN is not vetoed
		Applied down = assertInstanceOf(Applied.class,
				RollSelector.random(PlayerStacks.EMPTY, BaseLookup.VANILLA, scripted(0, 1), noGrowing));
		assertSame(Caps.SCALE, down.spec());

		Capped c = assertInstanceOf(Capped.class,
				RollSelector.fixed(PlayerStacks.EMPTY, BaseLookup.VANILLA, new Recipe(Caps.SCALE, Direction.UP), noGrowing));
		assertEquals(0, c.steps());
		assertEquals(100.0, c.displayNow(), 1e-9);
		assertInstanceOf(Applied.class,
				RollSelector.fixed(PlayerStacks.EMPTY, BaseLookup.VANILLA, new Recipe(Caps.SCALE, Direction.DOWN), noGrowing));
	}

	@Test
	void vetoedPairsAreLeftOutOfTheFallback() {
		List<AttributeSpec> specs = List.of(SMALL, OTHER);
		RollVeto noSmall = c -> c.spec() == SMALL;
		// SMALL DOWN capped, OTHER DOWN capped; fallback pairs = [OTHER UP] only (SMALL UP is vetoed)
		List<Integer> bounds = new ArrayList<>();
		Applied a = assertInstanceOf(Applied.class,
				RollSelector.random(specs, PlayerStacks.EMPTY, BaseLookup.VANILLA, recording(bounds, 0, 1, 0, 1, 0), noSmall));
		assertSame(OTHER, a.spec());
		assertEquals(Direction.UP, a.direction());
		assertEquals(List.of(2, 2, 1, 2, 1), bounds);
		assertSame(RollOutcome.Nothing.INSTANCE,
				RollSelector.random(specs, PlayerStacks.EMPTY, BaseLookup.VANILLA, scripted(0, 0, 0, 0), c -> true));
	}

	@Test
	void vetoOr() {
		RollVeto scale = c -> c.spec() == Caps.SCALE;
		RollVeto up = c -> c.direction() == Direction.UP;
		Applied scaleDown = new Applied(Caps.SCALE, Direction.DOWN, 0, -1, 1.0);
		Applied luckUp = new Applied(Caps.LUCK, Direction.UP, 0, 1, 0.0);
		Applied luckDown = new Applied(Caps.LUCK, Direction.DOWN, 0, -1, 0.0);
		assertTrue(scale.or(up).vetoes(scaleDown));
		assertTrue(scale.or(up).vetoes(luckUp));
		assertTrue(!scale.or(up).vetoes(luckDown));
		assertTrue(!RollVeto.NONE.vetoes(luckUp));
	}

	@Test
	void mobilityGuardAlwaysApplies() {
		// jump 90% -> 80% at vanilla gravity and step height: 0.846-block jump < 1.02 -> vetoed in both modes
		Recipe jumpDown = new Recipe(Caps.JUMP_STRENGTH, Direction.DOWN);
		PlayerStacks jump90 = PlayerStacks.EMPTY.withSteps(Caps.JUMP_STRENGTH, -1);
		assertInstanceOf(Applied.class, RollSelector.fixed(PlayerStacks.EMPTY, BaseLookup.VANILLA, jumpDown),
				"100% -> 90% jumps 1.047: allowed");
		Capped c = assertInstanceOf(Capped.class, RollSelector.fixed(jump90, BaseLookup.VANILLA, jumpDown));
		assertEquals(90.0, c.displayNow(), 1e-9);
		int jump = Caps.ALL.indexOf(Caps.JUMP_STRENGTH);
		Applied a = assertInstanceOf(Applied.class,
				RollSelector.random(jump90, BaseLookup.VANILLA, scripted(jump, 1, 0, 0)));
		assertSame(Caps.SCALE, a.spec(), "jump DOWN vetoed -> a different attribute");
		// gravity: 130% is the limit at 100% jump
		PlayerStacks gravity130 = PlayerStacks.EMPTY.withSteps(Caps.GRAVITY, 3);
		assertInstanceOf(Applied.class, RollSelector.fixed(PlayerStacks.EMPTY.withSteps(Caps.GRAVITY, 2), BaseLookup.VANILLA,
				new Recipe(Caps.GRAVITY, Direction.UP)));
		assertInstanceOf(Capped.class, RollSelector.fixed(gravity130, BaseLookup.VANILLA, new Recipe(Caps.GRAVITY, Direction.UP)));
		// the same step is fine once gravity is lower, or step height alone climbs a block
		PlayerStacks lowGravity = jump90.withSteps(Caps.GRAVITY, -3);
		assertInstanceOf(Applied.class, RollSelector.fixed(lowGravity, BaseLookup.VANILLA, jumpDown));
		PlayerStacks highStep = jump90.withSteps(Caps.STEP_HEIGHT, 2);
		assertInstanceOf(Applied.class, RollSelector.fixed(highStep, BaseLookup.VANILLA, jumpDown));
		// ... and step height can't then drop below a block while the jump is too weak
		PlayerStacks weakJump = highStep.withSteps(Caps.JUMP_STRENGTH, -3);
		assertInstanceOf(Capped.class,
				RollSelector.fixed(weakJump, BaseLookup.VANILLA, new Recipe(Caps.STEP_HEIGHT, Direction.DOWN)));
		assertInstanceOf(Applied.class,
				RollSelector.fixed(weakJump, BaseLookup.VANILLA, new Recipe(Caps.JUMP_STRENGTH, Direction.DOWN)),
				"step height 1.0 climbs whatever the jump");
		assertInstanceOf(Applied.class,
				RollSelector.fixed(weakJump, BaseLookup.VANILLA, new Recipe(Caps.GRAVITY, Direction.UP)),
				"step height 1.0 climbs whatever the gravity");
		assertInstanceOf(Applied.class,
				RollSelector.fixed(weakJump, BaseLookup.VANILLA, new Recipe(Caps.STEP_HEIGHT, Direction.UP)));
		// an extra veto is combined with the guard, not instead of it
		assertInstanceOf(Capped.class, RollSelector.fixed(jump90, BaseLookup.VANILLA, jumpDown, RollVeto.NONE));
	}

	@Test
	void stackingSameAttribute() {
		PlayerStacks stacks = PlayerStacks.EMPTY;
		for (int i = 1; i <= 3; i++) {
			RollOutcome o = RollSelector.random(stacks, BaseLookup.VANILLA, scripted(0, 0));
			Applied a = assertInstanceOf(Applied.class, o);
			assertEquals(i - 1, a.oldSteps());
			assertEquals(i, a.newSteps());
			stacks = stacks.after(o);
		}
		assertEquals(3, stacks.steps(Caps.SCALE));
		assertEquals(3, stacks.bites());
		assertEquals(124.0, Caps.SCALE.displayValue(1.0, stacks.steps(Caps.SCALE)), 1e-9);
		RollOutcome down = RollSelector.random(stacks, BaseLookup.VANILLA, scripted(0, 1));
		stacks = stacks.after(down);
		assertEquals(2, stacks.steps(Caps.SCALE));
	}

	@Test
	void inputsAreNotMutated() {
		List<AttributeSpec> specs = new ArrayList<>(Caps.ALL);
		PlayerStacks stacks = PlayerStacks.EMPTY.withSteps(Caps.ARMOR, 20).withSteps(Caps.LUCK, -10);
		PlayerStacks copy = new PlayerStacks(stacks.steps(), stacks.bites());
		Random random = new Random(7);
		for (int i = 0; i < 500; i++) RollSelector.random(specs, stacks, BaseLookup.VANILLA, checked(random));
		RollSelector.fixed(stacks, BaseLookup.VANILLA, new Recipe(Caps.ARMOR, Direction.UP));
		assertEquals(Caps.ALL, specs);
		assertEquals(copy, stacks);
	}

	@Test
	void uniformOverAttributesAndDirections() {
		// every spec at a point where both directions are legal, so rerolls never bias the draw
		Map<String, Integer> start = new HashMap<>();
		for (AttributeSpec spec : Caps.ALL) start.put(spec.key(), spec.minSteps(spec.playerBase()) < 0 ? 0 : 1);
		// from 100% jump and gravity, jump DOWN (90%: 1.047 blocks) and gravity UP (110%) both keep a 1.02-block jump
		PlayerStacks stacks = new PlayerStacks(start, 0);
		for (AttributeSpec spec : Caps.ALL) {
			for (Direction direction : Direction.values()) {
				assertInstanceOf(Applied.class, RollSelector.fixed(stacks, BaseLookup.VANILLA, new Recipe(spec, direction)),
						spec.key() + " " + direction);
			}
		}
		Map<String, Integer> counts = new HashMap<>();
		int up = 0;
		int draws = 40_000;
		RandomIndex random = checked(new Random(42));
		for (int i = 0; i < draws; i++) {
			Applied a = assertInstanceOf(Applied.class, RollSelector.random(stacks, BaseLookup.VANILLA, random));
			counts.merge(a.spec().key(), 1, Integer::sum);
			if (a.direction() == Direction.UP) up++;
		}
		for (AttributeSpec spec : Caps.ALL) {
			double share = counts.getOrDefault(spec.key(), 0) / (double) draws;
			assertTrue(Math.abs(share - 0.05) <= 0.006, spec.key() + " share " + share);
		}
		double upShare = up / (double) draws;
		assertTrue(Math.abs(upShare - 0.5) <= 0.015, "UP share " + upShare);
	}

	@Test
	void freshPlayerAlwaysGetsAChange() {
		for (int seed = 0; seed < 2000; seed++) {
			assertInstanceOf(Applied.class, RollSelector.random(PlayerStacks.EMPTY, BaseLookup.VANILLA, checked(new Random(seed))));
		}
	}

	@Test
	void longRandomWalkStaysInRange() {
		Random random = new Random(1);
		PlayerStacks stacks = PlayerStacks.EMPTY;
		for (int i = 0; i < 10_000; i++) {
			RollOutcome o = RollSelector.random(stacks, BaseLookup.VANILLA, checked(random));
			assertInstanceOf(Applied.class, o, "bite " + i);
			stacks = stacks.after(o);
			for (AttributeSpec spec : Caps.ALL) {
				double value = spec.valueAt(spec.playerBase(), stacks.steps(spec));
				assertTrue(spec.inRange(value), spec.key() + " left its range at bite " + i + ": " + value);
			}
			assertTrue(Mobility.canClimb(stacks, BaseLookup.VANILLA), "can't climb a ledge after bite " + i + ": " + stacks);
		}
		assertEquals(10_000, stacks.bites());
		assertTrue(Caps.MAX_HEALTH.valueAt(20.0, stacks.steps(Caps.MAX_HEALTH)) >= 2.0);
	}
}
