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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mallmini.android.data.AfterSaleDetail
import com.mallmini.android.data.AfterSaleItemRequest
import com.mallmini.android.ui.MallViewModel
import com.mallmini.android.ui.Routes
import com.mallmini.android.ui.components.EmptyContent
import com.mallmini.android.ui.components.ErrorContent
import com.mallmini.android.ui.components.LoadingContent
import com.mallmini.android.ui.components.MallTopBar
import com.mallmini.android.ui.components.PriceText
import com.mallmini.android.ui.components.RemoteImage
import com.mallmini.android.ui.components.formatTime
import com.mallmini.android.ui.theme.Faint
import com.mallmini.android.ui.theme.Ink
import com.mallmini.android.ui.theme.Line
import com.mallmini.android.ui.theme.Muted
import com.mallmini.android.ui.theme.SurfaceSoft
import com.mallmini.android.ui.theme.SurfaceWarm
import com.mallmini.android.ui.theme.Terracotta
import com.mallmini.android.ui.theme.TerracottaDark
import com.mallmini.android.ui.theme.TerracottaSoft

private fun afterSaleStatusLabel(status: String?): String = when (status) {
    "APPLIED" -> "待审核"
    "APPROVED" -> "待退款"
    "WAIT_RETURN" -> "待退货"
    "RETURNED" -> "退货运输中"
    "RETURN_RECEIVED" -> "已收到退货"
    "REFUNDING" -> "退款中"
    "REFUNDED" -> "已退款"
    "REFUND_FAILED" -> "退款失败"
    "REJECTED" -> "已拒绝"
    "CANCELLED" -> "已取消"
    else -> status ?: ""
}

private fun afterSaleTypeLabel(type: String?): String =
    if (type == "RETURN_REFUND") "退货退款" else "仅退款"

