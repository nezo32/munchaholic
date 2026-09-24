package dev.munchaholic.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

/** The jump simulation against known vanilla numbers, and the ledge rule over every reachable jump/gravity/step state. */
class MobilityTest {
	private static final double JUMP = (double) 0.42F;
	private static final double GRAVITY = 0.08;

	private static PlayerStacks stacks(int jump, int gravity, int step) {
		return new PlayerStacks(Map.of(Caps.JUMP_STRENGTH.key(), jump, Caps.GRAVITY.key(), gravity,
				Caps.STEP_HEIGHT.key(), step), 0);
	}

	@Test
	void vanillaJumpIsTheWellKnown1_2522Blocks() {
		assertEquals(1.2522, Mobility.jumpHeight(JUMP, GRAVITY), 1e-4);
	}

	@Test
	void matchesTheReviewTable() {
		// peak heights for jump 100/70/50% and gravity 100/150/200/300% (review-server.md M2)
		double[] jumps = {JUMP, JUMP * 0.7, JUMP * 0.5};
		double[] gravities = {GRAVITY, GRAVITY * 1.5, GRAVITY * 2.0, GRAVITY * 3.0};
		double[][] expected = {
				{1.25, 0.93, 0.77, 0.60},
				{0.68, 0.51, 0.43, 0.35},
				{0.38, 0.30, 0.26, 0.21}};
		for (int j = 0; j < jumps.length; j++) {
			for (int g = 0; g < gravities.length; g++) {
				assertEquals(expected[j][g], Mobility.jumpHeight(jumps[j], gravities[g]), 0.006, "jump " + j + " gravity " + g);
			}
		}
	}

	@Test
	void firstTicksFollowVanillaOrder() {
		// move by vy, then vy = (vy - g) * 0.98F: two ticks for 0.2 / 0.1 -> 0.2 + (0.2 - 0.1) * 0.98F
		double vy0 = (float) 0.2;
		double vy1 = (vy0 - 0.1) * (double) 0.98F;
		double vy2 = (vy1 - 0.1) * (double) 0.98F;
		assertTrue(vy2 <= 0);
		assertEquals(vy0 + vy1, Mobility.jumpHeight(0.2, 0.1), 0.0);
	}

	@Test
	void degenerateValues() {
		assertEquals(0.0, Mobility.jumpHeight(0.0, GRAVITY));
		assertEquals(0.0, Mobility.jumpHeight(1.0E-6, GRAVITY), "jumpFromGround ignores powers <= 1e-5");
		assertEquals(0.0, Mobility.jumpHeight(-1.0, GRAVITY));
		assertEquals(0.0, Mobility.jumpHeight(Double.NaN, GRAVITY));
		assertEquals((float) JUMP / (1.0 - (double) 0.98F), Mobility.jumpHeight(JUMP, 0.0), 1e-6, "no gravity: drag alone");
		assertEquals(0.0, Mobility.jumpHeight(JUMP, Double.NaN));
		assertEquals(Double.POSITIVE_INFINITY, Mobility.jumpHeight(JUMP, -0.08));
		double tiny = Mobility.jumpHeight(JUMP, 1e-12);
		assertTrue(tiny > 20 && tiny < 22, "tiny gravity terminates: drag alone stops it near 0.42 / 0.02 = 21 blocks, was " + tiny);
		assertTrue(Mobility.canClimb(JUMP, Double.NaN, 1.0), "step height alone");
		assertFalse(Mobility.canClimb(JUMP, Double.NaN, 0.6));
	}

	@Test
	void monotoneInJumpAndGravity() {
		for (int j = -5; j < 20; j++) {
			for (int g = -8; g < 20; g++) {
				double here = Mobility.jumpHeight(Caps.JUMP_STRENGTH.valueAt(JUMP, j), Caps.GRAVITY.valueAt(GRAVITY, g));
				assertTrue(Mobility.jumpHeight(Caps.JUMP_STRENGTH.valueAt(JUMP, j + 1), Caps.GRAVITY.valueAt(GRAVITY, g)) > here);
				assertTrue(Mobility.jumpHeight(Caps.JUMP_STRENGTH.valueAt(JUMP, j), Caps.GRAVITY.valueAt(GRAVITY, g + 1)) <= here,
						"gravity " + g + " jump " + j); // equal once the jump is a single tick
			}
		}
	}

