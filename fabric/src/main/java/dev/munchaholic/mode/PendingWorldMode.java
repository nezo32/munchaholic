package dev.munchaholic.mode;

import dev.munchaholic.core.RollMode;

/**
 * Duck interface on {@code LevelStorageSource.LevelStorageAccess} (see LevelStorageAccessMixin).
 * The Create World screen stores the button values on the access object of the world it just created;
 * the integrated server that is handed that same object consumes them on SERVER_STARTING.
 */
public interface PendingWorldMode {
	void munchaholic$setPendingMode(boolean enabled);

	/** Returns and clears the pending value; null if none (existing world, dedicated server). */
	Boolean munchaholic$takePendingMode();

	void munchaholic$setPendingRollMode(RollMode mode);

	/** Returns and clears the pending Roll Mode; null if none. */
	RollMode munchaholic$takePendingRollMode();
}
