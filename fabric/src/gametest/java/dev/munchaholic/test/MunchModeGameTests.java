package dev.munchaholic.test;

import static dev.munchaholic.test.TestSupport.assertModifiersMatch;
import static dev.munchaholic.test.TestSupport.containsKey;
import static dev.munchaholic.test.TestSupport.defaults;
import static dev.munchaholic.test.TestSupport.mockPlayer;
import static dev.munchaholic.test.TestSupport.modifier;
import static dev.munchaholic.test.TestSupport.run;
import static dev.munchaholic.test.TestSupport.server;
import static dev.munchaholic.test.TestSupport.survivalPlayer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.munchaholic.core.AttributeSpec;
import dev.munchaholic.core.Caps;
import dev.munchaholic.core.Discoveries;
import dev.munchaholic.core.PlayerStacks;
import dev.munchaholic.core.RollMode;
import dev.munchaholic.mixin.MinecraftServerAccessor;
import dev.munchaholic.mode.ModeBootstrap;
import dev.munchaholic.mode.MunchaholicMode;
import dev.munchaholic.mode.PendingWorldMode;
import dev.munchaholic.player.PlayerMunch;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.SavedDataStorage;

/**
 * Per-world settings (D12) and the /munchaholic command tree (§5.3, D16): reading is open, changing needs
 * LEVEL_GAMEMASTERS; the saved data codec, the Create World handoff and a save/reload of mode.dat.
 *
 * <p>Settings are server-global: every test calls {@link TestSupport#defaults} first and restores them in {@code finally}.
 */
public class MunchModeGameTests {
	private static CommandSourceStack op(GameTestHelper h) {
		return server(h).createCommandSourceStack();
	}

	private static CommandSourceStack nobody(GameTestHelper h) {
		return server(h).createCommandSourceStack().withPermission(PermissionSet.NO_PERMISSIONS);
	}

	/**
	 * A player-only selector that matches exactly this player, whatever the other mock players are called (a UUID is
	 * rejected by EntityArgument.player(): vanilla treats UUID selectors as "may include entities").
	 */
	private static String target(ServerPlayer p) {
		String tag = "munch_" + p.getStringUUID().substring(0, 8);
		p.addTag(tag);
		return "@a[tag=" + tag + ",limit=1]";
	}

	private static void assertFails(GameTestHelper h, CommandSourceStack source, String command, String who) {
		try {
			run(h, source, command);
			h.fail(who + " could run /" + command);
		} catch (CommandSyntaxException expected) {
			// requires() hides the node from this source: unknown or incomplete command
		}
	}

	@GameTest
	public void commandOnOffStatus(GameTestHelper h) throws CommandSyntaxException {
		defaults(h);
		MinecraftServer server = server(h);
		try {
			h.assertValueEqual(run(h, op(h), "munchaholic off"), 0, "off result");
			h.assertTrue(!MunchaholicMode.isEnabled(server), "off stored");
			h.assertValueEqual(run(h, op(h), "munchaholic status"), 0, "status off");
			h.assertValueEqual(run(h, op(h), "munchaholic"), 0, "bare = status");
			h.assertValueEqual(run(h, op(h), "munchaholic on"), 1, "on result");
			h.assertTrue(MunchaholicMode.isEnabled(server), "on stored");
			h.assertValueEqual(run(h, op(h), "munchaholic status"), 1, "status on");
			h.assertValueEqual(run(h, op(h), "munchaholic"), 1, "bare on");
			h.assertTrue(MunchaholicMode.get(server).isDirty(), "state marked dirty for saving");
			h.assertValueEqual(MunchaholicMode.rollMode(server), RollMode.RANDOM, "roll mode untouched by on/off");
			h.assertTrue(MunchaholicMode.keepOnDeath(server), "keep on death untouched by on/off");
		} finally {
			defaults(h);
		}
		h.succeed();
	}

