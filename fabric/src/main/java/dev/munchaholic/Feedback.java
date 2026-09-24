package dev.munchaholic;

import dev.munchaholic.core.AttributeSpec;
import dev.munchaholic.core.RollOutcome;
import dev.munchaholic.net.RolledPayload;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

/**
 * Actionbar message and a quiet chime, only for the eater.
 *
 * <p>If the player's client has Munchaholic (it accepts the {@code munchaholic:rolled} channel), the server sends
 * only a {@code RolledPayload} with the message and tone, and the client applies its own notification settings.
 * Otherwise (vanilla client, server-only install) the server sends the overlay message and the sound packet itself.
 */
public final class Feedback {
	public static final SoundEvent SOUND = SoundEvents.AMETHYST_BLOCK_CHIME;
	public static final float VOLUME = 0.5F;
	public static final int TONE_BUFF = 0;
	public static final int TONE_DEBUFF = 1;
	public static final int TONE_NEUTRAL = 2;
	public static final float PITCH_BUFF = 1.5F;
	public static final float PITCH_DEBUFF = 0.8F;
	public static final float PITCH_NEUTRAL = 1.1F;

	private Feedback() {}

	/** Unknown tone -> {@link #PITCH_NEUTRAL}. */
	public static float pitch(int tone) {
		return switch (tone) {
			case TONE_BUFF -> PITCH_BUFF;
			case TONE_DEBUFF -> PITCH_DEBUFF;
			default -> PITCH_NEUTRAL;
		};
	}

	/** Applied: buff or debuff; Capped and Nothing: neutral. */
	public static int tone(RollOutcome outcome) {
		if (outcome instanceof RollOutcome.Applied applied) {
			return applied.isBuff() ? TONE_BUFF : TONE_DEBUFF;
		}
		return TONE_NEUTRAL;
	}

	/**
	 * "✦ food → Attribute change (now value)" and the capped / nothing variants. Reads the food's hover name, so call
	 * it before the stack shrinks (the eat hooks do).
	 */
	public static Component message(ItemStack food, RollOutcome outcome) {
		MutableComponent foodName = food.getHoverName().copy().withStyle(ChatFormatting.GOLD);
		// fallbacks: server-only installs (vanilla clients have no mod lang)
		MutableComponent msg = switch (outcome) {
			case RollOutcome.Applied a -> Component.translatableWithFallback("munchaholic.message.rolled", "✦ %1$s → %2$s %3$s %4$s",
					foodName,
					Texts.attributeName(a.spec()),
					Texts.change(a.spec(), a.newSteps() - a.oldSteps()),
					Texts.now(a.spec(), a.displayNow()));
			case RollOutcome.Capped c -> {
				AttributeSpec spec = c.spec();
				yield Component.translatableWithFallback("munchaholic.message.capped", "✦ %1$s → %2$s %3$s (at the limit: %4$s)",
						foodName,
						Texts.attributeName(spec),
						Texts.amount(spec, c.displayDelta(), true).withStyle(ChatFormatting.GRAY),
						Texts.amount(spec, c.displayNow(), false).withStyle(ChatFormatting.GRAY));
			}
			case RollOutcome.Nothing _ -> Component.translatableWithFallback("munchaholic.message.nothing", "✦ %s → nothing left to change",
					foodName);
		};
		return msg.withStyle(ChatFormatting.LIGHT_PURPLE);
	}

	public static void send(ServerPlayer player, ItemStack food, RollOutcome outcome) {
		send(player, food, outcome, ServerPlayNetworking.canSend(player, RolledPayload.TYPE));
	}

	/** modded = the client has the munchaholic:rolled channel. Public for gametests. */
	public static void send(ServerPlayer player, ItemStack food, RollOutcome outcome, boolean modded) {
		Component msg = message(food, outcome);
		int tone = tone(outcome);
		if (modded) {
			// the client decides message/sound from its own config
			player.connection.send(ServerPlayNetworking.createClientboundPacket(new RolledPayload(msg, tone)));
			return;
		}
		player.sendOverlayMessage(msg);
		player.connection.send(new ClientboundSoundPacket(
				BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SOUND),
				SoundSource.PLAYERS, player.getX(), player.getY(), player.getZ(),
				VOLUME, pitch(tone), player.getRandom().nextLong()));
	}
}
