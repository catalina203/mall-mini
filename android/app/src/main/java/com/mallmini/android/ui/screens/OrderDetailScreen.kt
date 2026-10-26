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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mallmini.android.data.OrderDetail
import com.mallmini.android.ui.MallViewModel
import com.mallmini.android.ui.components.ErrorContent
import com.mallmini.android.ui.components.LoadingContent
import com.mallmini.android.ui.components.MallTopBar
import com.mallmini.android.ui.components.PriceText
import com.mallmini.android.ui.components.RemoteImage
import com.mallmini.android.ui.components.StatusChip
import com.mallmini.android.ui.components.formatTime
import com.mallmini.android.ui.components.orderStatusLabel
import com.mallmini.android.ui.theme.Faint
import com.mallmini.android.ui.theme.Ink
import com.mallmini.android.ui.theme.Line
import com.mallmini.android.ui.theme.Muted
import com.mallmini.android.ui.theme.Success
import com.mallmini.android.ui.theme.SurfaceSoft
import com.mallmini.android.ui.theme.SurfaceWarm
import com.mallmini.android.ui.theme.Terracotta
import com.mallmini.android.ui.theme.TerracottaDark
import com.mallmini.android.ui.theme.TerracottaSoft
import com.mallmini.android.ui.theme.Warning

