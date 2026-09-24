package dev.munchaholic;

import dev.munchaholic.core.AttributeSpec;
import dev.munchaholic.core.Direction;
import dev.munchaholic.core.RollMode;
import net.minecraft.network.chat.MutableComponent;

/** Component builders shared by Feedback, the commands and the client tooltip. Server-safe (common source set). */
public final class Texts {
	private Texts() {}

	/** The vanilla attribute name, white. */
	public static MutableComponent attributeName(AttributeSpec spec) {
		throw new UnsupportedOperationException("TODO");
	}

	/** A number in the spec's unit ({@code 92%}, {@code +2}). */
	public static MutableComponent amount(AttributeSpec spec, double displayValue, boolean signed) {
		throw new UnsupportedOperationException("TODO");
	}

	/** A signed change of {@code stepsDelta} steps; green if it is a buff, else red. */
	public static MutableComponent change(AttributeSpec spec, int stepsDelta) {
		throw new UnsupportedOperationException("TODO");
	}

	/** {@code (now 92%)}, gray. */
	public static MutableComponent now(AttributeSpec spec, double displayValue) {
		throw new UnsupportedOperationException("TODO");
	}

	/** The roll mode name, gold. */
	public static MutableComponent rollMode(RollMode mode) {
		throw new UnsupportedOperationException("TODO");
	}

	/** {@code "Random"} / {@code "Recipes"}: the en_us value of {@link RollMode#translationKey()}. */
	public static String englishRollMode(RollMode mode) {
		throw new UnsupportedOperationException("TODO");
	}

	/** ▲ / ▼ for the client tooltip; green if it is a buff, else red. */
	public static MutableComponent arrow(AttributeSpec spec, Direction direction) {
		throw new UnsupportedOperationException("TODO");
	}
}
