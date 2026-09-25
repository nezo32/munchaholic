package dev.munchaholic.test;

import static dev.munchaholic.test.TestSupport.EPS;
import static dev.munchaholic.test.TestSupport.assertModifiersMatch;
import static dev.munchaholic.test.TestSupport.containsKey;
import static dev.munchaholic.test.TestSupport.defaults;
import static dev.munchaholic.test.TestSupport.mockPlayer;
import static dev.munchaholic.test.TestSupport.modifier;
import static dev.munchaholic.test.TestSupport.modifierAmount;
import static dev.munchaholic.test.TestSupport.server;
import static dev.munchaholic.test.TestSupport.survivalPlayer;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import dev.munchaholic.core.AttributeSpec;
import dev.munchaholic.core.Caps;
import dev.munchaholic.core.Discoveries;
import dev.munchaholic.core.PlayerStacks;
import dev.munchaholic.mode.MunchaholicMode;
import dev.munchaholic.net.RecipesPayload;
import dev.munchaholic.player.MunchAttachments;
import dev.munchaholic.player.PlayerMunch;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

/**
 * Death, End exit and save/load (D10/D11): stacks follow the keep-on-death setting, discoveries always survive, and
 * modifiers plus health are restored after a respawn.
 */
public class MunchDeathGameTests {
	/** +2 max health (one step = +2) and +2 scale steps, 5 bites, carrot discovered. */
	private static final PlayerStacks BUILD = PlayerStacks.EMPTY
			.withSteps(Caps.MAX_HEALTH, 1)
			.withSteps(Caps.SCALE, 2)
			.withSteps(Caps.LUCK, -3);

	private static ServerPlayer prepared(GameTestHelper h, ServerPlayer p) {
		PlayerMunch.setStacks(p, new PlayerStacks(BUILD.steps(), 5));
		PlayerMunch.discover(p, "minecraft:carrot");
		return p;
	}

	private static ServerPlayer die(GameTestHelper h, ServerPlayer p) {
		p.kill(h.getLevel());
		return server(h).getPlayerList().respawn(p, false, Entity.RemovalReason.KILLED);
	}

	@GameTest
	public void keepOnDeathKeepsChanges(GameTestHelper h) {
		defaults(h);
		ServerPlayer p = prepared(h, survivalPlayer(h));
		ServerPlayer np = die(h, p);
		h.assertTrue(np != p, "respawn made a new player entity");
		h.assertValueEqual(PlayerMunch.stacks(np), new PlayerStacks(BUILD.steps(), 5), "stacks after respawn");
		assertModifiersMatch(h, np, "after respawn");
		h.assertTrue(Math.abs(modifierAmount(np, Caps.MAX_HEALTH) - 2.0) < EPS, "max_health modifier");
		h.assertTrue(Math.abs(np.getMaxHealth() - 22.0F) < EPS, "max health " + np.getMaxHealth());
		h.assertTrue(Math.abs(np.getHealth() - np.getMaxHealth()) < EPS,
				"health " + np.getHealth() + " != max " + np.getMaxHealth() + " (setHealth after reapplying)");
		h.assertTrue(PlayerMunch.discoveries(np).has("minecraft:carrot"), "discoveries kept");
		h.succeed();
	}

	@GameTest
	public void keepOnDeathOffLosesChangesKeepsRecipes(GameTestHelper h) {
		defaults(h);
		TestSupport.Mock mock = mockPlayer(h);
		ServerPlayer p = prepared(h, mock.player());
		try {
			MunchaholicMode.setKeepOnDeath(server(h), false);
			mock.drain();
			ServerPlayer np = die(h, p);
			h.assertValueEqual(PlayerMunch.stacks(np), PlayerStacks.EMPTY, "stacks wiped (bites 0 too)");
			h.assertValueEqual(PlayerMunch.stacks(np).bites(), 0, "bites");
			for (AttributeSpec spec : Caps.ALL) {
				h.assertTrue(modifier(np, spec) == null, "modifier left on " + spec.key());
			}
			h.assertTrue(Math.abs(np.getMaxHealth() - 20.0F) < EPS, "max health back to 20: " + np.getMaxHealth());
			h.assertValueEqual(PlayerMunch.discoveries(np), new Discoveries(Set.of("minecraft:carrot")), "discoveries kept");
			List<Object> out = mock.drain();
			h.assertTrue(TestSupport.Mock.chats(out).stream().anyMatch(c -> containsKey(c.content(), "munchaholic.message.lostOnDeath")),
					"lostOnDeath notice; outbound=" + out);
		} finally {
			defaults(h);
		}
		h.succeed();
	}

