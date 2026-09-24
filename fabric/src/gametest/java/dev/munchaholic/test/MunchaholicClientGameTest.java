package dev.munchaholic.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import dev.munchaholic.client.CreateWorldModeHolder;
import dev.munchaholic.core.RollMode;
import dev.munchaholic.mode.MunchaholicMode;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Client gametest for the Create World screen (D17) and the handoff to the new world (D12). Not part of {@code build};
 * run with {@code ./gradlew runClientGameTest} under Xvfb.
 * <ol>
 * <li>The Game tab has Munchaholic Mode and Roll Mode right below Difficulty and above Allow Commands; defaults ON and
 *     Random; the toggle deactivates Roll Mode; clicking Roll Mode selects Recipes.</li>
 * <li>World 1 (cheats on): created with ON + Recipes → the server has enabled, RECIPES, keepOnDeath; mode.dat is on disk.</li>
 * <li>Cancel after changing the buttons leaks nothing into the next screen.</li>
 * <li>World 2: toggle OFF → enabled false, RANDOM, keepOnDeath true.</li>
 * <li>Re-open both worlds: the stored values are read back, nothing pending is applied.</li>
 * <li>ru_ru: the toggle reads «Режим Munchaholic: …»; screenshot.</li>
 * </ol>
 */
public class MunchaholicClientGameTest implements FabricClientGameTest {
	private static final String TOGGLE = "munchaholic.createWorld.toggle";
	private static final String ROLL = "munchaholic.createWorld.rollMode";
	private static final String DIFFICULTY = "options.difficulty";
	private static final String ALLOW_COMMANDS = "selectWorld.allowCommands";

	@Override
	public void runTest(ClientGameTestContext ctx) {
		// 1. layout and defaults
		openCreateWorld(ctx);
		int yDifficulty = widgetY(ctx, DIFFICULTY);
		int yToggle = widgetY(ctx, TOGGLE);
		int yRoll = widgetY(ctx, ROLL);
		int yAllow = widgetY(ctx, ALLOW_COMMANDS);
		if (!(yDifficulty < yToggle && yToggle < yRoll && yRoll < yAllow)) {
			throw new AssertionError("widget order: difficulty=" + yDifficulty + " toggle=" + yToggle + " roll=" + yRoll
					+ " allowCommands=" + yAllow);
		}
		if (!uiMode(ctx)) throw new AssertionError("Munchaholic Mode does not default to ON");
		if (uiRoll(ctx) != RollMode.RANDOM) throw new AssertionError("Roll Mode does not default to Random: " + uiRoll(ctx));
		if (!widgetActive(ctx, ROLL)) throw new AssertionError("Roll Mode inactive while the mode is ON");
		ctx.takeScreenshot("create_world_game_tab");

		ctx.clickScreenButton(TOGGLE);
		if (uiMode(ctx)) throw new AssertionError("toggle click did not turn the mode OFF");
		if (widgetActive(ctx, ROLL)) throw new AssertionError("Roll Mode still active while the mode is OFF");
		ctx.clickScreenButton(TOGGLE);
		if (!uiMode(ctx)) throw new AssertionError("second toggle click did not turn the mode ON");
		if (!widgetActive(ctx, ROLL)) throw new AssertionError("Roll Mode not re-activated");

		ctx.clickScreenButton(ROLL);
		if (uiRoll(ctx) != RollMode.RECIPES) throw new AssertionError("Roll Mode click did not select Recipes: " + uiRoll(ctx));
		String rollText = widgetText(ctx, ROLL);
		ctx.takeScreenshot("create_world_game_tab_recipes");

		// 2. world 1: ON + Recipes, cheats on
		ctx.runOnClient(mc -> ((CreateWorldScreen) mc.gui.screen()).getUiState().setAllowCommands(true));
		Path world1 = createWorld(ctx);
		assertSettings(ctx, true, RollMode.RECIPES, true, "world 1 after create");
		if (!Files.isRegularFile(world1.resolve("data/munchaholic/mode.dat"))) {
			throw new AssertionError("mode.dat not written right after creating " + world1);
		}
		CompoundTag saved = savedData(world1);
		if (!saved.getBooleanOr("enabled", false) || !"recipes".equals(saved.getStringOr("rollMode", ""))) {
			throw new AssertionError("mode.dat content of world 1: " + saved);
		}
		leaveWorld(ctx);

		// 3. cancel leaks nothing
		ctx.runOnClient(mc -> CreateWorldScreen.openFresh(mc, () -> mc.gui.setScreen(new TitleScreen())));
		ctx.waitForScreen(CreateWorldScreen.class);
		ctx.clickScreenButton(ROLL);
		ctx.clickScreenButton(TOGGLE);
		ctx.clickScreenButton("gui.cancel");
		ctx.waitForScreen(TitleScreen.class);

		// 4. world 2: toggle OFF
		openCreateWorld(ctx);
		if (!uiMode(ctx) || uiRoll(ctx) != RollMode.RANDOM) {
			throw new AssertionError("fresh screen after cancel: mode=" + uiMode(ctx) + " roll=" + uiRoll(ctx));
		}
		ctx.clickScreenButton(TOGGLE);
		Path world2 = createWorld(ctx);
		assertSettings(ctx, false, RollMode.RANDOM, true, "world 2 after create");
		if (world1.equals(world2)) throw new AssertionError("same world folder twice: " + world1);
		leaveWorld(ctx);

		// 5. re-open: stored values win, nothing pending
		joinWorld(ctx, world1);
		assertSettings(ctx, true, RollMode.RECIPES, true, "world 1 re-opened");
		leaveWorld(ctx);
		joinWorld(ctx, world2);
		assertSettings(ctx, false, RollMode.RANDOM, true, "world 2 re-opened");
		leaveWorld(ctx);

		// 6. ru_ru
		setLanguage(ctx, "ru_ru");
		ctx.runOnClient(mc -> CreateWorldScreen.openFresh(mc, () -> mc.gui.setScreen(new TitleScreen())));
		ctx.waitForScreen(CreateWorldScreen.class);
		String ruToggle = widgetText(ctx, TOGGLE);
		if (!ruToggle.startsWith("Режим Munchaholic")) throw new AssertionError("ru toggle text: " + ruToggle);
		String ruRoll = widgetText(ctx, ROLL);
		ctx.takeScreenshot("create_world_game_tab_ru");
		ctx.clickScreenButton("gui.cancel");
		ctx.waitForScreen(TitleScreen.class);
		setLanguage(ctx, "en_us");

		ctx.setScreen(TitleScreen::new);
		System.out.println("MUNCHAHOLIC_CLIENT_TEST_OK world1=" + world1.getFileName() + " world2=" + world2.getFileName()
				+ " roll=\"" + rollText + "\" ruToggle=\"" + ruToggle + "\" ruRoll=\"" + ruRoll + "\"");
	}

