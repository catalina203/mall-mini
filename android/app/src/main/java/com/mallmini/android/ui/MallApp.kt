package com.mallmini.android.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mallmini.android.ui.screens.AddressEditScreen
import com.mallmini.android.ui.screens.AddressListScreen
import com.mallmini.android.ui.screens.CartScreen
import com.mallmini.android.ui.screens.CatalogScreen
import com.mallmini.android.ui.screens.CheckoutScreen
import com.mallmini.android.ui.screens.HomeScreen
import com.mallmini.android.ui.screens.OrderDetailScreen
import com.mallmini.android.ui.screens.OrdersScreen
import com.mallmini.android.ui.screens.ProductDetailScreen
import com.mallmini.android.ui.screens.ProfileScreen
import com.mallmini.android.ui.screens.SearchScreen
import com.mallmini.android.ui.theme.Cream
import com.mallmini.android.ui.theme.Muted
import com.mallmini.android.ui.theme.SurfaceWarm
import com.mallmini.android.ui.theme.Terracotta
import com.mallmini.android.ui.theme.TerracottaDark
import com.mallmini.android.ui.theme.TerracottaSoft

object Routes {
    const val HOME = "home"
    const val CATALOG = "catalog"
    const val CART = "cart"
    const val PROFILE = "profile"
    const val SEARCH = "search"
    const val ADDRESSES = "addresses"
    const val ORDERS = "orders"
    const val PRODUCT = "product/{id}"
    const val ADDRESS_EDIT = "addressEdit?addressId={addressId}"
    const val CHECKOUT = "checkout?source={source}&cartItemIds={cartItemIds}&skuId={skuId}&quantity={quantity}"
    const val ORDER_DETAIL = "orderDetail/{orderNo}"

    fun product(id: Long) = "product/$id"
    fun orderDetail(orderNo: String) = "orderDetail/$orderNo"
    fun addressEdit(id: Long?) = "addressEdit?addressId=${id ?: 0L}"
    fun checkout(source: String, cartItemIds: List<Long>, skuId: Long?, quantity: Int?): String {
        val ids = cartItemIds.joinToString(",")
        return "checkout?source=$source&cartItemIds=$ids&skuId=${skuId ?: 0L}&quantity=${quantity ?: 0}"
    }
}

private val rootRoutes = setOf(Routes.HOME, Routes.CATALOG, Routes.CART, Routes.PROFILE)

@Composable
fun MallApp(viewModel: MallViewModel = viewModel()) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val backEntry by navController.currentBackStackEntryAsState()
    val route = backEntry?.destination?.route
    val cart by viewModel.cart.collectAsStateWithLifecycle()
    val badge = cart.data?.items?.sumOf { it.quantity } ?: 0

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        containerColor = Cream,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (route in rootRoutes) {
                MallBottomBar(
                    currentRoute = route,
                    cartBadge = badge,
                    onNavigate = { target ->
                        navController.navigate(target) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
            ) {
                composable(Routes.HOME) {
                    HomeScreen(navController, viewModel)
                }
                composable(Routes.CATALOG) {
                    CatalogScreen(navController, viewModel)
                }
                composable(Routes.CART) {
                    CartScreen(navController, viewModel)
                }
                composable(Routes.PROFILE) {
                    ProfileScreen(navController, viewModel)
                }
                composable(Routes.SEARCH) {
                    SearchScreen(navController, viewModel)
                }
                composable(
                    Routes.PRODUCT,
                    arguments = listOf(navArgument("id") { type = NavType.LongType }),
                ) { entry ->
                    ProductDetailScreen(
                        navController = navController,
                        viewModel = viewModel,
                        productId = entry.arguments?.getLong("id") ?: 0L,
                    )
                }
                composable(Routes.ADDRESSES) {
                    AddressListScreen(navController, viewModel)
                }
                composable(
                    Routes.ADDRESS_EDIT,
                    arguments = listOf(
                        navArgument("addressId") {
                            type = NavType.LongType
                            defaultValue = 0L
                        },
                    ),
                ) { entry ->
                    AddressEditScreen(
                        navController = navController,
                        viewModel = viewModel,
                        addressId = entry.arguments?.getLong("addressId") ?: 0L,
                    )
                }
                composable(
                    Routes.CHECKOUT,
                    arguments = listOf(
                        navArgument("source") { type = NavType.StringType; defaultValue = "CART" },
                        navArgument("cartItemIds") { type = NavType.StringType; defaultValue = "" },
                        navArgument("skuId") { type = NavType.LongType; defaultValue = 0L },
                        navArgument("quantity") { type = NavType.IntType; defaultValue = 0 },
                    ),
                ) { entry ->
                    CheckoutScreen(
                        navController = navController,
                        viewModel = viewModel,
                        source = entry.arguments?.getString("source") ?: "CART",
                        cartItemIds = entry.arguments?.getString("cartItemIds")
                            ?.split(",")
                            ?.mapNotNull { it.toLongOrNull() }
                            ?: emptyList(),
                        skuId = entry.arguments?.getLong("skuId")?.takeIf { it > 0 },
                        quantity = entry.arguments?.getInt("quantity")?.takeIf { it > 0 },
                    )
                }
                composable(Routes.ORDERS) {
                    OrdersScreen(navController, viewModel)
                }
                composable(
                    Routes.ORDER_DETAIL,
                    arguments = listOf(navArgument("orderNo") { type = NavType.StringType }),
                ) { entry ->
                    OrderDetailScreen(
                        navController = navController,
                        viewModel = viewModel,
                        orderNo = entry.arguments?.getString("orderNo").orEmpty(),
                    )
                }
            }
        }
    }
}

@Composable
private fun MallBottomBar(
    currentRoute: String?,
    cartBadge: Int,
    onNavigate: (String) -> Unit,
) {
    val items = listOf(
        Triple(Routes.HOME, "首页", Icons.Outlined.Home),
        Triple(Routes.CATALOG, "分类", Icons.Outlined.GridView),
        Triple(Routes.CART, "购物车", Icons.Outlined.ShoppingCart),
        Triple(Routes.PROFILE, "我的", Icons.Outlined.Person),
    )
    NavigationBar(
        containerColor = SurfaceWarm,
        tonalElevation = 0.dp,
    ) {
        items.forEach { (route, label, icon) ->
            NavigationBarItem(
                selected = currentRoute == route,
                onClick = { onNavigate(route) },
                icon = {
                    if (route == Routes.CART && cartBadge > 0) {
                        BadgedBox(
                            badge = {
                                Badge(containerColor = Terracotta, contentColor = androidx.compose.ui.graphics.Color.White) {
                                    Text(if (cartBadge > 99) "99+" else cartBadge.toString())
                                }
                            },
                        ) {
                            Icon(icon, contentDescription = label)
                        }
                    } else {
                        Icon(icon, contentDescription = label)
                    }
                },
                label = { Text(label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = TerracottaDark,
                    selectedTextColor = TerracottaDark,
                    indicatorColor = TerracottaSoft,
                    unselectedIconColor = Muted,
                    unselectedTextColor = Muted,
                ),
            )
        }
    }
}
