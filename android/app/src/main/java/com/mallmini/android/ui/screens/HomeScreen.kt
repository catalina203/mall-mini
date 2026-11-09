package com.mallmini.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.mallmini.android.data.Category
import com.mallmini.android.ui.MallViewModel
import com.mallmini.android.ui.Routes
import com.mallmini.android.ui.components.ErrorContent
import com.mallmini.android.ui.components.LoadingContent
import com.mallmini.android.ui.components.MallTopBar
import com.mallmini.android.ui.components.ProductGrid
import com.mallmini.android.ui.components.RemoteImage
import com.mallmini.android.ui.components.SectionHeader
import com.mallmini.android.ui.theme.Danger
import com.mallmini.android.ui.theme.Faint
import com.mallmini.android.ui.theme.Ink
import com.mallmini.android.ui.theme.Line
import com.mallmini.android.ui.theme.Muted
import com.mallmini.android.ui.theme.SurfaceSoft
import com.mallmini.android.ui.theme.SurfaceWarm
import com.mallmini.android.ui.theme.Terracotta
import com.mallmini.android.ui.theme.TerracottaSoft

@Composable
fun HomeScreen(navController: NavController, viewModel: MallViewModel) {
    val home by viewModel.home.collectAsStateWithLifecycle()
    val user by viewModel.user.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadHome()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        MallTopBar(
            title = "优选商城",
            subtitle = "Curated Store",
            actions = {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(TerracottaSoft)
                        .clickable { navController.navigate(Routes.PROFILE) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (user?.nickname.isNullOrBlank()) {
                        Icon(
                            Icons.Outlined.Person,
                            contentDescription = "个人中心",
                            tint = Terracotta,
                            modifier = Modifier.size(19.dp),
                        )
                    } else {
                        Text(
                            text = user!!.nickname.take(1),
                            color = Terracotta,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        )
                    }
                }
            },
        )

        val data = home.data
        when {
            home.loading && data == null -> LoadingContent()
            home.error != null && data == null -> ErrorContent(
                message = home.error ?: "首页加载失败",
                onRetry = { viewModel.loadHome(force = true) },
            )

            data != null -> LazyColumn(
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 26.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                item {
                    SearchEntry(onClick = { navController.navigate(Routes.SEARCH) })
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        HomeShortcut("领券中心", "满减 / 折扣", Modifier.weight(1f)) {
                            navController.navigate(Routes.COUPONS)
                        }
                        HomeShortcut("联系客服", "工单 / 售后", Modifier.weight(1f)) {
                            navController.navigate(Routes.SUPPORT)
                        }
                        HomeShortcut("会员积分", "积分 / 等级", Modifier.weight(1f)) {
                            navController.navigate(Routes.MEMBER)
                        }
                    }
                }

                if (data.banners.isNotEmpty()) {
                    item {
                        BannerPager(data.banners.map { it.imageUrl }) { index ->
                            val banner = data.banners[index]
                            if (banner.linkType == "PRODUCT") {
                                banner.linkTarget.toLongOrNull()?.let {
                                    navController.navigate(Routes.product(it))
                                }
                            }
                        }
                    }
                }

                if (data.categories.isNotEmpty()) {
                    item {
                        SectionHeader(title = "分类精选", subtitle = "按生活场景挑选好物")
                        Spacer(Modifier.height(12.dp))
                        CategoryRow(data.categories) {
                            navController.navigate(Routes.CATALOG)
                        }
                    }
                }

                if (data.hotProducts.isNotEmpty()) {
                    item {
                        SectionHeader(title = "热销榜单", subtitle = "按支付销量")
                        Spacer(Modifier.height(12.dp))
                        ProductGrid(data.hotProducts) { id -> navController.navigate(Routes.product(id)) }
                    }
                }

                if (data.newProducts.isNotEmpty()) {
                    item {
                        SectionHeader(title = "新品上架", subtitle = "最近上架")
                        Spacer(Modifier.height(12.dp))
                        ProductGrid(data.newProducts) { id -> navController.navigate(Routes.product(id)) }
                    }
                }

                item {
                    SectionHeader(
                        title = "今日推荐",
                        subtitle = "来自优选商城的样例商品",
                        actionLabel = "全部商品",
                        onAction = { navController.navigate(Routes.CATALOG) },
                    )
                }

                if (data.recommendedProducts.isEmpty()) {
                    item {
                        Text(
                            text = "暂无推荐商品",
                            color = Muted,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                } else {
                    item {
                        ProductGrid(data.recommendedProducts) { id ->
                            navController.navigate(Routes.product(id))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeShortcut(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        color = SurfaceWarm,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 13.dp)) {
            Text(title, color = Ink, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, color = Faint, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SearchEntry(onClick: () -> Unit) {
    Surface(
        color = SurfaceWarm,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Line.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Search,
                contentDescription = null,
                tint = Faint,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(9.dp))
            Text(
                text = "搜索商品名称或关键词",
                color = Faint,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "搜索",
                color = Terracotta,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun BannerPager(
    banners: List<String>,
    onBannerClick: (Int) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { banners.size })
    Column {
        HorizontalPager(
            state = pagerState,
            pageSpacing = 10.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(176.dp),
        ) { page ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onBannerClick(page) },
            ) {
                RemoteImage(
                    url = banners[page],
                    modifier = Modifier.fillMaxSize(),
                    radius = 20.dp,
                )
            }
        }
        if (banners.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                repeat(banners.size) { index ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .height(5.dp)
                            .width(if (pagerState.currentPage == index) 18.dp else 5.dp)
                            .clip(CircleShape)
                            .background(if (pagerState.currentPage == index) Terracotta else Line),
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(categories: List<Category>, onClick: () -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(categories.take(8)) { category ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(66.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onClick)
                    .padding(vertical = 4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(SurfaceWarm),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = category.name.take(1),
                        color = Terracotta,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                    )
                }
                Spacer(Modifier.height(7.dp))
                Text(
                    text = category.name,
                    color = Ink,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
