package dev.munchaholic.test;

import static dev.munchaholic.test.TestSupport.bites;
import static dev.munchaholic.test.TestSupport.cakeBites;
import static dev.munchaholic.test.TestSupport.defaults;
import static dev.munchaholic.test.TestSupport.eat;
import static dev.munchaholic.test.TestSupport.eatCake;
import static dev.munchaholic.test.TestSupport.eatStack;
import static dev.munchaholic.test.TestSupport.modifier;
import static dev.munchaholic.test.TestSupport.server;
import static dev.munchaholic.test.TestSupport.steps;
import static dev.munchaholic.test.TestSupport.survivalPlayer;
import static dev.munchaholic.test.TestSupport.totalSteps;
import static dev.munchaholic.test.TestSupport.useCake;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import dev.munchaholic.BiteHandler;
import dev.munchaholic.core.AttributeSpec;
import dev.munchaholic.core.Caps;
import dev.munchaholic.core.PlayerStacks;
import dev.munchaholic.mode.MunchaholicMode;
import dev.munchaholic.player.MunchAttachments;
import dev.munchaholic.player.PlayerMunch;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/**
 * Which consumptions trigger a roll (every food, cake slices) and which don't (drinks, creative/spectator, mode OFF,
 * non-players, fake players, a full player at a cake). Each triggering bite rolls exactly once.
 */
public class MunchEatingGameTests {
	/** The 40 vanilla foods (mc-hooks §1): identical in 26.2 and 26.3. */
	private static final List<Item> VANILLA_FOODS = List.of(Items.APPLE, Items.BAKED_POTATO, Items.BEEF, Items.BEETROOT,
			Items.BEETROOT_SOUP, Items.BREAD, Items.CARROT, Items.CHICKEN, Items.CHORUS_FRUIT, Items.COD, Items.COOKED_BEEF,
			Items.COOKED_CHICKEN, Items.COOKED_COD, Items.COOKED_MUTTON, Items.COOKED_PORKCHOP, Items.COOKED_RABBIT,
			Items.COOKED_SALMON, Items.COOKIE, Items.DRIED_KELP, Items.ENCHANTED_GOLDEN_APPLE, Items.GLOW_BERRIES,
			Items.GOLDEN_APPLE, Items.GOLDEN_CARROT, Items.HONEY_BOTTLE, Items.MELON_SLICE, Items.MUSHROOM_STEW, Items.MUTTON,
			Items.POISONOUS_POTATO, Items.PORKCHOP, Items.POTATO, Items.PUFFERFISH, Items.PUMPKIN_PIE, Items.RABBIT,
			Items.RABBIT_STEW, Items.ROTTEN_FLESH, Items.SALMON, Items.SPIDER_EYE, Items.SUSPICIOUS_STEW,
			Items.SWEET_BERRIES, Items.TROPICAL_FISH);

	/** Eats {@code item} as a fresh survival player and asserts exactly one roll happened. */
	private static ServerPlayer assertOneRoll(GameTestHelper h, Item item) {
		defaults(h);
		ServerPlayer p = survivalPlayer(h);
		eat(h, p, item);
		assertRolledOnce(h, p, BuiltInRegistries.ITEM.getKey(item).toString());
		return p;
	}

	/** bites == 1 and exactly one attribute moved by exactly one step, with our modifier on it. */
	private static void assertRolledOnce(GameTestHelper h, ServerPlayer p, String what) {
		h.assertValueEqual(bites(p), 1, what + ": bites");
		h.assertValueEqual(totalSteps(p), 1, what + ": total |steps| (exactly one attribute, one step)");
		AttributeSpec changed = Caps.ALL.stream().filter(s -> steps(p, s) != 0).findFirst().orElseThrow();
		h.assertTrue(modifier(p, changed) != null, what + ": munchaholic:bites modifier on " + changed.key());
	}

	private static void assertNoRoll(GameTestHelper h, ServerPlayer p, String what) {
		h.assertValueEqual(PlayerMunch.stacks(p), PlayerStacks.EMPTY, what + ": stacks");
		for (AttributeSpec spec : Caps.ALL) {
			h.assertTrue(modifier(p, spec) == null, what + ": unexpected modifier on " + spec.key());
		}
	}

