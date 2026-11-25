package com.fitlog.app.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

/** 一段聊天会话（配色问答历史） */
@Serializable
data class ChatSession(
    val id: Long,
    val title: String,
    val updatedAt: Long,
    val messages: List<ChatMessage> = emptyList()
)

/**
 * 聊天历史持久化：filesDir/chat_history.json（JSON 数组，按更新时间倒序维护）。
 * 保存时过滤错误消息与空内容；标题取首条用户消息前 15 字。
 */
object ChatHistory {
    private const val FILE = "chat_history.json"
    private val json = Json { ignoreUnknownKeys = true }

    fun load(ctx: Context): List<ChatSession> = runCatching {
        val f = File(ctx.filesDir, FILE)
        if (!f.exists()) return emptyList()
        json.decodeFromString(ListSerializer(ChatSession.serializer()), f.readText())
    }.getOrDefault(emptyList()).sortedByDescending { it.updatedAt }

    private fun save(ctx: Context, sessions: List<ChatSession>) {
        runCatching {
            File(ctx.filesDir, FILE)
                .writeText(json.encodeToString(ListSerializer(ChatSession.serializer()), sessions.sortedByDescending { it.updatedAt }))
        }
    }

    /** 把当前对话 upsert 进历史；无有效内容时不动历史。返回最新列表（已排序）。 */
    fun upsert(ctx: Context, sessionId: Long, messages: List<ChatMessage>): List<ChatSession> {
        val cleaned = messages.filter { !it.isError && it.content.isNotBlank() }
        if (cleaned.none { it.role == "assistant" }) return load(ctx)
        val title = cleaned.firstOrNull { it.role == "user" }?.content?.take(15)?.trim() ?: "配色问答"
        val all = load(ctx).toMutableList()
        all.removeAll { it.id == sessionId }
        all.add(0, ChatSession(sessionId, title, System.currentTimeMillis(), cleaned))
        save(ctx, all)
        return all
    }

    fun delete(ctx: Context, sessionId: Long): List<ChatSession> {
        val all = load(ctx).toMutableList()
        all.removeAll { it.id == sessionId }
        save(ctx, all)
        return all
    }
}
