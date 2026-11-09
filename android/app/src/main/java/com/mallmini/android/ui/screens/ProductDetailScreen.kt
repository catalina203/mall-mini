package com.mallmini.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
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
import com.mallmini.android.data.ProductDetail
import com.mallmini.android.data.Sku
import com.mallmini.android.ui.MallViewModel
import com.mallmini.android.ui.Routes
import com.mallmini.android.ui.components.ErrorContent
import com.mallmini.android.ui.components.LoadingContent
import com.mallmini.android.ui.components.MallTopBar
import com.mallmini.android.ui.components.PriceText
import com.mallmini.android.ui.components.QuantityStepper
import com.mallmini.android.ui.components.RemoteImage
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductDetailScreen(
    navController: NavController,
    viewModel: MallViewModel,
    productId: Long,
) {
    val productState by viewModel.product.collectAsStateWithLifecycle()
    val loggedIn by viewModel.loggedIn.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val favoriteStatus by viewModel.favoriteStatus.collectAsStateWithLifecycle()
    val reviewSummary by viewModel.reviewSummary.collectAsStateWithLifecycle()
    val detail = productState.data
    val isFavorite = favoriteStatus[productId] == true

    var selectedSkuId by remember(productId) { mutableLongStateOf(0L) }
    var quantity by remember(productId) { mutableIntStateOf(1) }

    LaunchedEffect(productId) {
        viewModel.loadProduct(productId, force = true)
        viewModel.loadFavoriteStatus(productId)
        viewModel.loadProductReviews(productId)
        viewModel.recordProductHistory(productId)
    }

    LaunchedEffect(detail?.id, detail?.skus) {
        if (detail != null) {
            val available = detail.skus.filter { it.status == "ENABLED" && it.stock > 0 }
            if (available.none { it.id == selectedSkuId }) {
                selectedSkuId = if (available.size == 1) available.first().id else 0L
                quantity = 1
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        MallTopBar(
            title = "商品详情",
            subtitle = "Product",
            onBack = { navController.popBackStack() },
            actions = {
                IconButton(onClick = { viewModel.toggleFavorite(productId) }) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (isFavorite) "取消收藏" else "收藏",
                        tint = if (isFavorite) Terracotta else Muted,
                    )
                }
            },
        )

        when {
            productState.loading && detail == null -> LoadingContent()
            productState.error != null && detail == null -> ErrorContent(
                message = productState.error ?: "商品加载失败",
                onRetry = { viewModel.loadProduct(productId, force = true) },
            )

            detail != null -> {
                val selectedSku = detail.skus.firstOrNull { it.id == selectedSkuId }
                val availableSkus = detail.skus.filter { it.status == "ENABLED" && it.stock > 0 }
                val maxQuantity = selectedSku?.stock?.coerceAtMost(99) ?: 1
                val displayPrice = selectedSku?.price ?: detail.skus.filter { it.status == "ENABLED" }
                    .minOfOrNull { it.price } ?: 0.0

                Column(modifier = Modifier.weight(1f)) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 22.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        item {
                            ProductGallery(detail)
                        }
                        item {
                            Column {
                                if (detail.subtitle.isNotBlank()) {
                                    Text(
                                        text = detail.subtitle,
                                        color = Terracotta,
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                    Spacer(Modifier.height(6.dp))
                                }
                                Text(
                                    text = detail.name,
                                    color = Ink,
                                    style = MaterialTheme.typography.headlineSmall,
                                )
                                Spacer(Modifier.height(10.dp))
                                Row(verticalAlignment = Alignment.Bottom) {
                                    PriceText(displayPrice, large = true)
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = if (availableSkus.isEmpty()) "暂时缺货" else "库存 ${availableSkus.sumOf { it.stock }}",
                                        color = if (availableSkus.isEmpty()) Terracotta else Faint,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(bottom = 5.dp),
                                    )
                                }
                            }
                        }
                        item {
                            Surface(
                                color = SurfaceWarm,
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "选择规格",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Ink,
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    if (detail.skus.isEmpty()) {
                                        Text("暂无可用规格", color = Muted)
                                    } else {
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(9.dp),
                                            verticalArrangement = Arrangement.spacedBy(9.dp),
                                        ) {
                                            detail.skus.forEach { sku ->
                                                SkuChip(
                                                    sku = sku,
                                                    selected = sku.id == selectedSkuId,
                                                    onClick = {
                                                        selectedSkuId = sku.id
                                                        quantity = 1
                                                    },
                                                )
                                            }
                                        }
                                    }
                                    if (selectedSku != null) {
                                        Spacer(Modifier.height(16.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("购买数量", color = Ink, style = MaterialTheme.typography.titleMedium)
                                                Text(
                                                    "库存 ${selectedSku.stock} 件",
                                                    color = Faint,
                                                    style = MaterialTheme.typography.bodySmall,
                                                )
                                            }
                                            QuantityStepper(
                                                quantity = quantity,
                                                max = maxQuantity,
                                                onChange = { quantity = it },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            Surface(
                                color = SurfaceWarm,
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { navController.navigate(Routes.productReviews(productId)) },
                            ) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("商品评价", style = MaterialTheme.typography.titleMedium, color = Ink)
                                        Text(
                                            "%.1f 分 · %d 条已审核评价".format(
                                                reviewSummary?.averageRating ?: 0.0,
                                                reviewSummary?.reviewCount ?: 0,
                                            ),
                                            color = Muted,
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                    Text("查看全部 ›", color = Terracotta, style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                        item {
                            Surface(
                                color = SurfaceWarm,
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "商品描述",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Ink,
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = detail.description.ifBlank { "暂无商品描述" },
                                        color = Muted,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                        }
                        item {
                            Text(
                                text = "登录后可加入购物车或立即购买；支付为开发环境模拟支付。",
                                color = Faint,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }

                    Surface(
                        color = SurfaceWarm,
                        shadowElevation = 12.dp,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                                    .padding(horizontal = 18.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("合计", color = Faint, fontSize = 10.sp)
                                PriceText(displayPrice * quantity, large = false)
                            }
                            Button(
                                onClick = {
                                    if (!loggedIn) {
                                        navController.navigate(Routes.PROFILE)
                                        return@Button
                                    }
                                    if (selectedSkuId == 0L) {
                                        return@Button
                                    }
                                    viewModel.addToCart(selectedSkuId, quantity)
                                },
                                enabled = selectedSkuId > 0L && !busy,
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = TerracottaSoft,
                                    contentColor = TerracottaDark,
                                    disabledContainerColor = SurfaceSoft,
                                    disabledContentColor = Faint,
                                ),
                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 11.dp),
                            ) {
                                Text(if (loggedIn) "加入购物车" else "登录后加购")
                            }
                            Button(
                                onClick = {
                                    if (!loggedIn) {
                                        navController.navigate(Routes.PROFILE)
                                        return@Button
                                    }
                                    if (selectedSkuId == 0L) {
                                        return@Button
                                    }
                                    navController.navigate(
                                        Routes.checkout(
                                            source = "BUY_NOW",
                                            cartItemIds = emptyList(),
                                            skuId = selectedSkuId,
                                            quantity = quantity,
                                        ),
                                    )
                                },
                                enabled = selectedSkuId > 0L && !busy,
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Terracotta,
                                    contentColor = Color.White,
                                ),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 11.dp),
                            ) {
                                Text("立即购买")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductGallery(detail: ProductDetail) {
    val urls = remember(detail) {
        (listOf(detail.mainImage) + detail.images.map { it.url })
            .filter { it.isNotBlank() }
            .distinct()
            .ifEmpty { listOf("") }
    }
    val pagerState = rememberPagerState(pageCount = { urls.size })

    Column {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(330.dp),
        ) { page ->
            RemoteImage(
                url = urls[page],
                modifier = Modifier.fillMaxSize(),
                radius = 22.dp,
            )
        }
        if (urls.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                repeat(urls.size) { index ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .height(5.dp)
                            .width(if (pagerState.currentPage == index) 18.dp else 5.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (pagerState.currentPage == index) Terracotta else Line),
                    )
                }
            }
        }
    }
}

@Composable
private fun SkuChip(sku: Sku, selected: Boolean, onClick: () -> Unit) {
    val available = sku.status == "ENABLED" && sku.stock > 0
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) TerracottaSoft else SurfaceSoft)
            .border(
                width = 1.dp,
                color = if (selected) Terracotta else Line,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(enabled = available, onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 9.dp),
    ) {
        Column {
            Text(
                text = specText(sku.spec),
                color = if (!available) Faint else if (selected) TerracottaDark else Ink,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (!available) "不可用" else "¥${"%.2f".format(sku.price)}",
                color = if (!available) Faint else Muted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
