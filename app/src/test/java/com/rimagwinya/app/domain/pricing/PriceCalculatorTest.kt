package com.rimagwinya.app.domain.pricing

import com.rimagwinya.app.domain.pricing.MenuFixtures.chips
import com.rimagwinya.app.domain.pricing.MenuFixtures.coke
import com.rimagwinya.app.domain.pricing.MenuFixtures.optionId
import com.rimagwinya.app.domain.pricing.MenuFixtures.sweets
import com.rimagwinya.app.domain.pricing.MenuFixtures.tea
import com.rimagwinya.app.domain.pricing.MenuFixtures.vetkoek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The check values from section 9.1.
 *
 * These same orders are placed against a real Postgres in
 * `supabase/tests/pricing_test.sql`. Both sides must produce the same number:
 * the server decides what is charged, and this decides what the student is
 * shown before they commit.
 */
class PriceCalculatorTest {

    // =====================================================================
    // The check values
    // =====================================================================

    @Test
    fun `2 vetkoeks plus 2 polony plus 1 cheese is R17`() {
        val selection = Selection(baseQty = 2)
            .withCount(optionId("polony"), 2)
            .withCount(optionId("cheese"), 1)

        assertEquals("R17.00", PriceCalculator.unitPrice(vetkoek, selection).format())
        assertEquals("R17.00", PriceCalculator.lineTotal(vetkoek, selection).format())
        assertEquals(2, PriceCalculator.unitsConsumed(vetkoek, selection))
    }

    @Test
    fun `0 vetkoeks plus 1 snoek is R10 and takes no vetkoek stock`() {
        val selection = Selection(baseQty = 0).withCount(optionId("snoek"), 1)

        assertEquals("R10.00", PriceCalculator.unitPrice(vetkoek, selection).format())
        // The rule most likely to be got wrong: fillings on their own are a
        // real order that consumes nothing off the vetkoek shelf.
        assertEquals(0, PriceCalculator.unitsConsumed(vetkoek, selection))
        assertTrue(PriceCalculator.canAddToCart(vetkoek, selection))
    }

    @Test
    fun `3 Chappies plus 1 mix packet is R13`() {
        val selection = Selection()
            .withCount(optionId("chappies"), 3)
            .withCount(optionId("mix"), 1)

        assertEquals("R13.00", PriceCalculator.unitPrice(sweets, selection).format())
    }

    @Test
    fun `large chips is R40 and not R68`() {
        val selection = Selection(singles = mapOf("grp-size" to optionId("l")))

        // The size replaces the base price rather than adding to it.
        assertEquals("R40.00", PriceCalculator.unitPrice(chips, selection).format())
    }

    @Test
    fun `tea is R10 whatever you do to it`() {
        val plain = Selection(
            singles = mapOf(
                "grp-brand" to optionId("five"),
                "grp-milk" to optionId("with"),
                "grp-sugar" to optionId("s2"),
            )
        )
        val fancy = Selection(
            singles = mapOf(
                "grp-brand" to optionId("rooibos"),
                "grp-milk" to optionId("black"),
                "grp-sugar" to optionId("s3"),
            )
        )

        assertEquals("R10.00", PriceCalculator.unitPrice(tea, plain).format())
        assertEquals("R10.00", PriceCalculator.unitPrice(tea, fancy).format())
    }

    // =====================================================================
    // Build items versus outer quantity
    // =====================================================================

    @Test
    fun `a plain item multiplies by the outer stepper`() {
        val selection = Selection(quantity = 4)

        assertEquals("R16.00", PriceCalculator.unitPrice(coke, selection).format())
        assertEquals("R64.00", PriceCalculator.lineTotal(coke, selection).format())
        assertEquals(4, PriceCalculator.quantity(coke, selection))
        assertEquals(4, PriceCalculator.unitsConsumed(coke, selection))
    }

    @Test
    fun `a build item ignores the outer stepper entirely`() {
        // Even if a quantity somehow arrives, the steppers are the quantity.
        val selection = Selection(baseQty = 2, quantity = 9)
            .withCount(optionId("polony"), 2)
            .withCount(optionId("cheese"), 1)

        assertEquals(1, PriceCalculator.quantity(vetkoek, selection))
        assertEquals("R17.00", PriceCalculator.lineTotal(vetkoek, selection).format())
    }

    @Test
    fun `sweets counts as a build item because of its qty group`() {
        assertTrue(sweets.isBuildItem)
        assertTrue(vetkoek.isBuildItem)
        // Chips has a replacing group but no qty group and no base step, so
        // it keeps its outer stepper.
        assertFalse(chips.isBuildItem)
        assertFalse(coke.isBuildItem)
    }

    @Test
    fun `two large chips is R80`() {
        val selection = Selection(singles = mapOf("grp-size" to optionId("l")), quantity = 2)
        assertEquals("R80.00", PriceCalculator.lineTotal(chips, selection).format())
    }

