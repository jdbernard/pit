package com.jdbernard.pit

import org.junit.Test
import static org.junit.Assert.assertEquals

import static com.jdbernard.pit.Category.toCategory

class CategoryTest {

    @Test void testToCategory() {

        assertEquals toCategory("BUG"), Category.BUG
        assertEquals toCategory("FEATURE"), Category.FEATURE
        assertEquals toCategory("TASK"), Category.TASK

        assertEquals toCategory("bug"), Category.BUG
        assertEquals toCategory("feature"), Category.FEATURE
        assertEquals toCategory("task"), Category.TASK

        assertEquals toCategory("b"), Category.BUG
        assertEquals toCategory("f"), Category.FEATURE
        assertEquals toCategory("t"), Category.TASK

    }

    @Test void testGetSymbol() {

        assertEquals Category.BUG.symbol,       "b"
        assertEquals Category.FEATURE.symbol,   "f"
        assertEquals Category.TASK.symbol,      "t"
    }
}
