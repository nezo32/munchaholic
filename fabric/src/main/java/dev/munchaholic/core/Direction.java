package dev.munchaholic.core;

/** Direction of the attribute VALUE (not good or bad: see {@link AttributeSpec#buff()}). */
public enum Direction {
	UP(1),
	DOWN(-1);

	private final int sign;

	Direction(int sign) {
		this.sign = sign;
	}

	/** +1 for UP, -1 for DOWN. */
	public int sign() {
		return sign;
	}

	public Direction opposite() {
		throw new UnsupportedOperationException("TODO");
	}
}
