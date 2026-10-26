package com.mallmini.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mallmini.android.data.AddressPayload
import com.mallmini.android.ui.MallViewModel
import com.mallmini.android.ui.components.MallTopBar
import com.mallmini.android.ui.theme.Faint
import com.mallmini.android.ui.theme.Ink
import com.mallmini.android.ui.theme.Line
import com.mallmini.android.ui.theme.Muted
import com.mallmini.android.ui.theme.SurfaceSoft
import com.mallmini.android.ui.theme.SurfaceWarm
import com.mallmini.android.ui.theme.Terracotta
import com.mallmini.android.ui.theme.TerracottaDark

@Composable
fun AddressEditScreen(
    navController: NavController,
    viewModel: MallViewModel,
    addressId: Long,
) {
    val addressesState by viewModel.addresses.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()

    var receiverName by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var province by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("") }
    var district by rememberSaveable { mutableStateOf("") }
    var detail by rememberSaveable { mutableStateOf("") }
    var isDefault by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var initialized by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (addressesState.data == null) {
            viewModel.loadAddresses()
        }
    }

    LaunchedEffect(addressesState.data, addressId, initialized) {
        if (!initialized && addressId > 0) {
            val address = addressesState.data?.firstOrNull { it.id == addressId }
            if (address != null) {
                receiverName = address.receiverName
                phone = address.phone
                province = address.province
                city = address.city
                district = address.district
                detail = address.detail
                isDefault = address.isDefault
                initialized = true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        MallTopBar(
            title = if (addressId > 0) "编辑地址" else "新增地址",
            subtitle = "Address",
            onBack = { navController.popBackStack() },
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                color = SurfaceWarm,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    FormField(
                        label = "收货人",
                        value = receiverName,
                        placeholder = "请输入收货人姓名",
                        onValueChange = { receiverName = it },
                    )
                    FormField(
                        label = "手机号",
                        value = phone,
                        placeholder = "请输入 11 位手机号",
                        keyboardType = KeyboardType.Phone,
                        onValueChange = { phone = it },
                    )
                    FormField(
                        label = "省份",
                        value = province,
                        placeholder = "例如：广东省",
                        onValueChange = { province = it },
                    )
                    FormField(
                        label = "城市",
                        value = city,
                        placeholder = "例如：深圳市",
                        onValueChange = { city = it },
                    )
                    FormField(
                        label = "区县",
                        value = district,
                        placeholder = "例如：南山区",
                        onValueChange = { district = it },
                    )
                    FormField(
                        label = "详细地址",
                        value = detail,
                        placeholder = "街道、门牌号等",
                        onValueChange = { detail = it },
                    )
                }
            }

            Surface(
                color = SurfaceWarm,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("设为默认地址", color = Ink, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "下单时优先使用此地址",
                            color = Faint,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Switch(
                        checked = isDefault,
                        onCheckedChange = { isDefault = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = androidx.compose.ui.graphics.Color.White,
                            checkedTrackColor = Terracotta,
                        ),
                    )
                }
            }

            error?.let {
                Text(
                    text = it,
                    color = TerracottaDark,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }

        Surface(color = SurfaceWarm, shadowElevation = 12.dp) {
            Button(
                onClick = {
                    val validation = validate(
                        receiverName, phone, province, city, district, detail,
                    )
                    if (validation != null) {
                        error = validation
                        return@Button
                    }
                    error = null
                    viewModel.saveAddress(
                        id = addressId.takeIf { it > 0 },
                        payload = AddressPayload(
                            receiverName = receiverName.trim(),
                            phone = phone.trim(),
                            province = province.trim(),
                            city = city.trim(),
                            district = district.trim(),
                            detail = detail.trim(),
                            isDefault = isDefault,
                        ),
                        onSuccess = { navController.popBackStack() },
                    )
                },
                enabled = !busy,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Terracotta,
                    contentColor = androidx.compose.ui.graphics.Color.White,
                ),
                contentPadding = PaddingValues(vertical = 13.dp),
                modifier = Modifier
                    .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 12.dp),
            ) {
                Text(if (busy) "保存中…" else "保存地址")
            }
        }
    }
}

@Composable
private fun FormField(
    label: String,
    value: String,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder, color = Faint) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = SurfaceSoft,
            unfocusedContainerColor = SurfaceSoft,
            focusedBorderColor = Terracotta.copy(alpha = 0.6f),
            unfocusedBorderColor = Line,
            focusedLabelColor = TerracottaDark,
            cursorColor = Terracotta,
        ),
    )
}

private fun validate(
    receiverName: String,
    phone: String,
    province: String,
    city: String,
    district: String,
    detail: String,
): String? {
    if (receiverName.isBlank()) return "请输入收货人"
    if (!Regex("^1[3-9]\\d{9}$").matches(phone.trim())) return "请输入正确的手机号"
    if (province.isBlank() || city.isBlank() || district.isBlank()) return "请完整填写省份、城市、区县"
    if (detail.isBlank()) return "请输入详细地址"
    return null
}
