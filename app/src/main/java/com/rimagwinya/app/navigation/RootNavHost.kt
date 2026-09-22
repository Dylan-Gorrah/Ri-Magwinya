package com.rimagwinya.app.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.background
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.rimagwinya.app.R
import com.rimagwinya.app.core.designsystem.component.RoleTabBar
import com.rimagwinya.app.core.designsystem.theme.RmTheme
import com.rimagwinya.app.core.designsystem.theme.Space
import com.rimagwinya.app.core.money.Money
import com.rimagwinya.app.data.repository.AuthState
import com.rimagwinya.app.domain.model.UserRole
import com.rimagwinya.app.feature.auth.ClaimStudentNumberScreen
import com.rimagwinya.app.feature.auth.LoginScreen
import com.rimagwinya.app.feature.auth.RegisterScreen
import com.rimagwinya.app.feature.auth.SessionViewModel
import com.rimagwinya.app.feature.auth.WelcomeScreen
import com.rimagwinya.app.feature.cart.CartScreen
import com.rimagwinya.app.feature.cart.CheckoutScreen
import com.rimagwinya.app.feature.menu.MenuScreen
import com.rimagwinya.app.feature.notifications.NotificationPermissionGate
import com.rimagwinya.app.feature.offline.SyncViewModel
import com.rimagwinya.app.feature.orders.OrderDetailScreen
import com.rimagwinya.app.feature.orders.OrdersScreen
import com.rimagwinya.app.feature.profile.AppLockScreen
import com.rimagwinya.app.feature.profile.PrivacyScreen
import com.rimagwinya.app.feature.profile.ProfileScreen
import com.rimagwinya.app.feature.staff.QueueScreen
import com.rimagwinya.app.feature.staff.SalesScreen
import com.rimagwinya.app.feature.staff.StockScreen
import com.rimagwinya.app.feature.staff.TopUpScreen

