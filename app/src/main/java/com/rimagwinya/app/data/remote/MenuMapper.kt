package com.rimagwinya.app.data.remote

import com.rimagwinya.app.core.money.Money
import com.rimagwinya.app.domain.model.BaseStep
import com.rimagwinya.app.domain.model.ItemOption
import com.rimagwinya.app.domain.model.MenuCategory
import com.rimagwinya.app.domain.model.MenuChange
import com.rimagwinya.app.domain.model.MenuItem
import com.rimagwinya.app.domain.model.OptionGroup
import com.rimagwinya.app.domain.model.OptionGroupType
import com.rimagwinya.app.domain.model.Temperature
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * The one place wire types become domain types.
 *
 * Most importantly, the one place `Double` becomes [Money]. After this
 * boundary no price is ever a decimal again.
 */
fun MenuItemDto.toDomain(): MenuItem = MenuItem(
    id = id,
    slug = slug,
    name = name,
    description = description,
    category = MenuCategory.from(category),
    price = Money.fromDecimal(price),
    iconKey = iconKey,
    imageUrl = imageUrl,
    stockQuantity = stockQuantity,
    reorderLevel = reorderLevel,
    isAvailable = isAvailable,
    temperature = Temperature.from(temperatureTag),
    baseStep = baseStepLabel?.let { label ->
        BaseStep(
            label = label,
            singular = baseStepSingular ?: label,
            min = baseStepMin ?: 0,
            // The sheet opens on one vetkoek, but zero is allowed.
            start = maxOf(baseStepMin ?: 0, 1),
        )
    },
    optionGroups = optionGroups
        .sortedBy { it.sortOrder }
        .map { it.toDomain() },
    sortOrder = sortOrder,
)

fun OptionGroupDto.toDomain(): OptionGroup = OptionGroup(
    id = id,
    key = key,
    label = label,
    type = OptionGroupType.from(type),
    replacesPrice = replacesPrice,
    options = options.sortedBy { it.sortOrder }.map { it.toDomain() },
    sortOrder = sortOrder,
)

/**
 * A realtime `menu_items` row as a [MenuChange.Updated], or null if it has
 * no id. Read field by field rather than decoded into [MenuItemDto], so a
 * payload that omits a column, or sends a numeric as a string, patches what
 * it can instead of failing.
 */
fun JsonObject.toMenuUpdate(): MenuChange.Updated? {
    val id = this["id"]?.jsonPrimitive?.contentOrNull ?: return null
    return MenuChange.Updated(
        id = id,
        stockQuantity = this["stock_quantity"]?.jsonPrimitive?.intOrNull,
        isAvailable = this["is_available"]?.jsonPrimitive?.booleanOrNull,
        price = this["price"]?.jsonPrimitive?.doubleOrNull?.let(Money::fromDecimal),
    )
}

fun OptionDto.toDomain(): ItemOption = ItemOption(
    id = id,
    key = key,
    name = name,
    price = Money.fromDecimal(price),
    isDefault = isDefault,
    sortOrder = sortOrder,
)
