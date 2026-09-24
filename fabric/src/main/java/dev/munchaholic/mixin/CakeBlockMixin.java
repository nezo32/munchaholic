package dev.munchaholic.mixin;

import dev.munchaholic.BiteHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * One roll per eaten cake slice (plain and candle cakes both go through {@code CakeBlock.eat}). The food type is
 * {@code minecraft:cake}. Also runs on the client (prediction), hence the ServerPlayer filter.
 */
@Mixin(CakeBlock.class)
public abstract class CakeBlockMixin {
	@Inject(method = "eat", at = @At("RETURN"))
	private static void munchaholic$afterSlice(LevelAccessor level, BlockPos pos, BlockState state, Player player,
			CallbackInfoReturnable<InteractionResult> cir) {
		if (cir.getReturnValue().consumesAction() && player instanceof ServerPlayer sp) {
			BiteHandler.onAte(sp, new ItemStack(Items.CAKE));
		}
	}
}