	/** Without changes nobody gets the "you lost" notice. */
	@GameTest
	public void keepOnDeathOffNoNoticeWithoutChanges(GameTestHelper h) {
		defaults(h);
		TestSupport.Mock mock = mockPlayer(h);
		try {
			MunchaholicMode.setKeepOnDeath(server(h), false);
			mock.drain();
			die(h, mock.player());
			List<Object> out = mock.drain();
			h.assertTrue(TestSupport.Mock.chats(out).stream().noneMatch(c -> containsKey(c.content(), "munchaholic.message.lostOnDeath")),
					"unexpected lostOnDeath notice; outbound=" + out);
		} finally {
			defaults(h);
		}
		h.succeed();
	}

	/** AFTER_RESPAWN resyncs recipes only to clients that have the channel: a vanilla client gets no payload (and no crash). */
	@GameTest
	public void respawnDoesNotSendRecipesToVanillaClient(GameTestHelper h) {
		defaults(h);
		TestSupport.Mock mock = mockPlayer(h);
		prepared(h, mock.player());
		mock.drain();
		die(h, mock.player());
		List<Object> out = mock.drain();
		h.assertTrue(TestSupport.Mock.payloads(out, RecipesPayload.class).isEmpty(), "RecipesPayload sent to a vanilla client");
		h.succeed();
	}

	/** Leaving the End (alive == true): vanilla copies permanent modifiers and Fabric copies attachments; keep on death is irrelevant. */
	@GameTest
	public void endExitKeepsEverything(GameTestHelper h) {
		defaults(h);
		ServerPlayer p = prepared(h, survivalPlayer(h));
		try {
			MunchaholicMode.setKeepOnDeath(server(h), false);
			ServerPlayer np = server(h).getPlayerList().respawn(p, true, Entity.RemovalReason.CHANGED_DIMENSION);
			h.assertValueEqual(PlayerMunch.stacks(np), new PlayerStacks(BUILD.steps(), 5), "stacks after End exit");
			assertModifiersMatch(h, np, "after End exit");
			h.assertTrue(PlayerMunch.discoveries(np).has("minecraft:carrot"), "discoveries after End exit");
		} finally {
			defaults(h);
		}
		h.succeed();
	}

	/** NBT round trip (mc-hooks §10): the attachments and the modifiers survive a save and load. */
	@GameTest
	public void survivesSaveLoad(GameTestHelper h) {
		ServerLevel level = h.getLevel();
		ServerPlayer p = prepared(h, survivalPlayer(h));
		TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
		p.saveWithoutId(out);
		CompoundTag tag = out.buildResult();
		// a different UUID: a ServerPlayer with the same profile would take over the live player's PlayerAdvancements
		GameProfile profile = new GameProfile(UUID.randomUUID(), "munch-reloaded");
		ServerPlayer fresh = new ServerPlayer(level.getServer(), level, profile, ClientInformation.createDefault());
		fresh.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
		h.assertValueEqual(fresh.getAttachedOrElse(MunchAttachments.STACKS, PlayerStacks.EMPTY),
				new PlayerStacks(BUILD.steps(), 5), "stacks after load");
		h.assertValueEqual(fresh.getAttachedOrElse(MunchAttachments.DISCOVERIES, Discoveries.EMPTY),
				new Discoveries(Set.of("minecraft:carrot")), "discoveries after load");
		assertModifiersMatch(h, fresh, "after load");
		h.succeed();
	}

	/** Attachment codecs: steps map + bites, discoveries encoded sorted; zero entries are never stored. */
	@GameTest
	public void attachmentCodecsRoundTrip(GameTestHelper h) {
		var ops = NbtOps.INSTANCE;
		PlayerStacks stacks = new PlayerStacks(BUILD.steps(), 7);
		var encoded = MunchAttachments.STACKS_CODEC.encodeStart(ops, stacks).getOrThrow();
		h.assertValueEqual(MunchAttachments.STACKS_CODEC.parse(ops, encoded).getOrThrow(), stacks, "stacks round trip");
		Discoveries d = new Discoveries(Set.of("minecraft:carrot", "minecraft:apple", "minecraft:cake"));
		var dEncoded = MunchAttachments.DISCOVERIES_CODEC.encodeStart(JsonOps.INSTANCE, d).getOrThrow();
		h.assertValueEqual(MunchAttachments.DISCOVERIES_CODEC.parse(JsonOps.INSTANCE, dEncoded).getOrThrow(), d, "discoveries round trip");
		h.assertValueEqual(dEncoded.toString(), "[\"minecraft:apple\",\"minecraft:cake\",\"minecraft:carrot\"]", "sorted on encode");
		h.succeed();
	}
}
