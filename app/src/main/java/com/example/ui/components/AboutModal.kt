package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.about.VersionChangelogEntry
import com.example.ui.theme.*

enum class AboutTab {
    INFO,
    CHANGELOG
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutModal(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(AboutTab.INFO) }
    var checkingUpdates by remember { mutableStateOf(false) }
    var updateCheckResult by remember { mutableStateOf<String?>(null) }

    val currentVersion = remember { AppVersionManager.getCurrentVersion() }
    val allVersions = remember { AppVersionManager.getAllVersions() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = StudioSurface,
        scrimColor = Color.Black.copy(alpha = 0.75f),
        dragHandle = { BottomSheetDefaults.DragHandle(color = StudioBorder) },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: App Logo, Name & Version Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Glow App Logo
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(StudioCyan, StudioViolet)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Logo",
                        tint = StudioDarkBg,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Harmonia Studio X",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = StudioCyan.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(0.5.dp, StudioCyan)
                        ) {
                            Text(
                                text = "v${currentVersion.versionName}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = StudioCyan,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = "新一代全功能专业移动端数字音频工作站 (DAW)",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            // Tab Switcher: [ 关于软件 ] vs [ 版本更新记录 (N) ]
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
            ) {
                SegmentedButton(
                    selected = selectedTab == AboutTab.INFO,
                    onClick = { selectedTab = AboutTab.INFO },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("关于软件", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                SegmentedButton(
                    selected = selectedTab == AboutTab.CHANGELOG,
                    onClick = { selectedTab = AboutTab.CHANGELOG },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Icon(Icons.Default.HistoryEdu, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("版本更新记录 (${allVersions.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Tab Content
            when (selectedTab) {
                AboutTab.INFO -> {
                    AboutInfoContent(
                        currentVersion = currentVersion,
                        checkingUpdates = checkingUpdates,
                        updateCheckResult = updateCheckResult,
                        onCheckUpdate = {
                            checkingUpdates = true
                            updateCheckResult = null
                            // Simulate update check
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                checkingUpdates = false
                                updateCheckResult = "当前已是最新稳定版本 v${currentVersion.versionName}（已包含多平台云端同步与动态琴键支持）"
                            }, 800)
                        }
                    )
                }
                AboutTab.CHANGELOG -> {
                    ChangelogHistoryContent(allVersions = allVersions)
                }
            }
        }
    }
}

@Composable
private fun AboutInfoContent(
    currentVersion: VersionChangelogEntry,
    checkingUpdates: Boolean,
    updateCheckResult: String?,
    onCheckUpdate: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Card 1: Technical Architecture & Specs
        Surface(
            color = StudioSurfaceElevated,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, StudioBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "⚙️ 核心技术与声学引擎架构",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = StudioCyan
                )

                val specs = listOf(
                    "实时音频引擎" to "44.1kHz 16-bit PCM 立体声低延迟多发音数 DSP 引擎",
                    "乐器物理建模" to "支持西式大钢琴/合成器与国风六大民乐 Karplus-Strong 拨弦算法",
                    "虚拟触控琴键" to "动态比例缩放布局引擎（维持 1:4.54 黄金人体工学触控宽高比）",
                    "多端账号同步" to "支持谷歌 (Google)、QQ、微信、X (Twitter) 登录与工程云端备份",
                    "数据本地持久化" to "Android Jetpack Room 数据库与容灾破坏性自动平滑迁移"
                )

                specs.forEach { (label, value) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(label, fontSize = 11.sp, color = TextMuted)
                        Text(value, fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }

        // Card 2: Version Info & Check for Updates
        Surface(
            color = StudioSurfaceElevated,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, StudioBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("当前版本: v${currentVersion.versionName} (Build #${currentVersion.versionCode})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("发布日期: ${currentVersion.releaseDate} • 正式稳定版", fontSize = 10.sp, color = TextMuted)
                    }

                    Button(
                        onClick = onCheckUpdate,
                        enabled = !checkingUpdates,
                        colors = ButtonDefaults.buttonColors(containerColor = StudioCyan),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp).testTag("check_update_button")
                    ) {
                        if (checkingUpdates) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = StudioDarkBg, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, tint = StudioDarkBg, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("检查更新", color = StudioDarkBg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (updateCheckResult != null) {
                    Text(
                        text = updateCheckResult,
                        fontSize = 11.sp,
                        color = StudioEmerald,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Card 3: Copyright & Studio Credits
        Surface(
            color = StudioSurfaceElevated,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, StudioBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "© 2026 Harmonia Audio Labs. All rights reserved.",
                    fontSize = 11.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "秉承低延迟、专业跨平台与无损音乐创作理念，专为音乐人、制作人与声音设计师打造。",
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun ChangelogHistoryContent(
    allVersions: List<VersionChangelogEntry>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 400.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(allVersions) { entry ->
            Surface(
                color = StudioSurfaceElevated,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(
                    1.dp,
                    if (entry.isCurrent) StudioCyan.copy(alpha = 0.8f) else StudioBorder
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "v${entry.versionName}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (entry.isCurrent) StudioCyan else TextPrimary
                            )
                            if (entry.isCurrent) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = StudioCyan,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "最新版本",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StudioDarkBg,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = entry.releaseDate,
                            fontSize = 11.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Text(
                        text = entry.summary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (entry.isCurrent) StudioAmber else TextSecondary
                    )

                    Divider(color = StudioBorder.copy(alpha = 0.5f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 2.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        entry.highlights.forEach { item ->
                            Row(verticalAlignment = Alignment.Top) {
                                Text("• ", color = if (entry.isCurrent) StudioCyan else TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(item, fontSize = 11.sp, color = TextPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}