	/** The status output: 3 lines (on/off, roll mode, keep on death) to the caller only. */
	@GameTest
	public void statusPrintsThreeLines(GameTestHelper h) throws CommandSyntaxException {
		defaults(h);
		TestSupport.Mock mock = mockPlayer(h);
		run(h, mock.player().createCommandSourceStack(), "munchaholic status");
		List<Object> out = mock.drain();
		var chats = TestSupport.Mock.chats(out);
		h.assertValueEqual(chats.size(), 3, "status lines; outbound=" + out);
		h.assertTrue(containsKey(chats.get(0).content(), "munchaholic.command.status.on"), "line 1: " + chats.get(0).content());
		h.assertTrue(containsKey(chats.get(1).content(), "munchaholic.command.mode.status"), "line 2: " + chats.get(1).content());
		h.assertTrue(containsKey(chats.get(1).content(), RollMode.RANDOM.translationKey()), "line 2 names the mode");
		h.assertTrue(containsKey(chats.get(2).content(), "munchaholic.command.keepOnDeath.status.on"), "line 3: " + chats.get(2).content());
		h.succeed();
	}

	@GameTest
	public void commandMode(GameTestHelper h) throws CommandSyntaxException {
		defaults(h);
		MinecraftServer server = server(h);
		try {
			h.assertValueEqual(run(h, op(h), "munchaholic mode"), 0, "bare mode = RANDOM ordinal");
			h.assertValueEqual(run(h, op(h), "munchaholic mode recipes"), 1, "mode recipes result");
			h.assertValueEqual(MunchaholicMode.rollMode(server), RollMode.RECIPES, "recipes stored");
			h.assertValueEqual(run(h, op(h), "munchaholic mode"), 1, "bare mode = RECIPES ordinal");
			h.assertValueEqual(run(h, op(h), "munchaholic mode random"), 0, "mode random result");
			h.assertValueEqual(MunchaholicMode.rollMode(server), RollMode.RANDOM, "random stored");
			h.assertTrue(MunchaholicMode.isEnabled(server), "enabled untouched by mode");
			assertFails(h, op(h), "munchaholic mode chaos", "op (unknown mode)");
		} finally {
			defaults(h);
		}
		h.succeed();
	}

	@GameTest
	public void commandKeepOnDeath(GameTestHelper h) throws CommandSyntaxException {
		defaults(h);
		MinecraftServer server = server(h);
		try {
			h.assertValueEqual(run(h, op(h), "munchaholic keep-on-death"), 1, "status on");
			h.assertValueEqual(run(h, op(h), "munchaholic keep-on-death off"), 0, "off result");
			h.assertTrue(!MunchaholicMode.keepOnDeath(server), "off stored");
			h.assertValueEqual(run(h, op(h), "munchaholic keep-on-death"), 0, "status off");
			h.assertValueEqual(run(h, op(h), "munchaholic keep-on-death on"), 1, "on result");
			h.assertTrue(MunchaholicMode.keepOnDeath(server), "on stored");
		} finally {
			defaults(h);
		}
		h.succeed();
	}

	/** Changing settings, looking at another player's stats and resetting need LEVEL_GAMEMASTERS. */
	@GameTest
	public void settersNeedGamemaster(GameTestHelper h) {
		defaults(h);
		ServerPlayer player = survivalPlayer(h); // the mock player is not an op
		ServerPlayer other = survivalPlayer(h);
		PlayerMunch.setStacks(other, PlayerStacks.EMPTY.withSteps(Caps.LUCK, 2));
		PlayerMunch.discover(other, "minecraft:carrot");
		try {
			for (CommandSourceStack source : new CommandSourceStack[] {nobody(h), player.createCommandSourceStack()}) {
				for (String cmd : new String[] {"munchaholic on", "munchaholic off", "munchaholic mode recipes",
						"munchaholic mode random", "munchaholic keep-on-death off", "munchaholic keep-on-death on",
						"munchaholic reset @s", "munchaholic reset " + target(other), "munchaholic reset " + target(other) + " recipes",
						"munchaholic stats " + target(other)}) {
					assertFails(h, source, cmd, "non-op");
				}
			}
			MinecraftServer server = server(h);
			h.assertTrue(MunchaholicMode.isEnabled(server), "mode unchanged");
			h.assertValueEqual(MunchaholicMode.rollMode(server), RollMode.RANDOM, "roll mode unchanged");
			h.assertTrue(MunchaholicMode.keepOnDeath(server), "keep on death unchanged");
			h.assertValueEqual(TestSupport.steps(other, Caps.LUCK), 2, "other player's stacks unchanged");
			h.assertTrue(PlayerMunch.discoveries(other).has("minecraft:carrot"), "other player's recipes unchanged");
		} finally {
			defaults(h);
		}
		h.succeed();
	}

