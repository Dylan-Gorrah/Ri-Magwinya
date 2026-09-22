package com.rimagwinya.app.core.database

import com.rimagwinya.app.core.money.Money
import com.rimagwinya.app.domain.model.BaseStep
import com.rimagwinya.app.domain.model.ItemOption
import com.rimagwinya.app.domain.model.MenuCategory
import com.rimagwinya.app.domain.model.MenuItem
import com.rimagwinya.app.domain.model.OptionGroup
import com.rimagwinya.app.domain.model.OptionGroupType
import com.rimagwinya.app.domain.model.Order
import com.rimagwinya.app.domain.model.OrderLine
import com.rimagwinya.app.domain.model.OrderStatus
import com.rimagwinya.app.domain.model.PaymentMethod
import com.rimagwinya.app.domain.model.Slot
import com.rimagwinya.app.domain.model.Temperature
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

// --- Domain to cache ---------------------------------------------------------

fun MenuItem.toEntity() = MenuItemEntity(
    id = id, slug = slug, name = name, description = description,
    category = category.serverName, priceCents = price.cents, iconKey = iconKey,
    imageUrl = imageUrl, stockQuantity = stockQuantity, reorderLevel = reorderLevel,
    isAvailable = isAvailable, temperature = temperature?.name?.lowercase(),
    baseStepLabel = baseStep?.label, baseStepSingular = baseStep?.singular,
    baseStepMin = baseStep?.min, sortOrder = sortOrder,
)

fun MenuItem.groupEntities() = optionGroups.map { group ->
    OptionGroupEntity(
        id = group.id, itemId = id, key = group.key, label = group.label,
        type = if (group.type == OptionGroupType.Qty) "qty" else "single",
        replacesPrice = group.replacesPrice, sortOrder = group.sortOrder,
    )
}

fun MenuItem.optionEntities() = optionGroups.flatMap { group ->
    group.options.map { option ->
        OptionEntity(
            id = option.id, groupId = group.id, key = option.key, name = option.name,
            priceCents = option.price.cents, isDefault = option.isDefault, sortOrder = option.sortOrder,
        )
    }
}

fun Slot.toEntity() = SlotEntity(
    id = id, name = name, startsAt = startsAt.toString(), endsAt = endsAt.toString(),
    capacity = capacity, ordersTaken = ordersTaken, serviceDate = serviceDate.toString(),
)

fun Order.toEntity() = OrderEntity(
    id = id, number = number, studentId = studentId, code = code, slotId = slotId,
    totalCents = total.cents, status = status.serverName, paymentMethod = paymentMethod.serverName,
    placedAt = placedAt.toString(), preparedAt = preparedAt?.toString(), readyAt = readyAt?.toString(),
    completedAt = completedAt?.toString(), cancelledAt = cancelledAt?.toString(),
    slotName = slotName, slotStart = slotStart?.toString(), slotEnd = slotEnd?.toString(),
    studentName = studentName,
)

fun Order.lineEntities() = lines.map { line ->
    OrderLineEntity(
        id = line.id, orderId = id, itemId = line.itemId, name = line.name,
        optionsLabel = line.optionsLabel, quantity = line.quantity,
        unitPriceCents = line.unitPrice.cents, subtotalCents = line.subtotal.cents,
    )
}

// --- Cache to domain ---------------------------------------------------------

fun CachedMenuItem.toDomain(): MenuItem = MenuItem(
    id = item.id, slug = item.slug, name = item.name, description = item.description,
    category = MenuCategory.from(item.category), price = Money(item.priceCents),
    iconKey = item.iconKey, imageUrl = item.imageUrl, stockQuantity = item.stockQuantity,
    reorderLevel = item.reorderLevel, isAvailable = item.isAvailable,
    temperature = Temperature.from(item.temperature),
    baseStep = item.baseStepLabel?.let {
        BaseStep(
            label = it,
            singular = item.baseStepSingular ?: it,
            min = item.baseStepMin ?: 0,
            start = maxOf(item.baseStepMin ?: 0, 1),
        )
    },
    optionGroups = groups.sortedBy { it.group.sortOrder }.map { it.toDomain() },
    sortOrder = item.sortOrder,
)

fun CachedOptionGroup.toDomain(): OptionGroup = OptionGroup(
    id = group.id, key = group.key, label = group.label,
    type = OptionGroupType.from(group.type), replacesPrice = group.replacesPrice,
    options = options.sortedBy { it.sortOrder }.map {
        ItemOption(it.id, it.key, it.name, Money(it.priceCents), it.isDefault, it.sortOrder)
    },
    sortOrder = group.sortOrder,
)

fun SlotEntity.toDomain(): Slot = Slot(
    id = id, name = name, startsAt = LocalTime.parse(startsAt), endsAt = LocalTime.parse(endsAt),
    capacity = capacity, ordersTaken = ordersTaken, serviceDate = LocalDate.parse(serviceDate),
)

fun CachedOrder.toDomain(): Order = Order(
    id = order.id, number = order.number, studentId = order.studentId, code = order.code,
    slotId = order.slotId, total = Money(order.totalCents), status = OrderStatus.from(order.status),
    paymentMethod = PaymentMethod.from(order.paymentMethod),
    placedAt = Instant.parse(order.placedAt),
    preparedAt = order.preparedAt?.let(Instant::parse),
    readyAt = order.readyAt?.let(Instant::parse),
    completedAt = order.completedAt?.let(Instant::parse),
    cancelledAt = order.cancelledAt?.let(Instant::parse),
    lines = lines.map {
        OrderLine(it.id, it.itemId, it.name, it.optionsLabel, it.quantity, Money(it.unitPriceCents), Money(it.subtotalCents))
    },
    slotName = order.slotName,
    slotStart = order.slotStart?.let(LocalTime::parse),
    slotEnd = order.slotEnd?.let(LocalTime::parse),
    studentName = order.studentName,
)
