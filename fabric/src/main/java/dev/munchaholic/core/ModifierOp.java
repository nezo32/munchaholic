package dev.munchaholic.core;

/**
 * How our single modifier combines with the attribute's base value. Mapped to
 * {@code AttributeModifier.Operation} in {@code player/PlayerMunch}; {@code ADD_MULTIPLIED_TOTAL} is never used, so
 * the shown "now" value stays exact whatever potions or sprinting do.
 */
public enum ModifierOp {
	ADD_VALUE,
	ADD_MULTIPLIED_BASE
}