@Composable
fun RootNavHost(
    biometricUnlock: Boolean = false,
    deepLinkOrderId: String? = null,
    onDeepLinkHandled: () -> Unit = {},
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    sessionViewModel: SessionViewModel = hiltViewModel(),
    syncViewModel: SyncViewModel = hiltViewModel(),
) {
    val session by sessionViewModel.state.collectAsStateWithLifecycle()

    // The role comes from the profile on the server, every time. Nothing the
    // phone chose is trusted — which is why the welcome screen's two cards
    // only pick which sign-in copy you see.
    val profile = (session as? AuthState.SignedIn)?.profile
    val role = profile?.role
    val signedIn = role != null

    val tabs = if (role == UserRole.Staff) staffTabs else studentTabs
    val roleGraph: Any = if (role == UserRole.Staff) Route.StaffGraph else Route.StudentGraph

    // The highlighted tab follows the screen actually showing, so arriving
    // at an order from checkout lights up Orders without anyone setting it.
    val backStackEntry by navController.currentBackStackEntryAsState()
    val tab = backStackEntry?.destination?.let(::tabFor)

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

    // A Google account with no student number yet can browse nothing until
    // it has one: staff need something to call out at the counter.
    if (signedIn && profile?.studentNumber == null) {
        ClaimStudentNumberScreen(modifier = modifier.fillMaxSize().background(RmTheme.colors.background))
        return
    }

    // The lock sits over everything once, per launch, and only when
    // somebody is signed in — there is nothing to protect otherwise.
    var unlocked by rememberSaveable { mutableStateOf(false) }
    if (signedIn && biometricUnlock && !unlocked) {
        AppLockScreen(
            onUnlocked = { unlocked = true },
            onUsePassword = { sessionViewModel.signOut() },
            modifier = modifier
                .fillMaxSize()
                .background(RmTheme.colors.background),
        )
        return
    }

    val sync by syncViewModel.state.collectAsStateWithLifecycle()

    // A tapped notification opens that order, once the graph is in place.
    LaunchedEffect(deepLinkOrderId, signedIn) {
        val id = deepLinkOrderId
        if (id != null && signedIn) {
            navController.navigate(Route.OrderDetail(id))
            onDeepLinkHandled()
        }
    }

    if (signedIn) {
        NotificationPermissionGate(
            enabled = true,
            onGranted = { sessionViewModel.syncPushToken() },
        )
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
                        navController.navigate(routeFor(selected.route)) {
                            popUpTo(roleGraph) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { insets ->
        Column(Modifier.padding(insets)) {
            // Slim, and only while it is true.
            if (!sync.online) {
                Text(
                    text = stringResource(R.string.offline_banner),
                    style = RmTheme.type.caption,
                    color = RmTheme.colors.text,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RmTheme.colors.warning.copy(alpha = 0.16f))
                        .padding(horizontal = Space.screen, vertical = Space.x8),
                )
            }
            NavHost(
                navController = navController,
                startDestination = Route.AuthGraph,
            ) {
                authGraph(navController)
                studentGraph(
                    navController = navController,
                    firstName = profile?.fullName?.substringBefore(' ').orEmpty(),
                    balance = profile?.walletBalance ?: Money.ZERO,
                )
                staffGraph(navController)
                // Profile is shared by both roles, so it lives at the root
                // rather than being declared twice — two graphs cannot own
                // the same route.
                composable<Route.Profile> {
                    ProfileScreen(onOpenPrivacy = { navController.navigate(Route.Privacy) })
                }
                composable<Route.Privacy> {
                    PrivacyScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}

/** Which tab a destination belongs to. */
private fun tabFor(destination: NavDestination): String? = when {
    destination.hasRoute(Route.Menu::class) -> TabKey.MENU
    destination.hasRoute(Route.Cart::class) || destination.hasRoute(Route.Checkout::class) -> TabKey.CART
    destination.hasRoute(Route.Orders::class) || destination.hasRoute(Route.OrderDetail::class) -> TabKey.ORDERS
    destination.hasRoute(Route.Queue::class) || destination.hasRoute(Route.TopUp::class) -> TabKey.QUEUE
    destination.hasRoute(Route.Stock::class) -> TabKey.STOCK
    destination.hasRoute(Route.Sales::class) -> TabKey.SALES
    destination.hasRoute(Route.Profile::class) -> TabKey.PROFILE
    else -> null
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

/** Menu, cart, checkout, orders. */
private fun NavGraphBuilder.studentGraph(
    navController: NavHostController,
    firstName: String,
    balance: Money,
) {
    navigation<Route.StudentGraph>(startDestination = Route.Menu) {
        composable<Route.Menu> {
            MenuScreen(
                fullName = firstName,
                balance = balance,
                onOpenOrder = { navController.navigate(Route.OrderDetail(it)) },
            )
        }
        composable<Route.Cart> {
            CartScreen(
                onBrowseMenu = {
                    navController.navigate(Route.Menu) {
                        popUpTo(Route.StudentGraph) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onCheckout = { navController.navigate(Route.Checkout) },
            )
        }
        composable<Route.Checkout> {
            CheckoutScreen(
                onBack = { navController.popBackStack() },
                onQueued = {
                    navController.navigate(Route.Orders) { popUpTo(Route.Menu) }
                },
                onPlaced = { orderId ->
                    // Back from the new order goes to the menu, not to an
                    // empty checkout.
                    navController.navigate(Route.OrderDetail(orderId)) {
                        popUpTo(Route.Menu)
                    }
                },
            )
        }
        composable<Route.Orders> {
            OrdersScreen(onOpen = { navController.navigate(Route.OrderDetail(it)) })
        }
        composable<Route.OrderDetail> {
            OrderDetailScreen(onBack = { navController.popBackStack() })
        }
    }
}

/** Queue, stock, sales, top-up. */
private fun NavGraphBuilder.staffGraph(navController: NavHostController) {
    navigation<Route.StaffGraph>(startDestination = Route.Queue) {
        composable<Route.Queue> { QueueScreen(onTopUp = { navController.navigate(Route.TopUp) }) }
        composable<Route.Stock> { StockScreen() }
        composable<Route.Sales> { SalesScreen() }
        composable<Route.TopUp> { TopUpScreen(onBack = { navController.popBackStack() }) }
    }
}
