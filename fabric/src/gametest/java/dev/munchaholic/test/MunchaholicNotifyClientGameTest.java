package dev.munchaholic.test;

import java.lang.reflect.Field;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.munchaholic.Feedback;
import dev.munchaholic.client.NotifyCommand;
import dev.munchaholic.client.NotifyConfig;
import dev.munchaholic.client.NotifySettingsScreen;
import dev.munchaholic.core.Caps;
import dev.munchaholic.core.Direction;
import dev.munchaholic.core.NotifySettings;
import dev.munchaholic.core.RollOutcome;
import dev.munchaholic.net.RolledPayload;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Client gametest for the notification settings (run with {@code ./gradlew runClientGameTest} under Xvfb).
 * <ol>
 * <li>The settings screen: each ON/OFF click updates {@link NotifyConfig} and writes {@code config/munchaholic.json}.</li>
 * <li>In a singleplayer world the {@code munchaholic:rolled} channel is negotiated, so the server takes the payload path.</li>
 * <li>With "Roll message" OFF the actionbar is left alone; with it ON the {@code ✦ Bread → …} message appears.</li>
 * <li>The client command {@code /munchaholic-notify sound|message [on|off]} changes and saves the same settings.</li>
 * </ol>
 */
public class MunchaholicNotifyClientGameTest implements FabricClientGameTest {
	private static final String SOUND = "munchaholic.settings.notifySound";
	private static final String MESSAGE = "munchaholic.settings.notifyMessage";
	private static final String SENTINEL = "sentinel";

	@Override
	public void runTest(ClientGameTestContext ctx) {
		NotifySettings original = NotifyConfig.get();
		try {
			settingsScreen(ctx);
			inWorld(ctx);
		} finally {
			NotifyConfig.set(original);
		}
		System.out.println("MUNCHAHOLIC_NOTIFY_CLIENT_TEST_OK");
	}

	private static void settingsScreen(ClientGameTestContext ctx) {
		NotifyConfig.set(NotifySettings.DEFAULT);
		ctx.setScreen(() -> new NotifySettingsScreen(new TitleScreen()));
		ctx.waitForScreen(NotifySettingsScreen.class);

		ctx.clickScreenButton(SOUND);
		if (NotifyConfig.get().sound()) throw new AssertionError("sound still ON in memory after click");
		expectSaved(new NotifySettings(false, true), "after sound click");
		ctx.clickScreenButton(MESSAGE);
		expectSaved(new NotifySettings(false, false), "after message click");
		ctx.clickScreenButton(SOUND);
		ctx.clickScreenButton(MESSAGE);
		expectSaved(new NotifySettings(true, true), "after clicking both again");

		ctx.takeScreenshot("notify_settings");
		ctx.clickScreenButton("gui.done");
		ctx.waitForScreen(TitleScreen.class);
	}

	private static void expectSaved(NotifySettings expected, String what) {
		if (!NotifyConfig.get().equals(expected)) throw new AssertionError(what + ": memory " + NotifyConfig.get());
		NotifySettings onDisk = NotifySettings.load(NotifyConfig.path());
		if (!onDisk.equals(expected)) throw new AssertionError(what + ": file " + onDisk + ", expected " + expected);
		if (!NotifyConfig.path().getFileName().toString().equals("munchaholic.json")) {
			throw new AssertionError("config file " + NotifyConfig.path());
		}
	}

