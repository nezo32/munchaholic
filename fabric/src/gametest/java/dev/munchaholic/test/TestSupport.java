package dev.munchaholic.test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.munchaholic.core.AttributeSpec;
import dev.munchaholic.core.Caps;
import dev.munchaholic.core.RandomIndex;
import dev.munchaholic.core.Recipe;
import dev.munchaholic.core.Recipes;
import dev.munchaholic.core.RollMode;
import dev.munchaholic.mode.MunchaholicMode;
import dev.munchaholic.player.AttributeHolders;
import dev.munchaholic.player.PlayerMunch;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CakeBlock;

/** Shared helpers for the server gametests (ARCHITECTURE §9.2). */
public final class TestSupport {
	/** Relative position of the cake in cake tests; a stone block is placed right below it. */
	public static final BlockPos CAKE_POS = new BlockPos(1, 2, 1);
	/** Tolerance for comparing attribute values computed in doubles. */
	public static final double EPS = 1e-6;
	/** Where mock players start: open air far above the test structures. */
	public static final double OPEN_AIR_X = 0.5, OPEN_AIR_Y = 200.0, OPEN_AIR_Z = 0.5;

	private TestSupport() {}

	/**
	 * A mock server player in the test level, in survival, with an empty inventory.
	 *
	 * <p>Not {@link GameTestHelper#makeMockServerPlayerInLevel()}: that player hard-wires {@code gameMode()} to CREATIVE.
	 */
	public static ServerPlayer survivalPlayer(GameTestHelper h) {
		return mockPlayer(h, "munch-mock-player").player();
	}

	/**
	 * Sets the world settings to the test defaults (mode ON, Random, keep on death ON). They are server-global and the
	 * gametests share one server, so every setting-dependent test calls this first and works synchronously afterwards.
	 */
	public static void defaults(GameTestHelper h) {
		MinecraftServer server = h.getLevel().getServer();
		MunchaholicMode.setEnabled(server, true);
		MunchaholicMode.setRollMode(server, RollMode.RANDOM);
		MunchaholicMode.setKeepOnDeath(server, true);
	}

	public static MinecraftServer server(GameTestHelper h) {
		return h.getLevel().getServer();
	}

	/** Eats one item of {@code item} the way {@code LivingEntity.completeUsingItem} does; returns what is left in hand. */
	public static ItemStack eat(GameTestHelper h, ServerPlayer p, Item item) {
		return eatStack(h, p, new ItemStack(item));
	}

	public static ItemStack eatStack(GameTestHelper h, ServerPlayer p, ItemStack stack) {
		p.setItemInHand(InteractionHand.MAIN_HAND, stack);
		ItemStack result = stack.finishUsingItem(h.getLevel(), p);
		p.setItemInHand(InteractionHand.MAIN_HAND, result);
		return result;
	}

	/**
	 * Places {@code cake} (plain or candle cake) at {@link #CAKE_POS} on a stone block, lowers the player's food level to
	 * 10 so they can eat, and right-clicks it with an empty hand. Returns whether the block changed (a slice was eaten).
	 */
	public static boolean eatCake(GameTestHelper h, ServerPlayer p, Block cake) {
		p.getFoodData().setFoodLevel(10);
		return useCake(h, p, cake);
	}

	/** Like {@link #eatCake} but leaves the food level alone (full-hunger players must not be able to eat). */
	public static boolean useCake(GameTestHelper h, ServerPlayer p, Block cake) {
		h.setBlock(CAKE_POS.below(), Blocks.STONE);
		h.setBlock(CAKE_POS, cake);
		p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		var before = h.getBlockState(CAKE_POS);
		h.useBlock(CAKE_POS, p);
		return !h.getBlockState(CAKE_POS).equals(before);
	}

