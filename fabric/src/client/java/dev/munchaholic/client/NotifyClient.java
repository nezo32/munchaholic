package dev.munchaholic.client;

import dev.munchaholic.Feedback;
import dev.munchaholic.core.NotifySettings;
import dev.munchaholic.net.RolledPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;

/** Receives {@link RolledPayload} and shows the message / plays the sound according to {@link NotifyConfig}. */
public final class NotifyClient {
	private NotifyClient() {}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(RolledPayload.TYPE, (payload, ctx) -> handle(payload.message(), payload.tone(), ctx.player()));
	}

	/**
	 * Shows / plays according to NotifyConfig.get(); the tone picks the pitch ({@link Feedback#pitch(int)}). Public so
	 * the client gametest can call it directly.
	 */
	public static void handle(Component message, int tone, LocalPlayer player) {
		if (player == null) return;
		NotifySettings settings = NotifyConfig.get();
		if (settings.message()) {
			player.sendOverlayMessage(message);
		}
		if (settings.sound()) {
			player.level().playLocalSound(player.getX(), player.getY(), player.getZ(), Feedback.SOUND,
					SoundSource.PLAYERS, Feedback.VOLUME, Feedback.pitch(tone), false);
		}
	}
}
