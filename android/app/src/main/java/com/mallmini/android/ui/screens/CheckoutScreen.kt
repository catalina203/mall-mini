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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.mallmini.android.data.Address
import com.mallmini.android.data.CheckoutPreview
import com.mallmini.android.ui.MallViewModel
import com.mallmini.android.ui.Routes
import com.mallmini.android.ui.components.ErrorContent
import com.mallmini.android.ui.components.LoadingContent
import com.mallmini.android.ui.components.MallTopBar
import com.mallmini.android.ui.components.PriceText
import com.mallmini.android.ui.components.RemoteImage
import com.mallmini.android.ui.theme.Faint
import com.mallmini.android.ui.theme.Ink
import com.mallmini.android.ui.theme.Line
import com.mallmini.android.ui.theme.Muted
import com.mallmini.android.ui.theme.SurfaceSoft
import com.mallmini.android.ui.theme.SurfaceWarm
import com.mallmini.android.ui.theme.Terracotta
import com.mallmini.android.ui.theme.TerracottaDark
import com.mallmini.android.ui.theme.TerracottaSoft
import java.util.UUID

@Composable
fun CheckoutScreen(
    navController: NavController,
    viewModel: MallViewModel,
    source: String,
    cartItemIds: List<Long>,
    skuId: Long?,
    quantity: Int?,
) {
    val addressesState by viewModel.addresses.collectAsStateWithLifecycle()
    val checkoutState by viewModel.checkout.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val loggedIn by viewModel.loggedIn.collectAsStateWithLifecycle()

    var selectedAddressId by rememberSaveable { mutableLongStateOf(0L) }
    var addressPickerVisible by remember { mutableStateOf(false) }
    val clientRequestNo = rememberSaveable(source, cartItemIds, skuId, quantity) {
        UUID.randomUUID().toString()
    }
    val addresses = addressesState.data.orEmpty()

    LaunchedEffect(Unit) {
        viewModel.loadAddresses()
    }

    LaunchedEffect(addressesState.data) {
        if (addresses.isNotEmpty() && addresses.none { it.id == selectedAddressId }) {
            selectedAddressId = addresses.firstOrNull { it.isDefault }?.id ?: addresses.first().id
        }
    }

    LaunchedEffect(selectedAddressId, source, cartItemIds, skuId, quantity) {
        if (selectedAddressId > 0) {
            viewModel.previewCheckout(
                addressId = selectedAddressId,
                source = source,
                cartItemIds = cartItemIds,
                skuId = skuId,
                quantity = quantity,
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        MallTopBar(
            title = "确认订单",
            subtitle = "Checkout",
            onBack = { navController.popBackStack() },
        )

        if (!loggedIn) {
            Text(
                text = "登录状态已失效，请重新登录",
                color = Muted,
                modifier = Modifier.padding(28.dp),
            )
            return@Column
        }

        val preview = checkoutState.data
        when {
            addressesState.loading && addressesState.data == null -> LoadingContent()

            addressesState.error != null && addressesState.data == null -> ErrorContent(
                message = addressesState.error ?: "地址加载失败",
                onRetry = { viewModel.loadAddresses() },
            )

            addresses.isEmpty() -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("请先添加收货地址", color = Ink, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text("添加地址后即可继续结算", color = Muted, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = { navController.navigate(Routes.addressEdit(null)) },
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Terracotta),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("新增地址")
                }
            }

            checkoutState.loading && preview == null -> LoadingContent(label = "正在计算金额")

            checkoutState.error != null && preview == null -> ErrorContent(
                message = checkoutState.error ?: "结算信息加载失败",
                onRetry = {
                    viewModel.previewCheckout(selectedAddressId, source, cartItemIds, skuId, quantity)
                },
            )

            preview != null -> Column(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    item {
                        AddressSelector(
                            address = preview.address,
                            onChange = { addressPickerVisible = true },
                        )
                    }
                    item {
                        CheckoutItems(preview)
                    }
                    item {
                        CheckoutAmounts(preview)
                    }
                    item {
                        Surface(
                            color = TerracottaSoft,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "提交订单后进入待付款状态，可在订单详情中使用开发环境模拟支付。",
                                color = TerracottaDark,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(14.dp),
                            )
                        }
                    }
                }

                Surface(color = SurfaceWarm, shadowElevation = 12.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("应付金额", color = Faint, style = MaterialTheme.typography.bodySmall)
                            PriceText(preview.payableAmount, large = true)
                        }
                        Button(
                            onClick = {
                                viewModel.createOrder(
                                    addressId = selectedAddressId,
                                    source = source,
                                    cartItemIds = cartItemIds,
                                    skuId = skuId,
                                    quantity = quantity,
                                    clientRequestNo = clientRequestNo,
                                ) { order ->
                                    navController.navigate(Routes.orderDetail(order.orderNo)) {
                                        popUpTo(Routes.HOME)
                                    }
                                }
                            },
                            enabled = !busy,
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Terracotta,
                                contentColor = Color.White,
                            ),
                            contentPadding = PaddingValues(horizontal = 26.dp, vertical = 13.dp),
                        ) {
                            Text(if (busy) "提交中…" else "提交订单")
                        }
                    }
                }
            }
        }
    }

    if (addressPickerVisible) {
        AddressPickerDialog(
            addresses = addresses,
            selectedId = selectedAddressId,
            onSelect = {
                selectedAddressId = it
                addressPickerVisible = false
            },
            onDismiss = { addressPickerVisible = false },
            onAdd = {
                addressPickerVisible = false
                navController.navigate(Routes.addressEdit(null))
            },
        )
    }
}

