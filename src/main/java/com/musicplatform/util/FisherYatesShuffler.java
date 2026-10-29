package com.musicplatform.util;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Component
public class FisherYatesShuffler {

    private final Random random;

    public FisherYatesShuffler(Random random) {
        this.random = random;
    }

    /**
     * Performs an unbiased in-place / copy Fisher-Yates (Knuth) shuffle in O(N) time.
     * Guarantees all N! permutations are equally probable.
     *
     * @param items Original list of items
     * @param <T>   Item type
     * @return New list containing shuffled elements
     */
    public <T> List<T> shuffle(List<T> items) {
        if (items == null || items.size() <= 1) {
            return items == null ? Collections.emptyList() : new ArrayList<>(items);
        }
        List<T> copy = new ArrayList<>(items);
        for (int i = copy.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            Collections.swap(copy, i, j);
        }
        return copy;
    }
}
