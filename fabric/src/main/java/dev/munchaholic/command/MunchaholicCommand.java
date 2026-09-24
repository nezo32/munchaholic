package dev.munchaholic.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

/**
 * {@code /munchaholic}: reading (status, mode, keep-on-death, own stats) is open to everyone; changing settings,
 * other players' stats and reset need gamemaster permission (ARCHITECTURE.md §5.3).
 */
public final class MunchaholicCommand {
	public static final String ROOT = "munchaholic";

	private MunchaholicCommand() {}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		// TODO (Dev B): Phase-0 no-op so the server boots; register the tree in §5.3.
	}
}
