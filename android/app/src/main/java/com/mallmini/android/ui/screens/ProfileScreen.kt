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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mallmini.android.BuildConfig
import com.mallmini.android.ui.MallViewModel
import com.mallmini.android.ui.Routes
import com.mallmini.android.ui.components.MallTopBar
import com.mallmini.android.ui.theme.Faint
import com.mallmini.android.ui.theme.Ink
import com.mallmini.android.ui.theme.Line
import com.mallmini.android.ui.theme.Muted
import com.mallmini.android.ui.theme.SurfaceSoft
import com.mallmini.android.ui.theme.Success
import com.mallmini.android.ui.theme.SurfaceWarm
import com.mallmini.android.ui.theme.Terracotta
import com.mallmini.android.ui.theme.TerracottaDark
import com.mallmini.android.ui.theme.TerracottaSoft

@Composable
fun ProfileScreen(navController: NavController, viewModel: MallViewModel) {
    val loggedIn by viewModel.loggedIn.collectAsStateWithLifecycle()
    val user by viewModel.user.collectAsStateWithLifecycle()
    val baseUrl by viewModel.baseUrl.collectAsStateWithLifecycle()
    val loginBusy by viewModel.loginBusy.collectAsStateWithLifecycle()
    val loginError by viewModel.loginError.collectAsStateWithLifecycle()

    var openId by rememberSaveable { mutableStateOf(viewModel.savedOpenId) }
    var nickname by rememberSaveable { mutableStateOf(viewModel.savedNickname) }
    var serverDialog by remember { mutableStateOf(false) }
    var serverInput by rememberSaveable { mutableStateOf(baseUrl) }
    var logoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(baseUrl) {
        serverInput = baseUrl
    }

    Column(modifier = Modifier.fillMaxSize()) {
        MallTopBar(
            title = "个人中心",
            subtitle = "Account",
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 18.dp, end = 18.dp, bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (loggedIn) {
                UserHero(
                    nickname = user?.nickname.orEmpty().ifBlank { "已登录用户" },
                    phone = user?.phone.orEmpty(),
                    onLogout = { logoutDialog = true },
                )
                QuickActions(
                    onOrders = { navController.navigate(Routes.ORDERS) },
                    onAddresses = { navController.navigate(Routes.ADDRESSES) },
                )
                MenuCard {
                    MenuRow(
                        icon = Icons.Outlined.Settings,
                        title = "服务器设置",
                        subtitle = baseUrl,
                        onClick = { serverDialog = true },
                    )
                    MenuRow(
                        icon = Icons.Outlined.Logout,
                        title = "退出登录",
                        subtitle = "清除本地登录态",
                        onClick = { logoutDialog = true },
                        danger = true,
                    )
                }
            } else {
                GuestHero()
                Surface(
                    color = SurfaceWarm,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("开发登录", color = Ink, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Android 端当前使用明确标记的开发登录，不是微信登录；生产环境应接入账号体系。",
                            color = Muted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        LoginField(
                            label = "用户标识（openId）",
                            value = openId,
                            placeholder = "例如 android-dev-user",
                            onValueChange = { openId = it },
                        )
                        LoginField(
                            label = "昵称",
                            value = nickname,
                            placeholder = "例如 Android 用户",
                            onValueChange = { nickname = it },
                        )
                        loginError?.let {
                            Text(it, color = TerracottaDark, style = MaterialTheme.typography.bodySmall)
                        }
                        Button(
                            onClick = { viewModel.devLogin(openId, nickname) },
                            enabled = !loginBusy,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Terracotta,
                                contentColor = Color.White,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 12.dp),
                        ) {
                            Text(if (loginBusy) "登录中…" else "开发登录（测试）")
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { serverDialog = true }
                                .padding(vertical = 4.dp),
                        ) {
                            Text("当前服务器", color = Faint, style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.weight(1f))
                            Text(
                                baseUrl,
                                color = Muted,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                            )
                            Icon(
                                Icons.Outlined.ChevronRight,
                                contentDescription = null,
                                tint = Faint,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }

            Surface(
                color = SurfaceWarm,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("关于", color = Ink, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "MallMini Android · v${BuildConfig.VERSION_NAME}",
                        color = Muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "开发环境模拟支付，不会产生真实扣款。",
                        color = Faint,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }

    if (serverDialog) {
        AlertDialog(
            onDismissRequest = { serverDialog = false },
            title = { Text("服务器地址") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "填写运行服务端的 Mac 局域网地址，例如 http://192.168.2.7:8080/api/v1",
                        color = Muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedTextField(
                        value = serverInput,
                        onValueChange = { serverInput = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Terracotta,
                            unfocusedBorderColor = Line,
                            focusedContainerColor = SurfaceSoft,
                            unfocusedContainerColor = SurfaceSoft,
                        ),
                    )
                    Text(
                        "修改服务器地址会清除当前登录状态。",
                        color = Faint,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    serverDialog = false
                    viewModel.updateBaseUrl(serverInput)
                }) { Text("保存", color = Terracotta) }
            },
            dismissButton = {
                TextButton(onClick = { serverDialog = false }) { Text("取消") }
            },
        )
    }

    if (logoutDialog) {
        AlertDialog(
            onDismissRequest = { logoutDialog = false },
            title = { Text("退出登录") },
            text = { Text("退出后购物车、地址与订单需要重新登录查看。") },
            confirmButton = {
                TextButton(onClick = {
                    logoutDialog = false
                    viewModel.logout()
                }) { Text("退出", color = Terracotta) }
            },
            dismissButton = {
                TextButton(onClick = { logoutDialog = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun UserHero(nickname: String, phone: String, onLogout: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    listOf(Color(0xFF241C16), Color(0xFF3A2A1E)),
                ),
                shape = RoundedCornerShape(22.dp),
            )
            .padding(20.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .background(TerracottaSoft, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = nickname.take(1),
                        color = Terracotta,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = nickname,
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = phone.ifBlank { "已登录 · 开发账号" },
                        color = Color.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "欢迎回到优选商城",
                    color = Color.White.copy(alpha = 0.55f),
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onLogout) {
                    Text("退出", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun GuestHero() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    listOf(Color(0xFF241C16), Color(0xFF3A2A1E)),
                ),
                shape = RoundedCornerShape(22.dp),
            )
            .padding(20.dp),
    ) {
        Column {
            Text(
                "优选商城",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "使用开发账号登录，体验完整交易流程",
                color = Color.White.copy(alpha = 0.55f),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun QuickActions(onOrders: () -> Unit, onAddresses: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            color = SurfaceWarm,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onOrders),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.ReceiptLong,
                    contentDescription = null,
                    tint = Terracotta,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text("我的订单", color = Ink, style = MaterialTheme.typography.titleMedium)
            }
        }
        Surface(
            color = SurfaceWarm,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onAddresses),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = Success,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text("收货地址", color = Ink, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}


@Composable
private fun MenuCard(content: @Composable () -> Unit) {
    Surface(
        color = SurfaceWarm,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column { content() }
    }
}

@Composable
private fun MenuRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    danger: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (danger) Terracotta else Muted,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = if (danger) TerracottaDark else Ink,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                subtitle,
                color = Faint,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
            )
        }
        Icon(
            Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = Faint,
            modifier = Modifier.size(17.dp),
        )
    }
}

@Composable
private fun LoginField(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder, color = Faint) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = SurfaceSoft,
            unfocusedContainerColor = SurfaceSoft,
            focusedBorderColor = Terracotta,
            unfocusedBorderColor = Line,
            focusedLabelColor = TerracottaDark,
            cursorColor = Terracotta,
        ),
    )
}