    // =====================================================================
    // Nothing chosen
    // =====================================================================

    @Test
    fun `an untouched sweets sheet cannot be added to the cart`() {
        val selection = Selection()

        assertEquals("R0.00", PriceCalculator.unitPrice(sweets, selection).format())
        assertFalse(PriceCalculator.canAddToCart(sweets, selection))
    }

    @Test
    fun `zero vetkoeks with no fillings cannot be added either`() {
        val selection = Selection(baseQty = 0)

        assertEquals("R0.00", PriceCalculator.unitPrice(vetkoek, selection).format())
        assertFalse(PriceCalculator.canAddToCart(vetkoek, selection))
    }

    // =====================================================================
    // How a sheet opens
    // =====================================================================

    @Test
    fun `a vetkoek sheet opens on one vetkoek`() {
        val selection = Selection.initial(vetkoek)

        assertEquals(1, selection.baseQty)
        assertEquals("R3.00", PriceCalculator.unitPrice(vetkoek, selection).format())
        assertTrue(PriceCalculator.canAddToCart(vetkoek, selection))
    }

    @Test
    fun `a chips sheet opens on small`() {
        val selection = Selection.initial(chips)
        assertEquals("R28.00", PriceCalculator.unitPrice(chips, selection).format())
    }

    @Test
    fun `a tea sheet opens on the defaults`() {
        val selection = Selection.initial(tea)

        assertEquals("R10.00", PriceCalculator.unitPrice(tea, selection).format())
        assertEquals("Five Roses, With milk, No sugar", PriceCalculator.label(tea, selection))
    }

    // =====================================================================
    // "from R…" on the menu
    // =====================================================================

    @Test
    fun `from prices match the prototype`() {
        assertEquals("R3.00", PriceCalculator.fromPrice(vetkoek).format())
        assertEquals("R28.00", PriceCalculator.fromPrice(chips).format())
        assertEquals("R1.00", PriceCalculator.fromPrice(sweets).format())
        assertEquals("R16.00", PriceCalculator.fromPrice(coke).format())
    }

    @Test
    fun `only variable-priced items say from`() {
        assertTrue(PriceCalculator.hasVariablePrice(vetkoek))
        assertTrue(PriceCalculator.hasVariablePrice(chips))
        assertTrue(PriceCalculator.hasVariablePrice(sweets))
        assertFalse(PriceCalculator.hasVariablePrice(coke))
        // Tea has options, but they are all free and none replaces the price.
        assertFalse(PriceCalculator.hasVariablePrice(tea))
    }

    // =====================================================================
    // Labels, which are frozen onto the order
    // =====================================================================

    @Test
    fun `labels read the way the prototype writes them`() {
        val built = Selection(baseQty = 2)
            .withCount(optionId("polony"), 2)
            .withCount(optionId("cheese"), 1)
        assertEquals("2 vetkoeks, 2 Polony, Cheese slice", PriceCalculator.label(vetkoek, built))

        val fillingsOnly = Selection(baseQty = 0).withCount(optionId("snoek"), 1)
        assertEquals("No vetkoek, Snoek", PriceCalculator.label(vetkoek, fillingsOnly))

        val single = Selection(baseQty = 1).withCount(optionId("polony"), 1)
        assertEquals("1 vetkoek, Polony", PriceCalculator.label(vetkoek, single))

        val mixed = Selection().withCount(optionId("chappies"), 3).withCount(optionId("mix"), 1)
        assertEquals("3 Chappies, Assorted mix packet", PriceCalculator.label(sweets, mixed))
    }

    @Test
    fun `options with a count of zero are left out of the label`() {
        val selection = Selection(baseQty = 1)
            .withCount(optionId("polony"), 2)
            .withCount(optionId("cheese"), 0)

        assertEquals("1 vetkoek, 2 Polony", PriceCalculator.label(vetkoek, selection))
    }

    @Test
    fun `a plain item has an empty label`() {
        assertEquals("", PriceCalculator.label(coke, Selection(quantity = 2)))
    }

    // =====================================================================
    // Cart line identity
    // =====================================================================

    @Test
    fun `different selections of the same item are different lines`() {
        val withPolony = Selection(baseQty = 1).withCount(optionId("polony"), 2)
        val withSnoek = Selection(baseQty = 1).withCount(optionId("snoek"), 1)

        // Selection is a data class, so equality is structural and the cart
        // can key lines on it directly.
        assertFalse(withPolony == withSnoek)
        assertTrue(withPolony == Selection(baseQty = 1).withCount(optionId("polony"), 2))
    }

    @Test
    fun `setting a count to zero removes it rather than storing a zero`() {
        // Otherwise two identical builds would compare unequal because one
        // carried a leftover zero.
        val a = Selection(baseQty = 1).withCount(optionId("polony"), 1).withCount(optionId("polony"), 0)
        val b = Selection(baseQty = 1)

        assertEquals(b, a)
    }
}