	@GameTest
	public void breadRolls(GameTestHelper h) {
		ServerPlayer p = assertOneRoll(h, Items.BREAD);
		h.assertTrue(PlayerMunch.discoveries(p).size() == 0, "Random mode discovers nothing");
		h.succeed();
	}

	@GameTest
	public void cookedBeefRolls(GameTestHelper h) {
		assertOneRoll(h, Items.COOKED_BEEF);
		h.succeed();
	}

	/** Every vanilla food (and every registry item that is edible, so datapack/modded foods are covered) rolls once. */
	@GameTest
	public void everyVanillaFoodRolls(GameTestHelper h) {
		defaults(h);
		Set<Item> edible = new LinkedHashSet<>();
		for (Item item : BuiltInRegistries.ITEM) {
			ItemStack s = new ItemStack(item);
			if (s.has(DataComponents.FOOD) && s.has(DataComponents.CONSUMABLE)) edible.add(item);
		}
		Set<String> missing = new TreeSet<>();
		for (Item food : VANILLA_FOODS) {
			if (!edible.contains(food)) missing.add(BuiltInRegistries.ITEM.getKey(food).toString());
		}
		h.assertTrue(missing.isEmpty(), "listed foods without FOOD+CONSUMABLE: " + missing);
		h.assertTrue(edible.size() >= VANILLA_FOODS.size(), "edible items in the registry: " + edible.size());

		List<String> failures = new ArrayList<>();
		for (Item food : edible) {
			String id = BuiltInRegistries.ITEM.getKey(food).toString();
			ServerPlayer p = survivalPlayer(h);
			eat(h, p, food);
			if (bites(p) != 1 || totalSteps(p) != 1) failures.add(id + " (bites " + bites(p) + ", |steps| " + totalSteps(p) + ")");
		}
		h.assertTrue(failures.isEmpty(), "foods that did not roll exactly once: " + failures);
		h.succeed();
	}

	@GameTest
	public void suspiciousStewRolls(GameTestHelper h) {
		ServerPlayer p = assertOneRoll(h, Items.SUSPICIOUS_STEW);
		h.assertTrue(p.getMainHandItem().is(Items.BOWL), "stew leaves a bowl: " + p.getMainHandItem());
		h.succeed();
	}

	@GameTest
	public void honeyBottleRolls(GameTestHelper h) {
		ServerPlayer p = assertOneRoll(h, Items.HONEY_BOTTLE);
		h.assertTrue(p.getMainHandItem().is(Items.GLASS_BOTTLE), "honey leaves a bottle: " + p.getMainHandItem());
		h.succeed();
	}

	/** Chorus fruit teleports the eater after FoodProperties.onConsume; the roll still happens exactly once. */
	@GameTest
	public void chorusFruitRolls(GameTestHelper h) {
		assertOneRoll(h, Items.CHORUS_FRUIT);
		h.succeed();
	}

	@GameTest
	public void goldenAppleRolls(GameTestHelper h) {
		assertOneRoll(h, Items.GOLDEN_APPLE);
		h.succeed();
	}

	@GameTest
	public void enchantedGoldenAppleRolls(GameTestHelper h) {
		assertOneRoll(h, Items.ENCHANTED_GOLDEN_APPLE);
		h.succeed();
	}

	/** The listener runs before stack.consume(1): the last item of a stack still counts (and is used up). */
	@GameTest
	public void lastItemOfStackRolls(GameTestHelper h) {
		defaults(h);
		ServerPlayer p = survivalPlayer(h);
		ItemStack left = eatStack(h, p, new ItemStack(Items.CARROT, 1));
		assertRolledOnce(h, p, "last carrot");
		h.assertTrue(left.isEmpty(), "the carrot was used up: " + left);
		h.succeed();
	}