@Composable
fun AfterSalesScreen(navController: NavController, viewModel: MallViewModel) {
    val state by viewModel.afterSales.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.loadAfterSales() }

    Column(modifier = Modifier.fillMaxSize()) {
        MallTopBar("售后服务", "After Sales", onBack = { navController.popBackStack() })
        when {
            state.loading && state.data == null -> LoadingContent()
            state.error != null && state.data == null -> ErrorContent(
                state.error ?: "售后加载失败",
                onRetry = { viewModel.loadAfterSales() },
            )
            state.data?.items.isNullOrEmpty() -> EmptyContent("暂无售后记录", "可在订单详情发起退款或退货")
            else -> LazyColumn(
                contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.data!!.items.size) { index ->
                    val item = state.data!!.items[index]
                    Surface(
                        color = SurfaceWarm,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate(Routes.afterSaleDetail(item.afterSaleNo)) },
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            RemoteImage(
                                item.firstProductImage,
                                modifier = Modifier
                                    .size(66.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                                radius = 10.dp,
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.firstProductName, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "${afterSaleTypeLabel(item.type)} · 共 ${item.totalQuantity} 件",
                                    color = Faint,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Text(item.afterSaleNo, color = Faint, style = MaterialTheme.typography.bodySmall)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(afterSaleStatusLabel(item.status), color = TerracottaDark, fontSize = 12.sp)
                                PriceText(item.refundAmount)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AfterSaleApplyScreen(
    navController: NavController,
    viewModel: MallViewModel,
    orderNo: String,
) {
    val orderState by viewModel.orderDetail.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val selected = remember(orderNo) { mutableStateMapOf<Long, Boolean>() }
    var type by rememberSaveable { mutableStateOf("REFUND_ONLY") }
    var reason by rememberSaveable { mutableStateOf("不想要了") }
    var description by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(orderNo) {
        if (orderState.data?.orderNo != orderNo) {
            viewModel.loadOrderDetail(orderNo)
        }
    }
    LaunchedEffect(orderState.data?.orderNo) {
        orderState.data?.items?.forEach { item ->
            item.orderItemId?.let { selected.putIfAbsent(it, true) }
        }
    }

    val order = orderState.data
    Column(modifier = Modifier.fillMaxSize()) {
        MallTopBar("申请售后", "After Sales", onBack = { navController.popBackStack() })
        if (order == null && orderState.loading) {
            LoadingContent()
            return@Column
        }
        if (order == null) {
            ErrorContent(orderState.error ?: "订单加载失败", onRetry = { viewModel.loadOrderDetail(orderNo) })
            return@Column
        }
        if (order.afterSaleNo != null) {
            LaunchedEffect(order.afterSaleNo) {
                navController.navigate(Routes.afterSaleDetail(order.afterSaleNo))
            }
            LoadingContent(label = "跳转售后详情")
            return@Column
        }
        val canReturnRefund = order.status == "PENDING_RECEIPT" || order.status == "COMPLETED"
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Surface(color = SurfaceWarm, shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("售后类型", color = Ink, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TypeChip("仅退款", type == "REFUND_ONLY") { type = "REFUND_ONLY" }
                            if (canReturnRefund) {
                                TypeChip("退货退款", type == "RETURN_REFUND") { type = "RETURN_REFUND" }
                            }
                        }
                        if (!canReturnRefund) {
                            Text("未发货订单只能申请仅退款", color = Faint, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            item {
                Surface(color = SurfaceWarm, shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("选择商品", color = Ink, style = MaterialTheme.typography.titleMedium)
                        order.items.forEach { item ->
                            val itemId = item.orderItemId ?: return@forEach
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = selected[itemId] == true,
                                    onCheckedChange = { selected[itemId] = it },
                                )
                                Text(item.productName, color = Ink, modifier = Modifier.weight(1f))
                                Text("× ${item.quantity}", color = Muted)
                            }
                        }
                    }
                }
            }
            item {
                Surface(color = SurfaceWarm, shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("售后原因", color = Ink, style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = reason,
                            onValueChange = { reason = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = fieldColors(),
                        )
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("补充说明（选填）", color = Faint) },
                            minLines = 3,
                            shape = RoundedCornerShape(12.dp),
                            colors = fieldColors(),
                        )
                    }
                }
            }
            item {
                Button(
                    onClick = {
                        val ids = selected.filterValues { it }.keys
                        val items = order.items
                            .filter { it.orderItemId in ids }
                            .map { AfterSaleItemRequest(it.orderItemId!!, it.quantity) }
                        viewModel.createAfterSale(orderNo, type, reason, description, items) { detail ->
                            navController.navigate(Routes.afterSaleDetail(detail.afterSaleNo)) {
                                popUpTo(Routes.ORDER_DETAIL) { inclusive = true }
                            }
                        }
                    },
                    enabled = !busy && selected.values.any { it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Terracotta, contentColor = Color.White),
                ) {
                    Text(if (busy) "提交中…" else "提交申请")
                }
            }
        }
    }
}

@Composable
private fun TypeChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) TerracottaSoft else SurfaceSoft,
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            text,
            color = if (selected) TerracottaDark else Muted,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
        )
    }
}

