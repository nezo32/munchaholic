package dev.munchaholic.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** valueAt, canStep (edges and recovery), displayValue / displayDelta for all three units, translationKey. */
class AttributeSpecTest {
	private static final double D = 1e-9;

	@Test
	void valueAtForBothOps() {
		assertEquals(0.92, Caps.SCALE.valueAt(1.0, -1), D);
		assertEquals(1.16, Caps.SCALE.valueAt(1.0, 2), D);
		assertEquals(2.32, Caps.SCALE.valueAt(2.0, 2), D, "multiplied base scales with the live base");
		assertEquals(1.0, Caps.SCALE.valueAt(1.0, 0));
		assertEquals(22.0, Caps.MAX_HEALTH.valueAt(20.0, 1), D);
		assertEquals(14.0, Caps.MAX_HEALTH.valueAt(20.0, -3), D);
		assertEquals(32.0, Caps.MAX_HEALTH.valueAt(30.0, 1), D, "add value adds to the live base");
		assertEquals(0.8, Caps.STEP_HEIGHT.valueAt(0.6, 1), D);
		assertEquals(-0.3, Caps.KNOCKBACK_RESISTANCE.valueAt(0.0, -3), D);
	}

	@Test
	void modifierAmountIsStepsTimesStep() {
		assertEquals(0.16, Caps.SCALE.modifierAmount(2), D);
		assertEquals(-0.08, Caps.SCALE.modifierAmount(-1), D);
		assertEquals(0.0, Caps.SCALE.modifierAmount(0));
		assertEquals(4.0, Caps.MAX_HEALTH.modifierAmount(2));
		assertEquals(-18.0, Caps.MAX_HEALTH.modifierAmount(-9));
		// modifier and value agree: base + amount (ADD_VALUE), base * (1 + amount) (ADD_MULTIPLIED_BASE)
		for (AttributeSpec spec : Caps.ALL) {
			for (int s = -5; s <= 5; s++) {
				double base = spec.playerBase();
				double viaModifier = spec.op() == ModifierOp.ADD_VALUE
						? base + spec.modifierAmount(s) : base * (1.0 + spec.modifierAmount(s));
				assertEquals(spec.valueAt(base, s), viaModifier, spec.key());
			}
		}
	}

	@Test
	void inRangeUsesEps() {
		AttributeSpec armor = Caps.ARMOR;
		assertTrue(armor.inRange(0.0));
		assertTrue(armor.inRange(20.0));
		assertTrue(armor.inRange(-1e-10));
		assertTrue(armor.inRange(20.0 + 1e-10));
		assertFalse(armor.inRange(-1e-8));
		assertFalse(armor.inRange(20.0 + 1e-8));
		assertFalse(armor.inRange(Double.NaN));
	}

	@Test
	void canStepAtTheEdges() {
		AttributeSpec armor = Caps.ARMOR;
		assertFalse(armor.canStep(0.0, 0, Direction.DOWN));
		assertTrue(armor.canStep(0.0, 0, Direction.UP));
		assertTrue(armor.canStep(0.0, 19, Direction.UP));
		assertFalse(armor.canStep(0.0, 20, Direction.UP));
		assertTrue(armor.canStep(0.0, 20, Direction.DOWN));

		// max_health: the step lands exactly on the 2.0 minimum
		assertTrue(Caps.MAX_HEALTH.canStep(20.0, -8, Direction.DOWN));
		assertFalse(Caps.MAX_HEALTH.canStep(20.0, -9, Direction.DOWN));
		// scale: 0.2 is the lowest step (0.12 would break the 0.15 floor)
		assertTrue(Caps.SCALE.canStep(1.0, -9, Direction.DOWN));
		assertFalse(Caps.SCALE.canStep(1.0, -10, Direction.DOWN));
		// float-based base: 0.1F * (1 + 25 * 0.08) = 0.3000000044703484 is inside 0.305
		assertTrue(Caps.MOVEMENT_SPEED.canStep((double) 0.1F, 24, Direction.UP));
		assertFalse(Caps.MOVEMENT_SPEED.canStep((double) 0.1F, 25, Direction.UP));
	}

