package dev.munchaholic.core;

import java.util.List;

/**
 * The per-world food -> (attribute, direction) mapping: rendezvous hashing over attribute keys with a SplitMix64
 * finalizer, keyed by (world seed, item id). Frozen (ARCHITECTURE.md §4.3): changing it re-rolls every world's recipes.
 */
public final class Recipes {
	/** {@code "munch"}. */
	public static final long DIRECTION_SALT = 0x6D756E6368L;

	private Recipes() {}

	/** The recipe over {@link Caps#ALL}. */
	public static Recipe of(long worldSeed, String foodId) {
		throw new UnsupportedOperationException("TODO");
	}

	/** The recipe over {@code specs}; independent of their order. */
	public static Recipe of(long worldSeed, String foodId, List<AttributeSpec> specs) {
		throw new UnsupportedOperationException("TODO");
	}

	/** 64-bit FNV-1a over the UTF-8 bytes. */
	public static long fnv1a64(String s) {
		throw new UnsupportedOperationException("TODO");
	}

	/** SplitMix64 finalizer. */
	public static long mix64(long z) {
		throw new UnsupportedOperationException("TODO");
	}
}
