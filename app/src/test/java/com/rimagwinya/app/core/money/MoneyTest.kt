package com.rimagwinya.app.core.money

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Money is the foundation the pricing engine sits on, so it gets tested
 * before anything depends on it.
 */
class MoneyTest {

    @Test
    fun `formats whole rands with two decimal places`() {
        assertEquals("R3.00", Money.ofRands(3).format())
        assertEquals("R1000.00", Money.ofRands(1000).format())
    }

    @Test
    fun `formats cents without dropping a leading zero`() {
        assertEquals("R12.50", Money.ofCents(1250).format())
        assertEquals("R0.05", Money.ofCents(5).format())
        assertEquals("R0.00", Money.ZERO.format())
    }

    @Test
    fun `formats negative amounts with the sign before the R`() {
        assertEquals("-R12.50", Money.ofCents(-1250).format())
    }

    @Test
    fun `arithmetic stays exact where a Double would drift`() {
        // 0.1 + 0.2 in Double is 0.30000000000000004. In cents it is 30.
        val sum = Money.ofCents(10) + Money.ofCents(20)
        assertEquals(30L, sum.cents)
        assertEquals("R0.30", sum.format())
    }

    @Test
    fun `a vetkoek order adds up to the value in the brief`() {
        // 2 vetkoeks at R3, 2 polony at R3, 1 cheese slice at R5
        val total = Money.ofRands(3) * 2 + Money.ofRands(3) * 2 + Money.ofRands(5)
        assertEquals("R17.00", total.format())
    }

    @Test
    fun `sums a list of lines`() {
        val lines = listOf(Money.ofRands(17), Money.ofRands(10))
        assertEquals("R27.00", lines.sum().format())
    }

    @Test
    fun `reads the servers decimal values without losing a cent`() {
        assertEquals(1250L, Money.fromDecimal(12.50).cents)
        // Rounds rather than truncating, so this is R12.50 and not R12.49
        assertEquals(1250L, Money.fromDecimal(12.499999999).cents)
        assertEquals(300L, Money.fromDecimal(3.0).cents)
    }

    @Test
    fun `compares and reports zero correctly`() {
        assertTrue(Money.ofRands(10) > Money.ofRands(9))
        assertTrue(Money.ZERO.isZero)
        assertFalse(Money.ZERO.isPositive)
        assertTrue(Money.ofCents(1).isPositive)
    }
}
