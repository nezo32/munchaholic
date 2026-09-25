package dev.munchaholic.client;

import java.util.List;

import dev.munchaholic.BiteHandler;
import dev.munchaholic.Texts;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Adds "Munchaholic: Attribute ▲" (or the undiscovered line) to food tooltips while Recipes mode is active, right
 * under the item name. The arrow is green for a buff and red for a debuff.
 */
public final class RecipeTooltip {
	private RecipeTooltip() {}

	/** Registers the ItemTooltipCallback. */
	public static void register() {
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> append(stack, lines));
	}

	/** Edible food (FOOD and CONSUMABLE components) or a cake. */
	public static boolean isFood(ItemStack stack) {
		return (stack.has(DataComponents.FOOD) && stack.has(DataComponents.CONSUMABLE)) || stack.is(Items.CAKE);
	}

	/** Inserts the recipe line at index {@code min(1, lines.size())} if Recipes are active and the stack is food. */
	public static void append(ItemStack stack, List<Component> lines) {
		// active() first: it is the cheap check, and the only one that runs for every item outside Recipes mode
		if (!ClientRecipeBook.active() || stack.isEmpty() || !isFood(stack)) return;
		Component line = ClientRecipeBook.recipe(BiteHandler.foodId(stack))
				.<Component>map(r -> Component.translatable("munchaholic.tooltip.recipe",
						Texts.attributeName(r.spec()), Texts.arrow(r.spec(), r.direction())).withStyle(ChatFormatting.GRAY))
				.orElseGet(() -> Component.translatable("munchaholic.tooltip.undiscovered").withStyle(ChatFormatting.DARK_GRAY));
		lines.add(Math.min(1, lines.size()), line);
	}
}