	@Test
	void recoveryWhenAnotherModMovedTheBaseOutOfRange() {
		AttributeSpec scale = Caps.SCALE;
		// base 10 is above max 6: moving back (DOWN) is allowed even though the result is still too high, UP is not
		assertTrue(scale.canStep(10.0, 0, Direction.DOWN));
		assertFalse(scale.canStep(10.0, 0, Direction.UP));
		// base 0.1 is below min 0.15: UP is allowed, DOWN is not
		assertTrue(scale.canStep(0.1, 0, Direction.UP));
		assertFalse(scale.canStep(0.1, 0, Direction.DOWN));
		// ADD_VALUE: max_health base 100 (> 60)
		assertTrue(Caps.MAX_HEALTH.canStep(100.0, 0, Direction.DOWN));
		assertFalse(Caps.MAX_HEALTH.canStep(100.0, 0, Direction.UP));
		// minSteps from an out-of-range base walks back into the range and on down to the floor (100 - 49*2 = 2)
		assertEquals(-49, Caps.MAX_HEALTH.minSteps(100.0));
		assertEquals(0, Caps.MAX_HEALTH.maxSteps(100.0));
		// a stored step count that is out of range for the live base can always move back
		assertTrue(Caps.ARMOR.canStep(0.0, 25, Direction.DOWN));
		assertFalse(Caps.ARMOR.canStep(0.0, 25, Direction.UP));
		assertTrue(Caps.ARMOR.canStep(0.0, -3, Direction.UP));
		assertFalse(Caps.ARMOR.canStep(0.0, -3, Direction.DOWN));
	}

	@Test
	void canStepNeverOverflowsSteps() {
		assertFalse(Caps.LUCK.canStep(0.0, Integer.MAX_VALUE, Direction.UP));
		assertFalse(Caps.LUCK.canStep(0.0, Integer.MIN_VALUE, Direction.DOWN));
		assertTrue(Caps.LUCK.canStep(0.0, Integer.MAX_VALUE, Direction.DOWN), "far above max: moving back is allowed");
		assertTrue(Caps.LUCK.canStep(0.0, Integer.MIN_VALUE, Direction.UP), "far below min: moving back is allowed");
	}

	@Test
	void nanBaseNeverSteps() {
		assertFalse(Caps.SCALE.canStep(Double.NaN, 0, Direction.UP));
		assertFalse(Caps.SCALE.canStep(Double.NaN, 0, Direction.DOWN));
		assertEquals(0, Caps.SCALE.minSteps(Double.NaN));
		assertEquals(0, Caps.SCALE.maxSteps(Double.NaN));
	}

	@Test
	void minMaxStepsTerminateOnDegenerateSpecs() {
		// base 0 with ADD_MULTIPLIED_BASE: every step keeps the value at 0, which is in range forever
		AttributeSpec flat = new AttributeSpec("flat", ModifierOp.ADD_MULTIPLIED_BASE, 0.1, -1, 1, 0.0,
				DisplayUnit.PERCENT_OF_BASE, Direction.UP);
		assertTrue(flat.maxSteps(0.0) > 1000);
		assertTrue(flat.minSteps(0.0) < -1000);
		AttributeSpec none = new AttributeSpec("none", ModifierOp.ADD_VALUE, 1.0, 0, 0, 0.0, DisplayUnit.POINTS, Direction.UP);
		assertEquals(0, none.minSteps(0.0));
		assertEquals(0, none.maxSteps(0.0));
	}

	@Test
	void isBuff() {
		assertTrue(Caps.SCALE.isBuff(Direction.UP));
		assertFalse(Caps.SCALE.isBuff(Direction.DOWN));
		assertTrue(Caps.GRAVITY.isBuff(Direction.DOWN));
		assertFalse(Caps.GRAVITY.isBuff(Direction.UP));
	}

	@Test
	void displayPercentOfBase() {
		assertEquals(92.0, Caps.SCALE.displayValue(1.0, -1), D);
		assertEquals(-8.0, Caps.SCALE.displayDelta(-1), D);
		assertEquals(116.0, Caps.SCALE.displayValue(1.0, 2), D);
		assertEquals(116.0, Caps.SCALE.displayValue(2.0, 2), D, "percent of the live base, whatever it is");
		assertEquals(90.0, Caps.GRAVITY.displayValue(0.08, -1), D);
		assertEquals(-10.0, Caps.GRAVITY.displayDelta(-1), D);
		assertEquals(300.0, Caps.MOVEMENT_SPEED.displayValue((double) 0.1F, 25), D);
		// base 0 (another mod zeroed it) must not divide by zero
		assertEquals(92.0, Caps.SCALE.displayValue(0.0, -1), D);
		assertEquals(100.0, Caps.SCALE.displayValue(0.0, 0), D);
	}

