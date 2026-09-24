package dev.munchaholic.test;

import static dev.munchaholic.test.TestSupport.EPS;
import static dev.munchaholic.test.TestSupport.assertModifiersMatch;
import static dev.munchaholic.test.TestSupport.bites;
import static dev.munchaholic.test.TestSupport.defaults;
import static dev.munchaholic.test.TestSupport.instance;
import static dev.munchaholic.test.TestSupport.modifier;
import static dev.munchaholic.test.TestSupport.modifierAmount;
import static dev.munchaholic.test.TestSupport.munchValue;
import static dev.munchaholic.test.TestSupport.script;
import static dev.munchaholic.test.TestSupport.steps;
import static dev.munchaholic.test.TestSupport.survivalPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import dev.munchaholic.BiteHandler;
import dev.munchaholic.core.AttributeSpec;
import dev.munchaholic.core.Caps;
import dev.munchaholic.core.Direction;
import dev.munchaholic.core.ModifierOp;
import dev.munchaholic.core.PlayerStacks;
import dev.munchaholic.core.RandomIndex;
import dev.munchaholic.core.RollOutcome;
import dev.munchaholic.player.AttributeHolders;
import dev.munchaholic.player.PlayerHooks;
import dev.munchaholic.player.PlayerMunch;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Random-mode rolls on a real player: modifiers derived from integer steps (D1/D2), stacking, the capped reroll (D6)
 * and the caps holding under long random walks, including walks that start at the limits.
 */
public class MunchRollGameTests {
	private static final ItemStack BREAD = new ItemStack(Items.BREAD);

	private static RollOutcome bite(ServerPlayer p, RandomIndex random) {
		return BiteHandler.bite(p, BREAD, random);
	}

	/** Every spec rolled UP once from 0 (scripted: pick spec 0 of the remaining list, UP): op and amount per spec. */
	@GameTest
	public void modifierMatchesSteps(GameTestHelper h) {
		defaults(h);
		ServerPlayer p = survivalPlayer(h);
		for (int i = 0; i < Caps.ALL.size(); i++) {
			AttributeSpec spec = Caps.ALL.get(i);
			RollOutcome o = bite(p, script(i, 0));
			h.assertTrue(o instanceof RollOutcome.Applied a && a.spec() == spec && a.direction() == Direction.UP
					&& a.oldSteps() == 0 && a.newSteps() == 1, "bite " + i + " outcome " + o);
			AttributeModifier m = modifier(p, spec);
			h.assertTrue(m != null, spec.key() + ": no modifier");
			h.assertTrue(Math.abs(m.amount() - spec.step()) < EPS, spec.key() + ": amount " + m.amount());
			AttributeModifier.Operation expectedOp = spec.op() == ModifierOp.ADD_VALUE
					? AttributeModifier.Operation.ADD_VALUE : AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
			h.assertValueEqual(m.operation(), expectedOp, spec.key() + ": operation");
			h.assertValueEqual(PlayerMunch.op(spec), expectedOp, spec.key() + ": PlayerMunch.op");
			AttributeInstance inst = instance(p, spec);
			double expected = spec.valueAt(inst.getBaseValue(), 1);
			h.assertTrue(Math.abs(inst.getValue() - expected) < EPS,
					spec.key() + ": value " + inst.getValue() + ", expected " + expected);
		}
		h.assertValueEqual(bites(p), Caps.ALL.size(), "bites");
		assertModifiersMatch(h, p, "after 20 scripted bites");
		h.succeed();
	}

