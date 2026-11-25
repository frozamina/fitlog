package com.fitlog.app.data

import android.content.Context

/** DeepSeek API key 本机存储（App 私有目录 SharedPreferences，不上传、不入备份导出）。 */
object AiSettings {
    private const val PREF = "ai_settings"
    private const val KEY = "deepseek_api_key"

    fun apiKey(ctx: Context): String =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY, "") ?: ""

    fun hasKey(ctx: Context): Boolean = apiKey(ctx).isNotBlank()

    fun saveKey(ctx: Context, key: String) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(KEY, key.trim()).apply()
    }

    fun clearKey(ctx: Context) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().remove(KEY).apply()
    }

    /** 掩码展示：sk-****1234 */
    fun mask(key: String): String =
        if (key.isBlank()) "未配置" else "已配置 · sk-****" + key.takeLast(4)
}