	@Test
	void displayPercentPoints() {
		assertEquals(30.0, Caps.KNOCKBACK_RESISTANCE.displayValue(0.0, 3), D);
		assertEquals(30.0, Caps.KNOCKBACK_RESISTANCE.displayDelta(3), D);
		assertEquals(-50.0, Caps.KNOCKBACK_RESISTANCE.displayValue(0.0, -5), D);
		assertEquals(10.0, Caps.WATER_MOVEMENT_EFFICIENCY.displayDelta(1), D);
		assertEquals(100.0, Caps.WATER_MOVEMENT_EFFICIENCY.displayValue(0.0, 10), D);
	}

	@Test
	void displayPoints() {
		assertEquals(22.0, Caps.MAX_HEALTH.displayValue(20.0, 1), D);
		assertEquals(2.0, Caps.MAX_HEALTH.displayDelta(1), D);
		assertEquals(-4.0, Caps.MAX_HEALTH.displayDelta(-2), D);
		assertEquals(0.8, Caps.STEP_HEIGHT.displayValue(0.6, 1), D);
		assertEquals(0.2, Caps.STEP_HEIGHT.displayDelta(1), D);
		assertEquals(-0.5, Caps.ATTACK_DAMAGE.displayDelta(-1), D);
	}

	@Test
	void displayDeltaDoesNotOverflowIntMath() {
		assertEquals(100.0 * Integer.MAX_VALUE * 0.08, Caps.SCALE.displayDelta(Integer.MAX_VALUE), 1e3);
		assertTrue(Caps.SCALE.displayDelta(Integer.MAX_VALUE) > 0);
	}

	@Test
	void translationKey() {
		assertEquals("attribute.name.scale", Caps.SCALE.translationKey());
		assertEquals("attribute.name.max_health", Caps.MAX_HEALTH.translationKey());
		for (AttributeSpec spec : Caps.ALL) assertEquals("attribute.name." + spec.key(), spec.translationKey());
	}

	@Test
	void constructorRejectsNullsOnly() {
		assertThrows(NullPointerException.class,
				() -> new AttributeSpec(null, ModifierOp.ADD_VALUE, 1, 0, 1, 0, DisplayUnit.POINTS, Direction.UP));
		assertThrows(NullPointerException.class,
				() -> new AttributeSpec("k", null, 1, 0, 1, 0, DisplayUnit.POINTS, Direction.UP));
		assertThrows(NullPointerException.class,
				() -> new AttributeSpec("k", ModifierOp.ADD_VALUE, 1, 0, 1, 0, null, Direction.UP));
		assertThrows(NullPointerException.class,
				() -> new AttributeSpec("k", ModifierOp.ADD_VALUE, 1, 0, 1, 0, DisplayUnit.POINTS, null));
		// degenerate values are allowed (tests build them)
		new AttributeSpec("k", ModifierOp.ADD_VALUE, -1, 5, 0, 0, DisplayUnit.POINTS, Direction.DOWN);
	}

	@Test
	void directionAndUnits() {
		assertEquals(1, Direction.UP.sign());
		assertEquals(-1, Direction.DOWN.sign());
		assertEquals(Direction.DOWN, Direction.UP.opposite());
		assertEquals(Direction.UP, Direction.DOWN.opposite());
		assertEquals(0, DisplayUnit.PERCENT_OF_BASE.maxDecimals());
		assertEquals(0, DisplayUnit.PERCENT_POINTS.maxDecimals());
		assertEquals(2, DisplayUnit.POINTS.maxDecimals());
		assertEquals("munchaholic.unit.percent", DisplayUnit.PERCENT_OF_BASE.translationKey());
		assertEquals("munchaholic.unit.percent", DisplayUnit.PERCENT_POINTS.translationKey());
		assertEquals("munchaholic.unit.points", DisplayUnit.POINTS.translationKey());
		assertEquals("%s%%", DisplayUnit.PERCENT_OF_BASE.fallback());
		assertEquals("%s", DisplayUnit.POINTS.fallback());
	}
}
