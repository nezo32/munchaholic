package dev.munchaholic.mode;

import net.minecraft.server.MinecraftServer;

/** ServerLifecycleEvents.SERVER_STARTING: initialize the world settings before levels load or anyone joins. */
public final class ModeBootstrap {
	private ModeBootstrap() {}

	/** ARCHITECTURE.md §3.3: pending Create World values, else the existing mode.dat, else stored defaults. */
	public static void onServerStarting(MinecraftServer server) {
		// TODO (Dev B): Phase-0 no-op so the server boots; implement §3.3.
	}
}