@Composable
fun AfterSaleDetailScreen(
    navController: NavController,
    viewModel: MallViewModel,
    afterSaleNo: String,
) {
    val state by viewModel.afterSaleDetail.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    var returnDialog by remember { mutableStateOf(false) }
    var cancelDialog by remember { mutableStateOf(false) }
    LaunchedEffect(afterSaleNo) { viewModel.loadAfterSaleDetail(afterSaleNo) }

    Column(modifier = Modifier.fillMaxSize()) {
        MallTopBar("售后详情", "After Sales", onBack = { navController.popBackStack() })
        val detail = state.data
        when {
            state.loading && detail == null -> LoadingContent()
            state.error != null && detail == null -> ErrorContent(
                state.error ?: "售后加载失败",
                onRetry = { viewModel.loadAfterSaleDetail(afterSaleNo) },
            )
            detail != null -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Surface(color = SurfaceWarm, shape = RoundedCornerShape(18.dp)) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    afterSaleStatusLabel(detail.status),
                                    color = TerracottaDark,
                                    style = MaterialTheme.typography.titleLarge,
                                )
                                Text(detail.afterSaleNo, color = Faint, style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.height(8.dp))
                                Text("退款金额", color = Faint, style = MaterialTheme.typography.bodySmall)
                                PriceText(detail.refundAmount, large = true)
                            }
                        }
                    }
                    item {
                        Surface(color = SurfaceWarm, shape = RoundedCornerShape(18.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("售后商品", color = Ink, style = MaterialTheme.typography.titleMedium)
                                detail.items.forEach { item ->
                                    Row(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        RemoteImage(
                                            item.productImage,
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(RoundedCornerShape(9.dp)),
                                            radius = 9.dp,
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.productName, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("${item.skuSpec} × ${item.quantity}", color = Faint, style = MaterialTheme.typography.bodySmall)
                                        }
                                        Text("¥%.2f".format(item.refundAmount), color = Ink)
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Surface(color = SurfaceWarm, shape = RoundedCornerShape(18.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("申请信息", color = Ink, style = MaterialTheme.typography.titleMedium)
                                Text("类型：${afterSaleTypeLabel(detail.type)}", color = Muted)
                                Text("原因：${detail.reason}", color = Muted)
                                if (detail.description.isNotBlank()) Text(detail.description, color = Muted)
                                if (!detail.returnTrackingNo.isNullOrBlank()) {
                                    Text("退货物流：${detail.returnCompany} ${detail.returnTrackingNo}", color = Muted)
                                }
                                if (!detail.reviewRemark.isNullOrBlank()) {
                                    Text("审核备注：${detail.reviewRemark}", color = Muted)
                                }
                            }
                        }
                    }
                    item {
                        Surface(color = SurfaceWarm, shape = RoundedCornerShape(18.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("售后记录", color = Ink, style = MaterialTheme.typography.titleMedium)
                                detail.logs.forEach { log ->
                                    Text(
                                        "${afterSaleStatusLabel(log.fromStatus)} → ${afterSaleStatusLabel(log.toStatus)}",
                                        color = Ink,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Text("${formatTime(log.createdAt)} ${log.remark}", color = Faint, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
                val cancellable = detail.status in listOf("APPLIED", "APPROVED", "WAIT_RETURN")
                if (cancellable || detail.status == "WAIT_RETURN") {
                    Surface(color = SurfaceWarm, shadowElevation = 12.dp) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            if (cancellable) {
                                TextButton(
                                    onClick = { cancelDialog = true },
                                    enabled = !busy,
                                    modifier = Modifier.weight(1f),
                                ) { Text("取消售后", color = Muted) }
                            }
                            if (detail.status == "WAIT_RETURN") {
                                Button(
                                    onClick = { returnDialog = true },
                                    enabled = !busy,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Terracotta),
                                ) { Text("填写退货物流") }
                            }
                        }
                    }
                }
            }
        }
    }

    if (cancelDialog) {
        AlertDialog(
            onDismissRequest = { cancelDialog = false },
            title = { Text("取消售后") },
            text = { Text("确定取消本次售后申请？") },
            confirmButton = {
                TextButton(onClick = { cancelDialog = false; viewModel.cancelAfterSale(afterSaleNo) }) {
                    Text("确定", color = Terracotta)
                }
            },
            dismissButton = { TextButton(onClick = { cancelDialog = false }) { Text("返回") } },
        )
    }
    if (returnDialog) {
        var company by remember { mutableStateOf("") }
        var trackingNo by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { returnDialog = false },
            title = { Text("填写退货物流") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(company, { company = it }, label = { Text("物流公司") }, singleLine = true)
                    OutlinedTextField(trackingNo, { trackingNo = it }, label = { Text("物流单号") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        returnDialog = false
                        viewModel.submitReturnShipment(afterSaleNo, company, trackingNo)
                    },
                    enabled = company.isNotBlank() && trackingNo.isNotBlank(),
                ) { Text("提交", color = Terracotta) }
            },
            dismissButton = { TextButton(onClick = { returnDialog = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = SurfaceSoft,
    unfocusedContainerColor = SurfaceSoft,
    focusedBorderColor = Terracotta,
    unfocusedBorderColor = Line,
)
