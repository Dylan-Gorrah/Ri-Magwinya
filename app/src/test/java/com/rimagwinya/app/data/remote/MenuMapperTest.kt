package com.rimagwinya.app.data.remote

import com.rimagwinya.app.domain.model.MenuCategory
import com.rimagwinya.app.domain.model.OptionGroupType
import com.rimagwinya.app.domain.model.Temperature
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The boundary where the server's decimals become integer cents. If this is
 * wrong, every price in the app is wrong, so it is tested against the real
 * seed values rather than invented ones.
 */
class MenuMapperTest {

    private fun dto(
        slug: String = "coke",
        price: Double = 16.0,
        category: String = "Drinks",
        stock: Int = 44,
        reorder: Int = 6,
        available: Boolean = true,
        temperature: String? = "cold",
        baseLabel: String? = null,
        baseSingular: String? = null,
        baseMin: Int? = null,
        groups: List<OptionGroupDto> = emptyList(),
    ) = MenuItemDto(
        id = "id-$slug",
        slug = slug,
        name = slug,
        description = null,
        category = category,
        price = price,
        iconKey = "can",
        stockQuantity = stock,
        reorderLevel = reorder,
        isAvailable = available,
        temperatureTag = temperature,
        baseStepLabel = baseLabel,
        baseStepSingular = baseSingular,
        baseStepMin = baseMin,
        optionGroups = groups,
    )

    @Test
    fun `price becomes integer cents`() {
        assertEquals(1600L, dto(price = 16.0).toDomain().price.cents)
        assertEquals(300L, dto(price = 3.0).toDomain().price.cents)
        assertEquals(1250L, dto(price = 12.50).toDomain().price.cents)
    }

    @Test
    fun `a plain drink is not a build item`() {
        val coke = dto().toDomain()
        assertFalse(coke.isBuildItem)
        assertNull(coke.baseStep)
        assertEquals(Temperature.Cold, coke.temperature)
        assertEquals(MenuCategory.Drinks, coke.category)
    }

    @Test
    fun `vetkoek maps its base step and counts as a build item`() {
        val vetkoek = dto(
            slug = "vetkoek",
            price = 3.0,
            category = "Meals",
            temperature = "hot",
            baseLabel = "Vetkoeks",
            baseSingular = "vetkoek",
            baseMin = 0,
        ).toDomain()

        assertTrue(vetkoek.isBuildItem)
        assertEquals("Vetkoeks", vetkoek.baseStep?.label)
        assertEquals("vetkoek", vetkoek.baseStep?.singular)
        // Zero vetkoeks is legal — fillings on their own.
        assertEquals(0, vetkoek.baseStep?.min)
        // but the sheet still opens on one
        assertEquals(1, vetkoek.baseStep?.start)
    }

    @Test
    fun `a qty group alone makes an item a build item`() {
        val sweets = dto(
            slug = "sweets",
            price = 0.0,
            category = "Snacks",
            temperature = null,
            groups = listOf(
                OptionGroupDto(
                    id = "g", key = "pick", label = "Pick your sweets", type = "qty",
                    options = listOf(
                        OptionDto(id = "o1", key = "chappies", name = "Chappies", price = 1.0),
                    ),
                )
            ),
        ).toDomain()

        assertTrue(sweets.isBuildItem)
        assertEquals(OptionGroupType.Qty, sweets.optionGroups.single().type)
        assertEquals(100L, sweets.optionGroups.single().options.single().price.cents)
    }

    @Test
    fun `a replacing single group is not a build item`() {
        // Chips has a size group that replaces the price, but no qty group
        // and no base step, so it keeps its outer quantity stepper.
        val chips = dto(
            slug = "chips", price = 28.0, category = "Meals", temperature = "hot",
            groups = listOf(
                OptionGroupDto(
                    id = "g", key = "size", label = "Choose a size", type = "single",
                    replacesPrice = true,
                    options = listOf(
                        OptionDto(id = "s", key = "s", name = "Small", price = 28.0, isDefault = true),
                        OptionDto(id = "l", key = "l", name = "Large", price = 40.0),
                    ),
                )
            ),
        ).toDomain()

        assertFalse(chips.isBuildItem)
        assertTrue(chips.optionGroups.single().replacesPrice)
        assertEquals(4000L, chips.optionGroups.single().options.last().price.cents)
    }

    @Test
    fun `stock state follows quantity and reorder level`() {
        assertFalse(dto(stock = 44, reorder = 6).toDomain().isLowStock)
        assertTrue(dto(stock = 4, reorder = 5).toDomain().isLowStock)
        assertTrue(dto(stock = 0).toDomain().isSoldOut)
        // Hidden by staff counts as sold out even with stock on the shelf
        assertTrue(dto(stock = 20, available = false).toDomain().isSoldOut)
    }

    @Test
    fun `groups and options come back in their sort order`() {
        val item = dto(
            groups = listOf(
                OptionGroupDto(
                    id = "b", key = "sugar", label = "Sugar", type = "single", sortOrder = 30,
                    options = listOf(
                        OptionDto(id = "o2", key = "s1", name = "1 spoon", sortOrder = 20),
                        OptionDto(id = "o1", key = "s0", name = "No sugar", sortOrder = 10),
                    ),
                ),
                OptionGroupDto(id = "a", key = "milk", label = "Milk", type = "single", sortOrder = 20),
            ),
        ).toDomain()

        assertEquals(listOf("milk", "sugar"), item.optionGroups.map { it.key })
        assertEquals(listOf("No sugar", "1 spoon"), item.optionGroups.last().options.map { it.name })
    }
}
