package dev.munchaholic.client;

import dev.munchaholic.core.RollMode;

/** Duck interface on CreateWorldScreen (CreateWorldScreenMixin): the Game tab buttons' values for this screen. */
public interface CreateWorldModeHolder {
	boolean munchaholic$isModeEnabled();

	void munchaholic$setModeEnabled(boolean enabled);

	RollMode munchaholic$getRollMode();

	void munchaholic$setRollMode(RollMode mode);
}
