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
    onChangePassword: (String, String, (String?) -> Unit) -> Unit,
    onLogout: () -> Unit,
    connectionOk: Boolean?,
    connectionStatus: String?
) {
    var serverUrlInput by remember(currentServerUrl) { mutableStateOf(currentServerUrl) }

    var showLoginDialog by remember { mutableStateOf(false) }
    var loginUsername by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }
    var isLoggingIn by remember { mutableStateOf(false) }

    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordChangeError by remember { mutableStateOf<String?>(null) }
    var isChangingPassword by remember { mutableStateOf(false) }
    val isSavedAddress = serverUrlInput.trim().trimEnd('/') == currentServerUrl.trim().trimEnd('/')

    LaunchedEffect(currentAccount?.username) {
        showChangePasswordDialog = false
        currentPassword = ""
        newPassword = ""
        confirmPassword = ""
        passwordChangeError = null
    }

    fun openChangePasswordDialog() {
        currentPassword = ""
        newPassword = ""
        confirmPassword = ""
        passwordChangeError = null
        showChangePasswordDialog = true
    }

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

        // User Profile Card
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

        if (currentAccount != null) {
            Spacer(modifier = Modifier.height(12.dp))
            if (currentAccount.requirePasswordChange) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CinemaRed, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "请先修改初始密码",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = CinemaRed,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "此账号修改密码后才能使用剧库和播放功能。",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = ::openChangePasswordDialog,
                            colors = ButtonDefaults.buttonColors(containerColor = CinemaRed),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("立即修改密码")
                        }
                    }
                }
            } else {
                OutlinedButton(onClick = ::openChangePasswordDialog) {
                    Text("修改密码")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Server Connection Config
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
                    onValueChange = { serverUrlInput = it },
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

                Button(
                    onClick = { onSaveServerUrl(serverUrlInput) },
                    enabled = serverUrlInput.isNotBlank(),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = CinemaRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("保存并连接")
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = connectionStatus ?: "未检测",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = when (connectionOk) {
                                true -> Color(0xFF52C41A)
                                false -> CinemaRed
                                null -> TextSecondary
                            },
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Button(
                        onClick = onTestConnection,
                        enabled = isSavedAddress,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = DarkElevated),
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                    ) {
                        Text(if (isSavedAddress) "测试连接" else "请先保存地址", color = TextPrimary, fontSize = 11.sp)
                    }
                }
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

    if (showChangePasswordDialog && currentAccount != null) {
        AlertDialog(
            onDismissRequest = { if (!isChangingPassword) showChangePasswordDialog = false },
            containerColor = DarkCard,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    if (currentAccount.requirePasswordChange) "修改初始密码" else "修改账号密码",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "新密码需为 10–128 个字符。修改后其他设备需要重新登录。",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    PasswordEntryField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = "当前密码",
                        enabled = !isChangingPassword
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    PasswordEntryField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = "新密码",
                        enabled = !isChangingPassword
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    PasswordEntryField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = "确认新密码",
                        enabled = !isChangingPassword
                    )
                    passwordChangeError?.let { error ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            error,
                            color = CinemaRed,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        passwordChangeError = when {
                            currentPassword.isBlank() -> "请输入当前密码"
                            newPassword.codePointCount(0, newPassword.length) !in 10..128 -> "新密码需为 10–128 个字符"
                            newPassword == currentPassword -> "新密码不能与当前密码相同"
                            confirmPassword != newPassword -> "两次输入的新密码不一致"
                            else -> null
                        }
                        if (passwordChangeError != null) return@Button
                        isChangingPassword = true
                        onChangePassword(currentPassword, newPassword) { error ->
                            isChangingPassword = false
                            if (error != null) {
                                passwordChangeError = error
                            } else {
                                currentPassword = ""
                                newPassword = ""
                                confirmPassword = ""
                                showChangePasswordDialog = false
                            }
                        }
                    },
                    enabled = !isChangingPassword,
                    colors = ButtonDefaults.buttonColors(containerColor = CinemaRed)
                ) {
                    if (isChangingPassword) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("确认修改")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showChangePasswordDialog = false },
                    enabled = !isChangingPassword
                ) {
                    Text("取消", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun PasswordEntryField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        enabled = enabled,
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }, enabled = enabled) {
                Icon(
                    imageVector = if (visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = if (visible) "隐藏密码" else "显示密码",
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
}
