package com.pshakhlovich.jackpot.support;

import java.util.Random;

/**
 * Deterministic random number generator for testing and evaluation.
 * Seeded by betId + jackpotId to ensure repeatable outcomes for the same bet.
 */
public class DeterministicRandom {

    private final Random random;

    public DeterministicRandom(String seed) {
        this.random = new Random(seed.hashCode());
    }

    /**
     * Generate a deterministic random draw in the range [0.0, 1.0).
     * Same seed will always produce the same value.
     */
    public double nextDouble() {
        return random.nextDouble();
    }
}
