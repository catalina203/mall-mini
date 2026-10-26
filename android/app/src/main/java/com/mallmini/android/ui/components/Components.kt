package com.mallmini.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.mallmini.android.ui.theme.Cream
import com.mallmini.android.ui.theme.Danger
import com.mallmini.android.ui.theme.Faint
import com.mallmini.android.ui.theme.Ink
import com.mallmini.android.ui.theme.InkSoft
import com.mallmini.android.ui.theme.Line
import com.mallmini.android.ui.theme.Muted
import com.mallmini.android.ui.theme.Success
import com.mallmini.android.ui.theme.SurfaceSoft
import com.mallmini.android.ui.theme.SurfaceWarm
import com.mallmini.android.ui.theme.Terracotta
import com.mallmini.android.ui.theme.TerracottaDark
import com.mallmini.android.ui.theme.TerracottaSoft
import com.mallmini.android.data.ProductSummary
import com.mallmini.android.ui.theme.Warning
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun BrandMark(size: Dp = 40.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size / 3.2f))
            .background(Terracotta),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "M",
            color = Color.White,
            fontFamily = FontFamily.Serif,
            fontSize = (size.value * 0.52f).sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun MallTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Cream)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "返回", tint = Ink)
            }
            Spacer(Modifier.width(2.dp))
        } else {
            BrandMark(34.dp)
            Spacer(Modifier.width(10.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            if (subtitle != null) {
                Text(
                    text = subtitle.uppercase(),
                    color = Terracotta,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.6.sp,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        actions()
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = Ink)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Muted)
            }
        }
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                color = Terracotta,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
fun RemoteImage(
    url: String,
    modifier: Modifier = Modifier,
    radius: Dp = 14.dp,
    contentScale: ContentScale = ContentScale.Crop,
) {
    SubcomposeAsyncImage(
        model = com.mallmini.android.data.ApiConfig.resolveImage(url),
        contentDescription = null,
        contentScale = contentScale,
        modifier = modifier.clip(RoundedCornerShape(radius)),
        loading = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SurfaceSoft),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Terracotta,
                )
            }
        },
        error = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SurfaceSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Image,
                    contentDescription = null,
                    tint = Faint,
                    modifier = Modifier.size(26.dp),
                )
            }
        },
    )
}

@Composable
fun PriceText(
    amount: Double,
    large: Boolean = false,
    color: Color = TerracottaDark,
) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = "¥",
            color = color,
            fontSize = if (large) 15.sp else 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = if (large) 4.dp else 2.dp),
        )
        Text(
            text = String.format(Locale.CHINA, "%.2f", amount),
            color = color,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = if (large) 28.sp else 17.sp,
        )
    }
}

@Composable
fun StatusChip(status: String) {
    val (text, color, background) = when (status) {
        "PENDING_PAYMENT" -> Triple("待付款", TerracottaDark, TerracottaSoft)
        "PENDING_SHIPMENT" -> Triple("待发货", Warning, Color(0xFFFDF2DF))
        "PENDING_RECEIPT" -> Triple("待收货", Color(0xFF1D4ED8), Color(0xFFEFF6FF))
        "COMPLETED" -> Triple("已完成", Success, Color(0xFFE8F4EF))
        "CANCELLED" -> Triple("已取消", Muted, Color(0xFFF2F0ED))
        else -> Triple(status, Muted, Color(0xFFF2F0ED))
    }
    Text(
        text = text,
        color = color,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .padding(horizontal = 9.dp, vertical = 4.dp),
    )
}

@Composable
fun QuantityStepper(
    quantity: Int,
    max: Int,
    onChange: (Int) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceSoft),
    ) {
        StepButton(
            text = "−",
            enabled = enabled && quantity > 1,
            onClick = { onChange(quantity - 1) },
        )
        Text(
            text = quantity.toString(),
            modifier = Modifier.width(38.dp),
            color = Ink,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
        StepButton(
            text = "+",
            enabled = enabled && quantity < max,
            onClick = { onChange(quantity + 1) },
        )
    }
}

@Composable
private fun StepButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (enabled) Ink else Faint,
            fontSize = 18.sp,
        )
    }
}

@Composable
fun SelectDot(selected: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(if (selected) Terracotta else Color.Transparent)
            .then(
                if (!selected) {
                    Modifier.background(SurfaceWarm, CircleShape)
                } else {
                    Modifier
                },
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                Icons.Outlined.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(if (enabled) Color.Transparent else SurfaceSoft),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = if (enabled) Line else Faint.copy(alpha = 0.4f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f),
                    )
                }
            }
        }
    }
}

@Composable
fun LoadingContent(modifier: Modifier = Modifier, label: String = "正在加载") {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = Terracotta,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(30.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(label, color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun ErrorContent(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            color = Muted,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(14.dp))
        Surface(
            color = TerracottaSoft,
            shape = RoundedCornerShape(999.dp),
            modifier = Modifier.clickable(onClick = onRetry),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Icon(
                    Icons.Outlined.Refresh,
                    contentDescription = null,
                    tint = TerracottaDark,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text("重试", color = TerracottaDark, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun EmptyContent(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BrandMark(46.dp)
        Spacer(Modifier.height(14.dp))
        Text(title, color = InkSoft, style = MaterialTheme.typography.titleMedium)
        if (subtitle != null) {
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = Faint, style = MaterialTheme.typography.bodySmall)
        }
    }
}


fun formatTime(value: String?): String {
    if (value.isNullOrBlank()) {
        return "—"
    }
    return runCatching {
        LocalDateTime.parse(value).format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))
    }.getOrElse {
        value.replace("T", " ").take(16)
    }
}

fun specText(spec: Map<String, String>): String =
    spec.values.joinToString(" / ").ifBlank { "默认规格" }


@Composable
fun ProductCard(product: ProductSummary, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceWarm)
            .clickable(onClick = onClick),
    ) {
        RemoteImage(
            url = product.mainImage,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            radius = 16.dp,
        )
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = product.name,
                color = Ink,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = product.subtitle.ifBlank { " " },
                color = Faint,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                PriceText(product.minPrice, large = false)
                if (product.maxPrice > product.minPrice) {
                    Spacer(Modifier.width(3.dp))
                    Text("起", color = Faint, fontSize = 10.sp, modifier = Modifier.padding(bottom = 2.dp))
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (product.totalStock > 0) "库存 ${product.totalStock}" else "缺货",
                    color = if (product.totalStock > 0) Faint else Danger,
                    fontSize = 9.sp,
                )
            }
        }
    }
}

@Composable
fun ProductGrid(products: List<ProductSummary>, onProductClick: (Long) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        products.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                row.forEach { product ->
                    Box(modifier = Modifier.weight(1f)) {
                        ProductCard(product) { onProductClick(product.id) }
                    }
                }
                if (row.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}


fun orderStatusLabel(status: String): String = when (status) {
    "PENDING_PAYMENT" -> "待付款"
    "PENDING_SHIPMENT" -> "待发货"
    "PENDING_RECEIPT" -> "待收货"
    "COMPLETED" -> "已完成"
    "CANCELLED" -> "已取消"
    else -> status
}
