package dev.munchaholic;

import dev.munchaholic.core.RandomIndex;
import dev.munchaholic.core.RollOutcome;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Glue from the eat hooks (FoodPropertiesMixin, CakeBlockMixin) to the roll logic and the feedback. */
public final class BiteHandler {
	private BiteHandler() {}

	/** Guards, roll, feedback. A failing roll is logged, never thrown into the server tick. */
	public static void onAte(ServerPlayer player, ItemStack food) {
		throw new UnsupportedOperationException("TODO");
	}

	/** Mode enabled, not a FakePlayer, game mode SURVIVAL or ADVENTURE. */
	public static boolean shouldTrigger(ServerPlayer player) {
		throw new UnsupportedOperationException("TODO");
	}

	/** One bite (ARCHITECTURE.md §3.1): roll, store, discover. No guards, no feedback: the gametest seam. */
	public static RollOutcome bite(ServerPlayer player, ItemStack food, RandomIndex random) {
		throw new UnsupportedOperationException("TODO");
	}

	/** The food type key: the item registry id, e.g. {@code "minecraft:carrot"}. */
	public static String foodId(ItemStack food) {
		throw new UnsupportedOperationException("TODO");
	}
}
