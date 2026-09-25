package dev.munchaholic.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** The table in ARCHITECTURE.md §2: 20 specs, derived step ranges, buff directions, vanilla ranges, safety floors. */
class CapsTest {
	private static final double TOLERANCE = 1e-6;

	/** One frozen row of §2: steps range, munch value range and shown range. */
	private record Row(String key, int minSteps, int maxSteps, double minValue, double maxValue, double minShown,
			double maxShown) {}

	private static final List<Row> TABLE = List.of(
			new Row("scale", -10, 62, 0.20, 5.96, 20, 596),
			new Row("gravity", -8, 20, 0.016, 0.24, 20, 300),
			new Row("jump_strength", -5, 20, 0.21, 1.26, 50, 300),
			new Row("step_height", -2, 10, 0.2, 2.6, 0.2, 2.6),
			new Row("movement_speed", -8, 25, 0.036, 0.30, 36, 300),
			new Row("sneaking_speed", -5, 23, 0.15, 0.99, 50, 330),
			new Row("attack_damage", -1, 18, 0.5, 10.0, 0.5, 10),
			new Row("attack_speed", -6, 21, 1.6, 12.4, 40, 310),
			new Row("max_health", -9, 20, 2, 60, 2, 60),
			new Row("armor", 0, 20, 0, 20, 0, 20),
			new Row("knockback_resistance", -5, 10, -0.5, 1.0, -50, 100),
			new Row("safe_fall_distance", -2, 20, 1, 23, 1, 23),
			new Row("fall_damage_multiplier", -9, 20, 0.1, 3.0, 10, 300),
			new Row("block_interaction_range", -7, 20, 1.35, 13.5, 30, 300),
			new Row("entity_interaction_range", -6, 20, 1.2, 9.0, 40, 300),
			new Row("mining_efficiency", 0, 20, 0, 40, 0, 40),
			new Row("oxygen_bonus", 0, 20, 0, 20, 0, 20),
			new Row("water_movement_efficiency", 0, 10, 0, 1.0, 0, 100),
			new Row("burning_time", -9, 20, 0.1, 3.0, 10, 300),
			new Row("luck", -10, 10, -10, 10, -10, 10));

	/** Vanilla RangedAttribute (min, max) per key (mc-hooks §4). */
	private static final Map<String, double[]> VANILLA = Map.ofEntries(
			Map.entry("scale", new double[] {0.0625, 16}),
			Map.entry("gravity", new double[] {-1, 1}),
			Map.entry("jump_strength", new double[] {0, 32}),
			Map.entry("step_height", new double[] {0, 10}),
			Map.entry("movement_speed", new double[] {0, 1024}),
			Map.entry("sneaking_speed", new double[] {0, 1}),
			Map.entry("attack_damage", new double[] {0, 2048}),
			Map.entry("attack_speed", new double[] {0, 1024}),
			Map.entry("max_health", new double[] {1, 1024}),
			Map.entry("armor", new double[] {0, 30}),
			Map.entry("knockback_resistance", new double[] {-2, 1}),
			Map.entry("safe_fall_distance", new double[] {-1024, 1024}),
			Map.entry("fall_damage_multiplier", new double[] {0, 100}),
			Map.entry("block_interaction_range", new double[] {0, 64}),
			Map.entry("entity_interaction_range", new double[] {0, 64}),
			Map.entry("mining_efficiency", new double[] {0, 1024}),
			Map.entry("oxygen_bonus", new double[] {0, 1024}),
			Map.entry("water_movement_efficiency", new double[] {0, 1}),
			Map.entry("burning_time", new double[] {0, 1024}),
			Map.entry("luck", new double[] {-1024, 1024}));

	private static double lowest(AttributeSpec spec) {
		return spec.valueAt(spec.playerBase(), spec.minSteps(spec.playerBase()));
	}

	@Test
	void exactlyTwentySpecsInTableOrderWithUniqueKeys() {
		assertEquals(20, Caps.ALL.size());
		assertEquals(TABLE.stream().map(Row::key).toList(), Caps.ALL.stream().map(AttributeSpec::key).toList());
		Set<String> keys = new HashSet<>();
		for (AttributeSpec spec : Caps.ALL) assertTrue(keys.add(spec.key()), "duplicate " + spec.key());
		assertThrows(UnsupportedOperationException.class, () -> Caps.ALL.add(Caps.SCALE));
	}

	@Test
	void fieldsMatchTheList() {
		assertEquals(List.of(Caps.SCALE, Caps.GRAVITY, Caps.JUMP_STRENGTH, Caps.STEP_HEIGHT, Caps.MOVEMENT_SPEED,
				Caps.SNEAKING_SPEED, Caps.ATTACK_DAMAGE, Caps.ATTACK_SPEED, Caps.MAX_HEALTH, Caps.ARMOR,
				Caps.KNOCKBACK_RESISTANCE, Caps.SAFE_FALL_DISTANCE, Caps.FALL_DAMAGE_MULTIPLIER,
				Caps.BLOCK_INTERACTION_RANGE, Caps.ENTITY_INTERACTION_RANGE, Caps.MINING_EFFICIENCY, Caps.OXYGEN_BONUS,
				Caps.WATER_MOVEMENT_EFFICIENCY, Caps.BURNING_TIME, Caps.LUCK), Caps.ALL);
	}

	@Test
	void basicInvariants() {
		for (AttributeSpec spec : Caps.ALL) {
			assertTrue(spec.step() > 0, spec.key());
			assertTrue(spec.min() < spec.max(), spec.key());
			assertTrue(spec.playerBase() >= spec.min() - AttributeSpec.EPS, spec.key());
			assertTrue(spec.playerBase() <= spec.max() + AttributeSpec.EPS, spec.key());
			assertTrue(spec.inRange(spec.valueAt(spec.playerBase(), 0)), spec.key());
		}
	}

