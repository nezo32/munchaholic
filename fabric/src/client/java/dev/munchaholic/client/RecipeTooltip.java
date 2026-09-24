package dev.munchaholic.client;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Adds "Munchaholic: Attribute ▲" (or the undiscovered line) to food tooltips while Recipes mode is active. */
public final class RecipeTooltip {
	private RecipeTooltip() {}

	/** Registers the ItemTooltipCallback. */
	public static void register() {
		// TODO (Dev D): Phase-0 registers nothing, so no skeleton body runs on hover; register ItemTooltipCallback.
	}

	/** Edible food (FOOD and CONSUMABLE components) or a cake. */
	public static boolean isFood(ItemStack stack) {
		throw new UnsupportedOperationException("TODO");
	}

	/** Inserts the recipe line at index {@code min(1, lines.size())} if Recipes are active and the stack is food. */
	public static void append(ItemStack stack, List<Component> lines) {
		throw new UnsupportedOperationException("TODO");
	}
}
