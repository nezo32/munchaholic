package dev.munchaholic.core;

/** Supplies the live base value of an attribute for the player being rolled (another mod may have changed it). */
@FunctionalInterface
public interface BaseLookup {
	/** The vanilla player base values from the table. */
	BaseLookup VANILLA = AttributeSpec::playerBase;

	double base(AttributeSpec spec);
}
