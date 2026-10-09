package com.example.ui.views

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.about.AppVersionManager
import com.example.auth.AuthProvider
import com.example.auth.UserProfile
import com.example.ui.components.OAuthDispatcherDialog
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.StudioI18n
import com.example.ui.theme.*
import com.example.viewmodel.StudioUiState
import com.example.viewmodel.StudioViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CollaborationView(
    state: StudioUiState,
    viewModel: StudioViewModel,
    onOpenAudioExport: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = StudioI18n.getStrings(state.language)
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }
    var showExportedJsonDialog by remember { mutableStateOf<String?>(null) }
    var authDispatcherProvider by remember { mutableStateOf<AuthProvider?>(null) }

    val userProfile = state.userProfile

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioDarkBg)
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // =========================================================================
        // Section 1: Multi-Platform Cloud Account & Sync Center (Google/QQ/WeChat/X)
        // =========================================================================
        Surface(
            color = StudioSurface,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, StudioBorder),
            modifier = Modifier.fillMaxWidth().testTag("auth_cloud_sync_card")
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Section Title & Cloud Connection Indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(StudioViolet.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = StudioViolet, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "多平台制作人云端同步中心",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (userProfile.isLoggedIn) "已激活多端工程自动备份与同步" else "支持谷歌、QQ、微信、X 快速登录",
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }
                    }

                    Surface(
                        color = if (userProfile.isLoggedIn) StudioEmerald.copy(alpha = 0.15f) else StudioSurfaceElevated,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(
                            0.5.dp,
                            if (userProfile.isLoggedIn) StudioEmerald else StudioBorder
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (userProfile.isLoggedIn) StudioEmerald else TextMuted)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (userProfile.isLoggedIn) "云端已连通" else "本地离线",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (userProfile.isLoggedIn) StudioEmerald else TextMuted
                            )
                        }
                    }
                }

                if (userProfile.isLoggedIn) {
                    // --- Logged-In User Profile Card ---
                    Surface(
                        color = StudioSurfaceElevated,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(userProfile.provider.brandColorHex).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Avatar circle with brand badge
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color(userProfile.provider.brandColorHex)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = userProfile.provider.badgeText,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (userProfile.provider == AuthProvider.TWITTER_X) Color.Black else Color.White,
                                        fontSize = 14.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = userProfile.displayName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = TextPrimary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = Color(userProfile.provider.brandColorHex).copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = userProfile.provider.displayName,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(userProfile.provider.brandColorHex),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = userProfile.accountTag,
                                        fontSize = 10.sp,
                                        color = TextSecondary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            // Storage usage & sync metadata
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val lastSyncStr = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(userProfile.lastSyncTimestamp))
                                    Text("上次同步时间: $lastSyncStr", fontSize = 10.sp, color = TextMuted)
                                    Text("云空间: ${String.format("%.1f", userProfile.storageUsedMb)} MB / 10 GB", fontSize = 10.sp, color = StudioCyan, fontWeight = FontWeight.SemiBold)
                                }
                                LinearProgressIndicator(
                                    progress = { (userProfile.storageUsedMb / 10240f).coerceIn(0.01f, 1f) },
                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                    color = StudioCyan,
                                    trackColor = StudioDarkBg
                                )
                            }

                            // Action buttons: Sync Now, Switch Provider, Logout
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.syncCloudData() },
                                    colors = ButtonDefaults.buttonColors(containerColor = StudioCyan),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1f).testTag("sync_cloud_button")
                                ) {
                                    Icon(Icons.Default.Sync, contentDescription = null, tint = StudioDarkBg, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("立即同步", color = StudioDarkBg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { viewModel.logoutAccount() },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, StudioBorder),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Logout, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("退出登录", color = TextSecondary, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                } else {
                    // --- Not Logged-In: 4 Branded One-Tap Login Buttons ---
                    Text(
                        text = "选择以下平台即可一键登录，实现作品多端即时同步与灾备：",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // 1. Google Login
                        Surface(
                            color = Color(0xFF4285F4),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    authDispatcherProvider = AuthProvider.GOOGLE
                                }
                                .testTag("login_google_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = Color.White,
                                    shape = CircleShape,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("G", fontWeight = FontWeight.ExtraBold, color = Color(0xFF4285F4), fontSize = 13.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("使用 Google 谷歌账号登录", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("全球多端工程实时同步与安全备份 (点击调起授权)", color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp)
                                }
                                Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
                            }
                        }

                        // 2. QQ Login
                        Surface(
                            color = Color(0xFF12B7F5),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    authDispatcherProvider = AuthProvider.QQ
                                }
                                .testTag("login_qq_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = Color.White,
                                    shape = CircleShape,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("QQ", fontWeight = FontWeight.ExtraBold, color = Color(0xFF12B7F5), fontSize = 10.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("使用 腾讯 QQ 快捷互联登录", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("快速同步音乐工程与乐段预设 (点击调起授权)", color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp)
                                }
                                Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
                            }
                        }

                        // 3. WeChat Login
                        Surface(
                            color = Color(0xFF07C160),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    authDispatcherProvider = AuthProvider.WECHAT
                                }
                                .testTag("login_wechat_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = Color.White,
                                    shape = CircleShape,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("微", fontWeight = FontWeight.ExtraBold, color = Color(0xFF07C160), fontSize = 12.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("使用 微信 (WeChat) 账号登录", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("多设备无缝流转与移动端编曲同步 (点击调起授权)", color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp)
                                }
                                Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
                            }
                        }

                        // 4. X (Twitter) Login
                        Surface(
                            color = Color(0xFF262626),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, StudioBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    authDispatcherProvider = AuthProvider.TWITTER_X
                                }
                                .testTag("login_twitter_x_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = Color.Black,
                                    shape = CircleShape,
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("𝕏", fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 13.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("使用 X (Twitter) 社交账号登录", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("云端安全存取与国际化音乐创作者同步 (点击调起授权)", color = TextMuted, fontSize = 10.sp)
                                }
                                Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = TextMuted, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // Section 2: About Software & Dynamic Version Changelog Card
        // =========================================================================
        Surface(
            color = StudioSurface,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, StudioCyan.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth().testTag("about_and_changelog_card")
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(StudioCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "关于 Harmonia Studio X",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = StudioCyan.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "v${AppVersionManager.CURRENT_VERSION_NAME}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StudioCyan,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "永久版本更新日志体系 • 每次版本更新均自动持久化",
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }
                    }

                    Button(
                        onClick = onOpenAbout,
                        colors = ButtonDefaults.buttonColors(containerColor = StudioCyan),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp).testTag("open_about_modal_button")
                    ) {
                        Text("查看更新", color = StudioDarkBg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Recent Update Highlights Preview
                val currentVersion = remember { AppVersionManager.getCurrentVersion() }
                Surface(
                    color = StudioSurfaceElevated,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "最新版本特性 (v${currentVersion.versionName})：",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = StudioAmber
                        )
                        currentVersion.highlights.take(3).forEach { hl ->
                            Row(verticalAlignment = Alignment.Top) {
                                Text("• ", color = StudioCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(hl, fontSize = 10.sp, color = TextPrimary)
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // Section 3: Cross-Platform File Interchange & Mixdown (.MID / .WAV / JSON)
        // =========================================================================
        Surface(
            color = StudioSurface,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, StudioBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = strings.crossPlatformInterchange,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = StudioAmber,
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Export MIDI File (.mid)
                    Button(
                        onClick = { viewModel.exportMidiFile(context) },
                        modifier = Modifier.weight(1f).testTag("export_midi_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = StudioSurfaceElevated),
                        border = BorderStroke(1.dp, StudioBorder),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AudioFile, contentDescription = null, tint = StudioAmber, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(strings.exportMidiFile, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("DAW 多轨通用 MIDI", fontSize = 9.sp, color = TextMuted)
                        }
                    }

                    // Render Mixdown WAV Audio
                    Button(
                        onClick = onOpenAudioExport,
                        modifier = Modifier.weight(1f).testTag("render_audio_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = StudioSurfaceElevated),
                        border = BorderStroke(1.dp, StudioBorder),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.GraphicEq, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(strings.renderAudioFile, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("44.1kHz 16-bit 母带", fontSize = 9.sp, color = TextMuted)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Export Project JSON Bundle
                    OutlinedButton(
                        onClick = {
                            val json = viewModel.exportProjectJson()
                            showExportedJsonDialog = json
                        },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, StudioViolet.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = StudioViolet, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.exportProjectJson, fontSize = 11.sp, color = StudioViolet)
                    }

                    // Import Project JSON Bundle
                    OutlinedButton(
                        onClick = { showImportDialog = true },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, StudioEmerald.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, tint = StudioEmerald, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.importProjectJson, fontSize = 11.sp, color = StudioEmerald)
                    }
                }
            }
        }

        // =========================================================================
        // Section 4: Project Share Code & Session
        // =========================================================================
        Surface(
            color = StudioSurface,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, StudioBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = strings.cloudWorkspaceTitle,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "${strings.statusLabel}: ${state.currentProject?.cloudSyncStatus ?: "Synced"} (Rev #${state.currentProject?.syncRevision ?: 1})",
                            color = StudioEmerald,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Button(
                        onClick = { viewModel.syncProjectNow() },
                        colors = ButtonDefaults.buttonColors(containerColor = StudioCyan),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("sync_now_button")
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, tint = StudioDarkBg, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.syncNow, color = StudioDarkBg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                // Share Session Code
                Surface(
                    color = StudioSurfaceElevated,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, StudioBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(strings.projectShareCode, fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                            Text(
                                text = state.currentProject?.shareCode ?: "HRMN-9042",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = StudioCyan
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Share Code", state.currentProject?.shareCode ?: "HRMN-9042")
                                clipboard.setPrimaryClip(clip)
                                viewModel.showToast("已将工程共享码复制至剪贴板")
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            border = BorderStroke(1.dp, StudioCyan)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.copyCode, color = StudioCyan, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // =========================================================================
        // Section 5: Language Settings / 语言设置
        // =========================================================================
        Surface(
            color = StudioSurface,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, StudioBorder),
            modifier = Modifier.fillMaxWidth().testTag("language_settings_card")
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Translate, contentDescription = null, tint = StudioViolet, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.languageSetting,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        AppLanguage.SIMPLIFIED_CHINESE to "简体中文 (Chinese)",
                        AppLanguage.ENGLISH to "English"
                    ).forEach { (lang, label) ->
                        val isSelected = state.language == lang
                        Surface(
                            color = if (isSelected) StudioViolet.copy(alpha = 0.2f) else StudioSurfaceElevated,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) StudioViolet else StudioBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.setLanguage(lang) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = StudioViolet, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) StudioViolet else TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // Section 6: Version Revision History Timeline
        // =========================================================================
        Surface(
            color = StudioSurface,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, StudioBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = strings.revisionHistory,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = StudioCoral,
                    letterSpacing = 1.sp
                )

                state.revisions.forEach { rev ->
                    val dateFormatted = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(rev.timestamp))
                    Surface(
                        color = StudioSurfaceElevated,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(StudioViolet.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("#${rev.revisionNumber}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StudioViolet)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(rev.message, fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                                Text("${rev.author} • $dateFormatted", fontSize = 10.sp, color = TextMuted)
                            }
                        }
                    }
                }
            }
        }
    }

    // Exported JSON Dialog
    if (showExportedJsonDialog != null) {
        val json = showExportedJsonDialog!!
        AlertDialog(
            onDismissRequest = { showExportedJsonDialog = null },
            title = { Text("工程 JSON 互导数据包", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("复制此工程数据包，可直接在其他设备导入恢复多轨工程：", fontSize = 12.sp, color = TextSecondary)
                    Surface(
                        color = StudioDarkBg,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp)
                    ) {
                        Text(
                            text = json,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = StudioCyan,
                            modifier = Modifier
                                .padding(8.dp)
                                .verticalScroll(rememberScrollState())
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Harmonia Project JSON", json))
                        viewModel.showToast("已将工程 JSON 复制至剪贴板")
                        showExportedJsonDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioCyan)
                ) {
                    Text("复制到剪贴板", color = StudioDarkBg)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportedJsonDialog = null }) {
                    Text("关闭", color = TextSecondary)
                }
            },
            containerColor = StudioSurface
        )
    }

    // Import JSON Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("导入外部工程 JSON", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("请在下方粘贴 Harmonia Studio 格式的工程 JSON：", fontSize = 12.sp, color = TextSecondary)
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        placeholder = { Text("{\"app\": \"Harmonia Studio\"...}", fontSize = 11.sp, color = TextMuted) },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TextPrimary)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importJsonText.isNotBlank()) {
                            viewModel.importProjectBundle(importJsonText)
                            showImportDialog = false
                            importJsonText = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioEmerald)
                ) {
                    Text("导入并打开", color = StudioDarkBg)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("取消", color = TextSecondary)
                }
            },
            containerColor = StudioSurface
        )
    }

    // Interactive OAuth Dispatch & Account Confirmation Modal
    if (authDispatcherProvider != null) {
        OAuthDispatcherDialog(
            provider = authDispatcherProvider!!,
            viewModel = viewModel,
            onDismiss = { authDispatcherProvider = null }
        )
    }
}
