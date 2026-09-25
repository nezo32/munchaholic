package dev.munchaholic.core;

import java.nio.charset.StandardCharsets;
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
		return of(worldSeed, foodId, Caps.ALL);
	}

	/** The recipe over {@code specs}; independent of their order. Throws for an empty list. */
	public static Recipe of(long worldSeed, String foodId, List<AttributeSpec> specs) {
		if (specs.isEmpty()) throw new IllegalArgumentException("no attribute specs");
		long foodHash = mix64(worldSeed ^ fnv1a64(foodId));
		AttributeSpec best = null;
		long bestScore = 0L;
		for (AttributeSpec spec : specs) {
			long score = mix64(foodHash ^ fnv1a64(spec.key()));
			if (best == null) {
				best = spec;
				bestScore = score;
				continue;
			}
			int cmp = Long.compareUnsigned(score, bestScore);
			if (cmp > 0 || (cmp == 0 && spec.key().compareTo(best.key()) < 0)) {
				best = spec;
				bestScore = score;
			}
		}
		Direction direction = (mix64(bestScore ^ DIRECTION_SALT) & 1L) == 0 ? Direction.UP : Direction.DOWN;
		return new Recipe(best, direction);
	}

	/** 64-bit FNV-1a over the UTF-8 bytes. */
	public static long fnv1a64(String s) {
		long h = 0xcbf29ce484222325L;
		for (byte b : s.getBytes(StandardCharsets.UTF_8)) {
			h ^= (b & 0xff);
			h *= 0x100000001b3L;
		}
		return h;
	}

	/** SplitMix64 finalizer. */
	public static long mix64(long z) {
		z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
		z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
		return z ^ (z >>> 31);
	}
}
