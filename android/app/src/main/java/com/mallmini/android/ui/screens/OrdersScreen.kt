package com.mallmini.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mallmini.android.data.OrderSummary
import com.mallmini.android.ui.MallViewModel
import com.mallmini.android.ui.Routes
import com.mallmini.android.ui.components.EmptyContent
import com.mallmini.android.ui.components.ErrorContent
import com.mallmini.android.ui.components.LoadingContent
import com.mallmini.android.ui.components.MallTopBar
import com.mallmini.android.ui.components.PriceText
import com.mallmini.android.ui.components.RemoteImage
import com.mallmini.android.ui.components.StatusChip
import com.mallmini.android.ui.components.formatTime
import com.mallmini.android.ui.theme.Faint
import com.mallmini.android.ui.theme.Ink
import com.mallmini.android.ui.theme.Muted
import com.mallmini.android.ui.theme.SurfaceWarm
import com.mallmini.android.ui.theme.Terracotta
import com.mallmini.android.ui.theme.TerracottaDark
import com.mallmini.android.ui.theme.TerracottaSoft

private data class OrderTab(val label: String, val value: String?)

@Composable
fun OrdersScreen(navController: NavController, viewModel: MallViewModel) {
    val loggedIn by viewModel.loggedIn.collectAsStateWithLifecycle()
    val ordersState by viewModel.orders.collectAsStateWithLifecycle()
    var selectedStatus by rememberSaveable { mutableStateOf<String?>(null) }

    val tabs = listOf(
        OrderTab("全部", null),
        OrderTab("待付款", "PENDING_PAYMENT"),
        OrderTab("待发货", "PENDING_SHIPMENT"),
        OrderTab("待收货", "PENDING_RECEIPT"),
        OrderTab("已完成", "COMPLETED"),
        OrderTab("已取消", "CANCELLED"),
    )

    LaunchedEffect(loggedIn, selectedStatus) {
        if (loggedIn) {
            viewModel.loadOrders(selectedStatus)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        MallTopBar(
            title = "我的订单",
            subtitle = "Orders",
            onBack = { navController.popBackStack() },
        )

        if (!loggedIn) {
            EmptyContent(title = "登录后查看订单", subtitle = "请先到「我的」完成登录")
            return@Column
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(tabs) { tab ->
                val selected = selectedStatus == tab.value
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (selected) Ink else SurfaceWarm)
                        .clickable { selectedStatus = tab.value }
                        .padding(horizontal = 15.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = tab.label,
                        color = if (selected) Color.White else Muted,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        val data = ordersState.data
        when {
            ordersState.loading && data == null -> LoadingContent()
            ordersState.error != null && data == null -> ErrorContent(
                message = ordersState.error ?: "订单加载失败",
                onRetry = { viewModel.loadOrders(selectedStatus) },
            )

            data == null || data.items.isEmpty() -> EmptyContent(
                title = "暂无订单",
                subtitle = "去首页挑选商品下单吧",
            )

            else -> LazyColumn(
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 26.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(data.items, key = { it.orderNo }) { order ->
                    OrderCard(order) {
                        navController.navigate(Routes.orderDetail(order.orderNo))
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderCard(order: OrderSummary, onClick: () -> Unit) {
    Surface(
        color = SurfaceWarm,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = order.orderNo,
                    color = Faint,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.weight(1f))
                StatusChip(order.status)
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                RemoteImage(
                    url = order.firstProductImage,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(11.dp)),
                    radius = 11.dp,
                )
                Spacer(Modifier.width(11.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = order.firstProductName,
                        color = Ink,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "共 ${order.totalQuantity} 件 · ${formatTime(order.createdAt)}",
                        color = Faint,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                PriceText(order.payableAmount)
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = onClick,
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TerracottaSoft,
                        contentColor = TerracottaDark,
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 7.dp),
                ) {
                    Text("查看详情")
                }
            }
        }
    }
}
