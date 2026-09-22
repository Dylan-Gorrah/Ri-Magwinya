package com.rimagwinya.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.rimagwinya.app.R
import com.rimagwinya.app.core.designsystem.component.RoleTabBar
import com.rimagwinya.app.core.designsystem.theme.RmTheme
import com.rimagwinya.app.data.repository.AuthState
import com.rimagwinya.app.domain.model.UserRole
import com.rimagwinya.app.feature.auth.LoginScreen
import com.rimagwinya.app.feature.auth.RegisterScreen
import com.rimagwinya.app.feature.auth.SessionViewModel
import com.rimagwinya.app.feature.auth.WelcomeScreen
import com.rimagwinya.app.feature.menu.MenuSmokeScreen

@Composable
fun RootNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    sessionViewModel: SessionViewModel = hiltViewModel(),
) {
    val session by sessionViewModel.state.collectAsStateWithLifecycle()

    // The role comes from the profile on the server, every time. Nothing the
    // phone chose is trusted — which is why the welcome screen's two cards
    // only pick which sign-in copy you see.
    val role = (session as? AuthState.SignedIn)?.profile?.role
    val signedIn = role != null

    val tabs = if (role == UserRole.Staff) staffTabs else studentTabs
    var tab by remember(role) {
        mutableStateOf(if (role == UserRole.Staff) TabKey.QUEUE else TabKey.MENU)
    }

    // Signing in or out swaps the whole graph underneath whatever is on
    // screen, so no individual screen has to know where to go afterwards.
    LaunchedEffect(session) {
        when {
            signedIn -> navController.navigate(
                if (role == UserRole.Staff) Route.StaffGraph else Route.StudentGraph
            ) {
                popUpTo(navController.graph.id) { inclusive = true }
            }

            session is AuthState.SignedOut -> navController.navigate(Route.AuthGraph) {
                popUpTo(navController.graph.id) { inclusive = true }
            }

            else -> Unit // Loading: the splash is still up.
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = RmTheme.colors.background,
        bottomBar = {
            if (signedIn) {
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
        Box(Modifier.padding(insets)) {
            NavHost(
                navController = navController,
                startDestination = Route.AuthGraph,
            ) {
                authGraph(navController)
                studentGraph()
                staffGraph()
                // Profile is shared by both roles, so it lives at the root
                // rather than being declared twice — two graphs cannot own
                // the same route.
                composable<Route.Profile> {
                    PlaceholderScreen(R.string.title_profile, icon = R.drawable.ic_user)
                }
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

/** Welcome, login, register. */
private fun NavGraphBuilder.authGraph(navController: NavHostController) {
    navigation<Route.AuthGraph>(startDestination = Route.Welcome) {
        composable<Route.Welcome> {
            WelcomeScreen(
                onContinue = { staffHint ->
                    navController.navigate(Route.Login(staffHint = staffHint))
                }
            )
        }
        composable<Route.Login> {
            LoginScreen(onRegister = { navController.navigate(Route.Register) })
        }
        composable<Route.Register> {
            RegisterScreen(onSignIn = { navController.popBackStack() })
        }
    }
}

/** Menu, cart, checkout, orders. Phases 4, 6 and 7. */
private fun NavGraphBuilder.studentGraph() {
    navigation<Route.StudentGraph>(startDestination = Route.Menu) {
        composable<Route.Menu> { MenuSmokeScreen() }
        composable<Route.Cart> { PlaceholderScreen(R.string.title_cart, icon = R.drawable.ic_cart) }
        composable<Route.Checkout> { PlaceholderScreen(R.string.title_checkout, icon = R.drawable.ic_wallet) }
        composable<Route.Orders> { PlaceholderScreen(R.string.title_orders, icon = R.drawable.ic_clock) }
        composable<Route.OrderDetail> { PlaceholderScreen(R.string.title_order, icon = R.drawable.ic_check) }
    }
}

/** Queue, stock, sales, top-up. Phase 8. */
private fun NavGraphBuilder.staffGraph() {
    navigation<Route.StaffGraph>(startDestination = Route.Queue) {
        composable<Route.Queue> { PlaceholderScreen(R.string.title_queue, icon = R.drawable.ic_clock) }
        composable<Route.Stock> { PlaceholderScreen(R.string.title_stock, icon = R.drawable.ic_box) }
        composable<Route.Sales> { PlaceholderScreen(R.string.title_sales, icon = R.drawable.ic_chart) }
        composable<Route.TopUp> { PlaceholderScreen(R.string.title_top_up, icon = R.drawable.ic_wallet) }
    }
}
