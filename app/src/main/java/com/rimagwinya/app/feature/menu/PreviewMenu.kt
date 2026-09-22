package com.rimagwinya.app.feature.menu

import com.rimagwinya.app.core.money.Money
import com.rimagwinya.app.domain.model.BaseStep
import com.rimagwinya.app.domain.model.ItemOption
import com.rimagwinya.app.domain.model.MenuCategory
import com.rimagwinya.app.domain.model.MenuItem
import com.rimagwinya.app.domain.model.OptionGroup
import com.rimagwinya.app.domain.model.OptionGroupType
import com.rimagwinya.app.domain.model.Temperature

/**
 * Sample items so the Compose previews render real content.
 *
 * Kept small and only used by `@Preview` functions, which R8 strips from
 * release builds along with the previews themselves. The equivalent fixtures
 * for tests live in the test source set.
 */
internal object PreviewMenu {

    private fun option(key: String, name: String, rands: Int, default: Boolean = false) =
        ItemOption("opt-$key", key, name, Money.ofRands(rands), default, 0)

    val vetkoek = MenuItem(
        id = "vetkoek",
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
        baseStep = BaseStep("Vetkoeks", "vetkoek", min = 0, start = 1),
        optionGroups = listOf(
            OptionGroup(
                id = "fill", key = "fill", label = "Fillings",
                type = OptionGroupType.Qty, replacesPrice = false, sortOrder = 10,
                options = listOf(
                    option("polony", "Polony", 3),
                    option("liver", "Liver spread", 4),
                    option("cheese", "Cheese slice", 5),
                    option("snoek", "Snoek", 10),
                ),
            )
        ),
        sortOrder = 20,
    )

    val chips = MenuItem(
        id = "chips",
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
                id = "size", key = "size", label = "Choose a size",
                type = OptionGroupType.Single, replacesPrice = true, sortOrder = 10,
                options = listOf(
                    option("s", "Small", 28, default = true),
                    option("m", "Medium", 35),
                    option("l", "Large", 40),
                ),
            )
        ),
        sortOrder = 30,
    )

    val coke = MenuItem(
        id = "coke",
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

    val score = MenuItem(
        id = "score",
        slug = "score",
        name = "Score Energy 500ml",
        description = "Classic value energy drink.",
        category = MenuCategory.Drinks,
        price = Money.ofRands(18),
        iconKey = "energy",
        imageUrl = null,
        stockQuantity = 4,
        reorderLevel = 5,
        isAvailable = true,
        temperature = Temperature.Cold,
        baseStep = null,
        optionGroups = emptyList(),
        sortOrder = 170,
    )

    val doritos = MenuItem(
        id = "doritos",
        slug = "doritos",
        name = "Doritos",
        description = "Sharing bag.",
        category = MenuCategory.Snacks,
        price = Money.ofRands(25),
        iconKey = "triangle",
        imageUrl = null,
        stockQuantity = 0,
        reorderLevel = 5,
        isAvailable = true,
        temperature = null,
        baseStep = null,
        optionGroups = emptyList(),
        sortOrder = 50,
    )

    val all = listOf(vetkoek, chips, doritos, coke, score)
}
