package dev.munchaholic.client;

import dev.munchaholic.net.RolledPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/** Receives {@link RolledPayload} and shows the message / plays the sound according to {@link NotifyConfig}. */
public final class NotifyClient {
	private NotifyClient() {}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(RolledPayload.TYPE, (payload, ctx) -> handle(payload.message(), payload.tone(), ctx.player()));
	}

	/** Shows / plays according to NotifyConfig.get(). Public so the client gametest can call it directly. */
	public static void handle(Component message, int tone, LocalPlayer player) {
		throw new UnsupportedOperationException("TODO");
	}
}
