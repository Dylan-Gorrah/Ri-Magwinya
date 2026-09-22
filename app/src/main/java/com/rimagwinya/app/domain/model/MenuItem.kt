package com.rimagwinya.app.domain.model

import com.rimagwinya.app.core.money.Money

/**
 * The menu as the rest of the app thinks about it.
 *
 * Money is [Money] here, not Double. The conversion from the server's
 * numeric(10,2) happens once, at the boundary, so nothing downstream can
 * accidentally do decimal arithmetic on a price.
 *
 * Phase 4 builds the option engine on top of this.
 */
data class MenuItem(
    val id: String,
    val slug: String,
    val name: String,
    val description: String?,
    val category: MenuCategory,
    val price: Money,
    val iconKey: String,
    val imageUrl: String?,
    val stockQuantity: Int,
    val reorderLevel: Int,
    val isAvailable: Boolean,
    val temperature: Temperature?,
    val baseStep: BaseStep?,
    val optionGroups: List<OptionGroup>,
    val sortOrder: Int,
) {
    /** A build item's steppers are its quantity, so it has no outer count. */
    val isBuildItem: Boolean
        get() = baseStep != null || optionGroups.any { it.type == OptionGroupType.Qty }

    val isSoldOut: Boolean
        get() = !isAvailable || stockQuantity <= 0

    val isLowStock: Boolean
        get() = !isSoldOut && stockQuantity <= reorderLevel
}

enum class MenuCategory(val serverName: String) {
    Meals("Meals"),
    Snacks("Snacks"),
    Drinks("Drinks");

    companion object {
        fun from(raw: String): MenuCategory =
            entries.firstOrNull { it.serverName.equals(raw, ignoreCase = true) } ?: Meals
    }
}

enum class Temperature { Hot, Cold;
    companion object {
        fun from(raw: String?): Temperature? = when (raw?.lowercase()) {
            "hot" -> Hot
            "cold" -> Cold
            else -> null
        }
    }
}

/**
 * A count of the item itself, priced per unit. Vetkoek is the only one:
 * "Vetkoeks" / "vetkoek", minimum 0, because fillings on their own are a
 * legitimate order that consumes no vetkoek stock.
 */
data class BaseStep(
    val label: String,
    val singular: String,
    val min: Int,
    val start: Int = 1,
)

enum class OptionGroupType { Single, Qty;
    companion object {
        fun from(raw: String): OptionGroupType =
            if (raw.equals("qty", ignoreCase = true)) Qty else Single
    }
}

data class OptionGroup(
    val id: String,
    val key: String,
    val label: String,
    val type: OptionGroupType,
    /** Chips sizes: the chosen price replaces the item price, not adds to it. */
    val replacesPrice: Boolean,
    val options: List<ItemOption>,
    val sortOrder: Int,
)

data class ItemOption(
    val id: String,
    val key: String,
    val name: String,
    val price: Money,
    val isDefault: Boolean,
    val sortOrder: Int,
)
