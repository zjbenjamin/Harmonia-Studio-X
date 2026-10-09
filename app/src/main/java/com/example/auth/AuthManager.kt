package com.example.auth

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

enum class AuthProvider(
    val displayName: String,
    val badgeText: String,
    val brandColorHex: Long,
    val buttonBgColorHex: Long,
    val description: String
) {
    GOOGLE(
        displayName = "Google",
        badgeText = "G",
        brandColorHex = 0xFF4285F4,
        buttonBgColorHex = 0xFF4285F4,
        description = "谷歌全球云端账号同步"
    ),
    QQ(
        displayName = "QQ",
        badgeText = "QQ",
        brandColorHex = 0xFF12B7F5,
        buttonBgColorHex = 0xFF12B7F5,
        description = "腾讯 QQ 快捷互联同步"
    ),
    WECHAT(
        displayName = "微信",
        badgeText = "微信",
        brandColorHex = 0xFF07C160,
        buttonBgColorHex = 0xFF07C160,
        description = "微信多端工程实时同步"
    ),
    TWITTER_X(
        displayName = "X (Twitter)",
        badgeText = "𝕏",
        brandColorHex = 0xFFFFFFFF,
        buttonBgColorHex = 0xFF262626,
        description = "X 社交账号云端存档"
    )
}

data class UserProfile(
    val id: String = "",
    val displayName: String = "未登录制作人",
    val accountTag: String = "guest@harmonia.studio",
    val provider: AuthProvider = AuthProvider.GOOGLE,
    val avatarColorHex: Long = 0xFF00E5FF,
    val isLoggedIn: Boolean = false,
    val cloudProjectsCount: Int = 0,
    val lastSyncTimestamp: Long = 0L,
    val storageUsedMb: Float = 14.5f,
    val storageLimitMb: Float = 10240f // 10 GB
)

class AuthManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("harmonia_auth_prefs", Context.MODE_PRIVATE)

    private val _userProfile = MutableStateFlow(loadSavedProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private fun loadSavedProfile(): UserProfile {
        val isLoggedIn = prefs.getBoolean("is_logged_in", false)
        if (!isLoggedIn) {
            return UserProfile(
                id = "guest_${UUID.randomUUID().toString().take(6)}",
                displayName = "访客制作人 (未登录)",
                accountTag = "点击下方快速登录同步",
                isLoggedIn = false
            )
        }
        val providerName = prefs.getString("provider", AuthProvider.GOOGLE.name) ?: AuthProvider.GOOGLE.name
        val provider = try { AuthProvider.valueOf(providerName) } catch (_: Exception) { AuthProvider.GOOGLE }
        return UserProfile(
            id = prefs.getString("user_id", "user_1001") ?: "user_1001",
            displayName = prefs.getString("display_name", "音乐制作人") ?: "音乐制作人",
            accountTag = prefs.getString("account_tag", "producer@harmonia.studio") ?: "producer@harmonia.studio",
            provider = provider,
            avatarColorHex = prefs.getLong("avatar_color", provider.brandColorHex),
            isLoggedIn = true,
            cloudProjectsCount = prefs.getInt("cloud_projects", 4),
            lastSyncTimestamp = prefs.getLong("last_sync_time", System.currentTimeMillis()),
            storageUsedMb = prefs.getFloat("storage_used", 38.6f)
        )
    }

    fun login(provider: AuthProvider, customName: String? = null, customTag: String? = null): UserProfile {
        val generatedName = if (!customName.isNullOrBlank()) {
            customName.trim()
        } else {
            when (provider) {
                AuthProvider.GOOGLE -> "Google Studio Producer"
                AuthProvider.QQ -> "QQ 音乐人"
                AuthProvider.WECHAT -> "微信制作达人"
                AuthProvider.TWITTER_X -> "X Audio Creator"
            }
        }

        val tag = if (!customTag.isNullOrBlank()) {
            customTag.trim()
        } else {
            when (provider) {
                AuthProvider.GOOGLE -> "producer@gmail.com"
                AuthProvider.QQ -> "QQ: 87293188"
                AuthProvider.WECHAT -> "wx_harmonia_producer"
                AuthProvider.TWITTER_X -> "@harmonia_producer"
            }
        }

        val newProfile = UserProfile(
            id = "uid_${provider.name.lowercase()}_${(1000..9999).random()}",
            displayName = generatedName,
            accountTag = tag,
            provider = provider,
            avatarColorHex = provider.brandColorHex,
            isLoggedIn = true,
            cloudProjectsCount = (3..8).random(),
            lastSyncTimestamp = System.currentTimeMillis(),
            storageUsedMb = (25..80).random().toFloat()
        )

        saveProfile(newProfile)
        _userProfile.update { newProfile }
        return newProfile
    }

    fun handleAuthCallback(provider: AuthProvider, authCode: String?, customName: String? = null, customTag: String? = null): UserProfile {
        val tag = if (!customTag.isNullOrBlank()) {
            customTag
        } else if (!authCode.isNullOrBlank()) {
            when (provider) {
                AuthProvider.GOOGLE -> "google_user_${authCode.take(6)}@gmail.com"
                AuthProvider.QQ -> "QQ: ${authCode.take(8)}"
                AuthProvider.WECHAT -> "wx_${authCode.take(8)}"
                AuthProvider.TWITTER_X -> "@x_user_${authCode.take(6)}"
            }
        } else {
            null
        }
        return login(provider, customName, tag)
    }

    fun logout() {
        prefs.edit().clear().apply()
        val guest = UserProfile(
            id = "guest_${UUID.randomUUID().toString().take(6)}",
            displayName = "访客制作人 (未登录)",
            accountTag = "点击下方快速登录同步",
            isLoggedIn = false
        )
        _userProfile.update { guest }
    }

    fun recordSyncCompleted(projectsCount: Int) {
        val now = System.currentTimeMillis()
        prefs.edit()
            .putLong("last_sync_time", now)
            .putInt("cloud_projects", projectsCount)
            .apply()

        _userProfile.update { current ->
            current.copy(
                lastSyncTimestamp = now,
                cloudProjectsCount = projectsCount
            )
        }
    }

    private fun saveProfile(profile: UserProfile) {
        prefs.edit()
            .putBoolean("is_logged_in", profile.isLoggedIn)
            .putString("user_id", profile.id)
            .putString("display_name", profile.displayName)
            .putString("account_tag", profile.accountTag)
            .putString("provider", profile.provider.name)
            .putLong("avatar_color", profile.avatarColorHex)
            .putInt("cloud_projects", profile.cloudProjectsCount)
            .putLong("last_sync_time", profile.lastSyncTimestamp)
            .putFloat("storage_used", profile.storageUsedMb)
            .apply()
    }
}