	// ---- Create World widgets ----

	/** The first widget on the current screen whose message contains a translatable with {@code key}. */
	private static AbstractWidget widget(Minecraft mc, String key) {
		Screen screen = mc.gui.screen();
		return screen.children().stream()
				.filter(c -> c instanceof AbstractWidget w && TestSupport.containsKey(w.getMessage(), key))
				.map(c -> (AbstractWidget) c)
				.findFirst()
				.orElseThrow(() -> new AssertionError("no widget with " + key + " on " + screen));
	}

	private static int widgetY(ClientGameTestContext ctx, String key) {
		return ctx.computeOnClient(mc -> widget(mc, key).getY());
	}

	private static boolean widgetActive(ClientGameTestContext ctx, String key) {
		return ctx.computeOnClient(mc -> widget(mc, key).active);
	}

	private static String widgetText(ClientGameTestContext ctx, String key) {
		return ctx.computeOnClient(mc -> widget(mc, key).getMessage().getString());
	}

	private static boolean uiMode(ClientGameTestContext ctx) {
		return ctx.computeOnClient(mc -> ((CreateWorldModeHolder) mc.gui.screen()).munchaholic$isModeEnabled());
	}

	private static RollMode uiRoll(ClientGameTestContext ctx) {
		RollMode holder = ctx.computeOnClient(mc -> ((CreateWorldModeHolder) mc.gui.screen()).munchaholic$getRollMode());
		Object button = ctx.computeOnClient(mc -> widget(mc, ROLL) instanceof CycleButton<?> b ? b.getValue() : null);
		if (button != null && button != holder) throw new AssertionError("Roll Mode button shows " + button + ", holder " + holder);
		return holder;
	}

	// ---- worlds ----

	private static void openCreateWorld(ClientGameTestContext ctx) {
		ctx.runOnClient(mc -> CreateWorldScreen.openFresh(mc, () -> {}));
		ctx.waitForScreen(CreateWorldScreen.class);
	}