	/** One modifier per attribute: two scale UP rolls replace it with the new total (D1). */
	@GameTest
	public void stackingSameAttribute(GameTestHelper h) {
		defaults(h);
		ServerPlayer p = survivalPlayer(h);
		bite(p, script(0, 0));
		bite(p, script(0, 0));
		AttributeInstance scale = instance(p, Caps.SCALE);
		long ours = scale.getModifiers().stream().filter(m -> m.is(PlayerMunch.MODIFIER_ID)).count();
		h.assertValueEqual(ours, 1L, "munchaholic:bites modifiers on scale");
		h.assertTrue(Math.abs(modifierAmount(p, Caps.SCALE) - 0.16) < EPS, "amount " + modifierAmount(p, Caps.SCALE));
		h.assertTrue(Math.abs(scale.getValue() - 1.16) < EPS, "scale value " + scale.getValue());
		h.assertValueEqual(steps(p, Caps.SCALE), 2, "scale steps");
		h.assertValueEqual(bites(p), 2, "bites");
		h.succeed();
	}

	@GameTest
	public void stackingOppositeCancels(GameTestHelper h) {
		defaults(h);
		ServerPlayer p = survivalPlayer(h);
		bite(p, script(0, 0));
		bite(p, script(0, 1));
		h.assertTrue(modifier(p, Caps.SCALE) == null, "modifier removed at 0 steps");
		h.assertTrue(PlayerMunch.stacks(p).isEmpty(), "stacks empty: " + PlayerMunch.stacks(p));
		h.assertValueEqual(bites(p), 2, "bites still counted");
		h.assertTrue(Math.abs(instance(p, Caps.SCALE).getValue() - 1.0) < EPS, "scale back to 1");
		h.succeed();
	}

	/** Armor DOWN at 0 is capped: armor leaves the candidates and a different attribute (scale, index 0) is rolled. */
	@GameTest
	public void cappedRerollsOtherAttribute(GameTestHelper h) {
		defaults(h);
		ServerPlayer p = survivalPlayer(h);
		h.assertValueEqual(Caps.ALL.indexOf(Caps.ARMOR), 9, "armor index in Caps.ALL");
		RollOutcome o = bite(p, script(9, 1, 0, 0));
		h.assertTrue(o instanceof RollOutcome.Applied a && a.spec() == Caps.SCALE && a.direction() == Direction.UP,
				"outcome " + o);
		h.assertValueEqual(steps(p, Caps.ARMOR), 0, "armor steps");
		h.assertValueEqual(steps(p, Caps.SCALE), 1, "scale steps");
		h.succeed();
	}

	/** 2000 real bites with the player's own RandomSource: every munch value stays inside its cap range. */
	@GameTest(maxTicks = 400)
	public void capsHoldUnderRandomWalk(GameTestHelper h) {
		defaults(h);
		ServerPlayer p = survivalPlayer(h);
		RandomIndex random = p.getRandom()::nextInt;
		for (int i = 0; i < 2000; i++) {
			RollOutcome o = bite(p, random);
			h.assertTrue(o instanceof RollOutcome.Applied, "bite " + i + " gave " + o);
			assertInsideCaps(h, p, "bite " + i);
		}
		h.assertValueEqual(bites(p), 2000, "bites");
		assertModifiersMatch(h, p, "after 2000 bites");
		assertSafe(h, p, "after 2000 bites");
		h.succeed();
	}

	/**
	 * Every attribute starts at its upper limit, then at its lower limit; 300 seeded bites each. No roll may push past a
	 * cap (capped picks are rerolled), and the walk only moves back inside.
	 */
	@GameTest(maxTicks = 400)
	public void capsHoldFromTheLimits(GameTestHelper h) {
		defaults(h);
		for (boolean upper : new boolean[] {true, false}) {
			ServerPlayer p = survivalPlayer(h);
			Map<String, Integer> start = new HashMap<>();
			for (AttributeSpec spec : Caps.ALL) {
				double base = PlayerMunch.base(p, spec);
				start.put(spec.key(), upper ? spec.maxSteps(base) : spec.minSteps(base));
			}
			PlayerMunch.setStacks(p, new PlayerStacks(start, 0));
			assertInsideCaps(h, p, (upper ? "upper" : "lower") + " start");
			assertModifiersMatch(h, p, (upper ? "upper" : "lower") + " start");
			Random random = new Random(upper ? 1L : 2L);
			for (int i = 0; i < 300; i++) {
				RollOutcome o = bite(p, random::nextInt);
				if (o instanceof RollOutcome.Applied a) {
					h.assertTrue(a.spec().canStep(a.base(), a.oldSteps(), a.direction()), "illegal step applied: " + a);
					if (i == 0) {
						// the very first roll from all-at-the-limit must move one attribute back inside
						h.assertValueEqual(a.direction(), upper ? Direction.DOWN : Direction.UP, "first direction from the limit");
					}
				} else {
					h.fail("bite " + i + " from the " + (upper ? "upper" : "lower") + " limits gave " + o);
				}
				assertInsideCaps(h, p, (upper ? "upper" : "lower") + " bite " + i);
			}
			assertModifiersMatch(h, p, (upper ? "upper" : "lower") + " walk");
			assertSafe(h, p, (upper ? "upper" : "lower") + " walk");
		}
		h.succeed();
	}

