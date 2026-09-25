package dev.munchaholic.mode;

import java.util.Objects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.munchaholic.Munchaholic;
import dev.munchaholic.core.RollMode;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Per-world settings, saved as {@code <world>/data/munchaholic/mode.dat} in the server-wide SavedDataStorage
 * (MinecraftServer#getDataStorage). An absent file means defaults; see {@link ModeBootstrap} for how it is created.
 */
public final class MunchaholicMode extends SavedData {
	public static final Codec<MunchaholicMode> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.BOOL.optionalFieldOf("enabled", false).forGetter(MunchaholicMode::enabled),
			Codec.STRING.xmap(id -> RollMode.byId(id, RollMode.RANDOM), RollMode::id)
					.optionalFieldOf("rollMode", RollMode.RANDOM).forGetter(MunchaholicMode::rollMode),
			Codec.BOOL.optionalFieldOf("keepOnDeath", true).forGetter(MunchaholicMode::keepOnDeath)
	).apply(i, MunchaholicMode::new));

	/** null DataFixTypes: no vanilla fixer applies; Fabric's SavedDataStorageMixin skips datafixing for null. */
	public static final SavedDataType<MunchaholicMode> TYPE = new SavedDataType<>(
			Munchaholic.id("mode"), MunchaholicMode::new, CODEC, null);

	private boolean enabled;
	private RollMode rollMode;
	private boolean keepOnDeath;

	/** Defaults: OFF, Random, keep on death. */
	public MunchaholicMode() {
		this(false, RollMode.RANDOM, true);
	}

	private MunchaholicMode(boolean enabled, RollMode rollMode, boolean keepOnDeath) {
		this.enabled = enabled;
		this.rollMode = rollMode;
		this.keepOnDeath = keepOnDeath;
	}

	public boolean enabled() {
		return enabled;
	}

	public void setEnabled(boolean value) {
		if (enabled != value) {
			enabled = value;
			setDirty();
		}
	}

	public RollMode rollMode() {
		return rollMode;
	}

	public void setRollMode(RollMode value) {
		Objects.requireNonNull(value, "value");
		if (rollMode != value) {
			rollMode = value;
			setDirty();
		}
	}

	public boolean keepOnDeath() {
		return keepOnDeath;
	}

	public void setKeepOnDeath(boolean value) {
		if (keepOnDeath != value) {
			keepOnDeath = value;
			setDirty();
		}
	}

	public static MunchaholicMode get(MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(TYPE);
	}

	public static boolean isEnabled(MinecraftServer server) {
		return get(server).enabled();
	}

	/** Always marks dirty, so the file exists even when the value did not change. */
	public static void setEnabled(MinecraftServer server, boolean value) {
		MunchaholicMode mode = get(server);
		mode.setEnabled(value);
		mode.setDirty();
	}

	public static RollMode rollMode(MinecraftServer server) {
		return get(server).rollMode();
	}

	public static void setRollMode(MinecraftServer server, RollMode value) {
		MunchaholicMode mode = get(server);
		mode.setRollMode(value);
		mode.setDirty();
	}

	public static boolean keepOnDeath(MinecraftServer server) {
		return get(server).keepOnDeath();
	}

	public static void setKeepOnDeath(MinecraftServer server, boolean value) {
		MunchaholicMode mode = get(server);
		mode.setKeepOnDeath(value);
		mode.setDirty();
	}

	/** enabled && rollMode == RECIPES: whether recipe tooltips are shown to clients. */
	public static boolean recipesActive(MinecraftServer server) {
		MunchaholicMode mode = get(server);
		return mode.enabled() && mode.rollMode() == RollMode.RECIPES;
	}
}
