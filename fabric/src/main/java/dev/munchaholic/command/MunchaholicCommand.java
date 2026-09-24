package dev.munchaholic.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.mojang.brigadier.CommandDispatcher;
import dev.munchaholic.Texts;
import dev.munchaholic.core.AttributeSpec;
import dev.munchaholic.core.BaseLookup;
import dev.munchaholic.core.Caps;
import dev.munchaholic.core.PlayerStacks;
import dev.munchaholic.core.RollMode;
import dev.munchaholic.mode.MunchaholicMode;
import dev.munchaholic.player.PlayerMunch;
import dev.munchaholic.player.RecipeSync;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /munchaholic}: reading (status, mode, keep-on-death, own stats) is open to everyone; changing settings,
 * other players' stats and reset need gamemaster permission (ARCHITECTURE.md §5.3). The root has no requirement,
 * so each changing node carries its own {@code requires}.
 */
public final class MunchaholicCommand {
	public static final String ROOT = "munchaholic";

	private MunchaholicCommand() {}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal(ROOT)
				.executes(c -> status(c.getSource()))
				.then(Commands.literal("status").executes(c -> status(c.getSource())))
				.then(Commands.literal("on").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.executes(c -> setEnabled(c.getSource(), true)))
				.then(Commands.literal("off").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.executes(c -> setEnabled(c.getSource(), false)))
				.then(Commands.literal("mode")
						.executes(c -> modeStatus(c.getSource()))
						.then(Commands.literal(RollMode.RANDOM.id()).requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
								.executes(c -> setRollMode(c.getSource(), RollMode.RANDOM)))
						.then(Commands.literal(RollMode.RECIPES.id()).requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
								.executes(c -> setRollMode(c.getSource(), RollMode.RECIPES))))
				.then(Commands.literal("keep-on-death")
						.executes(c -> keepOnDeathStatus(c.getSource()))
						.then(Commands.literal("on").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
								.executes(c -> setKeepOnDeath(c.getSource(), true)))
						.then(Commands.literal("off").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
								.executes(c -> setKeepOnDeath(c.getSource(), false))))
				.then(Commands.literal("stats")
						.executes(c -> stats(c.getSource(), c.getSource().getPlayerOrException()))
						.then(Commands.argument("player", EntityArgument.player())
								.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
								.executes(c -> stats(c.getSource(), EntityArgument.getPlayer(c, "player")))))
				.then(Commands.literal("reset").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(Commands.argument("targets", EntityArgument.players())
								.executes(c -> reset(c.getSource(), EntityArgument.getPlayers(c, "targets")))
								.then(Commands.literal("recipes")
										.executes(c -> resetRecipes(c.getSource(), EntityArgument.getPlayers(c, "targets")))))));
	}

	/** Three lines: mode on/off, roll mode, keep on death. Returns 1 if the mode is on. */
	private static int status(CommandSourceStack source) {
		boolean on = MunchaholicMode.isEnabled(source.getServer());
		source.sendSuccess(() -> on
				? Component.translatableWithFallback("munchaholic.command.status.on", "Munchaholic Mode is ON in this world")
				: Component.translatableWithFallback("munchaholic.command.status.off", "Munchaholic Mode is OFF in this world"), false);
		modeStatus(source);
		keepOnDeathStatus(source);
		return on ? 1 : 0;
	}

	private static int setEnabled(CommandSourceStack source, boolean value) {
		MunchaholicMode.setEnabled(source.getServer(), value);
		RecipeSync.sendAll(source.getServer()); // tooltips follow recipesActive
		source.sendSuccess(() -> value
				? Component.translatableWithFallback("munchaholic.command.on", "Munchaholic Mode is now ON for this world")
				: Component.translatableWithFallback("munchaholic.command.off", "Munchaholic Mode is now OFF for this world"), true);
		return value ? 1 : 0;
	}

	private static int modeStatus(CommandSourceStack source) {
		RollMode mode = MunchaholicMode.rollMode(source.getServer());
		source.sendSuccess(() -> Component.translatableWithFallback("munchaholic.command.mode.status", "Roll Mode is %s in this world",
				Texts.rollMode(mode)), false);
		return mode.ordinal();
	}

	private static int setRollMode(CommandSourceStack source, RollMode mode) {
		MunchaholicMode.setRollMode(source.getServer(), mode);
		RecipeSync.sendAll(source.getServer());
		source.sendSuccess(() -> Component.translatableWithFallback("munchaholic.command.mode.set", "Roll Mode is now %s for this world",
				Texts.rollMode(mode)), true);
		return mode.ordinal();
	}

	private static int keepOnDeathStatus(CommandSourceStack source) {
		boolean keep = MunchaholicMode.keepOnDeath(source.getServer());
		source.sendSuccess(() -> keep
				? Component.translatableWithFallback("munchaholic.command.keepOnDeath.status.on", "Attribute changes are kept on death in this world")
				: Component.translatableWithFallback("munchaholic.command.keepOnDeath.status.off", "Attribute changes are lost on death in this world"), false);
		return keep ? 1 : 0;
	}

	private static int setKeepOnDeath(CommandSourceStack source, boolean value) {
		MunchaholicMode.setKeepOnDeath(source.getServer(), value);
		source.sendSuccess(() -> value
				? Component.translatableWithFallback("munchaholic.command.keepOnDeath.on", "Attribute changes are now kept on death in this world")
				: Component.translatableWithFallback("munchaholic.command.keepOnDeath.off", "Attribute changes are now lost on death in this world"), true);
		return value ? 1 : 0;
	}

	/** Header plus one line per changed attribute (Caps.ALL order), or a single "none" line. Returns the line count. */
	private static int stats(CommandSourceStack source, ServerPlayer player) {
		PlayerStacks stacks = PlayerMunch.stacks(player);
		BaseLookup bases = PlayerMunch.bases(player);
		List<Component> lines = new ArrayList<>();
		for (AttributeSpec spec : Caps.ALL) {
			int steps = stacks.steps(spec);
			if (steps == 0) continue;
			lines.add(Component.translatableWithFallback("munchaholic.command.stats.line", "• %1$s %2$s %3$s",
					Texts.attributeName(spec), Texts.change(spec, steps), Texts.now(spec, spec.displayValue(bases.base(spec), steps))));
		}
		Component name = player.getDisplayName();
		if (lines.isEmpty()) {
			source.sendSuccess(() -> Component.translatableWithFallback("munchaholic.command.stats.none", "%s has no attribute changes yet",
					name), false);
			return 0;
		}
		int bites = stacks.bites();
		int discovered = PlayerMunch.discoveries(player).size();
		source.sendSuccess(() -> Component.translatableWithFallback("munchaholic.command.stats.header",
				"%1$s — bites eaten: %2$s, recipes discovered: %3$s", name, bites, discovered), false);
		for (Component line : lines) {
			source.sendSuccess(() -> line, false);
		}
		return lines.size();
	}

	private static int reset(CommandSourceStack source, Collection<ServerPlayer> targets) {
		for (ServerPlayer player : targets) {
			PlayerMunch.reset(player);
			player.sendSystemMessage(Component.translatableWithFallback("munchaholic.message.reset",
					"Your Munchaholic attribute changes were reset"));
		}
		if (targets.size() == 1) {
			Component name = targets.iterator().next().getDisplayName();
			source.sendSuccess(() -> Component.translatableWithFallback("munchaholic.command.reset.single",
					"Reset the attribute changes of %s", name), true);
		} else {
			int count = targets.size();
			source.sendSuccess(() -> Component.translatableWithFallback("munchaholic.command.reset.multiple",
					"Reset the attribute changes of %s players", count), true);
		}
		return targets.size();
	}

	/** Forgets discovered recipes only; attribute changes stay. forgetRecipes syncs the (now empty) tooltips. */
	private static int resetRecipes(CommandSourceStack source, Collection<ServerPlayer> targets) {
		for (ServerPlayer player : targets) {
			PlayerMunch.forgetRecipes(player);
			player.sendSystemMessage(Component.translatableWithFallback("munchaholic.message.recipesReset",
					"Your discovered Munchaholic recipes were forgotten"));
		}
		if (targets.size() == 1) {
			Component name = targets.iterator().next().getDisplayName();
			source.sendSuccess(() -> Component.translatableWithFallback("munchaholic.command.reset.recipes.single",
					"%s forgot all discovered recipes", name), true);
		} else {
			int count = targets.size();
			source.sendSuccess(() -> Component.translatableWithFallback("munchaholic.command.reset.recipes.multiple",
					"%s players forgot all discovered recipes", count), true);
		}
		return targets.size();
	}
}