	/** Two items from one stack: two bites, one roll each. */
	@GameTest
	public void exactlyOneRollPerBite(GameTestHelper h) {
		defaults(h);
		ServerPlayer p = survivalPlayer(h);
		ItemStack stack = new ItemStack(Items.BREAD, 5);
		for (int i = 1; i <= 5; i++) {
			stack = eatStack(h, p, stack);
			h.assertValueEqual(bites(p), i, "bites after " + i + " breads");
			h.assertTrue(totalSteps(p) <= i && (totalSteps(p) - i) % 2 == 0,
					"|steps| " + totalSteps(p) + " after " + i + " single-step rolls");
		}
		h.assertTrue(stack.isEmpty(), "all 5 breads eaten");
		h.succeed();
	}

	@GameTest
	public void cakeSliceRolls(GameTestHelper h) {
		defaults(h);
		ServerPlayer p = survivalPlayer(h);
		h.assertTrue(eatCake(h, p, Blocks.CAKE), "cake slice eaten");
		h.assertValueEqual(cakeBites(h), 1, "cake BITES");
		assertRolledOnce(h, p, "cake slice");
		h.succeed();
	}

	/** A candle cake is eaten via CakeBlock.eat too: it becomes a cake with one bite and the candle drops. */
	@GameTest
	public void candleCakeSliceRolls(GameTestHelper h) {
		defaults(h);
		ServerPlayer p = survivalPlayer(h);
		h.assertTrue(eatCake(h, p, Blocks.CANDLE_CAKE), "candle cake slice eaten");
		h.assertValueEqual(cakeBites(h), 1, "candle cake became a cake with BITES");
		assertRolledOnce(h, p, "candle cake slice");
		h.succeed();
	}

	/** A full player can't eat cake (PASS): no roll, cake untouched. */
	@GameTest
	public void fullHungerCakeDoesNotRoll(GameTestHelper h) {
		defaults(h);
		ServerPlayer p = survivalPlayer(h);
		p.getFoodData().setFoodLevel(20);
		h.assertTrue(!useCake(h, p, Blocks.CAKE), "a full player ate cake");
		h.assertValueEqual(cakeBites(h), 0, "cake BITES");
		assertNoRoll(h, p, "full player at a cake");
		h.assertValueEqual(bites(p), 0, "bites");
		h.succeed();
	}

	@GameTest
	public void potionDoesNotRoll(GameTestHelper h) {
		defaults(h);
		ServerPlayer p = survivalPlayer(h);
		eat(h, p, Items.POTION);
		assertNoRoll(h, p, "potion");
		h.succeed();
	}

	@GameTest
	public void milkDoesNotRoll(GameTestHelper h) {
		defaults(h);
		ServerPlayer p = survivalPlayer(h);
		ItemStack left = eat(h, p, Items.MILK_BUCKET);
		h.assertTrue(left.is(Items.BUCKET), "milk was drunk: " + left);
		assertNoRoll(h, p, "milk");
		h.succeed();
	}

	@GameTest
	public void ominousBottleDoesNotRoll(GameTestHelper h) {
		defaults(h);
		ServerPlayer p = survivalPlayer(h);
		eat(h, p, Items.OMINOUS_BOTTLE);
		assertNoRoll(h, p, "ominous bottle");
		h.succeed();
	}

	@GameTest
	public void creativeDoesNotRoll(GameTestHelper h) {
		defaults(h);
		ServerPlayer p = survivalPlayer(h);
		p.setGameMode(GameType.CREATIVE);
		eat(h, p, Items.BREAD);
		eat(h, p, Items.GOLDEN_APPLE);
		// creative players can always eat cake (canEat: invulnerable), the hook fires, shouldTrigger rejects it
		h.assertTrue(useCake(h, p, Blocks.CAKE), "creative player ate cake");
		h.assertValueEqual(cakeBites(h), 1, "cake BITES");
		assertNoRoll(h, p, "creative");
		h.assertTrue(!BiteHandler.shouldTrigger(p), "shouldTrigger(creative)");
		h.succeed();
	}

	@GameTest
	public void spectatorDoesNotRoll(GameTestHelper h) {
		defaults(h);
		ServerPlayer p = survivalPlayer(h);
		p.setGameMode(GameType.SPECTATOR);
		eat(h, p, Items.BREAD);
		assertNoRoll(h, p, "spectator");
		h.assertTrue(!BiteHandler.shouldTrigger(p), "shouldTrigger(spectator)");
		h.succeed();
	}

