package com.juku.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.juku.app.data.model.AccountPublic
import com.juku.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentServerUrl: String,
    currentAccount: AccountPublic?,
    onSaveServerUrl: (String) -> Unit,
    onTestConnection: () -> Unit,
    onLogin: (String, String, (String?) -> Unit) -> Unit,
    onLogout: () -> Unit,
    connectionStatus: String?
) {
    var serverUrlInput by remember(currentServerUrl) { mutableStateOf(currentServerUrl) }
    var autoPrefetch by remember { mutableStateOf(true) }
    var directStream by remember { mutableStateOf(true) }
    var autoMerge by remember { mutableStateOf(true) }
    var danmakuDefault by remember { mutableStateOf(true) }

    var showLoginDialog by remember { mutableStateOf(false) }
    var loginUsername by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }
    var isLoggingIn by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkVoid)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .padding(bottom = 80.dp)
    ) {
        Text(
            text = "个人中心与设置",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // User Profile Card (Supports Login / Logout / Role Tag)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, DarkBorder, RoundedCornerShape(18.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkCard)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (currentAccount != null) CinemaGold.copy(alpha = 0.2f) else DarkElevated)
                        .border(1.5.dp, if (currentAccount != null) CinemaGold else DarkBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (currentAccount != null) Icons.Default.Person else Icons.Default.PersonOutline,
                        contentDescription = "头像",
                        tint = if (currentAccount != null) CinemaGold else TextMuted,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (currentAccount != null) {
                            "${currentAccount.username} (${if (currentAccount.admin) "管理员" else "普通用户"})"
                        } else {
                            "未登录 (访客模式)"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (currentAccount != null) CinemaGold.copy(alpha = 0.2f) else DarkElevated)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (currentAccount != null) {
                                if (currentAccount.admin) "已授权 · 拥有全部站源与下载特权" else "已授权普通账号"
                            } else {
                                "登录后可跨设备同步观看记录与权限"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (currentAccount != null) CinemaGold else TextMuted,
                                fontSize = 10.sp
                            )
                        )
                    }
                }

                if (currentAccount != null) {
                    OutlinedButton(
                        onClick = onLogout,
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                    ) {
                        Text("退出", fontSize = 11.sp)
                    }
                } else {
                    Button(
                        onClick = {
                            loginError = null
                            loginUsername = ""
                            loginPassword = ""
                            showLoginDialog = true
                        },
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CinemaRed)
                    ) {
                        Text("登录账号", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Server Connection Config (Core Highlight Card)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, CinemaRed.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkCard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Dns,
                        contentDescription = "服务器",
                        tint = CinemaRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Go 后端服务接入 (Server Config)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = serverUrlInput,
                    onValueChange = {
                        serverUrlInput = it
                        onSaveServerUrl(it)
                    },
                    label = { Text("服务地址 (如 http://192.168.1.108:8999 或公网 VPS 域名)", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CinemaRed,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = connectionStatus ?: "未检测",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (connectionStatus?.contains("正常") == true || connectionStatus?.contains("运行中") == true)
                                Color(0xFF52C41A) else CinemaRed,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Button(
                        onClick = onTestConnection,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = DarkElevated),
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                    ) {
                        Text("测试连接", color = TextPrimary, fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Playback & Download Settings
        Text(
            text = "播放与网络优化",
            style = MaterialTheme.typography.titleSmall.copy(
                color = TextSecondary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkCard)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                SettingSwitchItem(
                    title = "自动预缓存下一集",
                    desc = "根据网速提前拉取分片，切集0等待",
                    checked = autoPrefetch,
                    onCheckedChange = { autoPrefetch = it }
                )
                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                SettingSwitchItem(
                    title = "客户端直连源站",
                    desc = "优先使用 302 播放，减少 Go 服务端转码与带宽开销",
                    checked = directStream,
                    onCheckedChange = { directStream = it }
                )
                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                SettingSwitchItem(
                    title = "短剧全集下载后智能无损合并",
                    desc = "分集下载完成后自动调用后台 FFmpeg 组装成单集 MP4",
                    checked = autoMerge,
                    onCheckedChange = { autoMerge = it }
                )
                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                SettingSwitchItem(
                    title = "默认开启弹幕",
                    desc = "进入播放器后自动载入实时滚动弹幕",
                    checked = danmakuDefault,
                    onCheckedChange = { danmakuDefault = it }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Storage & Cache
        Text(
            text = "存储与维护",
            style = MaterialTheme.typography.titleSmall.copy(
                color = TextSecondary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkCard)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                SettingClickItem(
                    title = "清除播放器本地分片缓存",
                    trailingText = "已占用 128 MB",
                    onClick = {}
                )
                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                SettingClickItem(
                    title = "关于果果剧库 Android 客户端",
                    trailingText = "v1.0.0 (Stitch Cinema Noir)",
                    onClick = {}
                )
            }
        }
    }

    // Login Dialog
    if (showLoginDialog) {
        AlertDialog(
            onDismissRequest = { if (!isLoggingIn) showLoginDialog = false },
            containerColor = DarkCard,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "登录",
                        tint = CinemaRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "登录果果剧库账号",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "请输入 Go 服务端设置的用户名与密码：",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = loginUsername,
                        onValueChange = { loginUsername = it },
                        label = { Text("用户名 (默认 admin)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CinemaRed,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = loginPassword,
                        onValueChange = { loginPassword = it },
                        label = { Text("密码") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "切换密码可见性",
                                    tint = TextSecondary
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CinemaRed,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    if (!loginError.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = loginError!!,
                            color = CinemaRed,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (loginUsername.isBlank()) {
                            loginError = "用户名不能为空"
                            return@Button
                        }
                        if (loginPassword.isBlank()) {
                            loginError = "密码不能为空"
                            return@Button
                        }
                        isLoggingIn = true
                        loginError = null
                        onLogin(loginUsername, loginPassword) { err ->
                            isLoggingIn = false
                            if (err != null) {
                                loginError = err
                            } else {
                                showLoginDialog = false
                            }
                        }
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = CinemaRed),
                    enabled = !isLoggingIn
                ) {
                    if (isLoggingIn) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("登录", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLoginDialog = false },
                    enabled = !isLoggingIn
                ) {
                    Text("取消", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun SettingSwitchItem(
    title: String,
    desc: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextMuted,
                    fontSize = 11.sp
                )
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = CinemaRed,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = DarkElevated
            )
        )
    }
}

@Composable
private fun SettingClickItem(
    title: String,
    trailingText: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = trailingText,
                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
