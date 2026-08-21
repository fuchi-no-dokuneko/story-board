package dev.storyblock.domain;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OrderKeyTest {
    @Test
    void midpointSortsBetweenNeighbors() {
        OrderKey first = OrderKey.rebalanced(0, 2);
        OrderKey last = OrderKey.rebalanced(1, 2);
        OrderKey middle = OrderKey.between(first, last);

        assertTrue(first.compareTo(middle) < 0);
        assertTrue(middle.compareTo(last) < 0);
    }
}
