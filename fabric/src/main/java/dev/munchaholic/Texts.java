package dev.munchaholic;

import dev.munchaholic.core.AttributeSpec;
import dev.munchaholic.core.Direction;
import dev.munchaholic.core.DisplayUnit;
import dev.munchaholic.core.Numbers;
import dev.munchaholic.core.RollMode;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Component builders shared by Feedback, the commands and the client tooltip. Server-safe (common source set).
 * Server-sent text carries its English fallback for vanilla clients; vanilla {@code attribute.name.*} keys don't need one.
 * Keys and fallbacks are written out as literals so LangFileTest checks them against en_us.
 */
public final class Texts {
	private Texts() {}

	/** The vanilla attribute name, white. */
	public static MutableComponent attributeName(AttributeSpec spec) {
		return Component.translatable(spec.translationKey()).withStyle(ChatFormatting.WHITE);
	}

	/** A number in the spec's unit ({@code 92%}, {@code +2}). Unstyled. Same key and fallback as {@link DisplayUnit}. */
	public static MutableComponent amount(AttributeSpec spec, double displayValue, boolean signed) {
		DisplayUnit unit = spec.unit();
		String number = signed
				? Numbers.signed(displayValue, unit.maxDecimals())
				: Numbers.format(displayValue, unit.maxDecimals());
		return switch (unit) {
			case PERCENT_OF_BASE, PERCENT_POINTS -> Component.translatableWithFallback("munchaholic.unit.percent", "%s%%", number);
			case POINTS -> Component.translatableWithFallback("munchaholic.unit.points", "%s", number);
		};
	}

	/** A signed change of {@code stepsDelta} steps; green if it is a buff, else red. */
	public static MutableComponent change(AttributeSpec spec, int stepsDelta) {
		Direction direction = stepsDelta >= 0 ? Direction.UP : Direction.DOWN;
		return amount(spec, spec.displayDelta(stepsDelta), true).withStyle(color(spec, direction));
	}

	/** {@code (now 92%)}, gray. */
	public static MutableComponent now(AttributeSpec spec, double displayValue) {
		return Component.translatableWithFallback("munchaholic.message.now", "(now %s)", amount(spec, displayValue, false))
				.withStyle(ChatFormatting.GRAY);
	}

	/** The roll mode name, gold. */
	public static MutableComponent rollMode(RollMode mode) {
		MutableComponent name = switch (mode) {
			case RANDOM -> Component.translatableWithFallback("munchaholic.rollMode.random", "Random");
			case RECIPES -> Component.translatableWithFallback("munchaholic.rollMode.recipes", "Recipes");
		};
		return name.withStyle(ChatFormatting.GOLD);
	}

	/** {@code "Random"} / {@code "Recipes"}: the en_us value of {@link RollMode#translationKey()}. */
	public static String englishRollMode(RollMode mode) {
		return switch (mode) {
			case RANDOM -> "Random";
			case RECIPES -> "Recipes";
		};
	}

	/** ▲ / ▼ for the client tooltip; green if it is a buff, else red. Client-only text: no fallback. */
	public static MutableComponent arrow(AttributeSpec spec, Direction direction) {
		String key = direction == Direction.UP ? "munchaholic.direction.up" : "munchaholic.direction.down";
		return Component.translatable(key).withStyle(color(spec, direction));
	}

	private static ChatFormatting color(AttributeSpec spec, Direction direction) {
		return spec.isBuff(direction) ? ChatFormatting.GREEN : ChatFormatting.RED;
	}
}