	/** Everybody may read: bare, status, mode, keep-on-death (and stats about themselves). */
	@GameTest
	public void statusReadableByEveryone(GameTestHelper h) throws CommandSyntaxException {
		defaults(h);
		ServerPlayer player = survivalPlayer(h);
		for (CommandSourceStack source : new CommandSourceStack[] {nobody(h), player.createCommandSourceStack()}) {
			h.assertValueEqual(run(h, source, "munchaholic"), 1, "bare");
			h.assertValueEqual(run(h, source, "munchaholic status"), 1, "status");
			h.assertValueEqual(run(h, source, "munchaholic mode"), 0, "mode");
			h.assertValueEqual(run(h, source, "munchaholic keep-on-death"), 1, "keep-on-death");
		}
		h.assertValueEqual(run(h, player.createCommandSourceStack(), "munchaholic stats"), 0, "own stats (no changes)");
		h.succeed();
	}

	/** stats returns the number of changed attributes; a non-op sees their own, an op sees anyone's. */
	@GameTest
	public void statsSelfAndOther(GameTestHelper h) throws CommandSyntaxException {
		defaults(h);
		TestSupport.Mock mock = mockPlayer(h);
		ServerPlayer p = mock.player();
		PlayerMunch.setStacks(p, new PlayerStacks(PlayerStacks.EMPTY.withSteps(Caps.SCALE, 2).withSteps(Caps.LUCK, -1)
				.withSteps(Caps.ARMOR, 3).steps(), 9));
		PlayerMunch.discover(p, "minecraft:carrot");
		mock.drain();
		h.assertValueEqual(run(h, p.createCommandSourceStack(), "munchaholic stats"), 3, "own stats");
		List<Object> out = mock.drain();
		var chats = TestSupport.Mock.chats(out);
		h.assertValueEqual(chats.size(), 4, "header + 3 lines; outbound=" + out);
		h.assertTrue(containsKey(chats.get(0).content(), "munchaholic.command.stats.header"), "header: " + chats.get(0).content());
		// Caps.ALL order: scale, armor, luck
		h.assertTrue(containsKey(chats.get(1).content(), Caps.SCALE.translationKey()), "line 1 = scale: " + chats.get(1).content());
		h.assertTrue(containsKey(chats.get(2).content(), Caps.ARMOR.translationKey()), "line 2 = armor: " + chats.get(2).content());
		h.assertTrue(containsKey(chats.get(3).content(), Caps.LUCK.translationKey()), "line 3 = luck: " + chats.get(3).content());

		h.assertValueEqual(run(h, op(h), "munchaholic stats " + target(p)), 3, "op: stats <player>");
		ServerPlayer empty = survivalPlayer(h);
		h.assertValueEqual(run(h, op(h), "munchaholic stats " + target(empty)), 0, "op: stats of a player without changes");
		TestSupport.Mock emptyMock = mockPlayer(h);
		run(h, emptyMock.player().createCommandSourceStack(), "munchaholic stats");
		var emptyChats = TestSupport.Mock.chats(emptyMock.drain());
		h.assertValueEqual(emptyChats.size(), 1, "only the 'none' line");
		h.assertTrue(containsKey(emptyChats.get(0).content(), "munchaholic.command.stats.none"), "none line");
		assertFails(h, server(h).createCommandSourceStack(), "munchaholic stats", "console (no player)");
		h.succeed();
	}

