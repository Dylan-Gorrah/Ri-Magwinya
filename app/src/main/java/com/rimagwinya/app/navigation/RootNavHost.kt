package com.rimagwinya.app.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.rimagwinya.app.R
import com.rimagwinya.app.core.designsystem.component.RoleTabBar
import com.rimagwinya.app.core.designsystem.theme.RmTheme

/**
 * Which side of the app is loaded.
 *
 * From Phase 3 this comes from the signed-in profile's role column on the
 * server. Until then it is local state so the graphs can be walked.
 */
enum class AppRole { SignedOut, Student, Staff }

@Composable
fun RootNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    // Phase 3 replaces this with the profile from Supabase.
    var role by remember { mutableStateOf(AppRole.SignedOut) }
    var tab by remember { mutableStateOf(TabKey.MENU) }

    val tabs = when (role) {
        AppRole.Staff -> staffTabs
        else -> studentTabs
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = RmTheme.colors.background,
        bottomBar = {
            if (role != AppRole.SignedOut) {
                RoleTabBar(
                    tabs = tabs,
                    selectedRoute = tab,
                    onSelect = { selected ->
                        tab = selected.route
                        navController.navigate(routeFor(selected.route)) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { insets ->
        NavHost(
            navController = navController,
            startDestination = Route.AuthGraph,
            modifier = Modifier.padding(insets),
        ) {
            authGraph(
                onSignedIn = { signedInRole ->
                    role = signedInRole
                    tab = if (signedInRole == AppRole.Staff) TabKey.QUEUE else TabKey.MENU
                    navController.navigate(
                        if (signedInRole == AppRole.Staff) Route.StaffGraph else Route.StudentGraph
                    ) {
                        popUpTo(Route.AuthGraph) { inclusive = true }
                    }
                },
            )
            studentGraph()
            staffGraph()
            // Profile is shared by both roles, so it lives at the root rather
            // than being declared twice — two graphs cannot both own a route.
            composable<Route.Profile> {
                PlaceholderScreen(R.string.title_profile, icon = R.drawable.ic_user)
            }
        }
    }
}

private fun routeFor(tabKey: String): Any = when (tabKey) {
    TabKey.MENU -> Route.Menu
    TabKey.CART -> Route.Cart
    TabKey.ORDERS -> Route.Orders
    TabKey.QUEUE -> Route.Queue
    TabKey.STOCK -> Route.Stock
    TabKey.SALES -> Route.Sales
    else -> Route.Profile
}

/** Welcome, login, register. Phase 3 fills these in. */
private fun androidx.navigation.NavGraphBuilder.authGraph(
    onSignedIn: (AppRole) -> Unit,
) {
    navigation<Route.AuthGraph>(startDestination = Route.Welcome) {
        composable<Route.Welcome> {
            WelcomePlaceholder(onSignedIn = onSignedIn)
        }
        composable<Route.Login> {
            PlaceholderScreen(R.string.title_login, icon = R.drawable.ic_lock)
        }
        composable<Route.Register> {
            PlaceholderScreen(R.string.title_register, icon = R.drawable.ic_user)
        }
    }
}

/** Menu, cart, checkout, orders, profile. Phases 4, 6, 7 and 9. */
private fun androidx.navigation.NavGraphBuilder.studentGraph() {
    navigation<Route.StudentGraph>(startDestination = Route.Menu) {
        composable<Route.Menu> { PlaceholderScreen(R.string.title_menu, icon = R.drawable.ic_grid) }
        composable<Route.Cart> { PlaceholderScreen(R.string.title_cart, icon = R.drawable.ic_cart) }
        composable<Route.Checkout> { PlaceholderScreen(R.string.title_checkout, icon = R.drawable.ic_wallet) }
        composable<Route.Orders> { PlaceholderScreen(R.string.title_orders, icon = R.drawable.ic_clock) }
        composable<Route.OrderDetail> { PlaceholderScreen(R.string.title_order, icon = R.drawable.ic_check) }
    }
}

/** Queue, stock, sales, top-up. Phase 8. */
private fun androidx.navigation.NavGraphBuilder.staffGraph() {
    navigation<Route.StaffGraph>(startDestination = Route.Queue) {
        composable<Route.Queue> { PlaceholderScreen(R.string.title_queue, icon = R.drawable.ic_clock) }
        composable<Route.Stock> { PlaceholderScreen(R.string.title_stock, icon = R.drawable.ic_box) }
        composable<Route.Sales> { PlaceholderScreen(R.string.title_sales, icon = R.drawable.ic_chart) }
        composable<Route.TopUp> { PlaceholderScreen(R.string.title_top_up, icon = R.drawable.ic_wallet) }
    }
}
