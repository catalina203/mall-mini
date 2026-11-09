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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import com.mallmini.android.data.CouponTemplate
import com.mallmini.android.data.FavoriteProduct
import com.mallmini.android.data.PointsLog
import com.mallmini.android.data.UserCoupon
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

@Composable
fun CouponsScreen(navController: NavController, viewModel: MallViewModel) {
    val claimable by viewModel.claimableCoupons.collectAsStateWithLifecycle()
    val mine by viewModel.myCoupons.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { viewModel.loadCoupons() }

    Column(modifier = Modifier.fillMaxSize()) {
        MallTopBar("优惠券", "Coupons", onBack = { navController.popBackStack() })
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TabChip("可领取", tab == 0) { tab = 0 }
            TabChip("我的优惠券", tab == 1) { tab = 1 }
        }
        Spacer(Modifier.height(12.dp))
        val state = if (tab == 0) claimable else mine
        when {
            state.loading && state.data == null -> LoadingContent()
            state.error != null && state.data == null -> ErrorContent(
                state.error ?: "优惠券加载失败",
                onRetry = { viewModel.loadCoupons() },
            )
            tab == 0 -> {
                val items = claimable.data.orEmpty()
                if (items.isEmpty()) EmptyContent("暂无可领取优惠券")
                else LazyColumn(
                    contentPadding = PaddingValues(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(items) { coupon ->
                        CouponCard(
                            name = coupon.name,
                            value = coupon.valueText(),
                            condition = coupon.conditionText(),
                            valid = "${coupon.validFrom.take(10)} 至 ${coupon.validTo.take(10)}",
                            action = {
                                Button(
                                    onClick = { viewModel.claimCoupon(coupon.id) },
                                    enabled = !busy,
                                    shape = RoundedCornerShape(999.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Terracotta),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                ) { Text("领取", fontSize = 12.sp) }
                            },
                        )
                    }
                }
            }
            else -> {
                val items = mine.data.orEmpty()
                if (items.isEmpty()) EmptyContent("暂无优惠券")
                else LazyColumn(
                    contentPadding = PaddingValues(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(items) { coupon ->
                        CouponCard(
                            name = coupon.name,
                            value = coupon.valueText(),
                            condition = coupon.conditionText(),
                            valid = couponStatus(coupon.status),
                            muted = coupon.status != "UNUSED",
                        )
                    }
                }
            }
        }
    }
}

private fun CouponTemplate.valueText(): String = if (type == "FIXED") "¥%.0f".format(value) else "%.0f%%".format(value)
private fun CouponTemplate.conditionText(): String = if (minAmount > 0) "满 ¥%.0f 可用".format(minAmount) else "无门槛"
private fun UserCoupon.valueText(): String = if (type == "FIXED") "¥%.0f".format(value) else "%.0f%%".format(value)
private fun UserCoupon.conditionText(): String = if (minAmount > 0) "满 ¥%.0f 可用".format(minAmount) else "无门槛"
private fun couponStatus(status: String): String = when (status) {
    "UNUSED" -> "未使用"
    "USED" -> "已使用"
    else -> "已过期"
}

@Composable
private fun CouponCard(
    name: String,
    value: String,
    condition: String,
    valid: String,
    muted: Boolean = false,
    action: (@Composable () -> Unit)? = null,
) {
    Surface(
        color = SurfaceWarm,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.width(96.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(value, color = if (muted) Muted else TerracottaDark, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(condition, color = Faint, fontSize = 10.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, color = if (muted) Muted else Ink, fontWeight = FontWeight.SemiBold)
                Text(valid, color = Faint, style = MaterialTheme.typography.bodySmall)
            }
            action?.invoke()
        }
    }
}

@Composable
fun MemberScreen(navController: NavController, viewModel: MallViewModel) {
    val member by viewModel.member.collectAsStateWithLifecycle()
    val logs by viewModel.pointsLogs.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.loadMember() }
    Column(modifier = Modifier.fillMaxSize()) {
        MallTopBar("会员积分", "Member", onBack = { navController.popBackStack() })
        val info = member.data
        when {
            member.loading && info == null -> LoadingContent()
            member.error != null && info == null -> ErrorContent(member.error ?: "加载失败", { viewModel.loadMember() })
            info != null -> LazyColumn(
                contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    Surface(
                        color = Color(0xFF2B211A),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(info.levelLabel, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.weight(1f))
                                Text("${info.points}", color = Color(0xFFF4A284), fontSize = 36.sp, fontWeight = FontWeight.Bold)
                                Text(" 积分", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                info.nextLevelPoints?.let { "累计 ${info.totalPoints} 分，距 ${info.nextLevelLabel} 还差 ${it - info.totalPoints} 分" }
                                    ?: "已达到最高会员等级",
                                color = Color.White.copy(alpha = 0.65f),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
                item {
                    Text("积分明细", color = Ink, style = MaterialTheme.typography.titleLarge)
                }
                val list = logs.data?.items.orEmpty()
                if (list.isEmpty()) {
                    item { EmptyContent("暂无积分记录", "订单完成后可获得积分") }
                } else {
                    items(list) { log -> PointsRow(log) }
                }
            }
        }
    }
}

@Composable
private fun PointsRow(log: PointsLog) {
    Surface(color = SurfaceWarm, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    when (log.changeType) {
                        "ORDER_REWARD" -> "订单奖励"
                        "ORDER_REFUND_REVOKE" -> "退款扣回"
                        else -> "管理员调整"
                    },
                    color = Ink,
                )
                Text("${formatTime(log.createdAt)} ${log.remark}", color = Faint, style = MaterialTheme.typography.bodySmall)
            }
            Text(
                (if (log.pointsDelta > 0) "+" else "") + log.pointsDelta,
                color = if (log.pointsDelta > 0) Terracotta else Color(0xFF047857),
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun SupportScreen(navController: NavController, viewModel: MallViewModel) {
    val tickets by viewModel.supportTickets.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    var orderNo by rememberSaveable { mutableStateOf("") }
    var subject by rememberSaveable { mutableStateOf("") }
    var content by rememberSaveable { mutableStateOf("") }
    var typeIndex by rememberSaveable { mutableIntStateOf(0) }
    val types = listOf("ORDER" to "订单问题", "PRODUCT" to "商品问题", "REFUND" to "退款/售后", "OTHER" to "其他")
    LaunchedEffect(Unit) { viewModel.loadSupportTickets() }
    Column(modifier = Modifier.fillMaxSize()) {
        MallTopBar("联系客服", "Support", onBack = { navController.popBackStack() })
        LazyColumn(
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Surface(color = SurfaceWarm, shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("提交工单", color = Ink, style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            types.forEachIndexed { index, item ->
                                TabChip(item.second, typeIndex == index) { typeIndex = index }
                            }
                        }
                        OutlinedTextField(orderNo, { orderNo = it }, label = { Text("订单号（选填）") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = supportColors())
                        OutlinedTextField(subject, { subject = it }, label = { Text("问题标题") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = supportColors())
                        OutlinedTextField(content, { content = it }, label = { Text("问题描述") }, minLines = 3, modifier = Modifier.fillMaxWidth(), colors = supportColors())
                        Button(
                            onClick = { viewModel.createTicket(orderNo, types[typeIndex].first, subject, content) },
                            enabled = !busy && subject.isNotBlank() && content.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Terracotta),
                        ) { Text(if (busy) "提交中…" else "提交工单") }
                    }
                }
            }
            item { Text("我的工单", color = Ink, style = MaterialTheme.typography.titleLarge) }
            val list = tickets.data?.items.orEmpty()
            if (tickets.loading && tickets.data == null) {
                item { LoadingContent() }
            } else if (list.isEmpty()) {
                item { EmptyContent("暂无工单") }
            } else {
                items(list) { ticket ->
                    Surface(color = SurfaceWarm, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(ticket.subject, color = Ink, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                Text(supportStatus(ticket.status), color = TerracottaDark, fontSize = 12.sp)
                            }
                            Text("${ticket.ticketNo} · ${formatTime(ticket.createdAt)}", color = Faint, style = MaterialTheme.typography.bodySmall)
                            Text(ticket.content, color = Muted, style = MaterialTheme.typography.bodyMedium)
                            if (ticket.reply.isNotBlank()) {
                                Surface(color = SurfaceSoft, shape = RoundedCornerShape(10.dp)) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("客服回复", color = TerracottaDark, style = MaterialTheme.typography.bodySmall)
                                        Text(ticket.reply, color = Muted, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                            if (ticket.status != "CLOSED") {
                                TextButton(
                                    onClick = { viewModel.closeTicket(ticket.ticketNo) },
                                    modifier = Modifier.align(Alignment.End),
                                ) { Text("关闭工单", color = Muted, fontSize = 12.sp) }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun supportStatus(status: String): String = when (status) {
    "OPEN" -> "待处理"
    "PROCESSING" -> "处理中"
    "RESOLVED" -> "已回复"
    else -> "已关闭"
}

@Composable
private fun supportColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = SurfaceSoft,
    unfocusedContainerColor = SurfaceSoft,
    focusedBorderColor = Terracotta,
    unfocusedBorderColor = Line,
)

@Composable
fun FavoritesScreen(navController: NavController, viewModel: MallViewModel) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        viewModel.loadFavorites()
        viewModel.loadHistory()
    }
    Column(modifier = Modifier.fillMaxSize()) {
        MallTopBar("收藏与足迹", "Saved", onBack = { navController.popBackStack() })
        Row(modifier = Modifier.padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TabChip("我的收藏", tab == 0) { tab = 0 }
            TabChip("浏览足迹", tab == 1) { tab = 1 }
        }
        Spacer(Modifier.height(12.dp))
        if (tab == 1) {
            Row(modifier = Modifier.padding(horizontal = 18.dp)) {
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { viewModel.clearHistory() }) { Text("清空足迹", color = Muted) }
            }
        }
        val state = if (tab == 0) favorites else history
        val list = state.data?.items.orEmpty()
        when {
            state.loading && state.data == null -> LoadingContent()
            list.isEmpty() -> EmptyContent(if (tab == 0) "暂无收藏" else "暂无浏览足迹")
            else -> LazyColumn(
                contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(list) { product -> FavoriteRow(product) { navController.navigate(Routes.product(product.productId)) } }
            }
        }
    }
}

@Composable
private fun FavoriteRow(product: FavoriteProduct, onClick: () -> Unit) {
    Surface(
        color = SurfaceWarm,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            RemoteImage(
                product.mainImage,
                modifier = Modifier
                    .size(78.dp)
                    .clip(RoundedCornerShape(10.dp)),
                radius = 10.dp,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(product.subtitle, color = Faint, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                PriceText(product.minPrice)
            }
            Text(formatTime(product.at), color = Faint, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun ProductReviewsScreen(navController: NavController, viewModel: MallViewModel, productId: Long) {
    val reviews by viewModel.productReviews.collectAsStateWithLifecycle()
    val summary by viewModel.reviewSummary.collectAsStateWithLifecycle()
    LaunchedEffect(productId) { viewModel.loadProductReviews(productId) }
    Column(modifier = Modifier.fillMaxSize()) {
        MallTopBar("商品评价", "Reviews", onBack = { navController.popBackStack() })
        val list = reviews.data?.items.orEmpty()
        LazyColumn(
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Surface(color = SurfaceWarm, shape = RoundedCornerShape(16.dp)) {
                    Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("%.1f".format(summary?.averageRating ?: 0.0), color = TerracottaDark, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(12.dp))
                        Text("共 ${summary?.reviewCount ?: 0} 条已审核评价", color = Muted)
                    }
                }
            }
            if (reviews.loading && reviews.data == null) {
                item { LoadingContent() }
            } else if (list.isEmpty()) {
                item { EmptyContent("暂无评价", "订单完成后可评价商品") }
            } else {
                items(list) { review ->
                    Surface(color = SurfaceWarm, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Row {
                                Text(review.userNickname, color = Ink, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.weight(1f))
                                Text("★".repeat(review.rating), color = Color(0xFFF59E0B))
                            }
                            Text(review.content.ifBlank { "用户未填写文字评价" }, color = Muted)
                            if (review.reply.isNotBlank()) {
                                Text("商家回复：${review.reply}", color = Faint, style = MaterialTheme.typography.bodySmall)
                            }
                            Text(formatTime(review.createdAt), color = Faint, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationsScreen(navController: NavController, viewModel: MallViewModel) {
    val state by viewModel.notifications.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.loadNotifications() }
    Column(modifier = Modifier.fillMaxSize()) {
        MallTopBar("消息通知", "Notifications", onBack = { navController.popBackStack() })
        val list = state.data?.items.orEmpty()
        when {
            state.loading && state.data == null -> LoadingContent()
            list.isEmpty() -> EmptyContent("暂无通知")
            else -> LazyColumn(
                contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(list) { item ->
                    Surface(
                        color = SurfaceWarm,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (!item.read) viewModel.markNotificationRead(item.id)
                                when (item.bizType) {
                                    "AFTER_SALE" -> navController.navigate(Routes.afterSaleDetail(item.bizNo))
                                    "ORDER" -> navController.navigate(Routes.orderDetail(item.bizNo))
                                }
                            },
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row {
                                Text(item.title, color = Ink, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                if (!item.read) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(RoundedCornerShape(99.dp))
                                            .background(Terracotta),
                                    )
                                }
                            }
                            Text(item.content, color = Muted, style = MaterialTheme.typography.bodyMedium)
                            Text(formatTime(item.createdAt), color = Faint, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) Terracotta else SurfaceWarm,
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            text,
            color = if (selected) Color.White else Muted,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
        )
    }
}
