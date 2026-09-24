package dev.munchaholic;

import dev.munchaholic.core.RollOutcome;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
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
		throw new UnsupportedOperationException("TODO");
	}

	/** Applied: buff or debuff; Capped and Nothing: neutral. */
	public static int tone(RollOutcome outcome) {
		throw new UnsupportedOperationException("TODO");
	}

	/** "✦ food → Attribute change (now value)" and the capped / nothing variants. */
	public static Component message(ItemStack food, RollOutcome outcome) {
		throw new UnsupportedOperationException("TODO");
	}

	public static void send(ServerPlayer player, ItemStack food, RollOutcome outcome) {
		throw new UnsupportedOperationException("TODO");
	}

	/** modded = the client has the munchaholic:rolled channel. Public for gametests. */
	public static void send(ServerPlayer player, ItemStack food, RollOutcome outcome, boolean modded) {
		throw new UnsupportedOperationException("TODO");
	}
}
