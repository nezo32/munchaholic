package dev.munchaholic.player;

import dev.munchaholic.Munchaholic;
import dev.munchaholic.core.AttributeSpec;
import dev.munchaholic.core.BaseLookup;
import dev.munchaholic.core.Discoveries;
import dev.munchaholic.core.PlayerStacks;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
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
		throw new UnsupportedOperationException("TODO");
	}

	public static Discoveries discoveries(ServerPlayer player) {
		throw new UnsupportedOperationException("TODO");
	}

	/** Stores, then {@link #applyModifiers}; {@code refreshDimensions()} if the scale steps changed. */
	public static void setStacks(ServerPlayer player, PlayerStacks stacks) {
		throw new UnsupportedOperationException("TODO");
	}

	/** Adds foodId; true if newly discovered. Does NOT send the sync (the caller decides). */
	public static boolean discover(ServerPlayer player, String foodId) {
		throw new UnsupportedOperationException("TODO");
	}

	/**
	 * For every spec in Caps.ALL: steps == 0 removes our modifier, anything else adds or replaces it with
	 * {@code spec.modifierAmount(steps)}. Unknown keys in the stacks are ignored.
	 */
	public static void applyModifiers(ServerPlayer player) {
		throw new UnsupportedOperationException("TODO");
	}

	public static AttributeModifier.Operation op(AttributeSpec spec) {
		throw new UnsupportedOperationException("TODO");
	}

	/** The live base value; a missing attribute instance -> {@code spec.playerBase()}. */
	public static double base(ServerPlayer player, AttributeSpec spec) {
		throw new UnsupportedOperationException("TODO");
	}

	/** {@code spec -> base(player, spec)}. */
	public static BaseLookup bases(ServerPlayer player) {
		throw new UnsupportedOperationException("TODO");
	}

	/** Removes every change and resets the bite counter. Discoveries are kept. */
	public static void reset(ServerPlayer player) {
		throw new UnsupportedOperationException("TODO");
	}

	/** Clears the discoveries and syncs the (now empty) recipe list to the client. */
	public static void forgetRecipes(ServerPlayer player) {
		throw new UnsupportedOperationException("TODO");
	}
}
