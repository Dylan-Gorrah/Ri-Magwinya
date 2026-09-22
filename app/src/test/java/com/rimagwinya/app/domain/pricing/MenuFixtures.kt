package com.rimagwinya.app.domain.pricing

import com.rimagwinya.app.core.money.Money
import com.rimagwinya.app.domain.model.BaseStep
import com.rimagwinya.app.domain.model.ItemOption
import com.rimagwinya.app.domain.model.MenuCategory
import com.rimagwinya.app.domain.model.MenuItem
import com.rimagwinya.app.domain.model.OptionGroup
import com.rimagwinya.app.domain.model.OptionGroupType
import com.rimagwinya.app.domain.model.Temperature

/**
 * The real menu from section 8, as domain objects.
 *
 * These mirror `0010_seed_menu.sql` exactly, so the client tests and the
 * database tests are checking the same items with the same prices. If the
 * seed changes, these change with it.
 */
object MenuFixtures {

    private fun option(key: String, name: String, rands: Int, default: Boolean = false) =
        ItemOption(
            id = "opt-$key",
            key = key,
            name = name,
            price = Money.ofRands(rands),
            isDefault = default,
            sortOrder = 0,
        )

    /** R3 each, minimum zero, with six fillings on their own steppers. */
    val vetkoek = MenuItem(
        id = "item-vetkoek",
        slug = "vetkoek",
        name = "Vetkoek",
        description = "Fried fresh every morning. Build it however you like it.",
        category = MenuCategory.Meals,
        price = Money.ofRands(3),
        iconKey = "vetkoek",
        imageUrl = null,
        stockQuantity = 40,
        reorderLevel = 8,
        isAvailable = true,
        temperature = Temperature.Hot,
        baseStep = BaseStep(label = "Vetkoeks", singular = "vetkoek", min = 0, start = 1),
        optionGroups = listOf(
            OptionGroup(
                id = "grp-fill",
                key = "fill",
                label = "Fillings",
                type = OptionGroupType.Qty,
                replacesPrice = false,
                sortOrder = 10,
                options = listOf(
                    option("polony", "Polony", 3),
                    option("liver", "Liver spread", 4),
                    option("salami", "Salami", 4),
                    option("cheese", "Cheese slice", 5),
                    option("achar", "Achar", 5),
                    option("snoek", "Snoek", 10),
                ),
            )
        ),
        sortOrder = 20,
    )

    /** The only item whose option group replaces the price. */
    val chips = MenuItem(
        id = "item-chips",
        slug = "chips",
        name = "Fried chips",
        description = "Hot slap chips. Salt and vinegar at the counter.",
        category = MenuCategory.Meals,
        price = Money.ofRands(28),
        iconKey = "chips",
        imageUrl = null,
        stockQuantity = 30,
        reorderLevel = 5,
        isAvailable = true,
        temperature = Temperature.Hot,
        baseStep = null,
        optionGroups = listOf(
            OptionGroup(
                id = "grp-size",
                key = "size",
                label = "Choose a size",
                type = OptionGroupType.Single,
                replacesPrice = true,
                sortOrder = 10,
                options = listOf(
                    option("s", "Small", 28, default = true),
                    option("m", "Medium", 35),
                    option("l", "Large", 40),
                ),
            )
        ),
        sortOrder = 30,
    )

    /** Free item, everything in a qty group. An untouched sheet costs nothing. */
    val sweets = MenuItem(
        id = "item-sweets",
        slug = "sweets",
        name = "Assorted sweets",
        description = "Pick and mix from the jar at the counter.",
        category = MenuCategory.Snacks,
        price = Money.ZERO,
        iconKey = "sweet",
        imageUrl = null,
        stockQuantity = 80,
        reorderLevel = 10,
        isAvailable = true,
        temperature = null,
        baseStep = null,
        optionGroups = listOf(
            OptionGroup(
                id = "grp-pick",
                key = "pick",
                label = "Pick your sweets",
                type = OptionGroupType.Qty,
                replacesPrice = false,
                sortOrder = 10,
                options = listOf(
                    option("chappies", "Chappies", 1),
                    option("fizzer", "Fizzer", 2),
                    option("sparkles", "Sparkles", 3),
                    option("lolly", "Lollipop", 3),
                    option("mix", "Assorted mix packet", 10),
                ),
            )
        ),
        sortOrder = 60,
    )

    /** Three questions, none of which change the price. */
    val tea = MenuItem(
        id = "item-tea",
        slug = "tea",
        name = "Tea",
        description = "Served hot. Brewed to order at the counter.",
        category = MenuCategory.Drinks,
        price = Money.ofRands(10),
        iconKey = "cup",
        imageUrl = null,
        stockQuantity = 50,
        reorderLevel = 10,
        isAvailable = true,
        temperature = Temperature.Hot,
        baseStep = null,
        optionGroups = listOf(
            OptionGroup(
                id = "grp-brand", key = "brand", label = "Which tea",
                type = OptionGroupType.Single, replacesPrice = false, sortOrder = 10,
                options = listOf(
                    option("five", "Five Roses", 0, default = true),
                    option("rooibos", "Freshpak Rooibos", 0),
                    option("joko", "Joko", 0),
                    option("glen", "Glen", 0),
                ),
            ),
            OptionGroup(
                id = "grp-milk", key = "milk", label = "Milk",
                type = OptionGroupType.Single, replacesPrice = false, sortOrder = 20,
                options = listOf(
                    option("with", "With milk", 0, default = true),
                    option("black", "Black", 0),
                ),
            ),
            OptionGroup(
                id = "grp-sugar", key = "sugar", label = "Sugar",
                type = OptionGroupType.Single, replacesPrice = false, sortOrder = 30,
                options = listOf(
                    option("s0", "No sugar", 0, default = true),
                    option("s1", "1 spoon", 0),
                    option("s2", "2 spoons", 0),
                    option("s3", "3 spoons", 0),
                ),
            ),
        ),
        sortOrder = 70,
    )

    /** No options at all. The plain case. */
    val coke = MenuItem(
        id = "item-coke",
        slug = "coke",
        name = "Coca-Cola 440ml",
        description = "Served chilled.",
        category = MenuCategory.Drinks,
        price = Money.ofRands(16),
        iconKey = "can",
        imageUrl = null,
        stockQuantity = 44,
        reorderLevel = 6,
        isAvailable = true,
        temperature = Temperature.Cold,
        baseStep = null,
        optionGroups = emptyList(),
        sortOrder = 90,
    )

    fun optionId(key: String) = "opt-$key"
}
