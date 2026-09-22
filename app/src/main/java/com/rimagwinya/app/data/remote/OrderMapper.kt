package com.rimagwinya.app.data.remote

import com.rimagwinya.app.core.money.Money
import com.rimagwinya.app.domain.model.CartLine
import com.rimagwinya.app.domain.model.Order
import com.rimagwinya.app.domain.model.OrderLine
import com.rimagwinya.app.domain.model.OrderStatus
import com.rimagwinya.app.domain.model.PaymentMethod
import com.rimagwinya.app.domain.model.Slot
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime

/** Postgres timestamptz, e.g. "2026-09-22T18:56:17.12+00:00". */
fun parseInstant(raw: String): Instant = OffsetDateTime.parse(raw).toInstant()

/** Postgres time, "09:40:00". */
internal fun parseTime(raw: String): LocalTime = LocalTime.parse(raw)

fun CollectionSlotDto.toDomain(): Slot = Slot(
    id = id,
    name = name,
    startsAt = parseTime(startsAt),
    endsAt = parseTime(endsAt),
    capacity = capacity,
    ordersTaken = ordersTaken,
    serviceDate = LocalDate.parse(serviceDate),
)

fun OrderDto.toDomain(): Order = Order(
    id = id,
    number = orderNumber,
    studentId = studentId,
    code = collectionCode.trim(),
    slotId = slotId,
    total = Money.fromDecimal(totalAmount),
    status = OrderStatus.from(status),
    paymentMethod = PaymentMethod.from(paymentMethod),
    placedAt = parseInstant(placedAt),
    preparedAt = preparedAt?.let(::parseInstant),
    readyAt = readyAt?.let(::parseInstant),
    completedAt = completedAt?.let(::parseInstant),
    cancelledAt = cancelledAt?.let(::parseInstant),
    lines = orderItems.map { it.toDomain() },
    slotName = slot?.name,
    slotStart = slot?.startsAt?.let(::parseTime),
    slotEnd = slot?.endsAt?.let(::parseTime),
    studentName = student?.fullName,
)

fun OrderItemDto.toDomain(): OrderLine = OrderLine(
    id = id,
    itemId = itemId,
    name = itemName,
    optionsLabel = optionsLabel,
    quantity = quantity,
    unitPrice = Money.fromDecimal(unitPrice),
    subtotal = Money.fromDecimal(subtotal),
)

/**
 * A cart line as the choices place-order expects. No prices: the server
 * works those out itself.
 *
 * `base_qty` goes only with base-step items and `quantity` only with plain
 * ones, because a build item's steppers are its quantity.
 */
fun CartLine.toBody(): OrderLineBody = OrderLineBody(
    itemId = item.id,
    baseQty = if (item.baseStep != null) selection.baseQty else null,
    quantity = if (item.isBuildItem) null else selection.quantity,
    options = selection.singles.values.map { OrderOptionBody(optionId = it) } +
        selection.counts.map { (id, count) -> OrderOptionBody(optionId = id, count = count) },
)