	@Test
	void opsAndUnitsFollowD3() {
		for (AttributeSpec spec : Caps.ALL) {
			if (spec.op() == ModifierOp.ADD_MULTIPLIED_BASE) {
				assertTrue(spec.playerBase() != 0, spec.key() + ": percent of a zero base is always zero");
				assertEquals(DisplayUnit.PERCENT_OF_BASE, spec.unit(), spec.key());
			} else {
				assertTrue(spec.unit() != DisplayUnit.PERCENT_OF_BASE, spec.key());
			}
		}
	}

	@Test
	void floatBasesAreExactlyTheVanillaFloats() {
		assertEquals((double) 0.42F, Caps.JUMP_STRENGTH.playerBase());
		assertEquals((double) 0.1F, Caps.MOVEMENT_SPEED.playerBase());
	}

	@Test
	void derivedRangesAreFrozen() {
		for (Row row : TABLE) {
			AttributeSpec spec = Caps.byKey(row.key()).orElseThrow();
			double base = spec.playerBase();
			int lo = spec.minSteps(base);
			int hi = spec.maxSteps(base);
			assertEquals(row.minSteps(), lo, row.key() + " min steps");
			assertEquals(row.maxSteps(), hi, row.key() + " max steps");
			assertEquals(row.minValue(), spec.valueAt(base, lo), TOLERANCE, row.key() + " min value");
			assertEquals(row.maxValue(), spec.valueAt(base, hi), TOLERANCE, row.key() + " max value");
			assertEquals(row.minShown(), spec.displayValue(base, lo), TOLERANCE, row.key() + " min shown");
			assertEquals(row.maxShown(), spec.displayValue(base, hi), TOLERANCE, row.key() + " max shown");
			// every step in between is legal, and the ends are hard stops
			for (int s = lo; s <= hi; s++) assertTrue(spec.inRange(spec.valueAt(base, s)), row.key() + " at " + s);
			assertFalse(spec.canStep(base, lo, Direction.DOWN), row.key());
			assertFalse(spec.canStep(base, hi, Direction.UP), row.key());
		}
	}

	@Test
	void buffIsDownOnlyForGravityFallDamageAndBurning() {
		Set<String> down = Set.of("gravity", "fall_damage_multiplier", "burning_time");
		for (AttributeSpec spec : Caps.ALL) {
			assertEquals(down.contains(spec.key()) ? Direction.DOWN : Direction.UP, spec.buff(), spec.key());
		}
	}

	@Test
	void capsLieInsideVanillaRanges() {
		assertEquals(Caps.ALL.size(), VANILLA.size());
		for (AttributeSpec spec : Caps.ALL) {
			double[] range = VANILLA.get(spec.key());
			assertTrue(range != null, spec.key());
			assertTrue(spec.min() >= range[0], spec.key() + " min " + spec.min() + " < vanilla " + range[0]);
			assertTrue(spec.max() <= range[1], spec.key() + " max " + spec.max() + " > vanilla " + range[1]);
			assertTrue(spec.playerBase() >= range[0] && spec.playerBase() <= range[1], spec.key());
		}
	}

	@Test
	void safetyFloors() {
		assertTrue(Caps.SCALE.min() >= 0.15);
		assertTrue(lowest(Caps.SCALE) >= 0.15);
		assertTrue(Caps.MAX_HEALTH.min() >= 2.0);
		assertTrue(lowest(Caps.MAX_HEALTH) >= 2.0 - AttributeSpec.EPS);
		assertTrue(Caps.GRAVITY.min() > 0);
		assertTrue(lowest(Caps.GRAVITY) > 0);
		// speeds stay well above 0 (at least a third of normal)
		for (AttributeSpec speed : List.of(Caps.MOVEMENT_SPEED, Caps.SNEAKING_SPEED, Caps.ATTACK_SPEED)) {
			assertTrue(speed.min() > 0, speed.key());
			assertTrue(lowest(speed) >= speed.playerBase() / 3, speed.key() + " lowest " + lowest(speed));
		}
		for (AttributeSpec range : List.of(Caps.BLOCK_INTERACTION_RANGE, Caps.ENTITY_INTERACTION_RANGE)) {
			assertTrue(range.min() >= 1.0, range.key());
			assertTrue(lowest(range) >= 1.0, range.key());
		}
		assertTrue(lowest(Caps.JUMP_STRENGTH) > 0);
		assertTrue(lowest(Caps.FALL_DAMAGE_MULTIPLIER) > 0);
		assertTrue(lowest(Caps.ATTACK_DAMAGE) > 0);
	}

	@Test
	void fromZeroAtLeastOneDirectionIsLegal() {
		for (AttributeSpec spec : Caps.ALL) {
			double base = spec.playerBase();
			assertTrue(spec.canStep(base, 0, Direction.UP) || spec.canStep(base, 0, Direction.DOWN), spec.key());
			assertTrue(spec.canStep(base, 0, Direction.UP), spec.key() + ": every attribute can grow from 0");
		}
	}

	@Test
	void byKeyRoundTripsAndUnknownIsEmpty() {
		for (AttributeSpec spec : Caps.ALL) assertSame(spec, Caps.byKey(spec.key()).orElseThrow());
		assertEquals(Optional.empty(), Caps.byKey("nope"));
		assertEquals(Optional.empty(), Caps.byKey(""));
		assertEquals(Optional.empty(), Caps.byKey("Scale"));
		assertEquals(Optional.empty(), Caps.byKey("minecraft:scale"));
		assertEquals(Optional.empty(), Caps.byKey(null));
	}
}