	/** The cake's BITES value at {@link #CAKE_POS}, or -1 if the block there is not a plain cake. */
	public static int cakeBites(GameTestHelper h) {
		var state = h.getBlockState(CAKE_POS);
		return state.is(Blocks.CAKE) ? state.getValue(CakeBlock.BITES) : -1;
	}

	public static int steps(ServerPlayer p, AttributeSpec spec) {
		return PlayerMunch.stacks(p).steps(spec);
	}

	public static int bites(ServerPlayer p) {
		return PlayerMunch.stacks(p).bites();
	}

	/** Sum of |steps| over all attributes: a fresh player that bit once must have exactly 1. */
	public static int totalSteps(ServerPlayer p) {
		int total = 0;
		for (AttributeSpec spec : Caps.ALL) total += Math.abs(steps(p, spec));
		return total;
	}

	/** A scripted {@link RandomIndex}: returns the draws in order; fails on a bound <= 0, a draw >= bound, or an extra call. */
	public static RandomIndex script(int... draws) {
		Deque<Integer> queue = new ArrayDeque<>();
		for (int d : draws) queue.add(d);
		return bound -> {
			if (bound <= 0) throw new AssertionError("nextInt called with bound " + bound);
			Integer next = queue.poll();
			if (next == null) throw new AssertionError("unexpected extra nextInt(" + bound + ")");
			if (next >= bound) throw new AssertionError("scripted draw " + next + " >= bound " + bound);
			return next;
		};
	}

	/**
	 * An item whose recipe in this world is {@code wanted}. Searches every item, not only foods: {@code BiteHandler.bite}
	 * keys the recipe by item id alone, and with ~1500 ids a match exists for any seed (each has a 1 in 40 chance).
	 */
	public static ItemStack itemWithRecipe(GameTestHelper h, Recipe wanted) {
		long seed = server(h).overworld().getSeed();
		for (Item item : BuiltInRegistries.ITEM) {
			ItemStack stack = new ItemStack(item);
			if (stack.isEmpty()) continue;
			if (Recipes.of(seed, BuiltInRegistries.ITEM.getKey(item).toString()).equals(wanted)) return stack;
		}
		throw new AssertionError("no item has the recipe " + wanted + " in this world");
	}

	/** A {@link RandomIndex} that must never be used (Recipes mode is deterministic). */
	public static RandomIndex noRandom() {
		return bound -> {
			throw new AssertionError("Recipes mode drew a random number (bound " + bound + ")");
		};
	}

	public static AttributeInstance instance(ServerPlayer p, AttributeSpec spec) {
		AttributeInstance inst = p.getAttribute(AttributeHolders.of(spec));
		if (inst == null) throw new AssertionError("player has no attribute instance for " + spec.key());
		return inst;
	}

	/** Our modifier on {@code spec}'s attribute, or null. */
	public static AttributeModifier modifier(ServerPlayer p, AttributeSpec spec) {
		return instance(p, spec).getModifier(PlayerMunch.MODIFIER_ID);
	}

	/** Amount of our modifier on {@code spec}'s attribute (0 if absent). */
	public static double modifierAmount(ServerPlayer p, AttributeSpec spec) {
		AttributeModifier m = modifier(p, spec);
		return m == null ? 0.0 : m.amount();
	}

	/** Base + our modifier only (the "munch value" the caps apply to, D4). */
	public static double munchValue(ServerPlayer p, AttributeSpec spec) {
		return spec.valueAt(instance(p, spec).getBaseValue(), steps(p, spec));
	}

	/** Asserts that every attribute's modifier matches the stored steps (absent for 0, right op and amount otherwise). */
	public static void assertModifiersMatch(GameTestHelper h, ServerPlayer p, String what) {
		for (AttributeSpec spec : Caps.ALL) {
			int s = steps(p, spec);
			AttributeModifier m = modifier(p, spec);
			if (s == 0) {
				h.assertTrue(m == null, what + ": " + spec.key() + " has a modifier but 0 steps: " + m);
			} else {
				h.assertTrue(m != null, what + ": " + spec.key() + " has " + s + " steps but no modifier");
				h.assertTrue(Math.abs(m.amount() - spec.modifierAmount(s)) < EPS,
						what + ": " + spec.key() + " amount " + m.amount() + " != " + spec.modifierAmount(s));
				h.assertValueEqual(m.operation(), PlayerMunch.op(spec), what + ": " + spec.key() + " operation");
			}
		}
	}

