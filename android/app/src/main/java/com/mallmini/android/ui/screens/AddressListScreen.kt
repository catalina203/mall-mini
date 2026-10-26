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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.LocationOn
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mallmini.android.data.Address
import com.mallmini.android.ui.MallViewModel
import com.mallmini.android.ui.Routes
import com.mallmini.android.ui.components.EmptyContent
import com.mallmini.android.ui.components.ErrorContent
import com.mallmini.android.ui.components.LoadingContent
import com.mallmini.android.ui.components.MallTopBar
import com.mallmini.android.ui.theme.Faint
import com.mallmini.android.ui.theme.Ink
import com.mallmini.android.ui.theme.Line
import com.mallmini.android.ui.theme.Muted
import com.mallmini.android.ui.theme.SurfaceWarm
import com.mallmini.android.ui.theme.Terracotta
import com.mallmini.android.ui.theme.TerracottaDark
import com.mallmini.android.ui.theme.TerracottaSoft

@Composable
fun AddressListScreen(navController: NavController, viewModel: MallViewModel) {
    val loggedIn by viewModel.loggedIn.collectAsStateWithLifecycle()
    val addressesState by viewModel.addresses.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    var deleteTarget by remember { mutableStateOf<Address?>(null) }

    LaunchedEffect(loggedIn) {
        if (loggedIn) {
            viewModel.loadAddresses()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        MallTopBar(
            title = "收货地址",
            subtitle = "Addresses",
            onBack = { navController.popBackStack() },
            actions = {
                IconButton(onClick = { navController.navigate(Routes.addressEdit(null)) }) {
                    Icon(Icons.Outlined.Add, contentDescription = "新增地址", tint = Terracotta)
                }
            },
        )

        if (!loggedIn) {
            EmptyContent(title = "登录后管理收货地址", subtitle = "请先到「我的」完成登录")
            return@Column
        }

        val data = addressesState.data
        when {
            addressesState.loading && data == null -> LoadingContent()
            addressesState.error != null && data == null -> ErrorContent(
                message = addressesState.error ?: "地址加载失败",
                onRetry = { viewModel.loadAddresses() },
            )

            data.isNullOrEmpty() -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                EmptyContent(
                    title = "暂无收货地址",
                    subtitle = "新增地址后即可下单",
                )
                Button(
                    onClick = { navController.navigate(Routes.addressEdit(null)) },
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Terracotta),
                ) {
                    Text("新增地址")
                }
            }

            else -> LazyColumn(
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 26.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(data, key = { it.id }) { address ->
                    AddressCard(
                        address = address,
                        busy = busy,
                        onEdit = { navController.navigate(Routes.addressEdit(address.id)) },
                        onSetDefault = { viewModel.setDefaultAddress(address.id) },
                        onDelete = { deleteTarget = address },
                    )
                }
                item {
                    Button(
                        onClick = { navController.navigate(Routes.addressEdit(null)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SurfaceWarm,
                            contentColor = TerracottaDark,
                        ),
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("新增收货地址")
                    }
                }
            }
        }
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除地址") },
            text = { Text("确定删除「${target.receiverName}」的收货地址？历史订单快照不受影响。") },
            confirmButton = {
                TextButton(onClick = {
                    deleteTarget = null
                    viewModel.deleteAddress(target.id)
                }) { Text("删除", color = Terracotta) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun AddressCard(
    address: Address,
    busy: Boolean,
    onEdit: () -> Unit,
    onSetDefault: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        color = SurfaceWarm,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(TerracottaSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = Terracotta,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.size(10.dp))
                Text(
                    text = address.receiverName,
                    color = Ink,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = address.phone,
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                if (address.isDefault) {
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = "默认",
                        color = TerracottaDark,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(TerracottaSoft)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "${address.province}${address.city}${address.district}",
                color = Muted,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = address.detail,
                color = Ink,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                if (!address.isDefault) {
                    TextButton(onClick = onSetDefault, enabled = !busy) {
                        Text("设为默认", color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
                TextButton(onClick = onEdit) {
                    Text("编辑", color = TerracottaDark, style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = onDelete, enabled = !busy) {
                    Text("删除", color = Terracotta, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
