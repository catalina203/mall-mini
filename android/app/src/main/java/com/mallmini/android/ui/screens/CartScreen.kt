package com.mallmini.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mallmini.android.data.CartItem
import com.mallmini.android.ui.MallViewModel
import com.mallmini.android.ui.Routes
import com.mallmini.android.ui.components.EmptyContent
import com.mallmini.android.ui.components.ErrorContent
import com.mallmini.android.ui.components.LoadingContent
import com.mallmini.android.ui.components.MallTopBar
import com.mallmini.android.ui.components.PriceText
import com.mallmini.android.ui.components.QuantityStepper
import com.mallmini.android.ui.components.RemoteImage
import com.mallmini.android.ui.components.SelectDot
import com.mallmini.android.ui.components.specText
import com.mallmini.android.ui.theme.Faint
import com.mallmini.android.ui.theme.Ink
import com.mallmini.android.ui.theme.Line
import com.mallmini.android.ui.theme.Muted
import com.mallmini.android.ui.theme.SurfaceSoft
import com.mallmini.android.ui.theme.SurfaceWarm
import com.mallmini.android.ui.theme.Terracotta
import com.mallmini.android.ui.theme.TerracottaDark
import com.mallmini.android.ui.theme.TerracottaSoft

@Composable
fun CartScreen(navController: NavController, viewModel: MallViewModel) {
    val loggedIn by viewModel.loggedIn.collectAsStateWithLifecycle()
    val cartState by viewModel.cart.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    var showClearDialog by remember { mutableStateOf(false) }

    LaunchedEffect(loggedIn) {
        if (loggedIn) {
            viewModel.loadCart()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        MallTopBar(
            title = "购物车",
            subtitle = "Shopping Bag",
            actions = {
                val data = cartState.data
                if (!data?.items.isNullOrEmpty()) {
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(Icons.Outlined.DeleteSweep, contentDescription = "清空购物车", tint = Muted)
                    }
                }
            },
        )

        if (!loggedIn) {
            LoginPrompt(
                onLogin = { navController.navigate(Routes.PROFILE) },
            )
            return@Column
        }

        val data = cartState.data
        when {
            cartState.loading && data == null -> LoadingContent()
            cartState.error != null && data == null -> ErrorContent(
                message = cartState.error ?: "购物车加载失败",
                onRetry = { viewModel.loadCart() },
            )

            data == null || data.items.isEmpty() -> EmptyContent(
                title = "购物车还是空的",
                subtitle = "去挑选几件喜欢的商品吧",
            )

            else -> Column(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Text(
                            text = "共 ${data.items.size} 件商品",
                            color = Muted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    items(data.items, key = { it.id }) { item ->
                        CartItemCard(
                            item = item,
                            busy = busy,
                            onSelect = { viewModel.updateCartItem(item.id, selected = it) },
                            onQuantity = { viewModel.updateCartItem(item.id, quantity = it) },
                            onDelete = { viewModel.deleteCartItem(item.id) },
                        )
                    }
                }
            }
        }

        if (data != null && data.items.isNotEmpty()) {
            CartSummaryBar(
                allSelected = data.items.all { it.selected },
                totalAmount = data.summary.selectedAmount,
                selectedQuantity = data.summary.selectedQuantity,
                enabled = !busy,
                onSelectAll = { viewModel.selectAllCart(!data.items.all { it.selected }) },
                onCheckout = {
                    val ids = data.items.filter { it.selected && it.available }.map { it.id }
                    if (ids.isNotEmpty()) {
                        navController.navigate(
                            Routes.checkout(
                                source = "CART",
                                cartItemIds = ids,
                                skuId = null,
                                quantity = null,
                            ),
                        )
                    } else {
                        viewModel.loadCart()
                    }
                },
            )
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清空购物车") },
            text = { Text("确定清空购物车中的全部商品？") },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    viewModel.clearCart()
                }) { Text("清空", color = Terracotta) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun LoginPrompt(onLogin: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(TerracottaSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.ShoppingCart,
                contentDescription = null,
                tint = Terracotta,
                modifier = Modifier.size(30.dp),
            )
        }
        Spacer(Modifier.height(18.dp))
        Text("登录后查看购物车", color = Ink, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text("登录后可同步购物车与订单", color = Muted, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onLogin,
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Terracotta),
            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 11.dp),
        ) {
            Text("去登录")
        }
    }
}

@Composable
private fun CartItemCard(
    item: CartItem,
    busy: Boolean,
    onSelect: (Boolean) -> Unit,
    onQuantity: (Int) -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        color = SurfaceWarm,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (item.available) 1f else 0.72f),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SelectDot(
                selected = item.selected,
                enabled = item.available && !busy,
                onClick = { onSelect(!item.selected) },
            )
            Spacer(Modifier.width(12.dp))
            RemoteImage(
                url = item.productImage,
                modifier = Modifier
                    .size(82.dp)
                    .clip(RoundedCornerShape(12.dp)),
                radius = 12.dp,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.productName,
                    color = Ink,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = specText(item.spec),
                    color = Faint,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!item.available && item.unavailableReason.isNotBlank()) {
                    Text(
                        text = item.unavailableReason,
                        color = TerracottaDark,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PriceText(item.price)
                    Spacer(Modifier.weight(1f))
                    QuantityStepper(
                        quantity = item.quantity,
                        max = item.stock.coerceAtLeast(1),
                        onChange = onQuantity,
                        enabled = item.available && !busy,
                    )
                }
            }
            IconButton(onClick = onDelete, enabled = !busy) {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    contentDescription = "删除",
                    tint = Faint,
                    modifier = Modifier.size(19.dp),
                )
            }
        }
    }
}

@Composable
private fun CartSummaryBar(
    allSelected: Boolean,
    totalAmount: Double,
    selectedQuantity: Int,
    enabled: Boolean,
    onSelectAll: () -> Unit,
    onCheckout: () -> Unit,
) {
    Surface(
        color = SurfaceWarm,
        shadowElevation = 12.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .padding(vertical = 4.dp),
            ) {
                SelectDot(
                    selected = allSelected,
                    enabled = enabled,
                    onClick = onSelectAll,
                )
                Spacer(Modifier.width(7.dp))
                Text("全选", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text("已选 $selectedQuantity 件", color = Faint, style = MaterialTheme.typography.bodySmall)
                PriceText(totalAmount, large = true)
            }
            Spacer(Modifier.width(14.dp))
            Button(
                onClick = onCheckout,
                enabled = enabled && selectedQuantity > 0,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Terracotta,
                    disabledContainerColor = TerracottaSoft,
                    disabledContentColor = Color.White,
                ),
                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 13.dp),
            ) {
                Text("去结算")
            }
        }
    }
}
