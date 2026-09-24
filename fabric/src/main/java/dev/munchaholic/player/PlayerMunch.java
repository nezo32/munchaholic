package dev.munchaholic.player;

import dev.munchaholic.Munchaholic;
import dev.munchaholic.core.AttributeSpec;
import dev.munchaholic.core.BaseLookup;
import dev.munchaholic.core.Caps;
import dev.munchaholic.core.Discoveries;
import dev.munchaholic.core.PlayerStacks;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * A player's Munchaholic state and its attribute modifiers. The integer steps in {@link MunchAttachments#STACKS}
 * are the source of truth; the single {@code munchaholic:bites} modifier per attribute is always derived from them.
 */
public final class PlayerMunch {
	/** The id of our modifier on every attribute instance (ids are scoped per instance). */
	public static final Identifier MODIFIER_ID = Munchaholic.id("bites");

	private PlayerMunch() {}

	public static PlayerStacks stacks(ServerPlayer player) {
		return player.getAttachedOrElse(MunchAttachments.STACKS, PlayerStacks.EMPTY);
	}

	public static Discoveries discoveries(ServerPlayer player) {
		return player.getAttachedOrElse(MunchAttachments.DISCOVERIES, Discoveries.EMPTY);
	}

	/**
	 * Stores, then {@link #applyModifiers}; {@code refreshDimensions()} if the scale steps changed. A lower max health
	 * clamps the current health at once (vanilla would only do it on the next tick).
	 */
	public static void setStacks(ServerPlayer player, PlayerStacks stacks) {
		int oldScale = stacks(player).steps(Caps.SCALE);
		player.setAttached(MunchAttachments.STACKS, stacks);
		applyModifiers(player);
		if (stacks.steps(Caps.SCALE) != oldScale) {
			player.refreshDimensions();
		}
		if (player.getHealth() > player.getMaxHealth()) {
			player.setHealth(player.getMaxHealth());
		}
	}

	/** Adds foodId; true if newly discovered. Does NOT send the sync (the caller decides). */
	public static boolean discover(ServerPlayer player, String foodId) {
		Discoveries known = discoveries(player);
		if (known.has(foodId)) return false;
		player.setAttached(MunchAttachments.DISCOVERIES, known.with(foodId));
		return true;
	}

	/**
	 * For every spec in Caps.ALL: steps == 0 removes our modifier, anything else adds or replaces it with
	 * {@code spec.modifierAmount(steps)}. Unknown keys in the stacks are ignored. A modifier that is already exactly
	 * right is left alone, so a join or respawn doesn't mark every attribute dirty.
	 */
	public static void applyModifiers(ServerPlayer player) {
		PlayerStacks stacks = stacks(player);
		for (AttributeSpec spec : Caps.ALL) {
			AttributeInstance instance = player.getAttribute(AttributeHolders.of(spec));
			if (instance == null) continue;
			int steps = stacks.steps(spec);
			if (steps == 0) {
				instance.removeModifier(MODIFIER_ID);
				continue;
			}
			AttributeModifier wanted = new AttributeModifier(MODIFIER_ID, spec.modifierAmount(steps), op(spec));
			// removed by /attribute or another mod, stale after a rebalance, or transient: (re)add it as permanent
			if (!wanted.equals(instance.getModifier(MODIFIER_ID)) || !instance.getPermanentModifiers().contains(wanted)) {
				instance.addOrReplacePermanentModifier(wanted);
			}
		}
	}

	public static AttributeModifier.Operation op(AttributeSpec spec) {
		return switch (spec.op()) {
			case ADD_VALUE -> AttributeModifier.Operation.ADD_VALUE;
			case ADD_MULTIPLIED_BASE -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
		};
	}

	/** The live base value; a missing attribute instance -> {@code spec.playerBase()}. */
	public static double base(ServerPlayer player, AttributeSpec spec) {
		AttributeInstance instance = player.getAttribute(AttributeHolders.of(spec));
		return instance == null ? spec.playerBase() : instance.getBaseValue();
	}

	/** {@code spec -> base(player, spec)}. */
	public static BaseLookup bases(ServerPlayer player) {
		return spec -> base(player, spec);
	}

	/** Removes every change and resets the bite counter. Discoveries are kept. */
	public static void reset(ServerPlayer player) {
		setStacks(player, PlayerStacks.EMPTY);
	}

	/** Clears the discoveries and syncs the (now empty) recipe list to the client. */
	public static void forgetRecipes(ServerPlayer player) {
		player.setAttached(MunchAttachments.DISCOVERIES, Discoveries.EMPTY);
		RecipeSync.send(player);
	}
}
