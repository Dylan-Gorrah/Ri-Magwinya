package com.rimagwinya.app.data.remote

import com.rimagwinya.app.core.money.Money
import com.rimagwinya.app.domain.model.MenuChange
import com.rimagwinya.app.domain.model.applying
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Live stock: turning a realtime row into a patch, and applying the patch
 * to exactly one item.
 */
class MenuChangeTest {

    private val menu = listOf(
        MenuItemDto(
            id = "coke", slug = "coke", name = "Coca-Cola 440ml", category = "Drinks",
            price = 16.0, iconKey = "can", stockQuantity = 44, reorderLevel = 6,
            isAvailable = true,
        ).toDomain(),
        MenuItemDto(
            id = "score", slug = "score", name = "Score Energy 500ml", category = "Drinks",
            price = 18.0, iconKey = "energy", stockQuantity = 1, reorderLevel = 5,
            isAvailable = true,
        ).toDomain(),
    )

    @Test
    fun `a full row becomes a patch`() {
        val change = buildJsonObject {
            put("id", "score")
            put("stock_quantity", 0)
            put("is_available", true)
            put("price", 18.0)
            put("name", "Score Energy 500ml")
        }.toMenuUpdate()!!

        assertEquals("score", change.id)
        assertEquals(0, change.stockQuantity)
        assertEquals(true, change.isAvailable)
        assertEquals(Money(1800), change.price)
    }

    @Test
    fun `a numeric sent as a string still parses`() {
        val change = buildJsonObject {
            put("id", "coke")
            put("price", JsonPrimitive("17.50"))
        }.toMenuUpdate()!!

        assertEquals(Money(1750), change.price)
    }

    @Test
    fun `missing columns are left alone`() {
        val change = buildJsonObject { put("id", "coke") }.toMenuUpdate()!!

        assertNull(change.stockQuantity)
        assertNull(change.isAvailable)
        assertNull(change.price)
        assertEquals(menu, menu.applying(change))
    }

    @Test
    fun `a row without an id is ignored`() {
        assertNull(buildJsonObject { put("stock_quantity", 3) }.toMenuUpdate())
    }

    @Test
    fun `the last one selling marks only that item sold out`() {
        val after = menu.applying(
            MenuChange.Updated(id = "score", stockQuantity = 0, isAvailable = null, price = null),
        )

        assertTrue(after.first { it.id == "score" }.isSoldOut)
        assertFalse(after.first { it.id == "coke" }.isSoldOut)
        assertSame(menu[0], after[0])
    }

    @Test
    fun `staff switching an item off hides it even with stock`() {
        val after = menu.applying(
            MenuChange.Updated(id = "coke", stockQuantity = null, isAvailable = false, price = null),
        )

        assertTrue(after.first { it.id == "coke" }.isSoldOut)
        assertEquals(44, after.first { it.id == "coke" }.stockQuantity)
    }

    @Test
    fun `an unknown id changes nothing`() {
        val after = menu.applying(
            MenuChange.Updated(id = "nope", stockQuantity = 0, isAvailable = false, price = null),
        )

        assertEquals(menu, after)
    }
}