	@GameTest
	public void adventureRolls(GameTestHelper h) {
		defaults(h);
		ServerPlayer p = survivalPlayer(h);
		p.setGameMode(GameType.ADVENTURE);
		h.assertTrue(BiteHandler.shouldTrigger(p), "shouldTrigger(adventure)");
		eat(h, p, Items.APPLE);
		assertRolledOnce(h, p, "adventure apple");
		h.succeed();
	}

	/** OFF only stops new rolls (D13): nothing happens on food or cake, earlier changes stay. */
	@GameTest
	public void modeOffDoesNotRoll(GameTestHelper h) {
		defaults(h);
		ServerPlayer p = survivalPlayer(h);
		PlayerMunch.setStacks(p, PlayerStacks.EMPTY.withSteps(Caps.LUCK, 3));
		try {
			MunchaholicMode.setEnabled(server(h), false);
			h.assertTrue(!BiteHandler.shouldTrigger(p), "shouldTrigger with mode OFF");
			eat(h, p, Items.BREAD);
			h.assertTrue(eatCake(h, p, Blocks.CAKE), "cake slice eaten with mode OFF");
			h.assertValueEqual(bites(p), 0, "bites with mode OFF");
			h.assertValueEqual(steps(p, Caps.LUCK), 3, "earlier changes are frozen, not removed");
			h.assertTrue(modifier(p, Caps.LUCK) != null, "earlier modifier kept");
		} finally {
			defaults(h);
		}
		h.succeed();
	}

	/** Foxes and zombies also run FoodProperties.onConsume: no exception, no state, no modifier. */
	@GameTest
	public void nonPlayerDoesNotRoll(GameTestHelper h) {
		defaults(h);
		Map<String, LivingEntity> eaters = new HashMap<>();
		eaters.put("fox", h.spawn(EntityTypes.FOX, new BlockPos(1, 2, 1)));
		eaters.put("zombie", h.spawn(EntityTypes.ZOMBIE, new BlockPos(2, 2, 2)));
		for (Map.Entry<String, LivingEntity> e : eaters.entrySet()) {
			LivingEntity eater = e.getValue();
			new ItemStack(Items.SWEET_BERRIES).finishUsingItem(h.getLevel(), eater);
			new ItemStack(Items.BREAD).finishUsingItem(h.getLevel(), eater);
			h.assertTrue(!eater.hasAttached(MunchAttachments.STACKS), e.getKey() + " got a stacks attachment");
			var scale = eater.getAttribute(Attributes.SCALE);
			h.assertTrue(scale == null || scale.getModifier(PlayerMunch.MODIFIER_ID) == null, e.getKey() + " got a modifier");
			eater.discard();
		}
		h.succeed();
	}

	/** Automation mods' FakePlayers are ServerPlayers in survival, but must never roll. */
	@GameTest
	public void fakePlayerDoesNotRoll(GameTestHelper h) {
		defaults(h);
		FakePlayer fake = FakePlayer.get(h.getLevel());
		h.assertTrue(!BiteHandler.shouldTrigger(fake), "shouldTrigger(FakePlayer)");
		ItemStack bread = new ItemStack(Items.BREAD);
		fake.setItemInHand(InteractionHand.MAIN_HAND, bread);
		bread.finishUsingItem(h.getLevel(), fake);
		h.assertValueEqual(PlayerMunch.stacks(fake).bites(), 0, "fake player bites");
		h.succeed();
	}

	@GameTest
	public void shouldTriggerSurvival(GameTestHelper h) {
		defaults(h);
		ServerPlayer p = survivalPlayer(h);
		h.assertTrue(BiteHandler.shouldTrigger(p), "shouldTrigger(survival, mode ON)");
		h.assertValueEqual(BiteHandler.foodId(new ItemStack(Items.CARROT)), "minecraft:carrot", "foodId");
		h.assertValueEqual(BiteHandler.foodId(new ItemStack(Items.CAKE)), "minecraft:cake", "foodId(cake)");
		h.succeed();
	}
}
