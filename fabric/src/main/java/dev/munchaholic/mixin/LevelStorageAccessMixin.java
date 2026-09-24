package dev.munchaholic.mixin;

import dev.munchaholic.core.RollMode;
import dev.munchaholic.mode.PendingWorldMode;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Implements {@link PendingWorldMode} on the storage access of one world directory. Volatile: the
 * client thread writes the values (Create World), the integrated server thread reads them (SERVER_STARTING).
 */
@Mixin(LevelStorageSource.LevelStorageAccess.class)
public abstract class LevelStorageAccessMixin implements PendingWorldMode {
	@Unique
	private volatile Boolean munchaholic$pendingMode;

	@Unique
	private volatile RollMode munchaholic$pendingRollMode;

	@Override
	public void munchaholic$setPendingMode(boolean enabled) {
		munchaholic$pendingMode = enabled;
	}

	@Override
	public Boolean munchaholic$takePendingMode() {
		Boolean value = munchaholic$pendingMode;
		munchaholic$pendingMode = null;
		return value;
	}

	@Override
	public void munchaholic$setPendingRollMode(RollMode mode) {
		munchaholic$pendingRollMode = mode;
	}

	@Override
	public RollMode munchaholic$takePendingRollMode() {
		RollMode value = munchaholic$pendingRollMode;
		munchaholic$pendingRollMode = null;
		return value;
	}
}
