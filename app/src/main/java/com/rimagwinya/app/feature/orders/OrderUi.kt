package com.rimagwinya.app.feature.orders

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rimagwinya.app.R
import com.rimagwinya.app.core.designsystem.component.OrderState
import com.rimagwinya.app.core.designsystem.component.RailStep
import com.rimagwinya.app.domain.model.Order
import com.rimagwinya.app.domain.model.OrderStatus
import com.rimagwinya.app.domain.model.PaymentMethod
import com.rimagwinya.app.domain.model.clockTime

/** Domain status to the design system's pill. */
fun OrderStatus.pill(): OrderState = when (this) {
    OrderStatus.Placed -> OrderState.Placed
    OrderStatus.Preparing -> OrderState.Preparing
    OrderStatus.Ready -> OrderState.Ready
    OrderStatus.Collected -> OrderState.Collected
    OrderStatus.Cancelled -> OrderState.Cancelled
    OrderStatus.NoShow -> OrderState.NoShow
}

/**
 * Where the order is, step by step, with the real time against each step
 * that has happened. Which step is current is decided here, not in the UI.
 */
fun railStages(order: Order): List<RailStage> {
    val wallet = order.paymentMethod == PaymentMethod.Wallet
    val happened = listOf(
        true,
        true,
        order.preparedAt != null,
        order.readyAt != null,
        order.completedAt != null,
    )
    // The ring sits on the latest thing that has happened while the order
    // is still live: "Payment confirmed" while it waits, then "Preparing",
    // then "Ready". A finished order has no current step.
    val currentIndex = if (order.status.isActive) happened.lastIndexOf(true) else -1
    fun state(i: Int) = when {
        i == currentIndex -> RailStage.State.Current
        happened[i] -> RailStage.State.Done
        else -> RailStage.State.Pending
    }
    return listOf(
        RailStage(RailStage.Kind.Placed, order.placedAt.clockTime(), state(0)),
        RailStage(
            if (wallet) RailStage.Kind.Paid else RailStage.Kind.PayOnCollection,
            if (wallet) order.placedAt.clockTime() else null,
            state(1),
        ),
        RailStage(RailStage.Kind.Preparing, order.preparedAt?.clockTime(), state(2)),
        RailStage(RailStage.Kind.Ready, order.readyAt?.clockTime(), state(3)),
        RailStage(RailStage.Kind.Collected, order.completedAt?.clockTime(), state(4)),
    )
}

data class RailStage(val kind: Kind, val time: String?, val state: State) {
    enum class Kind { Placed, Paid, PayOnCollection, Preparing, Ready, Collected }
    enum class State { Done, Current, Pending }
}

@Composable
fun RailStage.toStep(): RailStep {
    val label = stringResource(
        when (kind) {
            RailStage.Kind.Placed -> R.string.order_step_placed
            RailStage.Kind.Paid -> R.string.order_step_paid
            RailStage.Kind.PayOnCollection -> R.string.order_step_pay_on_collection
            RailStage.Kind.Preparing -> R.string.order_step_preparing
            RailStage.Kind.Ready -> R.string.order_step_ready
            RailStage.Kind.Collected -> R.string.order_step_collected
        }
    )
    val stamp = when {
        time == null -> if (kind == RailStage.Kind.PayOnCollection) null else stringResource(R.string.order_step_pending)
        kind == RailStage.Kind.Ready -> stringResource(R.string.order_step_notified, time)
        else -> time
    }
    return RailStep(
        label = label,
        timestamp = stamp,
        done = state == RailStage.State.Done,
        current = state == RailStage.State.Current,
    )
}
