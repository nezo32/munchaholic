package dev.munchaholic.test;

import static dev.munchaholic.test.TestSupport.bites;
import static dev.munchaholic.test.TestSupport.defaults;
import static dev.munchaholic.test.TestSupport.eat;
import static dev.munchaholic.test.TestSupport.eatCake;
import static dev.munchaholic.test.TestSupport.key;
import static dev.munchaholic.test.TestSupport.mockPlayer;
import static dev.munchaholic.test.TestSupport.noRandom;
import static dev.munchaholic.test.TestSupport.server;
import static dev.munchaholic.test.TestSupport.steps;
import static dev.munchaholic.test.TestSupport.survivalPlayer;

import java.util.List;
import java.util.Map;

import dev.munchaholic.BiteHandler;
import dev.munchaholic.core.AttributeSpec;
import dev.munchaholic.core.Caps;
import dev.munchaholic.core.Direction;
import dev.munchaholic.core.PlayerStacks;
import dev.munchaholic.core.Recipe;
import dev.munchaholic.core.Recipes;
import dev.munchaholic.core.RollMode;
import dev.munchaholic.core.RollOutcome;
import dev.munchaholic.mode.MunchaholicMode;
import dev.munchaholic.net.RecipesPayload;
import dev.munchaholic.player.PlayerMunch;
import dev.munchaholic.player.RecipeSync;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * Recipes mode (D7/D8/D15): each food has a fixed effect per world seed, eating discovers it, a capped recipe does
 * nothing but still counts, and only discovered recipes are synced.
 *
 * <p>Every test switches to Recipes and restores the defaults in {@code finally}, synchronously (shared server).
 */
public class MunchRecipesGameTests {
	private static void recipes(GameTestHelper h) {
		defaults(h);
		MunchaholicMode.setRollMode(server(h), RollMode.RECIPES);
	}

	private static long seed(GameTestHelper h) {
		return server(h).overworld().getSeed();
	}

	private static Recipe recipe(GameTestHelper h, String food) {
		return Recipes.of(seed(h), food);
	}

	@GameTest
	public void recipeMatchesSeedMapping(GameTestHelper h) {
		recipes(h);
		try {
			ServerPlayer p = survivalPlayer(h);
			Recipe r = recipe(h, "minecraft:carrot");
			RollOutcome o = BiteHandler.bite(p, new ItemStack(Items.CARROT), noRandom());
			h.assertTrue(o instanceof RollOutcome.Applied a && a.spec() == r.spec() && a.direction() == r.direction(),
					"outcome " + o + ", recipe " + r);
			h.assertValueEqual(steps(p, r.spec()), r.direction().sign(), "steps of " + r.spec().key());
			h.assertTrue(PlayerMunch.discoveries(p).has("minecraft:carrot"), "carrot discovered");
			// the real eating path gives the same effect
			ServerPlayer q = survivalPlayer(h);
			eat(h, q, Items.CARROT);
			h.assertValueEqual(steps(q, r.spec()), r.direction().sign(), "real eat: steps of " + r.spec().key());
			h.assertValueEqual(bites(q), 1, "real eat: bites");
			h.assertTrue(PlayerMunch.discoveries(q).has("minecraft:carrot"), "real eat: carrot discovered");
		} finally {
			defaults(h);
		}
		h.succeed();
	}

	/** Deterministic: the same food gives the same change every time, for every player. */
	@GameTest
	public void sameFoodSameEffectTwice(GameTestHelper h) {
		recipes(h);
		try {
			ServerPlayer p = survivalPlayer(h);
			for (var item : List.of(Items.BREAD, Items.GOLDEN_APPLE, Items.COOKED_BEEF)) {
				String id = BiteHandler.foodId(new ItemStack(item));
				Recipe r = recipe(h, id);
				int before = steps(p, r.spec());
				RollOutcome first = BiteHandler.bite(p, new ItemStack(item), noRandom());
				RollOutcome second = BiteHandler.bite(p, new ItemStack(item), noRandom());
				for (RollOutcome o : List.of(first, second)) {
					AttributeSpec spec = o instanceof RollOutcome.Applied a ? a.spec() : ((RollOutcome.Capped) o).spec();
					Direction dir = o instanceof RollOutcome.Applied a ? a.direction() : ((RollOutcome.Capped) o).direction();
					h.assertValueEqual(spec, r.spec(), id + " attribute");
					h.assertValueEqual(dir, r.direction(), id + " direction");
				}
				if (first instanceof RollOutcome.Applied && second instanceof RollOutcome.Applied) {
					h.assertValueEqual(steps(p, r.spec()), before + 2 * r.direction().sign(), id + " stacked twice");
				}
			}
			h.assertValueEqual(bites(p), 6, "bites");
			h.assertValueEqual(PlayerMunch.discoveries(p).size(), 3, "3 foods discovered once each");
		} finally {
			defaults(h);
		}
		h.succeed();
	}