	public static int run(GameTestHelper h, CommandSourceStack source, String command) throws CommandSyntaxException {
		return server(h).getCommands().getDispatcher().execute(command, source);
	}

	/** Whether {@code c} or any of its args/siblings (recursively) is a translatable with {@code key}. */
	public static boolean containsKey(Component c, String key) {
		if (c.getContents() instanceof TranslatableContents tc) {
			if (tc.getKey().equals(key)) return true;
			for (Object arg : tc.getArgs()) {
				if (arg instanceof Component ac && containsKey(ac, key)) return true;
			}
		}
		for (Component sibling : c.getSiblings()) {
			if (containsKey(sibling, key)) return true;
		}
		return false;
	}

	public static String key(Component c) {
		return c.getContents() instanceof TranslatableContents tc ? tc.getKey() : null;
	}

	/** A mock player whose outbound packets can be inspected (same setup as the reference NotifyGameTests). */
	public record Mock(ServerPlayer player, EmbeddedChannel channel) {
		public List<Object> drain() {
			channel.runPendingTasks();
			List<Object> out = new ArrayList<>(channel.outboundMessages());
			channel.outboundMessages().clear();
			return out;
		}

		public static List<ClientboundSystemChatPacket> overlays(List<Object> out) {
			return out.stream().filter(m -> m instanceof ClientboundSystemChatPacket p && p.overlay())
					.map(m -> (ClientboundSystemChatPacket) m).toList();
		}

		public static List<ClientboundSystemChatPacket> chats(List<Object> out) {
			return out.stream().filter(m -> m instanceof ClientboundSystemChatPacket p && !p.overlay())
					.map(m -> (ClientboundSystemChatPacket) m).toList();
		}

		public static <T extends CustomPacketPayload> List<T> payloads(List<Object> out, Class<T> type) {
			return out.stream().filter(m -> m instanceof ClientboundCustomPayloadPacket p && type.isInstance(p.payload()))
					.map(m -> type.cast(((ClientboundCustomPayloadPacket) m).payload())).toList();
		}
	}

	/** A survival mock player with an inspectable channel; the join packets are already drained. */
	public static Mock mockPlayer(GameTestHelper h) {
		return mockPlayer(h, "munch-mock-player");
	}

	/** A mock player with a unique name (at most 16 characters), for commands that take a player name. */
	public static Mock namedMockPlayer(GameTestHelper h) {
		return mockPlayer(h, "munch" + UUID.randomUUID().toString().replace("-", "").substring(0, 11));
	}

	public static Mock mockPlayer(GameTestHelper h, String name) {
		ServerLevel level = h.getLevel();
		CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), name), false);
		ServerPlayer p = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation());
		Connection connection = new Connection(PacketFlow.SERVERBOUND);
		EmbeddedChannel channel = new EmbeddedChannel(connection);
		level.getServer().getPlayerList().placeNewPlayer(connection, p, cookie);
		p.setGameMode(GameType.SURVIVAL);
		// in open air: placeNewPlayer puts the player at (0, 0, 0), inside the ground, where the growth check refuses
		// every scale UP; no gravity keeps them from falling during multi-tick tests
		p.setNoGravity(true);
		p.snapTo(OPEN_AIR_X, OPEN_AIR_Y, OPEN_AIR_Z, 0.0F, 0.0F);
		p.getInventory().clearContent();
		Mock mock = new Mock(p, channel);
		mock.drain();
		return mock;
	}
}
