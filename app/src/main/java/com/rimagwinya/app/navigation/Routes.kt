package com.rimagwinya.app.navigation

import com.rimagwinya.app.R
import com.rimagwinya.app.core.designsystem.component.RoleTab
import kotlinx.serialization.Serializable

/**
 * Type-safe routes. Each destination is a class rather than a string, so a
 * typo is a compile error instead of a blank screen.
 */
object Route {

    // --- Auth graph ---
    @Serializable
    data object AuthGraph

    @Serializable
    data object Welcome

    @Serializable
    data class Login(val staffHint: Boolean = false)

    @Serializable
    data object Register

    // --- Student graph ---
    @Serializable
    data object StudentGraph

    @Serializable
    data object Menu

    @Serializable
    data object Cart

    @Serializable
    data object Checkout

    @Serializable
    data object Orders

    @Serializable
    data class OrderDetail(val orderId: String)

    @Serializable
    data object Profile

    @Serializable
    data object Privacy

    // --- Staff graph ---
    @Serializable
    data object StaffGraph

    @Serializable
    data object Queue

    @Serializable
    data object Stock

    @Serializable
    data object Sales

    @Serializable
    data object TopUp
}

/** Route keys for the tab bar, which compares by name rather than by type. */
object TabKey {
    const val MENU = "menu"
    const val CART = "cart"
    const val ORDERS = "orders"
    const val PROFILE = "profile"
    const val QUEUE = "queue"
    const val STOCK = "stock"
    const val SALES = "sales"
}

val studentTabs = listOf(
    RoleTab(labelRes = R.string.tab_menu, icon = R.drawable.ic_grid, route = TabKey.MENU),
    RoleTab(labelRes = R.string.tab_cart, icon = R.drawable.ic_cart, route = TabKey.CART),
    RoleTab(labelRes = R.string.tab_orders, icon = R.drawable.ic_clock, route = TabKey.ORDERS),
    RoleTab(labelRes = R.string.tab_profile, icon = R.drawable.ic_user, route = TabKey.PROFILE),
)

val staffTabs = listOf(
    RoleTab(labelRes = R.string.tab_queue, icon = R.drawable.ic_clock, route = TabKey.QUEUE),
    RoleTab(labelRes = R.string.tab_stock, icon = R.drawable.ic_box, route = TabKey.STOCK),
    RoleTab(labelRes = R.string.tab_sales, icon = R.drawable.ic_chart, route = TabKey.SALES),
    RoleTab(labelRes = R.string.tab_profile, icon = R.drawable.ic_user, route = TabKey.PROFILE),
)