	/** A cake slice (plain or candle) is the food type minecraft:cake. */
	@GameTest
	public void cakeIsAFoodType(GameTestHelper h) {
		recipes(h);
		try {
			Recipe r = recipe(h, "minecraft:cake");
			ServerPlayer p = survivalPlayer(h);
			h.assertTrue(eatCake(h, p, Blocks.CAKE), "cake slice eaten");
			h.assertValueEqual(steps(p, r.spec()), r.direction().sign(), "cake recipe applied to " + r.spec().key());
			h.assertTrue(PlayerMunch.discoveries(p).has("minecraft:cake"), "cake discovered");
			h.assertValueEqual(PlayerMunch.discoveries(p).size(), 1, "only cake discovered");
			ServerPlayer q = survivalPlayer(h);
			h.assertTrue(eatCake(h, q, Blocks.CANDLE_CAKE), "candle cake slice eaten");
			h.assertValueEqual(steps(q, r.spec()), r.direction().sign(), "candle cake = cake recipe");
			h.assertTrue(PlayerMunch.discoveries(q).has("minecraft:cake"), "candle cake discovers minecraft:cake");
		} finally {
			defaults(h);
		}
		h.succeed();
	}

	/** At the cap nothing changes, the bite still counts, the food is discovered, and the player is told (D7). */
	@GameTest
	public void cappedRecipeDoesNothingButCounts(GameTestHelper h) {
		recipes(h);
		try {
			TestSupport.Mock mock = mockPlayer(h);
			ServerPlayer p = mock.player();
			Recipe r = recipe(h, "minecraft:carrot");
			double base = PlayerMunch.base(p, r.spec());
			int limit = r.direction() == Direction.UP ? r.spec().maxSteps(base) : r.spec().minSteps(base);
			PlayerMunch.setStacks(p, new PlayerStacks(Map.of(r.spec().key(), limit), 4));
			PlayerStacks before = PlayerMunch.stacks(p);
			if (limit == 0) {
				// e.g. armor DOWN from 0: already at the limit with no stacks at all
				h.assertTrue(before.isEmpty(), "limit 0 means empty stacks");
			}
			RollOutcome o = BiteHandler.bite(p, new ItemStack(Items.CARROT), noRandom());
			h.assertTrue(o instanceof RollOutcome.Capped c && c.spec() == r.spec() && c.direction() == r.direction()
					&& c.steps() == limit, "outcome " + o);
			h.assertValueEqual(steps(p, r.spec()), limit, "steps unchanged");
			h.assertValueEqual(bites(p), 5, "bite counted");
			h.assertTrue(PlayerMunch.discoveries(p).has("minecraft:carrot"), "discovered even when capped");

			// the real eating path shows the capped message (vanilla client: overlay)
			mock.drain();
			eat(h, p, Items.CARROT);
			List<Object> out = mock.drain();
			List<ClientboundSystemChatPacket> overlays = TestSupport.Mock.overlays(out);
			h.assertValueEqual(overlays.size(), 1, "overlay packets; outbound=" + out);
			h.assertValueEqual(key(overlays.get(0).content()), "munchaholic.message.capped", "capped message key");
			h.assertValueEqual(steps(p, r.spec()), limit, "steps unchanged after the real bite");
			h.assertValueEqual(bites(p), 6, "real bite counted");
		} finally {
			defaults(h);
		}
		h.succeed();
	}

	@GameTest
	public void randomModeDoesNotDiscover(GameTestHelper h) {
		defaults(h);
		ServerPlayer p = survivalPlayer(h);
		eat(h, p, Items.CARROT);
		BiteHandler.bite(p, new ItemStack(Items.BREAD), TestSupport.script(0, 0));
		h.assertValueEqual(PlayerMunch.discoveries(p).size(), 0, "discoveries in Random mode");
		h.assertValueEqual(bites(p), 2, "bites");
		h.succeed();
	}

