package io.smetweb.math

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RangeTest {

    @Test
    fun `test constructors and contains`() {
        val range0 = Range<Int>()
        assertFalse(range0.lowerFinite())
        assertFalse(range0.upperFinite())
        assertThrows(IllegalStateException::class.java) { range0.lowerBound() }
        assertThrows(IllegalStateException::class.java) { range0.upperBound() }
        assertThrows(IllegalStateException::class.java) { range0.lowerInclusive() }
        assertThrows(IllegalStateException::class.java) { range0.upperInclusive() }

        val range1 = Range(1)
        assertTrue(range1.lowerFinite())
        assertTrue(range1.upperFinite())
        assertEquals(1, range1.lowerBound())
        assertEquals(1, range1.upperBound())
        assertTrue(range1.lowerInclusive())
        assertTrue(range1.upperInclusive())

        assertFalse(range1.contains(0)) { "scalar range should be inclusive of scalar only" }
        assertTrue(range1.contains(1)) { "scalar range should be inclusive of scalar only" }
        assertFalse(range1.contains(2)) { "scalar range should be inclusive of scalar only" }

        val range2 = Range(2, 3)

        assertTrue(range2.lowerFinite())
        assertTrue(range2.upperFinite())
        assertEquals(2, range2.lowerBound())
        assertEquals(3, range2.upperBound())
        assertTrue(range2.lowerInclusive())
        assertFalse(range2.upperInclusive())

        assertFalse(range2.contains(1))
        assertTrue(range2.contains(2)) { "default range should be inclusive of minimum" }
        assertFalse(range2.contains(3)) { "default range should be exclusive of maximum" }
        assertFalse(range2.contains(4))

        val range3 = Range(4, false, 5, true)

        assertTrue(range3.lowerFinite())
        assertTrue(range3.upperFinite())
        assertEquals(4, range3.lowerBound())
        assertEquals(5, range3.upperBound())
        assertFalse(range3.lowerInclusive())
        assertTrue(range3.upperInclusive())

        assertFalse(range3.contains(3))
        assertFalse(range3.contains(4)) { "reverse range should be exclusive of lower bound" }
        assertTrue(range3.contains(5)) { "reverse range should be inclusive of upper bound" }
        assertFalse(range3.contains(6))

        val range4 = Range.downToAndIncluding(6)

        assertTrue(range4.lowerFinite())
        assertFalse(range4.upperFinite())
        assertEquals(6, range4.lowerBound())
        assertThrows(IllegalStateException::class.java) { range4.upperBound() }
        assertTrue(range4.lowerInclusive())
        assertThrows(IllegalStateException::class.java) { range4.upperInclusive() }

        assertFalse(range4.contains(5))
        assertTrue(range4.contains(6)) { "default upper range should be inclusive of lower bound" }
        assertTrue(range4.contains(Int.MAX_VALUE))

        val range5 = Range.upToAndIncluding(7)

        assertFalse(range5.lowerFinite())
        assertTrue(range5.upperFinite())
        assertThrows(IllegalStateException::class.java) { range5.lowerBound() }
        assertEquals(7, range5.upperBound())
        assertThrows(IllegalStateException::class.java) { range5.lowerInclusive() }
        assertTrue(range5.upperInclusive())

        assertTrue(range5.contains(Int.MIN_VALUE))
        assertTrue(range5.contains(7)) { "default lower range should be inclusive of upper bound" }
        assertFalse(range5.contains(8))
    }

    @Test
    fun `test intersects`() {
        val range1 = Range(1)
        val range2 = Range(2, 3)
        val range3 = Range(2, 4)
        val range4 = Range(3, 4)
        val range5 = Range(4, false, 5, false)

        assertNull(range1.intersect(range2)) { "$range1 ∩ $range2  should be <null>" }
        assertEquals(range2, range2.intersect(range3))
        assertEquals(range4, range3.intersect(range4))
        assertNull(range4.intersect(range5)) { "$range4 ∩ $range5 should be <null>" }
    }

    @Test
    fun `test comparisons`() {
        val range1 = Range(1)
        val range2 = Range(2, 3)
        val range3 = Range(2, 4)
        val range4 = Range(3, 4)
        val range5 = Range(4, true, 5, false)

        assertTrue(range1 < range2) { "$range1 < $range2" }
        assertTrue(range2 <= range3) { "$range2 <= $range3" }
        assertTrue(range2 >= range3) { "$range2 => $range3" }
        assertTrue(range3 <= range4) { "$range3 <= $range4" }
        assertTrue(range3 >= range4) { "$range3 => $range4" }
        assertTrue(range4 < range5) { "$range4 < $range5" }
    }

    @Test
    fun `test overlaps`() {
        val range0 = Range<Int>()
        val range1 = Range(1)
        val range2 = Range(2, 3)
        val range3 = Range(2, 4)
        val range4 = Range(3, 4)
        val range5 = Range(4, true, 5, false)

        assertTrue(range0.overlaps(range1)) { "$range0 overlaps anything" }
        assertTrue(range0.overlaps(range2)) { "$range0 overlaps anything" }
        assertTrue(range0.overlaps(range3)) { "$range0 overlaps anything" }
        assertTrue(range0.overlaps(range4)) { "$range0 overlaps anything" }
        assertTrue(range0.overlaps(range5)) { "$range0 overlaps anything" }
        assertFalse(range1.overlaps(range2)) { "$range1 does not overlap $range2" }
        assertTrue(range2.overlaps(range3)) { "$range2 overlaps $range3" }
        assertFalse(range2.overlaps(range4)) { "$range2 does not overlap $range4" }
        assertFalse(range4.overlaps(range5)) { "$range4 does not overlap $range5" }
    }
}