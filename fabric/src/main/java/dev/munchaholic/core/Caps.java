package dev.munchaholic.core;

import java.util.List;
import java.util.Optional;

/**
 * The attribute table: the ONLY place limits live (ARCHITECTURE.md §2). {@link #ALL} order is the Random-mode draw
 * order; Recipes don't depend on it.
 */
public final class Caps {
	public static final AttributeSpec SCALE = new AttributeSpec("scale",
			ModifierOp.ADD_MULTIPLIED_BASE, 0.08, 0.15, 6.0, 1.0, DisplayUnit.PERCENT_OF_BASE, Direction.UP);
	public static final AttributeSpec GRAVITY = new AttributeSpec("gravity",
			ModifierOp.ADD_MULTIPLIED_BASE, 0.10, 0.015, 0.245, 0.08, DisplayUnit.PERCENT_OF_BASE, Direction.DOWN);
	public static final AttributeSpec JUMP_STRENGTH = new AttributeSpec("jump_strength",
			ModifierOp.ADD_MULTIPLIED_BASE, 0.10, 0.2, 1.3, (double) 0.42F, DisplayUnit.PERCENT_OF_BASE, Direction.UP);
	public static final AttributeSpec STEP_HEIGHT = new AttributeSpec("step_height",
			ModifierOp.ADD_VALUE, 0.2, 0.1, 2.7, 0.6, DisplayUnit.POINTS, Direction.UP);
	public static final AttributeSpec MOVEMENT_SPEED = new AttributeSpec("movement_speed",
			ModifierOp.ADD_MULTIPLIED_BASE, 0.08, 0.035, 0.305, (double) 0.1F, DisplayUnit.PERCENT_OF_BASE, Direction.UP);
	public static final AttributeSpec SNEAKING_SPEED = new AttributeSpec("sneaking_speed",
			ModifierOp.ADD_MULTIPLIED_BASE, 0.10, 0.14, 1.0, 0.3, DisplayUnit.PERCENT_OF_BASE, Direction.UP);
	public static final AttributeSpec ATTACK_DAMAGE = new AttributeSpec("attack_damage",
			ModifierOp.ADD_VALUE, 0.5, 0.4, 10.1, 1.0, DisplayUnit.POINTS, Direction.UP);
	public static final AttributeSpec ATTACK_SPEED = new AttributeSpec("attack_speed",
			ModifierOp.ADD_MULTIPLIED_BASE, 0.10, 1.5, 12.5, 4.0, DisplayUnit.PERCENT_OF_BASE, Direction.UP);
	public static final AttributeSpec MAX_HEALTH = new AttributeSpec("max_health",
			ModifierOp.ADD_VALUE, 2.0, 2.0, 60.0, 20.0, DisplayUnit.POINTS, Direction.UP);
	public static final AttributeSpec ARMOR = new AttributeSpec("armor",
			ModifierOp.ADD_VALUE, 1.0, 0.0, 20.0, 0.0, DisplayUnit.POINTS, Direction.UP);
	public static final AttributeSpec KNOCKBACK_RESISTANCE = new AttributeSpec("knockback_resistance",
			ModifierOp.ADD_VALUE, 0.1, -0.55, 1.0, 0.0, DisplayUnit.PERCENT_POINTS, Direction.UP);
	public static final AttributeSpec SAFE_FALL_DISTANCE = new AttributeSpec("safe_fall_distance",
			ModifierOp.ADD_VALUE, 1.0, 1.0, 23.0, 3.0, DisplayUnit.POINTS, Direction.UP);
	public static final AttributeSpec FALL_DAMAGE_MULTIPLIER = new AttributeSpec("fall_damage_multiplier",
			ModifierOp.ADD_MULTIPLIED_BASE, 0.10, 0.05, 3.05, 1.0, DisplayUnit.PERCENT_OF_BASE, Direction.DOWN);
	public static final AttributeSpec BLOCK_INTERACTION_RANGE = new AttributeSpec("block_interaction_range",
			ModifierOp.ADD_MULTIPLIED_BASE, 0.10, 1.0, 13.6, 4.5, DisplayUnit.PERCENT_OF_BASE, Direction.UP);
	public static final AttributeSpec ENTITY_INTERACTION_RANGE = new AttributeSpec("entity_interaction_range",
			ModifierOp.ADD_MULTIPLIED_BASE, 0.10, 1.0, 9.1, 3.0, DisplayUnit.PERCENT_OF_BASE, Direction.UP);
	public static final AttributeSpec MINING_EFFICIENCY = new AttributeSpec("mining_efficiency",
			ModifierOp.ADD_VALUE, 2.0, 0.0, 40.0, 0.0, DisplayUnit.POINTS, Direction.UP);
	public static final AttributeSpec OXYGEN_BONUS = new AttributeSpec("oxygen_bonus",
			ModifierOp.ADD_VALUE, 1.0, 0.0, 20.0, 0.0, DisplayUnit.POINTS, Direction.UP);
	public static final AttributeSpec WATER_MOVEMENT_EFFICIENCY = new AttributeSpec("water_movement_efficiency",
			ModifierOp.ADD_VALUE, 0.1, 0.0, 1.0, 0.0, DisplayUnit.PERCENT_POINTS, Direction.UP);
	public static final AttributeSpec BURNING_TIME = new AttributeSpec("burning_time",
			ModifierOp.ADD_MULTIPLIED_BASE, 0.10, 0.05, 3.05, 1.0, DisplayUnit.PERCENT_OF_BASE, Direction.DOWN);
	public static final AttributeSpec LUCK = new AttributeSpec("luck",
			ModifierOp.ADD_VALUE, 1.0, -10.0, 10.0, 0.0, DisplayUnit.POINTS, Direction.UP);

	/** Every spec, in table order. */
	public static final List<AttributeSpec> ALL = List.of(SCALE, GRAVITY, JUMP_STRENGTH, STEP_HEIGHT, MOVEMENT_SPEED,
			SNEAKING_SPEED, ATTACK_DAMAGE, ATTACK_SPEED, MAX_HEALTH, ARMOR, KNOCKBACK_RESISTANCE, SAFE_FALL_DISTANCE,
			FALL_DAMAGE_MULTIPLIER, BLOCK_INTERACTION_RANGE, ENTITY_INTERACTION_RANGE, MINING_EFFICIENCY, OXYGEN_BONUS,
			WATER_MOVEMENT_EFFICIENCY, BURNING_TIME, LUCK);

	private Caps() {}

	/** The spec with this attribute key (e.g. {@code "scale"}); empty for unknown keys. */
	public static Optional<AttributeSpec> byKey(String key) {
		if (key == null) return Optional.empty();
		for (AttributeSpec spec : ALL) {
			if (spec.key().equals(key)) return Optional.of(spec);
		}
		return Optional.empty();
	}
}
