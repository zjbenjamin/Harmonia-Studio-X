package com.example.about

import com.example.BuildConfig

data class VersionChangelogEntry(
    val versionName: String,
    val versionCode: Int,
    val releaseDate: String,
    val isCurrent: Boolean = false,
    val summary: String,
    val highlights: List<String>
)

object AppVersionManager {

    val CURRENT_VERSION_NAME: String
        get() = try {
            val name = BuildConfig.VERSION_NAME
            if (!name.isNullOrBlank() && name != "1.0") name else "1.0.3"
        } catch (_: Throwable) {
            "1.0.3"
        }

    val CURRENT_VERSION_CODE: Int
        get() = try {
            val code = BuildConfig.VERSION_CODE
            if (code > 1) code else 103
        } catch (_: Throwable) {
            103
        }

    const val RELEASE_CHANNEL = "Official Public Release"
    const val BUILD_DATE = "2026-10-09"

    // Persistent changelog history containing all versions and future version update records
    private val changelogHistory = mutableListOf(
        VersionChangelogEntry(
            versionName = "1.0.3",
            versionCode = 103,
            releaseDate = "2026-10-09",
            isCurrent = true,
            summary = "多平台云端同步、全新关于页面与完整更新历史系统上线",
            highlights = listOf(
                "新增支持使用谷歌 (Google)、QQ、微信、X (Twitter) 多平台登录同步数据",
                "新增专属「关于 Harmonia Studio X」页面，支持随时查看技术架构与版权协议",
                "建立永久版本更新日志体系，每次版本更新均自动持久化记录更新内容",
                "实现虚拟琴键动态比例自适应缩放（Dynamic Scaling），保证横竖屏黄金手感",
                "彻底修复顶部控制栏播放/录音/音量滑块按钮错位与步进音序器溢出问题",
                "全量集成麦克风实时人声录音与 MIDI 虚拟键盘双模式音轨录制",
                "重构音频合成引擎发声生命周期与音符时值自动释放，杜绝爆音与无限延音"
            )
        ),
        VersionChangelogEntry(
            versionName = "1.0.2",
            versionCode = 102,
            releaseDate = "2026-10-05",
            isCurrent = false,
            summary = "专业多轨 MIDI 导出系统与步进鼓机体验优化",
            highlights = listOf(
                "重构鼓机步进音序器界面，默认显示全套 808 与 House 核心打击乐器组",
                "支持一键渲染并导出 16-bit 44.1kHz Stereo PCM 无损母带 WAV 文件",
                "集成 Android 系统原生分享菜单，支持一键将工程 MIDI 发送至其他 DAW 与云盘",
                "为 Room 数据库加入破坏性迁移容灾保护机制，杜绝更新安装闪退"
            )
        ),
        VersionChangelogEntry(
            versionName = "1.0.1",
            versionCode = 101,
            releaseDate = "2026-10-01",
            isCurrent = false,
            summary = "国风民乐民族乐器合成系统与智能和弦生成器发布",
            highlights = listOf(
                "新增 6 大中华传统民族乐器独家物理建模合成器（古筝、琵琶、二胡、竹笛、唢呐、编钟）",
                "上线 AI 智能和弦进行生成器，支持流行、古风仙侠、赛博朋克、爵士和弦自动生成",
                "支持一键将推荐和弦套路直接灌入编曲时间轴作为全新音轨",
                "引入中英双语国际化无缝即时切换"
            )
        ),
        VersionChangelogEntry(
            versionName = "1.0.0",
            versionCode = 100,
            releaseDate = "2026-09-20",
            isCurrent = false,
            summary = "Harmonia Studio X 初始移动端工作站架构发布",
            highlights = listOf(
                "核心多轨道编曲时间轴（Arranger View）与实时走带控制系统",
                "直观触控钢琴卷帘编辑器（Piano Roll），支持音符自由点按与视口缩放",
                "内置多发音数低延迟实时声学合成引擎（AudioEngine）",
                "支持基于 Room 的本地多工程持久化存储与会话房间代码分享"
            )
        )
    )

    fun getAllVersions(): List<VersionChangelogEntry> {
        val currentVName = CURRENT_VERSION_NAME
        val currentVCode = CURRENT_VERSION_CODE
        // Synchronize top entry with build config if newer
        val first = changelogHistory.firstOrNull()
        if (first != null && (first.versionName != currentVName || first.versionCode != currentVCode)) {
            val updatedFirst = first.copy(
                versionName = currentVName,
                versionCode = currentVCode,
                isCurrent = true
            )
            changelogHistory[0] = updatedFirst
        }
        return changelogHistory.toList()
    }

    fun getCurrentVersion(): VersionChangelogEntry {
        return getAllVersions().first { it.isCurrent }
    }

    /**
     * Records a new version update dynamically to ensure future updates are automatically tracked.
     */
    fun recordNewVersion(
        versionName: String,
        versionCode: Int,
        releaseDate: String,
        summary: String,
        highlights: List<String>
    ) {
        val entry = VersionChangelogEntry(
            versionName = versionName,
            versionCode = versionCode,
            releaseDate = releaseDate,
            isCurrent = true,
            summary = summary,
            highlights = highlights
        )
        // Mark old versions as not current
        for (i in 0 until changelogHistory.size) {
            changelogHistory[i] = changelogHistory[i].copy(isCurrent = false)
        }
        changelogHistory.add(0, entry)
    }
}
