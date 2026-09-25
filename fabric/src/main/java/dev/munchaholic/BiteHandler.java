package dev.munchaholic;

import java.util.concurrent.atomic.AtomicBoolean;

import dev.munchaholic.core.BaseLookup;
import dev.munchaholic.core.Caps;
import dev.munchaholic.core.Direction;
import dev.munchaholic.core.PlayerStacks;
import dev.munchaholic.core.RandomIndex;
import dev.munchaholic.core.Recipes;
import dev.munchaholic.core.RollMode;
import dev.munchaholic.core.RollOutcome;
import dev.munchaholic.core.RollSelector;
import dev.munchaholic.core.RollVeto;
import dev.munchaholic.mode.MunchaholicMode;
import dev.munchaholic.player.PlayerMunch;
import dev.munchaholic.player.RecipeSync;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

/** Glue from the eat hooks (FoodPropertiesMixin, CakeBlockMixin) to the roll logic and the feedback. */
public final class BiteHandler {
	private static final AtomicBoolean LOGGED_FAILURE = new AtomicBoolean();

	private BiteHandler() {}

	/**
	 * Guards, roll, feedback. A failing roll is logged, never thrown into the server tick. Runs synchronously inside
	 * the eat hook, so the food stack has not shrunk yet and its hover name is still the eaten item's.
	 */
	public static void onAte(ServerPlayer player, ItemStack food) {
		try {
			if (!shouldTrigger(player)) return;
			RollOutcome outcome = bite(player, food, player.getRandom()::nextInt);
			Feedback.send(player, food, outcome);
		} catch (VirtualMachineError e) {
			throw e; // out of memory, stack overflow: nothing sensible to do here
		} catch (Throwable e) {
			// never let a roll crash the server tick (the food is already eaten either way). Not only RuntimeException:
			// also a LinkageError from a mismatched MC/Fabric version, or another mod's AssertionError.
			if (LOGGED_FAILURE.compareAndSet(false, true)) {
				Munchaholic.LOGGER.error("Munchaholic failed to roll a bite; further failures are logged at debug level", e);
			} else {
				Munchaholic.LOGGER.debug("Munchaholic failed to roll a bite", e);
			}
		}
	}

	/** Mode enabled, not a FakePlayer, game mode SURVIVAL or ADVENTURE. */
	public static boolean shouldTrigger(ServerPlayer player) {
		if (player instanceof FakePlayer) return false; // machine/automation mods "eating"
		// server-authoritative game mode: gametest mock players hard-wire Player.gameMode() to CREATIVE
		GameType mode = player.gameMode.getGameModeForPlayer();
		if (!mode.isSurvival()) return false;
		return MunchaholicMode.isEnabled(player.level().getServer());
	}

	/** One bite (ARCHITECTURE.md §3.1): roll, store, discover. No guards, no feedback: the gametest seam. */
	public static RollOutcome bite(ServerPlayer player, ItemStack food, RandomIndex random) {
		String foodId = foodId(food);
		MinecraftServer server = player.level().getServer();
		PlayerStacks before = PlayerMunch.stacks(player);
		BaseLookup bases = PlayerMunch.bases(player);
		RollVeto veto = growthVeto(player);
		RollMode rollMode = MunchaholicMode.rollMode(server);
		RollOutcome outcome = rollMode == RollMode.RECIPES
				? RollSelector.fixed(before, bases, Recipes.of(server.overworld().getSeed(), foodId), veto)
				: RollSelector.random(before, bases, random, veto);
		// the bite always counts; steps change only for Applied. Reapplies the modifiers.
		PlayerMunch.setStacks(player, before.after(outcome));
		if (rollMode == RollMode.RECIPES && PlayerMunch.discover(player, foodId)) {
			RecipeSync.send(player);
		}
		return outcome;
	}

	/**
	 * Scale UP is vetoed (treated as capped) when the grown player would not fit where they stand: players are never
	 * pushed out of blocks when they grow, so they would suffocate.
	 */
	public static RollVeto growthVeto(ServerPlayer player) {
		return candidate -> candidate.spec() == Caps.SCALE && candidate.direction() == Direction.UP
				&& !PlayerMunch.fitsWithScaleSteps(player, candidate.newSteps());
	}

	/** The food type key: the item registry id, e.g. {@code "minecraft:carrot"}. */
	public static String foodId(ItemStack food) {
		return BuiltInRegistries.ITEM.getKey(food.getItem()).toString();
	}
}
