package dev.munchaholic.core;

/**
 * The cross-attribute mobility rule (ARCHITECTURE.md §2): no roll may leave a player unable to get up a 1-block ledge.
 * Jump strength, gravity and step height each stay inside their own caps, but together they could still make a full
 * block unreachable (a 1-deep pit becomes a trap, pillaring stops working), so a step that makes climbing worse is
 * vetoed when the result could not climb.
 */
public final class Mobility {
	/** Jump height (blocks) that counts as "can get up a ledge": one block plus room for a carpet or snow layer. */
	public static final double CLIMB_JUMP_HEIGHT = 1.05;
	/** A step height at or above one block walks up a ledge without jumping. */
	public static final double CLIMB_STEP_HEIGHT = 1.0;
	/** {@code LivingEntity.travelInAir}: vertical velocity is multiplied by this float drag after gravity. */
	static final double VERTICAL_DRAG = 0.98F;
	/** {@code LivingEntity.jumpFromGround} does nothing for a jump power at or below this. */
	static final float MIN_JUMP_POWER = 1.0E-5F;
	/** Bound on the simulated ticks (degenerate tiny gravity). */
	private static final int MAX_TICKS = 10_000;

	private Mobility() {}

	/**
	 * Peak height in blocks of a standing jump, simulated tick by tick like vanilla (26.2 and 26.3):
	 * {@code jumpFromGround} sets {@code vy = (float) jumpStrength} (no Jump Boost, block jump factor 1), then each tick
	 * {@code travelInAir} moves by {@code vy}, then {@code vy = (vy - gravity) * 0.98F}. Air drag modifier 1 (the
	 * default). Negative gravity never brings the player back down: infinite; zero or tiny gravity is
	 * stopped by the drag alone (about 50 x the jump power), bounded by the tick limit. NaN: 0.
	 */
	public static double jumpHeight(double jumpStrength, double gravity) {
		double vy = (float) jumpStrength;
		if (!(vy > MIN_JUMP_POWER)) return 0.0;
		if (Double.isNaN(gravity)) return 0.0;
		if (gravity < 0.0) return Double.POSITIVE_INFINITY; // pushed up forever
		double y = 0.0;
		for (int tick = 0; tick < MAX_TICKS && vy > 0.0; tick++) {
			y += vy;
			vy = (vy - gravity) * VERTICAL_DRAG;
		}
		return y;
	}

	/** {@code jumpHeight >= CLIMB_JUMP_HEIGHT || stepHeight >= CLIMB_STEP_HEIGHT}. */
	public static boolean canClimb(double jumpStrength, double gravity, double stepHeight) {
		return stepHeight >= CLIMB_STEP_HEIGHT - AttributeSpec.EPS
				|| jumpHeight(jumpStrength, gravity) >= CLIMB_JUMP_HEIGHT;
	}

	/** {@link #canClimb(double, double, double)} on the munch values (base + our modifier only, D4). */
	public static boolean canClimb(PlayerStacks stacks, BaseLookup bases) {
		return canClimb(munchValue(Caps.JUMP_STRENGTH, stacks, bases), munchValue(Caps.GRAVITY, stacks, bases),
				munchValue(Caps.STEP_HEIGHT, stacks, bases));
	}

	/** Jump strength DOWN, gravity UP and step height DOWN make climbing harder; nothing else affects it. */
	public static boolean hindersClimbing(AttributeSpec spec, Direction direction) {
		String key = spec.key();
		if (key.equals(Caps.JUMP_STRENGTH.key()) || key.equals(Caps.STEP_HEIGHT.key())) return direction == Direction.DOWN;
		if (key.equals(Caps.GRAVITY.key())) return direction == Direction.UP;
		return false;
	}

	/**
	 * Vetoes a step that {@link #hindersClimbing} if the player could not climb after it. This keeps a climbing player
	 * climbing, and a player who already can't (their base values were changed by a command or another mod) can only
	 * roll steps that help.
	 */
	public static RollVeto guard(PlayerStacks stacks, BaseLookup bases) {
		return candidate -> hindersClimbing(candidate.spec(), candidate.direction())
				&& !canClimb(stacks.withSteps(candidate.spec(), candidate.newSteps()), bases);
	}

	private static double munchValue(AttributeSpec spec, PlayerStacks stacks, BaseLookup bases) {
		return spec.valueAt(bases.base(spec), stacks.steps(spec));
	}
}
