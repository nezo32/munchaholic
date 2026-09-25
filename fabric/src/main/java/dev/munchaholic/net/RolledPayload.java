package dev.munchaholic.net;

import dev.munchaholic.Munchaholic;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: one bite's complete actionbar message and its tone (buff, debuff, neutral). Sent instead of the
 * vanilla overlay + sound packets when the client has Munchaholic, so the client can apply its own settings.
 */
public record RolledPayload(Component message, int tone) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<RolledPayload> TYPE = new CustomPacketPayload.Type<>(Munchaholic.id("rolled"));
	public static final StreamCodec<RegistryFriendlyByteBuf, RolledPayload> CODEC = StreamCodec.composite(
			ComponentSerialization.TRUSTED_STREAM_CODEC, RolledPayload::message,
			ByteBufCodecs.VAR_INT, RolledPayload::tone,
			RolledPayload::new);

	@Override
	public Type<RolledPayload> type() {
		return TYPE;
	}

	/** Called once from Munchaholic#onInitialize (runs on both sides). */
	public static void register() {
		PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC);
	}
}