@Composable
fun OrderDetailScreen(
    navController: NavController,
    viewModel: MallViewModel,
    orderNo: String,
) {
    val detailState by viewModel.orderDetail.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val paymentConfig by viewModel.paymentConfig.collectAsStateWithLifecycle()
    var payDialog by remember { mutableStateOf(false) }
    var cancelDialog by remember { mutableStateOf(false) }
    var receiptDialog by remember { mutableStateOf(false) }

    LaunchedEffect(orderNo) {
        viewModel.loadOrderDetail(orderNo)
        viewModel.loadPaymentConfig()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        MallTopBar(
            title = "订单详情",
            subtitle = "Order",
            onBack = { navController.popBackStack() },
        )

        val detail = detailState.data
        when {
            detailState.loading && detail == null -> LoadingContent()
            detailState.error != null && detail == null -> ErrorContent(
                message = detailState.error ?: "订单加载失败",
                onRetry = { viewModel.loadOrderDetail(orderNo) },
            )

            detail != null -> Column(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item { StatusHero(detail) }
                    item { AddressCard(detail) }
                    item { ItemsCard(detail) }
                    item { AmountCard(detail) }
                    if (!detail.logisticsNo.isNullOrBlank()) {
                        item { LogisticsCard(detail) }
                    }
                    item { TimelineCard(detail) }
                    item { OrderMetaCard(detail) }
                }

                OrderActions(
                    detail = detail,
                    busy = busy,
                    mockEnabled = paymentConfig.mockEnabled,
                    onPay = { payDialog = true },
                    onCancel = { cancelDialog = true },
                    onConfirmReceipt = { receiptDialog = true },
                )
            }
        }
    }

    if (payDialog) {
        AlertDialog(
            onDismissRequest = { payDialog = false },
            title = { Text("模拟支付") },
            text = { Text("当前为开发环境模拟支付，不会产生真实扣款，是否继续？") },
            confirmButton = {
                TextButton(onClick = {
                    payDialog = false
                    viewModel.payOrder(orderNo)
                }) { Text("确认支付", color = Terracotta) }
            },
            dismissButton = {
                TextButton(onClick = { payDialog = false }) { Text("取消") }
            },
        )
    }

    if (cancelDialog) {
        AlertDialog(
            onDismissRequest = { cancelDialog = false },
            title = { Text("取消订单") },
            text = { Text("确定取消该订单？库存将自动回补。") },
            confirmButton = {
                TextButton(onClick = {
                    cancelDialog = false
                    viewModel.cancelOrder(orderNo)
                }) { Text("确定取消", color = Terracotta) }
            },
            dismissButton = {
                TextButton(onClick = { cancelDialog = false }) { Text("返回") }
            },
        )
    }

    if (receiptDialog) {
        AlertDialog(
            onDismissRequest = { receiptDialog = false },
            title = { Text("确认收货") },
            text = { Text("请确认已经收到商品，确认后订单将完成。") },
            confirmButton = {
                TextButton(onClick = {
                    receiptDialog = false
                    viewModel.confirmReceipt(orderNo)
                }) { Text("确认收货", color = Terracotta) }
            },
            dismissButton = {
                TextButton(onClick = { receiptDialog = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun StatusHero(detail: OrderDetail) {
    val colors = when (detail.status) {
        "PENDING_PAYMENT" -> listOf(Terracotta, TerracottaDark)
        "PENDING_SHIPMENT" -> listOf(Warning, Color(0xFFD97706))
        "PENDING_RECEIPT" -> listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))
        "COMPLETED" -> listOf(Success, Color(0xFF10B981))
        else -> listOf(Color(0xFF6B7280), Color(0xFF4B5563))
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(colors)),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = orderStatusLabel(detail.status),
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "支付 ${if (detail.paymentStatus == "PAID") "已支付" else "未支付"}",
                    color = Color.White.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = statusNote(detail),
                color = Color.White.copy(alpha = 0.82f),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = detail.orderNo,
                color = Color.White.copy(alpha = 0.55f),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private fun statusNote(detail: OrderDetail): String = when (detail.status) {
    "PENDING_PAYMENT" -> "订单待付款，可在下方完成模拟支付。"
    "PENDING_SHIPMENT" -> "商家正在备货，请耐心等待发货。"
    "PENDING_RECEIPT" -> "商品已发出，请注意查收。"
    "COMPLETED" -> "订单已完成，感谢你的购买。"
    "CANCELLED" -> detail.cancelReason?.let { "订单已取消：$it" } ?: "订单已取消。"
    else -> ""
}

@Composable
private fun AddressCard(detail: OrderDetail) {
    Surface(
        color = SurfaceWarm,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("收货信息", color = Ink, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${detail.address.receiverName}  ${detail.address.phone}",
                color = Ink,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "${detail.address.province}${detail.address.city}${detail.address.district}${detail.address.detail}",
                color = Muted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun ItemsCard(detail: OrderDetail) {
    Surface(
        color = SurfaceWarm,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("商品清单", color = Ink, style = MaterialTheme.typography.titleMedium)
            detail.items.forEachIndexed { index, item ->
                if (index > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Line),
                    )
                }
                Row(
                    modifier = Modifier.padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RemoteImage(
                        url = item.productImage,
                        modifier = Modifier
                            .size(62.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        radius = 10.dp,
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.productName,
                            color = Ink,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${item.skuSpec} × ${item.quantity}",
                            color = Faint,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Text(
                        text = "¥%.2f".format(item.subtotal),
                        color = Ink,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun AmountCard(detail: OrderDetail) {
    Surface(
        color = SurfaceWarm,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("金额明细", color = Ink, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            AmountRow("商品金额", detail.goodsAmount)
            AmountRow("运费", detail.shippingFee)
            AmountRow("优惠", -detail.discountAmount)
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Line),
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("应付金额", color = Ink, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                PriceText(detail.payableAmount, large = true)
            }
        }
    }
}

@Composable
private fun AmountRow(label: String, amount: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
    ) {
        Text(label, color = Muted, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.weight(1f))
        Text("¥%.2f".format(amount), color = Ink, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun LogisticsCard(detail: OrderDetail) {
    Surface(
        color = SurfaceWarm,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("物流信息", color = Ink, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${detail.logisticsCompany.orEmpty()}  ${detail.logisticsNo.orEmpty()}",
                color = Muted,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun TimelineCard(detail: OrderDetail) {
    Surface(
        color = SurfaceWarm,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("状态记录", color = Ink, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            detail.statusLogs.forEach { log ->
                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                    Box(
                        modifier = Modifier
                            .padding(top = 5.dp)
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Terracotta),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "${log.fromStatus?.let(::orderStatusLabel) ?: "创建"} → ${orderStatusLabel(log.toStatus)}",
                            color = Ink,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = "${log.operatorType} · ${formatTime(log.createdAt)} ${log.remark}",
                            color = Faint,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderMetaCard(detail: OrderDetail) {
    Surface(
        color = SurfaceWarm,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("订单信息", color = Ink, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            MetaRow("下单时间", formatTime(detail.createdAt))
            if (!detail.paidAt.isNullOrBlank()) MetaRow("支付时间", formatTime(detail.paidAt))
            if (!detail.shippedAt.isNullOrBlank()) MetaRow("发货时间", formatTime(detail.shippedAt))
            if (!detail.completedAt.isNullOrBlank()) MetaRow("完成时间", formatTime(detail.completedAt))
            if (!detail.cancelledAt.isNullOrBlank()) MetaRow("取消时间", formatTime(detail.cancelledAt))
            if (!detail.remark.isNullOrBlank()) MetaRow("订单备注", detail.remark)
        }
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    ) {
        Text(label, color = Faint, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.weight(1f))
        Text(value, color = Muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun OrderActions(
    detail: OrderDetail,
    busy: Boolean,
    mockEnabled: Boolean,
    onPay: () -> Unit,
    onCancel: () -> Unit,
    onConfirmReceipt: () -> Unit,
) {
    if (detail.status == "PENDING_PAYMENT") {
        Surface(color = SurfaceWarm, shadowElevation = 12.dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 12.dp),
            ) {
                if (mockEnabled) {
                    Button(
                        onClick = onPay,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Terracotta,
                            contentColor = Color.White,
                        ),
                        contentPadding = PaddingValues(vertical = 12.dp),
                    ) {
                        Text(if (busy) "处理中…" else "模拟支付（开发环境）")
                    }
                    Spacer(Modifier.height(8.dp))
                } else {
                    Text(
                        text = "当前环境未启用支付方式",
                        color = Muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(8.dp))
                }
                TextButton(onClick = onCancel, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                    Text("取消订单", color = Terracotta)
                }
            }
        }
    } else if (detail.status == "PENDING_RECEIPT") {
        Surface(color = SurfaceWarm, shadowElevation = 12.dp) {
            Button(
                onClick = onConfirmReceipt,
                enabled = !busy,
                modifier = Modifier
                    .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Terracotta, contentColor = Color.White),
                contentPadding = PaddingValues(vertical = 12.dp),
            ) {
                Text(if (busy) "处理中…" else "确认收货")
            }
        }
    }
}
