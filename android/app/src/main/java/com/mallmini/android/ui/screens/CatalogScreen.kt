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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.mallmini.android.ui.components.SectionHeader
import com.mallmini.android.ui.theme.Faint
import com.mallmini.android.ui.theme.Ink
import com.mallmini.android.ui.theme.Line
import com.mallmini.android.ui.theme.Muted
import com.mallmini.android.ui.theme.SurfaceWarm
import com.mallmini.android.ui.theme.Terracotta
import com.mallmini.android.ui.theme.TerracottaDark
import com.mallmini.android.ui.theme.TerracottaSoft

@Composable
fun CatalogScreen(navController: NavController, viewModel: MallViewModel) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val categoriesLoading by viewModel.categoriesLoading.collectAsStateWithLifecycle()
    val categoriesError by viewModel.categoriesError.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()

    var selectedRootId by rememberSaveable { mutableStateOf(0L) }
    var selectedChildId by rememberSaveable { mutableStateOf<Long?>(null) }

    LaunchedEffect(categories) {
        if (selectedRootId == 0L && categories.isNotEmpty()) {
            selectedRootId = categories.first().id
        }
    }

    val root = categories.firstOrNull { it.id == selectedRootId }
    val selectedCategoryId = selectedChildId ?: selectedRootId

    LaunchedEffect(selectedCategoryId) {
        if (selectedCategoryId > 0) {
            viewModel.loadProducts(categoryId = selectedCategoryId)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        MallTopBar(
            title = "商品分类",
            subtitle = "Collections",
            actions = {
                IconButton(onClick = { navController.navigate(Routes.SEARCH) }) {
                    Icon(Icons.Outlined.Search, contentDescription = "搜索")
                }
            },
        )

        when {
            categoriesLoading && categories.isEmpty() -> LoadingContent()
            categoriesError != null && categories.isEmpty() -> ErrorContent(
                message = categoriesError ?: "分类加载失败",
                onRetry = { viewModel.loadCategories() },
            )

            else -> Column(modifier = Modifier.fillMaxSize()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(categories) { category ->
                        ChoiceChip(
                            text = category.name,
                            selected = category.id == selectedRootId,
                            onClick = {
                                selectedRootId = category.id
                                selectedChildId = null
                            },
                        )
                    }
                }

                val children = root?.children.orEmpty()
                if (children.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item {
                            SmallChoiceChip(
                                text = "全部",
                                selected = selectedChildId == null,
                                onClick = { selectedChildId = null },
                            )
                        }
                        items(children) { child ->
                            SmallChoiceChip(
                                text = child.name,
                                selected = selectedChildId == child.id,
                                onClick = { selectedChildId = child.id },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                when {
                    products.loading && products.data == null -> LoadingContent()
                    products.error != null && products.data == null -> ErrorContent(
                        message = products.error ?: "商品加载失败",
                        onRetry = { viewModel.loadProducts(categoryId = selectedCategoryId, refresh = true) },
                    )

                    products.data?.items.isNullOrEmpty() -> EmptyContent(
                        title = "该分类暂无商品",
                        subtitle = "换个分类看看",
                    )

                    else -> LazyColumn(
                        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 26.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        item {
                            SectionHeader(
                                title = root?.name ?: "全部商品",
                                subtitle = "共 ${products.data?.total ?: 0} 件商品",
                            )
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
    }
}

@Composable
private fun ChoiceChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) Ink else SurfaceWarm)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 9.dp),
    ) {
        Text(
            text = text,
            color = if (selected) androidx.compose.ui.graphics.Color.White else Muted,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun SmallChoiceChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) TerracottaSoft else androidx.compose.ui.graphics.Color.Transparent)
            .border(
                width = 1.dp,
                color = if (selected) Terracotta.copy(alpha = 0.25f) else Line,
                shape = RoundedCornerShape(999.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(
            text = text,
            color = if (selected) TerracottaDark else Muted,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
