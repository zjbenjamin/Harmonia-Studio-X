package com.example.auth

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

data class AuthLaunchResult(
    val isLaunched: Boolean,
    val targetType: String, // "APP" or "BROWSER"
    val targetDescription: String,
    val error: String? = null
)

object OAuthDispatcher {

    const val PACKAGE_QQ = "com.tencent.mobileqq"
    const val PACKAGE_WECHAT = "com.tencent.mm"
    const val PACKAGE_TWITTER_X = "com.twitter.android"
    const val PACKAGE_GMS = "com.google.android.gms"

    fun isAppInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    fun getClientStatus(context: Context, provider: AuthProvider): String {
        return when (provider) {
            AuthProvider.GOOGLE -> {
                if (isAppInstalled(context, PACKAGE_GMS)) "已就绪 (Google 服务可用)"
                else "通过系统安全浏览器调起 Google 授权"
            }
            AuthProvider.QQ -> {
                if (isAppInstalled(context, PACKAGE_QQ)) "已安装 QQ 客户端 (可直接调起)"
                else "未安装客户端 (将调起 QQ 互联网页授权)"
            }
            AuthProvider.WECHAT -> {
                if (isAppInstalled(context, PACKAGE_WECHAT)) "已安装 微信 客户端 (可直接调起)"
                else "未安装客户端 (将调起 微信 开放平台网页授权)"
            }
            AuthProvider.TWITTER_X -> {
                if (isAppInstalled(context, PACKAGE_TWITTER_X)) "已安装 𝕏 客户端 (可直接调起)"
                else "未安装客户端 (将调起 𝕏 OAuth 网页授权)"
            }
        }
    }

    fun getOAuthWebUrl(provider: AuthProvider): String {
        return when (provider) {
            AuthProvider.GOOGLE ->
                "https://accounts.google.com/signin"
            AuthProvider.QQ ->
                "https://connect.qq.com/"
            AuthProvider.WECHAT ->
                "https://open.weixin.qq.com/"
            AuthProvider.TWITTER_X ->
                "https://x.com/i/flow/login"
        }
    }

    fun launchProviderAuth(
        context: Context,
        provider: AuthProvider,
        preferBrowser: Boolean = false
    ): AuthLaunchResult {
        val webUrl = getOAuthWebUrl(provider)

        // Try App-specific intent if not forced to browser
        if (!preferBrowser) {
            when (provider) {
                AuthProvider.QQ -> {
                    if (isAppInstalled(context, PACKAGE_QQ)) {
                        try {
                            val intent = context.packageManager.getLaunchIntentForPackage(PACKAGE_QQ)?.apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            } ?: Intent(Intent.ACTION_VIEW, Uri.parse("mqqapi://forward/url?url_type=auth")).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                            return AuthLaunchResult(true, "APP", "已成功调起手机 QQ 客户端授权")
                        } catch (_: Exception) {
                            // Fallback to browser
                        }
                    }
                }
                AuthProvider.WECHAT -> {
                    if (isAppInstalled(context, PACKAGE_WECHAT)) {
                        try {
                            val intent = context.packageManager.getLaunchIntentForPackage(PACKAGE_WECHAT)?.apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            } ?: Intent(Intent.ACTION_VIEW, Uri.parse("weixin://")).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                            return AuthLaunchResult(true, "APP", "已成功调起微信客户端授权")
                        } catch (_: Exception) {
                            // Fallback to browser
                        }
                    }
                }
                AuthProvider.TWITTER_X -> {
                    if (isAppInstalled(context, PACKAGE_TWITTER_X)) {
                        try {
                            val intent = context.packageManager.getLaunchIntentForPackage(PACKAGE_TWITTER_X)?.apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            } ?: Intent(Intent.ACTION_VIEW, Uri.parse("twitter://timeline")).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                            return AuthLaunchResult(true, "APP", "已成功调起 𝕏 客户端授权")
                        } catch (_: Exception) {
                            // Fallback to browser
                        }
                    }
                }
                AuthProvider.GOOGLE -> {
                    // Google can open Google Accounts OAuth URL directly in Chrome/Browser
                }
            }
        }

        // Browser Fallback (works on any Android phone / tablet / emulator)
        return try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
            AuthLaunchResult(
                isLaunched = true,
                targetType = "BROWSER",
                targetDescription = "已成功调起系统安全浏览器进行 ${provider.displayName} 授权"
            )
        } catch (e: Exception) {
            AuthLaunchResult(
                isLaunched = false,
                targetType = "NONE",
                targetDescription = "调起失败",
                error = e.localizedMessage ?: "无法启动浏览器或授权组件"
            )
        }
    }
}
