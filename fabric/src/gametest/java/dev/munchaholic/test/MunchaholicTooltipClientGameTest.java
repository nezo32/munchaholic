package dev.munchaholic.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import dev.munchaholic.BiteHandler;
import dev.munchaholic.client.ClientRecipeBook;
import dev.munchaholic.client.RecipeTooltip;
import dev.munchaholic.core.Caps;
import dev.munchaholic.core.Direction;
import dev.munchaholic.core.Recipe;
import dev.munchaholic.core.Recipes;
import dev.munchaholic.core.RollMode;
import dev.munchaholic.mode.MunchaholicMode;
import dev.munchaholic.net.RecipesPayload;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Client gametest for Recipes-mode tooltips (D15; run with {@code ./gradlew runClientGameTest} under Xvfb).
 * <ol>
 * <li>{@link ClientRecipeBook} alone: apply/clear, unknown attribute keys skipped, {@link RecipeTooltip#isFood}.</li>
 * <li>A world made through the Create World screen with the default buttons starts ON + Random: no recipe lines.</li>
 * <li>{@code /munchaholic mode recipes}: the book becomes active; eating a carrot on the server syncs exactly its recipe,
 *     the carrot tooltip names the attribute, bread shows the undiscovered line.</li>
 * <li>{@code /munchaholic mode random}: the book is inactive again and tooltips have no Munchaholic line.</li>
 * </ol>
 */
public class MunchaholicTooltipClientGameTest implements FabricClientGameTest {
	private static final String UNDISCOVERED = "munchaholic.tooltip.undiscovered";
	private static final String RECIPE_LINE = "munchaholic.tooltip.recipe";

	@Override
	public void runTest(ClientGameTestContext ctx) {
		recipeBookAlone(ctx);
		inWorld(ctx);
		System.out.println("MUNCHAHOLIC_TOOLTIP_CLIENT_TEST_OK");
	}

	private static void recipeBookAlone(ClientGameTestContext ctx) {
		ctx.runOnClient(mc -> {
			ClientRecipeBook.clear();
			check(!ClientRecipeBook.active(), "cleared book is inactive");
			ClientRecipeBook.apply(new RecipesPayload(true, List.of(
					new RecipesPayload.Entry("minecraft:apple", "luck", false),
					new RecipesPayload.Entry("minecraft:bread", "not_an_attribute", true))));
			check(ClientRecipeBook.active(), "active after apply");
			check(ClientRecipeBook.recipe("minecraft:apple").equals(Optional.of(new Recipe(Caps.LUCK, Direction.DOWN))),
					"apple recipe " + ClientRecipeBook.recipe("minecraft:apple"));
			check(ClientRecipeBook.recipe("minecraft:bread").isEmpty(), "unknown attribute key skipped");
			check(ClientRecipeBook.recipe("minecraft:carrot").isEmpty(), "carrot not discovered");

			List<Component> apple = tooltip(new ItemStack(Items.APPLE));
			check(apple.size() == 2 && TestSupport.containsKey(apple.get(1), RECIPE_LINE)
					&& TestSupport.containsKey(apple.get(1), Caps.LUCK.translationKey())
					&& TestSupport.containsKey(apple.get(1), "munchaholic.direction.down"), "apple tooltip " + apple);

			ClientRecipeBook.apply(new RecipesPayload(false, List.of(new RecipesPayload.Entry("minecraft:apple", "luck", false))));
			check(!ClientRecipeBook.active(), "inactive payload");
			check(tooltip(new ItemStack(Items.APPLE)).size() == 1, "no line while inactive");
			ClientRecipeBook.clear();
			check(ClientRecipeBook.recipe("minecraft:apple").isEmpty(), "clear forgets recipes");

			check(RecipeTooltip.isFood(new ItemStack(Items.CARROT)), "carrot is food");
			check(RecipeTooltip.isFood(new ItemStack(Items.CAKE)), "cake is food");
			check(RecipeTooltip.isFood(new ItemStack(Items.SUSPICIOUS_STEW)), "suspicious stew is food");
			check(!RecipeTooltip.isFood(new ItemStack(Items.POTION)), "potion is not food");
			check(!RecipeTooltip.isFood(new ItemStack(Items.MILK_BUCKET)), "milk is not food");
			check(!RecipeTooltip.isFood(new ItemStack(Items.PUFFERFISH_BUCKET)), "pufferfish bucket (FOOD, no CONSUMABLE) is not food");
			check(!RecipeTooltip.isFood(new ItemStack(Items.STONE)), "stone is not food");
		});
	}

	private static void inWorld(ClientGameTestContext ctx) {
		try (TestSingleplayerContext sp = ctx.worldBuilder().create()) {
			ctx.waitFor(mc -> mc.player != null, 20 * 60);
			String initial = sp.getServer().computeOnServer(s -> MunchaholicMode.isEnabled(s) + "/" + MunchaholicMode.rollMode(s)
					+ "/" + MunchaholicMode.keepOnDeath(s));
			if (!("true/" + RollMode.RANDOM + "/true").equals(initial)) {
				throw new AssertionError("world from the Create World screen with default buttons: " + initial);
			}
			ctx.waitTicks(10);
			if (ctx.computeOnClient(mc -> ClientRecipeBook.active())) throw new AssertionError("book active in Random mode");
			expectNoLine(ctx, Items.CARROT, "Random mode");

			sp.getServer().runCommand("munchaholic mode recipes");
			ctx.waitFor(mc -> ClientRecipeBook.active(), 20 * 5);
			expectLine(ctx, Items.BREAD, UNDISCOVERED, "undiscovered bread");

			Recipe carrot = sp.getServer().computeOnServer(s -> Recipes.of(s.overworld().getSeed(), "minecraft:carrot"));
			sp.getServer().runOnServer(s -> BiteHandler.bite(s.getPlayerList().getPlayers().get(0), new ItemStack(Items.CARROT),
					bound -> {
						throw new AssertionError("Recipes mode drew a random number");
					}));
			ctx.waitFor(mc -> ClientRecipeBook.recipe("minecraft:carrot").isPresent(), 20 * 5);
			Recipe synced = ctx.computeOnClient(mc -> ClientRecipeBook.recipe("minecraft:carrot").orElseThrow());
			if (!synced.equals(carrot)) throw new AssertionError("synced carrot recipe " + synced + ", server " + carrot);
			if (ctx.computeOnClient(mc -> ClientRecipeBook.recipe("minecraft:bread").isPresent())) {
				throw new AssertionError("an undiscovered recipe reached the client");
			}
			List<Component> lines = ctx.computeOnClient(mc -> tooltip(new ItemStack(Items.CARROT)));
			String attributeName = ctx.computeOnClient(mc -> I18n.get(carrot.spec().translationKey()));
			if (lines.size() != 2 || !TestSupport.containsKey(lines.get(1), RECIPE_LINE)
					|| !lines.get(1).getString().contains(attributeName)) {
				throw new AssertionError("carrot tooltip " + lines + " does not name " + attributeName);
			}
			expectLine(ctx, Items.BREAD, UNDISCOVERED, "bread still undiscovered");
			ctx.takeScreenshot("tooltip_recipes_world");

			sp.getServer().runCommand("munchaholic mode random");
			ctx.waitFor(mc -> !ClientRecipeBook.active(), 20 * 5);
			expectNoLine(ctx, Items.CARROT, "back in Random mode");
			expectNoLine(ctx, Items.BREAD, "back in Random mode");

			sp.getServer().runCommand("munchaholic mode recipes");
			ctx.waitFor(mc -> ClientRecipeBook.active(), 20 * 5);
			sp.getServer().runCommand("munchaholic off");
			ctx.waitFor(mc -> !ClientRecipeBook.active(), 20 * 5);
			if (ctx.computeOnClient(mc -> ClientRecipeBook.recipe("minecraft:carrot").isEmpty())) {
				throw new AssertionError("discovered carrot recipe lost after /munchaholic off");
			}
		}
		if (ctx.computeOnClient(mc -> ClientRecipeBook.active() || ClientRecipeBook.recipe("minecraft:carrot").isPresent())) {
			throw new AssertionError("recipe book not cleared on disconnect");
		}
	}

	private static List<Component> tooltip(ItemStack stack) {
		List<Component> lines = new ArrayList<>();
		lines.add(stack.getHoverName());
		RecipeTooltip.append(stack, lines);
		return lines;
	}

	private static void expectLine(ClientGameTestContext ctx, Item item, String key, String what) {
		List<Component> lines = ctx.computeOnClient(mc -> tooltip(new ItemStack(item)));
		if (lines.size() != 2 || !TestSupport.containsKey(lines.get(1), key)) {
			throw new AssertionError(what + ": expected a " + key + " line at index 1, got " + lines);
		}
	}

	private static void expectNoLine(ClientGameTestContext ctx, Item item, String what) {
		List<Component> lines = ctx.computeOnClient(mc -> tooltip(new ItemStack(item)));
		if (lines.size() != 1) throw new AssertionError(what + ": unexpected Munchaholic tooltip lines " + lines);
	}

	private static void check(boolean condition, String what) {
		if (!condition) throw new AssertionError(what);
	}
}
