package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.auth.AuthProvider
import com.example.auth.OAuthDispatcher
import com.example.ui.theme.*
import com.example.viewmodel.StudioViewModel

@Composable
fun OAuthDispatcherDialog(
    provider: AuthProvider,
    viewModel: StudioViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clientStatus = remember(provider) { OAuthDispatcher.getClientStatus(context, provider) }

    var accountInput by remember(provider) {
        mutableStateOf(
            when (provider) {
                AuthProvider.GOOGLE -> "producer.harmonia@gmail.com"
                AuthProvider.QQ -> "87293188"
                AuthProvider.WECHAT -> "wx_producer_harmonia"
                AuthProvider.TWITTER_X -> "@harmonia_creator"
            }
        )
    }
    var nicknameInput by remember(provider) {
        mutableStateOf(
            when (provider) {
                AuthProvider.GOOGLE -> "Google Studio Producer"
                AuthProvider.QQ -> "QQ 音乐制作人"
                AuthProvider.WECHAT -> "微信制作达人"
                AuthProvider.TWITTER_X -> "X Audio Creator"
            }
        )
    }

    var hasDispatchedExternal by remember { mutableStateOf(false) }
    var dispatchStatusText by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = StudioSurface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(provider.brandColorHex).copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header: Provider Icon & Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(provider.brandColorHex)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = provider.badgeText,
                            color = if (provider.badgeText == "𝕏") Color.Black else Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${provider.displayName} 账号登录授权",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = StudioEmerald.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "安全互联",
                                    fontSize = 9.sp,
                                    color = StudioEmerald,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = provider.description,
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                // Scope Permissions List
                Surface(
                    color = StudioSurfaceElevated,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(0.5.dp, StudioBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "授权 Harmonia Studio 获取以下权限：",
                            fontSize = 10.sp,
                            color = TextMuted,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = StudioEmerald, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("多轨工程自动云端灾备与乐段同步", fontSize = 10.sp, color = TextPrimary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = StudioEmerald, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("分配 10 GB 独享音乐工程云存储空间", fontSize = 10.sp, color = TextPrimary)
                        }
                    }
                }

                // Producer account info inputs
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "制作人身份认证信息：",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = StudioCyan
                    )

                    OutlinedTextField(
                        value = nicknameInput,
                        onValueChange = { nicknameInput = it },
                        label = { Text("制作人昵称 / 签名", fontSize = 10.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StudioCyan,
                            unfocusedBorderColor = StudioBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = accountInput,
                        onValueChange = { accountInput = it },
                        label = { Text("平台账号 (QQ号 / 微信 / Google邮箱 / X Handle)", fontSize = 10.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StudioCyan,
                            unfocusedBorderColor = StudioBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // If user dispatched to external app/browser
                AnimatedVisibility(visible = hasDispatchedExternal) {
                    Surface(
                        color = StudioCyan.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(0.5.dp, StudioCyan)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = dispatchStatusText ?: "已发出外部调起请求，请在外部完成或返回后点击下方确认登录。",
                                fontSize = 10.sp,
                                color = TextPrimary
                            )
                        }
                    }
                }

                // Primary One-Tap Login Button (Works 100% reliably in all environments)
                Button(
                    onClick = {
                        val finalTag = when (provider) {
                            AuthProvider.GOOGLE -> if (accountInput.contains("@")) accountInput else "$accountInput@gmail.com"
                            AuthProvider.QQ -> if (accountInput.startsWith("QQ")) accountInput else "QQ: $accountInput"
                            AuthProvider.WECHAT -> if (accountInput.startsWith("wx_")) accountInput else "wx_$accountInput"
                            AuthProvider.TWITTER_X -> if (accountInput.startsWith("@")) accountInput else "@$accountInput"
                        }
                        viewModel.loginWithProvider(
                            provider = provider,
                            customName = nicknameInput,
                            customTag = finalTag
                        )
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(provider.brandColorHex),
                        contentColor = if (provider.brandColorHex == 0xFFFFFFFFL) Color.Black else Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("confirm_oauth_login_button")
                ) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "一键授权登录并开启云同步",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // External Launch Fallback Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val res = viewModel.launchProviderAuth(context, provider, preferBrowser = false)
                            hasDispatchedExternal = true
                            dispatchStatusText = res.targetDescription
                        },
                        border = BorderStroke(1.dp, StudioBorder),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("launch_native_app_button")
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, tint = TextMuted, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("调起客户端", fontSize = 11.sp, color = TextSecondary)
                    }

                    OutlinedButton(
                        onClick = {
                            val res = viewModel.launchProviderAuth(context, provider, preferBrowser = true)
                            hasDispatchedExternal = true
                            dispatchStatusText = res.targetDescription
                        },
                        border = BorderStroke(1.dp, StudioBorder),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("launch_browser_oauth_button")
                    ) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("调起网页授权", fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }
        }
    }
}