@Composable
private fun AddressSelector(
    address: com.mallmini.android.data.AddressSnapshot,
    onChange: () -> Unit,
) {
    Surface(
        color = SurfaceWarm,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onChange),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(TerracottaSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = Terracotta,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = address.receiverName,
                        color = Ink,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(address.phone, color = Muted, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${address.province}${address.city}${address.district}${address.detail}",
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text("更换", color = Terracotta, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun CheckoutItems(preview: CheckoutPreview) {
    Surface(
        color = SurfaceWarm,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("商品清单", color = Ink, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            preview.items.forEachIndexed { index, item ->
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
private fun CheckoutAmounts(preview: CheckoutPreview) {
    Surface(
        color = SurfaceWarm,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("金额明细", color = Ink, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            AmountRow("商品金额", preview.goodsAmount)
            AmountRow("运费", preview.shippingFee, freeText = true)
            if (preview.discountAmount > 0) {
                AmountRow("优惠", -preview.discountAmount)
            }
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Line),
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("应付合计", color = Ink, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                PriceText(preview.payableAmount, large = true)
            }
            Text(
                text = if (preview.freeShippingThreshold > 0) {
                    "满 ¥%.2f 包邮".format(preview.freeShippingThreshold)
                } else {
                    "全场包邮"
                },
                color = Faint,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun AmountRow(label: String, amount: Double, freeText: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
    ) {
        Text(label, color = Muted, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.weight(1f))
        Text(
            text = if (freeText && amount <= 0) "免运费" else "¥%.2f".format(amount),
            color = Ink,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun AddressPickerDialog(
    addresses: List<Address>,
    selectedId: Long,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择收货地址") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                addresses.forEach { address ->
                    Surface(
                        color = if (address.id == selectedId) TerracottaSoft else SurfaceSoft,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(address.id) },
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = address.receiverName,
                                    color = Ink,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(address.phone, color = Muted, style = MaterialTheme.typography.bodySmall)
                                if (address.isDefault) {
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "默认",
                                        color = TerracottaDark,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                            Text(
                                text = "${address.province}${address.city}${address.district}${address.detail}",
                                color = Muted,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onAdd) { Text("新增地址", color = Terracotta) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = Muted) }
        },
    )
}