	/** A non-op may name themselves in stats (names are resolved without selector permission); naming anyone else fails. */
	@GameTest
	public void nonOpStatsByName(GameTestHelper h) throws CommandSyntaxException {
		defaults(h);
		TestSupport.Mock self = TestSupport.namedMockPlayer(h);
		TestSupport.Mock other = TestSupport.namedMockPlayer(h);
		ServerPlayer p = self.player();
		PlayerMunch.setStacks(p, PlayerStacks.EMPTY.withSteps(Caps.LUCK, 2).withSteps(Caps.ARMOR, 1));
		PlayerMunch.setStacks(other.player(), PlayerStacks.EMPTY.withSteps(Caps.LUCK, 5));
		CommandSourceStack source = p.createCommandSourceStack();
		h.assertTrue(!source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER),
				"the mock player is not an op");
		self.drain();
		h.assertValueEqual(run(h, source, "munchaholic stats " + p.getGameProfile().name()), 2, "stats <own name>");
		var chats = TestSupport.Mock.chats(self.drain());
		h.assertValueEqual(chats.size(), 3, "header + 2 lines");
		h.assertTrue(containsKey(chats.get(0).content(), "munchaholic.command.stats.header"), "header");
		assertFails(h, source, "munchaholic stats " + other.player().getGameProfile().name(), "non-op (other player)");
		h.assertTrue(TestSupport.Mock.chats(self.drain()).isEmpty(), "nothing about the other player was sent");
		h.succeed();
	}

	/** An op resetting themselves gets the command feedback only: one line, no separate "your changes were reset". */
	@GameTest
	public void opResetSelfOneLine(GameTestHelper h) throws CommandSyntaxException {
		defaults(h);
		TestSupport.Mock mock = mockPlayer(h);
		ServerPlayer p = mock.player();
		PlayerMunch.setStacks(p, PlayerStacks.EMPTY.withSteps(Caps.ARMOR, 2));
		mock.drain();
		CommandSourceStack source = p.createCommandSourceStack().withPermission(LevelBasedPermissionSet.GAMEMASTER);
		h.assertValueEqual(run(h, source, "munchaholic reset @s"), 1, "reset @s");
		h.assertTrue(PlayerMunch.stacks(p).isEmpty(), "reset");
		var chats = TestSupport.Mock.chats(mock.drain());
		h.assertValueEqual(chats.size(), 1, "exactly one line: " + chats);
		h.assertTrue(containsKey(chats.get(0).content(), "munchaholic.command.reset.single"), "the command feedback line");
		h.succeed();
	}

	@GameTest
	public void resetClearsModifiers(GameTestHelper h) throws CommandSyntaxException {
		defaults(h);
		TestSupport.Mock mock = mockPlayer(h);
		ServerPlayer p = mock.player();
		PlayerMunch.setStacks(p, new PlayerStacks(PlayerStacks.EMPTY.withSteps(Caps.SCALE, 2).withSteps(Caps.MAX_HEALTH, 3).steps(), 5));
		PlayerMunch.discover(p, "minecraft:carrot");
		mock.drain();
		h.assertValueEqual(run(h, op(h), "munchaholic reset " + target(p)), 1, "reset result = number of targets");
		h.assertValueEqual(PlayerMunch.stacks(p), PlayerStacks.EMPTY, "stacks reset (bites 0)");
		for (AttributeSpec spec : Caps.ALL) h.assertTrue(modifier(p, spec) == null, "modifier left on " + spec.key());
		h.assertTrue(Math.abs(p.getMaxHealth() - 20.0F) < 1e-6, "max health back to 20");
		h.assertTrue(PlayerMunch.discoveries(p).has("minecraft:carrot"), "reset keeps recipes");
		var chats = TestSupport.Mock.chats(mock.drain());
		h.assertTrue(chats.stream().anyMatch(c -> containsKey(c.content(), "munchaholic.message.reset")), "target told");
		h.succeed();
	}

	@GameTest
	public void resetRecipesOnly(GameTestHelper h) throws CommandSyntaxException {
		defaults(h);
		TestSupport.Mock mock = mockPlayer(h);
		ServerPlayer p = mock.player();
		PlayerStacks stacks = new PlayerStacks(PlayerStacks.EMPTY.withSteps(Caps.LUCK, 4).steps(), 3);
		PlayerMunch.setStacks(p, stacks);
		PlayerMunch.discover(p, "minecraft:carrot");
		PlayerMunch.discover(p, "minecraft:bread");
		mock.drain();
		h.assertValueEqual(run(h, op(h), "munchaholic reset " + target(p) + " recipes"), 1, "reset recipes result");
		h.assertValueEqual(PlayerMunch.discoveries(p), Discoveries.EMPTY, "recipes forgotten");
		h.assertValueEqual(PlayerMunch.stacks(p), stacks, "attribute changes kept");
		assertModifiersMatch(h, p, "after reset recipes");
		var chats = TestSupport.Mock.chats(mock.drain());
		h.assertTrue(chats.stream().anyMatch(c -> containsKey(c.content(), "munchaholic.message.recipesReset")), "target told");
		h.succeed();
	}

	@GameTest
	public void resetMultiple(GameTestHelper h) throws CommandSyntaxException {
		defaults(h);
		String tag = "munch_reset_" + UUID.randomUUID().toString().substring(0, 8);
		ServerPlayer a = survivalPlayer(h);
		ServerPlayer b = survivalPlayer(h);
		ServerPlayer untouched = survivalPlayer(h);
		for (ServerPlayer p : List.of(a, b, untouched)) PlayerMunch.setStacks(p, PlayerStacks.EMPTY.withSteps(Caps.ARMOR, 2));
		a.addTag(tag);
		b.addTag(tag);
		h.assertValueEqual(run(h, op(h), "munchaholic reset @a[tag=" + tag + "]"), 2, "two targets");
		h.assertTrue(PlayerMunch.stacks(a).isEmpty() && PlayerMunch.stacks(b).isEmpty(), "both reset");
		h.assertValueEqual(TestSupport.steps(untouched, Caps.ARMOR), 2, "a player outside the selector is untouched");
		h.succeed();
	}

	/** mode.dat codec: defaults for an empty tag (D12), unknown rollMode → RANDOM, and a lossless round trip. */
	@GameTest
	public void codecDefaultsAndRoundTrip(GameTestHelper h) {
		MunchaholicMode empty = MunchaholicMode.CODEC.parse(NbtOps.INSTANCE, new CompoundTag()).getOrThrow();
		h.assertTrue(!empty.enabled(), "empty tag: enabled false");
		h.assertValueEqual(empty.rollMode(), RollMode.RANDOM, "empty tag: RANDOM");
		h.assertTrue(empty.keepOnDeath(), "empty tag: keepOnDeath true");

		MunchaholicMode fresh = new MunchaholicMode();
		h.assertTrue(!fresh.enabled() && fresh.rollMode() == RollMode.RANDOM && fresh.keepOnDeath(), "new MunchaholicMode() defaults");

		CompoundTag weird = new CompoundTag();
		weird.putString("rollMode", "chaos");
		h.assertValueEqual(MunchaholicMode.CODEC.parse(NbtOps.INSTANCE, weird).getOrThrow().rollMode(), RollMode.RANDOM,
				"unknown rollMode");

		for (boolean enabled : new boolean[] {true, false}) {
			for (RollMode mode : RollMode.values()) {
				for (boolean keep : new boolean[] {true, false}) {
					CompoundTag tag = new CompoundTag();
					tag.putBoolean("enabled", enabled);
					tag.putString("rollMode", mode.id());
					tag.putBoolean("keepOnDeath", keep);
					MunchaholicMode state = MunchaholicMode.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow();
					String what = enabled + "/" + mode + "/" + keep;
					h.assertValueEqual(state.enabled(), enabled, "enabled " + what);
					h.assertValueEqual(state.rollMode(), mode, "rollMode " + what);
					h.assertValueEqual(state.keepOnDeath(), keep, "keepOnDeath " + what);
					Tag encoded = MunchaholicMode.CODEC.encodeStart(NbtOps.INSTANCE, state).getOrThrow();
					MunchaholicMode again = MunchaholicMode.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
					h.assertTrue(again.enabled() == enabled && again.rollMode() == mode && again.keepOnDeath() == keep,
							"round trip " + what);
				}
			}
		}
		MunchaholicMode dirty = new MunchaholicMode();
		dirty.setRollMode(RollMode.RECIPES);
		h.assertTrue(dirty.isDirty(), "setRollMode marks dirty on change");
		h.succeed();
	}

	/** The Create World handoff: pending values on the storage access are applied by onServerStarting, then cleared. */
	@GameTest
	public void pendingHandoff(GameTestHelper h) {
		defaults(h);
		MinecraftServer server = server(h);
		PendingWorldMode access = (PendingWorldMode) ((MinecraftServerAccessor) server).munchaholic$getStorageSource();
		try {
			h.assertTrue(access.munchaholic$takePendingMode() == null, "nothing pending on a running server");
			h.assertTrue(access.munchaholic$takePendingRollMode() == null, "no roll mode pending on a running server");

			access.munchaholic$setPendingMode(true);
			h.assertValueEqual(access.munchaholic$takePendingMode(), Boolean.TRUE, "take returns the value");
			h.assertTrue(access.munchaholic$takePendingMode() == null, "take clears the value");
			access.munchaholic$setPendingRollMode(RollMode.RECIPES);
			h.assertValueEqual(access.munchaholic$takePendingRollMode(), RollMode.RECIPES, "take returns the roll mode");
			h.assertTrue(access.munchaholic$takePendingRollMode() == null, "take clears the roll mode");

			// new world, mode ON + Recipes; keepOnDeath is forced to true (no button)
			MunchaholicMode.setEnabled(server, false);
			MunchaholicMode.setKeepOnDeath(server, false);
			access.munchaholic$setPendingMode(true);
			access.munchaholic$setPendingRollMode(RollMode.RECIPES);
			ModeBootstrap.onServerStarting(server);
			h.assertTrue(MunchaholicMode.isEnabled(server), "pending mode ON applied");
			h.assertValueEqual(MunchaholicMode.rollMode(server), RollMode.RECIPES, "pending Recipes applied");
			h.assertTrue(MunchaholicMode.keepOnDeath(server), "new world: keepOnDeath true");
			h.assertTrue(access.munchaholic$takePendingMode() == null && access.munchaholic$takePendingRollMode() == null,
					"both pending values consumed");

			// new world, mode OFF, no roll value (defensive): RANDOM
			access.munchaholic$setPendingMode(false);
			ModeBootstrap.onServerStarting(server);
			h.assertTrue(!MunchaholicMode.isEnabled(server), "pending mode OFF applied");
			h.assertValueEqual(MunchaholicMode.rollMode(server), RollMode.RANDOM, "no pending roll mode -> RANDOM");

			// existing world (nothing pending but a stray roll mode): the stored file is authoritative
			MunchaholicMode.setEnabled(server, true);
			MunchaholicMode.setRollMode(server, RollMode.RANDOM);
			MunchaholicMode.setKeepOnDeath(server, false);
			access.munchaholic$setPendingRollMode(RollMode.RECIPES);
			ModeBootstrap.onServerStarting(server);
			h.assertTrue(MunchaholicMode.isEnabled(server), "existing world: enabled kept");
			h.assertValueEqual(MunchaholicMode.rollMode(server), RollMode.RANDOM, "stray pending roll mode not applied");
			h.assertTrue(!MunchaholicMode.keepOnDeath(server), "existing world: keepOnDeath kept");
			h.assertTrue(access.munchaholic$takePendingRollMode() == null, "stray pending roll mode consumed anyway");
		} finally {
			access.munchaholic$takePendingMode();
			access.munchaholic$takePendingRollMode();
			defaults(h);
		}
		h.succeed();
	}

	@GameTest
	public void savedDataFileId(GameTestHelper h) {
		h.assertValueEqual(MunchaholicMode.TYPE.id().toString(), "munchaholic:mode", "saved data id");
		h.assertTrue(MunchaholicMode.get(server(h)) == MunchaholicMode.get(server(h)), "get returns the cached instance");
		h.succeed();
	}

	/**
	 * Save and reload: the settings written to data/munchaholic/mode.dat are read back by a fresh SavedDataStorage
	 * (what the next server start does). A storage over an empty folder has no settings (the ModeBootstrap "no file"
	 * branch then writes the OFF defaults).
	 */
	@GameTest
	public void settingsSurviveSaveAndReload(GameTestHelper h) throws IOException {
		defaults(h);
		MinecraftServer server = server(h);
		Path tmp = Files.createTempDirectory("munchaholic-mode");
		try {
			MunchaholicMode.setRollMode(server, RollMode.RECIPES);
			MunchaholicMode.setKeepOnDeath(server, false);
			server.getDataStorage().saveAndJoin();
			Path file = server.getWorldPath(LevelResource.DATA).resolve("munchaholic").resolve("mode.dat");
			h.assertTrue(Files.isRegularFile(file), "mode.dat written at " + file);

			CompoundTag root = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
			CompoundTag data = root.getCompoundOrEmpty("data");
			MunchaholicMode onDisk = MunchaholicMode.CODEC.parse(NbtOps.INSTANCE, data).getOrThrow();
			h.assertTrue(onDisk.enabled() && onDisk.rollMode() == RollMode.RECIPES && !onDisk.keepOnDeath(),
					"mode.dat content " + data);

			Files.createDirectories(tmp.resolve("munchaholic"));
			Files.copy(file, tmp.resolve("munchaholic").resolve("mode.dat"));
			try (SavedDataStorage reloaded = new SavedDataStorage(tmp, server.getFixerUpper(), server.registryAccess())) {
				MunchaholicMode loaded = reloaded.get(MunchaholicMode.TYPE);
				h.assertTrue(loaded != null, "reloaded storage has the settings");
				h.assertTrue(loaded.enabled() && loaded.rollMode() == RollMode.RECIPES && !loaded.keepOnDeath(),
						"reloaded settings");
			}
			Path emptyDir = Files.createDirectories(tmp.resolve("empty"));
			try (SavedDataStorage none = new SavedDataStorage(emptyDir, server.getFixerUpper(), server.registryAccess())) {
				h.assertTrue(none.get(MunchaholicMode.TYPE) == null, "no file -> no settings");
			}
		} finally {
			defaults(h);
			try (Stream<Path> walk = Files.walk(tmp)) {
				walk.sorted(Comparator.reverseOrder()).forEach(f -> f.toFile().delete());
			}
		}
		h.succeed();
	}

	/** Reading commands answer the caller with one line each (mode.status, keepOnDeath.status.on). */
	@GameTest
	public void commandFeedbackKeys(GameTestHelper h) throws CommandSyntaxException {
		defaults(h);
		TestSupport.Mock mock = mockPlayer(h);
		try {
			run(h, mock.player().createCommandSourceStack(), "munchaholic mode");
			var chats = TestSupport.Mock.chats(mock.drain());
			h.assertValueEqual(chats.size(), 1, "mode status: one line");
			h.assertTrue(containsKey(chats.get(0).content(), "munchaholic.command.mode.status"), "mode status key");
			run(h, mock.player().createCommandSourceStack(), "munchaholic keep-on-death");
			chats = TestSupport.Mock.chats(mock.drain());
			h.assertTrue(chats.size() == 1 && containsKey(chats.get(0).content(), "munchaholic.command.keepOnDeath.status.on"),
					"keep-on-death status key: " + chats);
		} finally {
			defaults(h);
		}
		h.succeed();
	}
}
