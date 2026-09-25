package dev.munchaholic.core;

/** Per-world roll mode. {@link #id()} is the value saved in mode.dat and typed in commands. */
public enum RollMode {
	RANDOM("random"),
	RECIPES("recipes");

	private final String id;

	RollMode(String id) {
		this.id = id;
	}

	public String id() {
		return id;
	}

	/** {@code "munchaholic.rollMode." + id}. */
	public String translationKey() {
		return "munchaholic.rollMode." + id;
	}

	/** Case-sensitive lookup by {@link #id()}; null or unknown -> fallback. Used by the mode.dat codec. */
	public static RollMode byId(String id, RollMode fallback) {
		for (RollMode mode : values()) {
			if (mode.id.equals(id)) return mode;
		}
		return fallback;
	}
}
