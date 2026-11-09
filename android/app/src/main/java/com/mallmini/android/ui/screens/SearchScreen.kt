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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mallmini.android.ui.MallViewModel
import com.mallmini.android.ui.Routes
import com.mallmini.android.ui.components.EmptyContent
import com.mallmini.android.ui.components.ErrorContent
import com.mallmini.android.ui.components.LoadingContent
import com.mallmini.android.ui.components.MallTopBar
import com.mallmini.android.ui.components.ProductGrid
import com.mallmini.android.ui.theme.Faint
import com.mallmini.android.ui.theme.Ink
import com.mallmini.android.ui.theme.Line
import com.mallmini.android.ui.theme.Muted
import com.mallmini.android.ui.theme.SurfaceWarm
import com.mallmini.android.ui.theme.Terracotta
import com.mallmini.android.ui.theme.TerracottaDark
import com.mallmini.android.ui.theme.TerracottaSoft

@Composable
fun SearchScreen(navController: NavController, viewModel: MallViewModel) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    val hotKeywords by viewModel.hotKeywords.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current
    var keyword by rememberSaveable { mutableStateOf("") }
    var searched by rememberSaveable { mutableStateOf(false) }
    var searchedKeyword by rememberSaveable { mutableStateOf("") }

    fun performSearch(value: String) {
        val normalized = value.trim()
        if (normalized.isEmpty()) {
            return
        }
        keyword = normalized
        searchedKeyword = normalized
        searched = true
        keyboard?.hide()
        viewModel.loadProducts(keyword = normalized, refresh = true)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        MallTopBar(
            title = "搜索商品",
            subtitle = "Search",
            onBack = { navController.popBackStack() },
        )

        Column(modifier = Modifier.padding(horizontal = 18.dp)) {
            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("搜索商品名称或关键词", color = Faint) },
                leadingIcon = {
                    Icon(Icons.Outlined.Search, contentDescription = null, tint = Faint)
                },
                trailingIcon = {
                    if (keyword.isNotEmpty()) {
                        IconButton(onClick = { keyword = "" }) {
                            Icon(Icons.Outlined.Close, contentDescription = "清空", tint = Faint)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceWarm,
                    unfocusedContainerColor = SurfaceWarm,
                    focusedBorderColor = Terracotta.copy(alpha = 0.6f),
                    unfocusedBorderColor = Line,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { performSearch(keyword) }),
            )

            Spacer(Modifier.height(12.dp))
            val suggestions = hotKeywords.ifEmpty { listOf("耳机", "咖啡", "T恤") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                suggestions.take(5).forEach { suggestion ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(SurfaceWarm)
                            .border(1.dp, Line, RoundedCornerShape(999.dp))
                            .clickable { performSearch(suggestion) }
                            .padding(horizontal = 13.dp, vertical = 6.dp),
                    ) {
                        Text(suggestion, color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        when {
            !searched -> EmptyContent(
                title = "搜索你想找的商品",
                subtitle = "支持按商品名称与副标题搜索",
            )

            products.loading && products.data == null -> LoadingContent(label = "正在搜索")

            products.error != null && products.data == null -> ErrorContent(
                message = products.error ?: "搜索失败",
                onRetry = { viewModel.loadProducts(keyword = searchedKeyword, refresh = true) },
            )

            products.data?.items.isNullOrEmpty() -> EmptyContent(
                title = "没有找到「$searchedKeyword」",
                subtitle = "换个关键词试试",
            )

            else -> LazyColumn(
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 26.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Text(
                            text = "「$searchedKeyword」",
                            color = Ink,
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = "共 ${products.data?.total ?: 0} 件",
                            color = Faint,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                item {
                    ProductGrid(products.data?.items.orEmpty()) { id ->
                        navController.navigate(Routes.product(id))
                    }
                }
            }
        }
    }
}
