package dev.munchaholic.client.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.munchaholic.client.CreateWorldModeHolder;
import dev.munchaholic.core.RollMode;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds "Munchaholic Mode: ON/OFF" and "Roll Mode: Random/Recipes" to the "Game" tab of the Create World screen,
 * directly under Difficulty (after the difficulty listener, addListener ordinal 2). Roll Mode is inactive while the
 * mode is OFF. The values live on the screen (CreateWorldScreenMixin), not in game rules.
 */
@Mixin(targets = "net.minecraft.client.gui.screens.worldselection.CreateWorldScreen$GameTab")
public abstract class GameTabMixin {
	@Inject(method = "<init>", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screens/worldselection/WorldCreationUiState;addListener(Ljava/util/function/Consumer;)V",
			ordinal = 2, shift = At.Shift.AFTER))
	private void munchaholic$addToggles(CreateWorldScreen screen, CallbackInfo ci, @Local GridLayout.RowHelper helper) {
		CreateWorldModeHolder holder = (CreateWorldModeHolder) screen;
		// built first so the mode toggle can switch it on and off, added second so it lands below the toggle
		CycleButton<RollMode> rollButton = CycleButton.builder((RollMode m) -> Component.translatable(m.translationKey()), holder.munchaholic$getRollMode())
				.withValues(RollMode.values())
				.withTooltip(m -> Tooltip.create(Component.translatable("munchaholic.createWorld.rollMode.tooltip." + m.id())))
				.create(0, 0, 210, 20, Component.translatable("munchaholic.createWorld.rollMode"),
						(b, m) -> holder.munchaholic$setRollMode(m));
		rollButton.active = holder.munchaholic$isModeEnabled();
		helper.addChild(CycleButton.onOffBuilder(holder.munchaholic$isModeEnabled())
				.withTooltip(v -> Tooltip.create(Component.translatable("munchaholic.createWorld.toggle.tooltip")))
				.create(0, 0, 210, 20, Component.translatable("munchaholic.createWorld.toggle"),
						(b, v) -> {
							holder.munchaholic$setModeEnabled(v);
							rollButton.active = v;
						}));
		helper.addChild(rollButton);
	}
}