	private static void inWorld(ClientGameTestContext ctx) {
		try (TestSingleplayerContext sp = ctx.worldBuilder().create()) {
			ctx.waitFor(mc -> mc.player != null, 20 * 60);
			// polled from the test thread: computeOnServer must not be called from inside a client-thread predicate
			boolean negotiated = false;
			for (int i = 0; i < 20 * 10 && !negotiated; i++) {
				negotiated = sp.getServer().computeOnServer(MunchaholicNotifyClientGameTest::canSend);
				if (!negotiated) ctx.waitTick();
			}
			if (!negotiated) throw new AssertionError("server cannot send munchaholic:rolled to the modded client");

			// message OFF: the actionbar is untouched
			NotifyConfig.set(new NotifySettings(true, false));
			setSentinel(ctx);
			sendFeedback(sp);
			ctx.waitTicks(5);
			String off = ctx.computeOnClient(MunchaholicNotifyClientGameTest::overlay);
			if (!SENTINEL.equals(off)) throw new AssertionError("message OFF but the overlay changed to: " + off);

			// message ON (defaults): the roll message appears
			NotifyConfig.set(NotifySettings.DEFAULT);
			setSentinel(ctx);
			sendFeedback(sp);
			ctx.waitFor(mc -> !SENTINEL.equals(overlay(mc)), 20 * 5);
			String on = ctx.computeOnClient(MunchaholicNotifyClientGameTest::overlay);
			if (!on.startsWith("✦") || !on.contains("Bread") || !on.contains("→") || !on.contains("Scale")) {
				throw new AssertionError("unexpected overlay: " + on);
			}

			// the client command
			NotifyConfig.set(NotifySettings.DEFAULT);
			clientCommand(ctx, NotifyCommand.ROOT + " message off");
			expectSaved(new NotifySettings(true, false), "/munchaholic-notify message off");
			clientCommand(ctx, NotifyCommand.ROOT + " sound off");
			expectSaved(new NotifySettings(false, false), "/munchaholic-notify sound off");
			clientCommand(ctx, NotifyCommand.ROOT + " sound");
			expectSaved(new NotifySettings(true, false), "/munchaholic-notify sound (toggle)");
			clientCommand(ctx, NotifyCommand.ROOT + " message on");
			expectSaved(NotifySettings.DEFAULT, "/munchaholic-notify message on");
			clientCommand(ctx, NotifyCommand.ROOT + " status");
			expectSaved(NotifySettings.DEFAULT, "/munchaholic-notify status changes nothing");
		}
	}

	private static void clientCommand(ClientGameTestContext ctx, String command) {
		int result = ctx.computeOnClient(mc -> {
			FabricClientCommandSource source = (FabricClientCommandSource) (Object) mc.getConnection().getSuggestionsProvider();
			try {
				return ClientCommands.getActiveDispatcher().execute(command, source);
			} catch (CommandSyntaxException e) {
				throw new AssertionError("/" + command + " failed", e);
			}
		});
		if (result != 1) throw new AssertionError("/" + command + " returned " + result);
	}

	private static boolean canSend(MinecraftServer s) {
		var players = s.getPlayerList().getPlayers();
		return !players.isEmpty() && ServerPlayNetworking.canSend(players.get(0), RolledPayload.TYPE);
	}

	private static void setSentinel(ClientGameTestContext ctx) {
		ctx.runOnClient(mc -> mc.gui.hud.setOverlayMessage(Component.literal(SENTINEL), false));
	}

	/** Scale +1 on bread, through the public Feedback.send (the server picks the payload path by itself). */
	private static void sendFeedback(TestSingleplayerContext sp) {
		sp.getServer().runOnServer(s -> Feedback.send(s.getPlayerList().getPlayers().get(0), new ItemStack(Items.BREAD),
				new RollOutcome.Applied(Caps.SCALE, Direction.UP, 0, 1, Caps.SCALE.playerBase())));
	}

	/** The private Hud.overlayMessageString (Mojang names at runtime on 26.x). */
	private static String overlay(Minecraft mc) {
		try {
			Field f = Hud.class.getDeclaredField("overlayMessageString");
			f.setAccessible(true);
			Component c = (Component) f.get(mc.gui.hud);
			return c == null ? null : c.getString();
		} catch (ReflectiveOperationException e) {
			throw new AssertionError("cannot read Hud.overlayMessageString", e);
		}
	}
}
