package dev.munchaholic.player;

import java.util.Map;

import dev.munchaholic.core.AttributeSpec;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Maps a table spec to its vanilla attribute. Kept out of core so core stays free of Minecraft. */
public final class AttributeHolders {
	private static final Map<String, Holder<Attribute>> BY_KEY = Map.ofEntries(
			Map.entry("scale", Attributes.SCALE),
			Map.entry("gravity", Attributes.GRAVITY),
			Map.entry("jump_strength", Attributes.JUMP_STRENGTH),
			Map.entry("step_height", Attributes.STEP_HEIGHT),
			Map.entry("movement_speed", Attributes.MOVEMENT_SPEED),
			Map.entry("sneaking_speed", Attributes.SNEAKING_SPEED),
			Map.entry("attack_damage", Attributes.ATTACK_DAMAGE),
			Map.entry("attack_speed", Attributes.ATTACK_SPEED),
			Map.entry("max_health", Attributes.MAX_HEALTH),
			Map.entry("armor", Attributes.ARMOR),
			Map.entry("knockback_resistance", Attributes.KNOCKBACK_RESISTANCE),
			Map.entry("safe_fall_distance", Attributes.SAFE_FALL_DISTANCE),
			Map.entry("fall_damage_multiplier", Attributes.FALL_DAMAGE_MULTIPLIER),
			Map.entry("block_interaction_range", Attributes.BLOCK_INTERACTION_RANGE),
			Map.entry("entity_interaction_range", Attributes.ENTITY_INTERACTION_RANGE),
			Map.entry("mining_efficiency", Attributes.MINING_EFFICIENCY),
			Map.entry("oxygen_bonus", Attributes.OXYGEN_BONUS),
			Map.entry("water_movement_efficiency", Attributes.WATER_MOVEMENT_EFFICIENCY),
			Map.entry("burning_time", Attributes.BURNING_TIME),
			Map.entry("luck", Attributes.LUCK));

	private AttributeHolders() {}

	/** Throws IllegalArgumentException for a key that is not in the table. */
	public static Holder<Attribute> of(AttributeSpec spec) {
		throw new UnsupportedOperationException("TODO");
	}
}