	/** Only discovered foods go over the wire, each with its fixed attribute and direction, sorted by food id. */
	@GameTest
	public void syncPayloadOnlyDiscovered(GameTestHelper h) {
		recipes(h);
		try {
			ServerPlayer p = survivalPlayer(h);
			RecipesPayload empty = RecipeSync.payloadFor(p);
			h.assertTrue(empty.active(), "active in Recipes mode");
			h.assertTrue(empty.entries().isEmpty(), "nothing discovered yet: " + empty.entries());

			eat(h, p, Items.CARROT);
			eat(h, p, Items.APPLE);
			eat(h, p, Items.CARROT);
			RecipesPayload payload = RecipeSync.payloadFor(p);
			h.assertTrue(payload.active(), "active");
			List<RecipesPayload.Entry> expected = List.of(entry(h, "minecraft:apple"), entry(h, "minecraft:carrot"));
			h.assertValueEqual(payload.entries(), expected, "entries");
		} finally {
			defaults(h);
		}
		h.succeed();
	}

	private static RecipesPayload.Entry entry(GameTestHelper h, String food) {
		Recipe r = recipe(h, food);
		return new RecipesPayload.Entry(food, r.spec().key(), r.direction() == Direction.UP);
	}

	/** active = enabled && Recipes: Random mode or mode OFF deactivates the tooltips (entries may still be listed). */
	@GameTest
	public void syncPayloadInactiveInRandomMode(GameTestHelper h) {
		recipes(h);
		try {
			ServerPlayer p = survivalPlayer(h);
			eat(h, p, Items.CARROT);
			h.assertTrue(MunchaholicMode.recipesActive(server(h)), "recipesActive in Recipes mode");
			MunchaholicMode.setRollMode(server(h), RollMode.RANDOM);
			h.assertTrue(!RecipeSync.payloadFor(p).active(), "inactive in Random mode");
			h.assertTrue(!MunchaholicMode.recipesActive(server(h)), "recipesActive in Random mode");
			MunchaholicMode.setRollMode(server(h), RollMode.RECIPES);
			MunchaholicMode.setEnabled(server(h), false);
			h.assertTrue(!RecipeSync.payloadFor(p).active(), "inactive with the mode OFF");
			h.assertTrue(!MunchaholicMode.recipesActive(server(h)), "recipesActive with the mode OFF");
			h.assertValueEqual(PlayerMunch.discoveries(p).size(), 1, "discovery kept across mode changes");
		} finally {
			defaults(h);
		}
		h.succeed();
	}

	@GameTest
	public void recipesPayloadCodecRoundTrip(GameTestHelper h) {
		RecipesPayload payload = new RecipesPayload(true, List.of(
				new RecipesPayload.Entry("minecraft:apple", "luck", false),
				new RecipesPayload.Entry("minecraft:carrot", "scale", true)));
		RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), h.getLevel().registryAccess());
		try {
			RecipesPayload.CODEC.encode(buf, payload);
			RecipesPayload decoded = RecipesPayload.CODEC.decode(buf);
			h.assertValueEqual(decoded, payload, "decoded payload");
			h.assertValueEqual(buf.readableBytes(), 0, "bytes left unread");
		} finally {
			buf.release();
		}
		h.assertValueEqual(RecipesPayload.TYPE.id().toString(), "munchaholic:recipes", "channel id");
		h.succeed();
	}

	/** A vanilla client (no munchaholic:recipes channel) gets no RecipesPayload on discovery, and nothing throws. */
	@GameTest
	public void discoverySyncSkippedForVanillaClient(GameTestHelper h) {
		recipes(h);
		try {
			TestSupport.Mock mock = mockPlayer(h);
			h.assertTrue(!ServerPlayNetworking.canSend(mock.player(), RecipesPayload.TYPE), "mock player has no recipes channel");
			eat(h, mock.player(), Items.CARROT);
			RecipeSync.send(mock.player());
			RecipeSync.sendAll(server(h));
			List<Object> out = mock.drain();
			h.assertTrue(TestSupport.Mock.payloads(out, RecipesPayload.class).isEmpty(), "RecipesPayload sent; outbound=" + out);
			h.assertTrue(PlayerMunch.discoveries(mock.player()).has("minecraft:carrot"), "discovered");
		} finally {
			defaults(h);
		}
		h.succeed();
	}

	/** Every vanilla food maps to a real attribute in this world (the table is complete, the mapping is total). */
	@GameTest
	public void everyFoodHasARecipe(GameTestHelper h) {
		long seed = seed(h);
		for (var item : BuiltInRegistries.ITEM) {
			ItemStack s = new ItemStack(item);
			if (!s.has(DataComponents.FOOD)) continue;
			String id = BiteHandler.foodId(s);
			Recipe r = Recipes.of(seed, id);
			h.assertTrue(Caps.ALL.contains(r.spec()), id + " -> unknown spec " + r.spec());
			h.assertValueEqual(Recipes.of(seed, id), r, id + " deterministic");
		}
		h.succeed();
	}
}
