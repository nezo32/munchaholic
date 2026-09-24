package dev.munchaholic.mixin;

import dev.munchaholic.BiteHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * One roll per finished eat of anything with a FOOD component. Runs before the stack shrinks, so the stack still
 * holds the eaten item. Also runs on the client and for foxes, hence the ServerPlayer filter.
 */
@Mixin(FoodProperties.class)
public abstract class FoodPropertiesMixin {
	@Inject(method = "onConsume", at = @At("TAIL"))
	private void munchaholic$afterEat(Level level, LivingEntity user, ItemStack stack, Consumable consumable, CallbackInfo ci) {
		if (user instanceof ServerPlayer sp) {
			BiteHandler.onAte(sp, stack);
		}
	}
}