	@Test
	void canClimb() {
		assertTrue(Mobility.canClimb(PlayerStacks.EMPTY, BaseLookup.VANILLA), "vanilla player");
		assertFalse(Mobility.canClimb(stacks(-1, 0, 0), BaseLookup.VANILLA), "90% jump: 1.047 < 1.05");
		assertTrue(Mobility.canClimb(stacks(-1, -1, 0), BaseLookup.VANILLA), "90% jump, 90% gravity");
		assertFalse(Mobility.canClimb(stacks(0, 3, 0), BaseLookup.VANILLA), "130% gravity: 1.027");
		assertTrue(Mobility.canClimb(stacks(0, 2, 0), BaseLookup.VANILLA), "120% gravity: 1.095");
		assertTrue(Mobility.canClimb(stacks(-5, 20, 2), BaseLookup.VANILLA), "step height 1.0 alone");
		assertFalse(Mobility.canClimb(stacks(-5, 20, 1), BaseLookup.VANILLA));
		assertTrue(Mobility.canClimb(0.0, GRAVITY, 1.0));
		assertFalse(Mobility.canClimb(0.0, GRAVITY, 0.99));
	}

	@Test
	void hindersClimbing() {
		for (AttributeSpec spec : Caps.ALL) {
			for (Direction direction : Direction.values()) {
				boolean expected = spec == Caps.JUMP_STRENGTH && direction == Direction.DOWN
						|| spec == Caps.GRAVITY && direction == Direction.UP
						|| spec == Caps.STEP_HEIGHT && direction == Direction.DOWN;
				assertEquals(expected, Mobility.hindersClimbing(spec, direction), spec.key() + " " + direction);
			}
		}
	}

	/** Every state inside the caps: a climbing player stays climbing after any legal step, and only hindering steps are vetoed. */
	@Test
	void guardKeepsEveryReachableStateClimbable() {
		AttributeSpec[] specs = {Caps.JUMP_STRENGTH, Caps.GRAVITY, Caps.STEP_HEIGHT};
		int climbable = 0;
		for (int j = Caps.JUMP_STRENGTH.minSteps(JUMP); j <= Caps.JUMP_STRENGTH.maxSteps(JUMP); j++) {
			for (int g = Caps.GRAVITY.minSteps(GRAVITY); g <= Caps.GRAVITY.maxSteps(GRAVITY); g++) {
				for (int s = Caps.STEP_HEIGHT.minSteps(0.6); s <= Caps.STEP_HEIGHT.maxSteps(0.6); s++) {
					PlayerStacks here = stacks(j, g, s);
					boolean climbs = Mobility.canClimb(here, BaseLookup.VANILLA);
					if (climbs) climbable++;
					RollVeto guard = Mobility.guard(here, BaseLookup.VANILLA);
					for (AttributeSpec spec : specs) {
						for (Direction direction : Direction.values()) {
							int from = here.steps(spec);
							RollOutcome.Applied step = new RollOutcome.Applied(spec, direction, from, from + direction.sign(),
									spec.playerBase());
							PlayerStacks after = here.withSteps(spec, step.newSteps());
							boolean vetoed = guard.vetoes(step);
							if (!Mobility.hindersClimbing(spec, direction)) {
								assertFalse(vetoed, "a helping step is never vetoed: " + step);
							} else {
								assertEquals(!Mobility.canClimb(after, BaseLookup.VANILLA), vetoed, step + " from " + here);
							}
							if (climbs && !vetoed) {
								assertTrue(Mobility.canClimb(after, BaseLookup.VANILLA), step + " from " + here);
							}
						}
					}
				}
			}
		}
		assertTrue(climbable > 0);
	}

	@Test
	void otherAttributesAreNeverVetoed() {
		PlayerStacks stuck = stacks(-5, 20, -2);
		assertFalse(Mobility.canClimb(stuck, BaseLookup.VANILLA));
		RollVeto guard = Mobility.guard(stuck, BaseLookup.VANILLA);
		for (AttributeSpec spec : Caps.ALL) {
			if (spec == Caps.JUMP_STRENGTH || spec == Caps.GRAVITY || spec == Caps.STEP_HEIGHT) continue;
			for (Direction direction : Direction.values()) {
				assertFalse(guard.vetoes(new RollOutcome.Applied(spec, direction, 0, direction.sign(), spec.playerBase())), spec.key());
			}
		}
	}

	@Test
	void usesTheLiveBase() {
		// another mod halved the jump base: the player can't climb, and only helping jump/gravity/step rolls are allowed
		BaseLookup weak = spec -> spec == Caps.JUMP_STRENGTH ? 0.21 : spec.playerBase();
		assertFalse(Mobility.canClimb(PlayerStacks.EMPTY, weak));
		RollVeto guard = Mobility.guard(PlayerStacks.EMPTY, weak);
		assertTrue(guard.vetoes(new RollOutcome.Applied(Caps.GRAVITY, Direction.UP, 0, 1, GRAVITY)));
		assertFalse(guard.vetoes(new RollOutcome.Applied(Caps.GRAVITY, Direction.DOWN, 0, -1, GRAVITY)));
		assertFalse(guard.vetoes(new RollOutcome.Applied(Caps.JUMP_STRENGTH, Direction.UP, 0, 1, 0.21)));
	}
}
