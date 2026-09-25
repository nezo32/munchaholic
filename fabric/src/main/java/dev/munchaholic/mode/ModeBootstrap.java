package dev.munchaholic.mode;

import java.nio.file.Files;
import java.nio.file.Path;

import dev.munchaholic.Munchaholic;
import dev.munchaholic.core.RollMode;
import dev.munchaholic.mixin.MinecraftServerAccessor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/** ServerLifecycleEvents.SERVER_STARTING: initialize the world settings before levels load or anyone joins. */
public final class ModeBootstrap {
	private ModeBootstrap() {}

	/** ARCHITECTURE.md §3.3: pending Create World values, else the existing mode.dat, else stored defaults. */
	public static void onServerStarting(MinecraftServer server) {
		// 1. new world from the Create World screen: the button values ride on this world's storage access
		PendingWorldMode access = (PendingWorldMode) ((MinecraftServerAccessor) server).munchaholic$getStorageSource();
		Boolean pending = access.munchaholic$takePendingMode();
		RollMode pendingRoll = access.munchaholic$takePendingRollMode(); // always taken: never outlives this start
		if (pending != null) {
			RollMode rollMode = pendingRoll != null ? pendingRoll : RollMode.RANDOM;
			MunchaholicMode mode = MunchaholicMode.get(server);
			mode.setEnabled(pending);
			mode.setRollMode(rollMode);
			mode.setKeepOnDeath(true); // no Create World button: always the default
			mode.setDirty();
			server.getDataStorage().scheduleSave(); // persist now: a crash before the first autosave must not lose the choice
			Munchaholic.LOGGER.info("Munchaholic Mode {}, Roll Mode {} for new world", pending ? "ON" : "OFF", rollMode.id());
			return;
		}
		// 2. existing world that already has mode.dat: it is authoritative
		if (server.getDataStorage().get(MunchaholicMode.TYPE) != null) {
			MunchaholicMode mode = MunchaholicMode.get(server);
			Munchaholic.LOGGER.debug("Munchaholic settings loaded: enabled {}, Roll Mode {}, keep on death {}",
					mode.enabled(), mode.rollMode().id(), mode.keepOnDeath());
			return;
		}
		// 3. no mode.dat yet (dedicated server, world made without the mod, other launcher): store OFF so installing
		//    the mod never silently changes an existing world. Always written, so this runs once per world.
		Path file = server.getWorldPath(LevelResource.ROOT).resolve("data").resolve(Munchaholic.MOD_ID).resolve("mode.dat");
		if (Files.exists(file)) {
			// vanilla already logged the read error and cached "absent"; the file is replaced on the next save
			Munchaholic.LOGGER.warn("Unreadable Munchaholic mode.dat; resetting the Munchaholic settings to the defaults (OFF)");
		}
		MunchaholicMode.get(server).setDirty(); // computeIfAbsent stored the defaults: OFF, Random, keep on death
		server.getDataStorage().scheduleSave();
		Munchaholic.LOGGER.info("No Munchaholic settings in this world: Munchaholic Mode is OFF. Operators can turn it on with /munchaholic on");
	}
}
