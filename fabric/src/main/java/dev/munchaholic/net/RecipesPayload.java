package dev.munchaholic.net;

import java.util.List;

import dev.munchaholic.Munchaholic;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: whether Recipes mode is active, and the player's DISCOVERED recipes only. Undiscovered recipes
 * never leave the server.
 */
public record RecipesPayload(boolean active, List<Entry> entries) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<RecipesPayload> TYPE = new CustomPacketPayload.Type<>(Munchaholic.id("recipes"));
	public static final StreamCodec<RegistryFriendlyByteBuf, RecipesPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, RecipesPayload::active,
			Entry.CODEC.apply(ByteBufCodecs.list()), RecipesPayload::entries,
			RecipesPayload::new);

	/** One discovered food: its item id, the attribute key, and whether the value goes up. */
	public record Entry(String food, String attribute, boolean up) {
		public static final StreamCodec<ByteBuf, Entry> CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, Entry::food,
				ByteBufCodecs.STRING_UTF8, Entry::attribute,
				ByteBufCodecs.BOOL, Entry::up,
				Entry::new);
	}

	@Override
	public Type<RecipesPayload> type() {
		return TYPE;
	}

	/** Called once from Munchaholic#onInitialize (runs on both sides). */
	public static void register() {
		PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC);
	}
}
