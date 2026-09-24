package dev.munchaholic.player;

import java.util.List;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.munchaholic.Munchaholic;
import dev.munchaholic.core.Discoveries;
import dev.munchaholic.core.PlayerStacks;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

/**
 * The two per-player Fabric data attachments. Stacks are persistent but NOT copyOnDeath (our COPY_FROM listener
 * decides, see PlayerHooks); discoveries are player knowledge, so they are persistent and copyOnDeath.
 */
public final class MunchAttachments {
	public static final Codec<PlayerStacks> STACKS_CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("steps").forGetter(PlayerStacks::steps),
			Codec.INT.optionalFieldOf("bites", 0).forGetter(PlayerStacks::bites)
	).apply(i, PlayerStacks::new));

	/** A string list, sorted on encode so the NBT is stable. */
	public static final Codec<Discoveries> DISCOVERIES_CODEC = Codec.STRING.listOf().xmap(
			(List<String> foods) -> new Discoveries(Set.copyOf(foods)),
			(Discoveries d) -> d.foods().stream().sorted().toList());

	public static final AttachmentType<PlayerStacks> STACKS = AttachmentRegistry.create(Munchaholic.id("stacks"),
			b -> b.persistent(STACKS_CODEC).initializer(() -> PlayerStacks.EMPTY));

	public static final AttachmentType<Discoveries> DISCOVERIES = AttachmentRegistry.create(Munchaholic.id("discoveries"),
			b -> b.persistent(DISCOVERIES_CODEC).copyOnDeath().initializer(() -> Discoveries.EMPTY));

	private MunchAttachments() {}

	/** Loads this class (registers both attachment types). Called once from Munchaholic#onInitialize. */
	public static void init() {}
}