	private static Path createWorld(ClientGameTestContext ctx) {
		ctx.clickScreenButton("selectWorld.create");
		return waitInWorld(ctx);
	}

	private static Path waitInWorld(ClientGameTestContext ctx) {
		ctx.waitFor(mc -> mc.getSingleplayerServer() != null && mc.player != null, 20 * 60);
		return ctx.computeOnClient(mc -> mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize());
	}

	// not server.submit(..).join(): the client-gametest framework holds the server thread while the test thread runs
	private static void assertSettings(ClientGameTestContext ctx, boolean enabled, RollMode mode, boolean keep, String what) {
		String actual = ctx.computeOnClient(mc -> {
			MinecraftServer s = mc.getSingleplayerServer();
			return MunchaholicMode.isEnabled(s) + "/" + MunchaholicMode.rollMode(s) + "/" + MunchaholicMode.keepOnDeath(s);
		});
		String expected = enabled + "/" + mode + "/" + keep;
		if (!expected.equals(actual)) throw new AssertionError(what + ": settings " + actual + ", expected " + expected);
	}

	private static CompoundTag savedData(Path world) {
		try {
			CompoundTag root = NbtIo.readCompressed(world.resolve("data/munchaholic/mode.dat"), NbtAccounter.unlimitedHeap());
			return root.getCompoundOrEmpty("data");
		} catch (IOException e) {
			throw new AssertionError("cannot read mode.dat of " + world, e);
		}
	}

	/** Leaves the world; otherwise the client-gametest framework fails ("finished while a server is still running"). */
	private static void leaveWorld(ClientGameTestContext ctx) {
		ctx.runOnClient(mc -> {
			mc.level.disconnect(Component.translatable("menu.savingLevel"));
			mc.disconnect(new GenericMessageScreen(Component.translatable("menu.savingLevel")), false);
		});
		ctx.waitFor(mc -> mc.level == null && mc.getSingleplayerServer() == null, 20 * 60);
		ctx.setScreen(TitleScreen::new);
		ctx.waitTicks(20);
	}

	private static void openWorldList(ClientGameTestContext ctx) {
		ctx.setScreen(() -> new SelectWorldScreen(new TitleScreen()));
		ctx.waitFor(mc -> worldList(mc).map(l -> l.children().stream()
				.anyMatch(e -> e instanceof WorldSelectionList.WorldListEntry)).orElse(false), 20 * 30);
	}

	private static void joinWorld(ClientGameTestContext ctx, Path world) {
		openWorldList(ctx);
		ctx.runOnClient(mc -> worldEntry(mc, world).joinWorld());
		Path joined = waitInWorld(ctx);
		if (!joined.equals(world)) throw new AssertionError("joined " + joined + " instead of " + world);
	}

	private static Optional<WorldSelectionList> worldList(Minecraft mc) {
		if (!(mc.gui.screen() instanceof SelectWorldScreen screen)) return Optional.empty();
		return screen.children().stream().filter(c -> c instanceof WorldSelectionList).map(c -> (WorldSelectionList) c).findFirst();
	}

	private static WorldSelectionList.WorldListEntry worldEntry(Minecraft mc, Path world) {
		String id = world.getFileName().toString();
		return worldList(mc).orElseThrow().children().stream()
				.filter(e -> e instanceof WorldSelectionList.WorldListEntry)
				.map(e -> (WorldSelectionList.WorldListEntry) e)
				.filter(e -> e.getLevelSummary().getLevelId().equals(id))
				.findFirst().orElseThrow(() -> new AssertionError("world " + id + " not in the world list"));
	}

	/** Same as picking a language in Options → Language, minus saving options.txt. */
	private static void setLanguage(ClientGameTestContext ctx, String code) {
		AtomicReference<CompletableFuture<Void>> reload = new AtomicReference<>();
		ctx.runOnClient(mc -> {
			mc.getLanguageManager().setSelected(code);
			mc.options.languageCode = code;
			reload.set(mc.reloadResourcePacks());
		});
		ctx.waitFor(mc -> reload.get().isDone() && mc.gui.overlay() == null, 20 * 120);
		String selected = ctx.computeOnClient(mc -> mc.getLanguageManager().getSelected());
		if (!code.equals(selected)) throw new AssertionError("language is " + selected + ", expected " + code);
	}
}