	private static void assertInsideCaps(GameTestHelper h, ServerPlayer p, String what) {
		for (AttributeSpec spec : Caps.ALL) {
			double base = PlayerMunch.base(p, spec);
			int s = steps(p, spec);
			double v = munchValue(p, spec);
			if (!spec.inRange(v) || s < spec.minSteps(base) || s > spec.maxSteps(base)) {
				h.fail(what + ": " + spec.key() + " at " + s + " steps = " + v + ", outside [" + spec.min() + ", " + spec.max() + "]");
			}
		}
	}

	/** The safety floors from §2, checked on the live attribute values. */
	private static void assertSafe(GameTestHelper h, ServerPlayer p, String what) {
		h.assertTrue(p.getMaxHealth() >= 2.0F, what + ": max health " + p.getMaxHealth());
		h.assertTrue(p.getAttributeValue(Attributes.MOVEMENT_SPEED) > 0, what + ": speed");
		h.assertTrue(p.getAttributeValue(Attributes.GRAVITY) > 0, what + ": gravity");
		h.assertTrue(p.getAttributeValue(Attributes.SCALE) >= 0.15, what + ": scale");
		h.assertTrue(p.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE) >= 1.0, what + ": block range");
		h.assertTrue(p.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE) >= 1.0, what + ": entity range");
	}

	/** A max_health drop below the current health is clamped by vanilla on the player's next tick. */
	@GameTest
	public void maxHealthDropClampsHealth(GameTestHelper h) {
		defaults(h);
		ServerPlayer p = survivalPlayer(h);
		p.setHealth(20.0F);
		PlayerMunch.setStacks(p, PlayerStacks.EMPTY.withSteps(Caps.MAX_HEALTH, -8));
		int index = Caps.ALL.indexOf(Caps.MAX_HEALTH);
		RollOutcome o = bite(p, script(index, 1));
		h.assertTrue(o instanceof RollOutcome.Applied a && a.spec() == Caps.MAX_HEALTH && a.newSteps() == -9, "outcome " + o);
		h.assertTrue(Math.abs(p.getMaxHealth() - 2.0F) < EPS, "max health " + p.getMaxHealth());
		p.doTick();
		h.assertTrue(p.getHealth() <= p.getMaxHealth(), "health " + p.getHealth() + " > max " + p.getMaxHealth());
		h.assertTrue(p.getHealth() > 0, "still alive");
		// one more DOWN is capped (2 hp is the floor): the script must fall through to another attribute
		RollOutcome again = bite(p, script(index, 1, 0, 0));
		h.assertTrue(again instanceof RollOutcome.Applied a && a.spec() == Caps.SCALE, "at the max_health floor: " + again);
		h.assertValueEqual(steps(p, Caps.MAX_HEALTH), -9, "max_health steps at the floor");
		h.succeed();
	}

	/** setStacks refreshes dimensions when scale changes, so the hitbox is right immediately. */
	@GameTest
	public void scaleChangesHitbox(GameTestHelper h) {
		defaults(h);
		ServerPlayer p = survivalPlayer(h);
		float before = p.getBbWidth();
		bite(p, script(0, 0));
		float after = p.getBbWidth();
		h.assertTrue(Math.abs(after - before * 1.08F) < 1e-3, "width " + before + " -> " + after + ", expected x1.08");
		bite(p, script(0, 1));
		h.assertTrue(Math.abs(p.getBbWidth() - before) < 1e-3, "width back to " + before + ": " + p.getBbWidth());
		h.succeed();
	}

	/** All 20 attributes exist on players, have the right id and base, and our caps lie inside vanilla's ranges. */
	@GameTest
	public void capsInsideVanillaRanges(GameTestHelper h) {
		ServerPlayer p = survivalPlayer(h);
		for (AttributeSpec spec : Caps.ALL) {
			Holder<Attribute> holder = AttributeHolders.of(spec);
			String id = holder.unwrapKey().orElseThrow().identifier().toString();
			h.assertValueEqual(id, "minecraft:" + spec.key(), "attribute id");
			h.assertValueEqual(holder.value().getDescriptionId(), spec.translationKey(), spec.key() + " translation key");
			AttributeInstance inst = p.getAttribute(holder);
			h.assertTrue(inst != null, spec.key() + ": player has no instance");
			h.assertTrue(Math.abs(inst.getBaseValue() - spec.playerBase()) < 1e-9,
					spec.key() + ": player base " + inst.getBaseValue() + " != table " + spec.playerBase());
			h.assertTrue(holder.value() instanceof RangedAttribute, spec.key() + " is not a RangedAttribute");
			RangedAttribute ranged = (RangedAttribute) holder.value();
			h.assertTrue(ranged.getMinValue() <= spec.min() && spec.max() <= ranged.getMaxValue(),
					spec.key() + ": cap [" + spec.min() + ", " + spec.max() + "] outside vanilla [" + ranged.getMinValue()
							+ ", " + ranged.getMaxValue() + "]");
		}
		h.succeed();
	}

	/** JOIN repairs a modifier removed by hand (or by another mod) and rebalances a wrong amount (D2). */
	@GameTest
	public void joinReappliesModifiers(GameTestHelper h) {
		defaults(h);
		ServerPlayer p = survivalPlayer(h);
		PlayerMunch.setStacks(p, PlayerStacks.EMPTY.withSteps(Caps.SCALE, 1).withSteps(Caps.ARMOR, 3));
		instance(p, Caps.SCALE).removeModifier(PlayerMunch.MODIFIER_ID);
		instance(p, Caps.ARMOR).addOrReplacePermanentModifier(
				new AttributeModifier(PlayerMunch.MODIFIER_ID, 99.0, AttributeModifier.Operation.ADD_VALUE));
		instance(p, Caps.LUCK).addOrReplacePermanentModifier(
				new AttributeModifier(PlayerMunch.MODIFIER_ID, 5.0, AttributeModifier.Operation.ADD_VALUE));
		PlayerHooks.onJoin(p);
		h.assertTrue(Math.abs(modifierAmount(p, Caps.SCALE) - 0.08) < EPS, "scale modifier repaired");
		h.assertTrue(Math.abs(modifierAmount(p, Caps.ARMOR) - 3.0) < EPS, "armor modifier rebalanced");
		h.assertTrue(modifier(p, Caps.LUCK) == null, "stray modifier on a 0-step attribute removed");
		assertModifiersMatch(h, p, "after onJoin");
		h.succeed();
	}

	/** PlayerMunch.base reads the live base value (a changed base shifts the cap math), BaseLookup delegates to it. */
	@GameTest
	public void basesAreLive(GameTestHelper h) {
		ServerPlayer p = survivalPlayer(h);
		for (AttributeSpec spec : Caps.ALL) {
			h.assertTrue(Math.abs(PlayerMunch.bases(p).base(spec) - PlayerMunch.base(p, spec)) < 1e-12, spec.key() + " bases()");
		}
		instance(p, Caps.MAX_HEALTH).setBaseValue(30.0);
		h.assertTrue(Math.abs(PlayerMunch.base(p, Caps.MAX_HEALTH) - 30.0) < 1e-12, "live base");
		h.succeed();
	}
}
