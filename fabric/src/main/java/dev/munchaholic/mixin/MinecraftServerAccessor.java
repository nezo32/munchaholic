package dev.munchaholic.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes the server's {@code storageSource}, which carries the pending Create World choices. */
@Mixin(MinecraftServer.class)
public interface MinecraftServerAccessor {
	@Accessor("storageSource")
	LevelStorageSource.LevelStorageAccess munchaholic$getStorageSource();
}
