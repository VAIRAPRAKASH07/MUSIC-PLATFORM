package com.musicplatform.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class FisherYatesShufflerTest {

    @Test
    @DisplayName("Null or empty list returns empty/copy safely without error")
    void testEdgeCases() {
        FisherYatesShuffler shuffler = new FisherYatesShuffler(new Random(42L));

        assertTrue(shuffler.shuffle(null).isEmpty());
        assertTrue(shuffler.shuffle(new ArrayList<String>()).isEmpty());

        List<String> single = List.of("OnlyTrack");
        List<String> shuffledSingle = shuffler.shuffle(single);
        assertEquals(1, shuffledSingle.size());
        assertEquals("OnlyTrack", shuffledSingle.get(0));
    }

    @Test
    @DisplayName("Deterministic shuffle with seeded Random preserves all elements as a permutation")
    void testDeterministicShuffle() {
        Random testRandom = new Random(12345L);
        FisherYatesShuffler shuffler = new FisherYatesShuffler(testRandom);

        List<Integer> original = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        List<Integer> shuffled = shuffler.shuffle(original);

        // Same size and contains all original elements
        assertEquals(original.size(), shuffled.size());
        assertTrue(shuffled.containsAll(original));

        // Permutation should not be in identical ascending order
        assertNotEquals(original, shuffled);

        // Reproducibility with the same seed
        Random secondRandom = new Random(12345L);
        FisherYatesShuffler secondShuffler = new FisherYatesShuffler(secondRandom);
        List<Integer> secondShuffled = secondShuffler.shuffle(original);

        assertEquals(shuffled, secondShuffled, "Identical seed must produce identical permutation");
    }
}
