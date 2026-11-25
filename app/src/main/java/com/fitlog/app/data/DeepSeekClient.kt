package com.fitlog.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * DeepSeek API 客户端（HttpURLConnection，无第三方依赖）。
 * - chat()：非流式，用于色板生成（强制 JSON 输出）
 * - chatStream()：SSE 流式，用于配色问答（onDelta 逐段回调，在 IO 线程执行）
 * 协程取消时主动断开连接；错误统一抛 DeepSeekException（中文文案，可直接展示）。
 */
object DeepSeekClient {
    private const val API = "https://api.deepseek.com/chat/completions"
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Serializable
    data class Message(val role: String, val content: String)

    @Serializable
    private data class Req(
        val model: String = "deepseek-chat",
        val messages: List<Message>,
        val temperature: Double = 0.8,
        @SerialName("max_tokens") val maxTokens: Int = 1024,
        val stream: Boolean = false,
        @SerialName("response_format") val fmt: Fmt? = null
    )

    @Serializable
    private data class Fmt(val type: String = "json_object")

    @Serializable
    private data class Resp(val choices: List<Choice> = emptyList())

    @Serializable
    private data class Choice(val message: Message? = null)

    @Serializable
    private data class Chunk(val choices: List<ChunkChoice> = emptyList())

    @Serializable
    private data class ChunkChoice(val delta: Delta? = null)

    @Serializable
    private data class Delta(val content: String? = null)

    /** 带中文文案的 AI 异常，VM 层可直接透出给 UI。 */
    class DeepSeekException(message: String) : IOException(message)

    /** 非流式对话（jsonMode=true 时强制 JSON 输出）。 */
    suspend fun chat(
        apiKey: String,
        messages: List<Message>,
        jsonMode: Boolean = false,
        maxTokens: Int = 1024,
        temperature: Double = 0.8
    ): String = withContext(Dispatchers.IO) {
        val conn = connect(apiKey, Req(messages = messages, maxTokens = maxTokens, temperature = temperature, fmt = if (jsonMode) Fmt() else null))
        try {
            val body = conn.inputStream.bufferedReader().readText()
            val resp = runCatching { json.decodeFromString(Resp.serializer(), body) }
                .getOrElse { throw DeepSeekException("AI 返回格式异常，请重试") }
            resp.choices.firstOrNull()?.message?.content?.takeIf { it.isNotBlank() }
                ?: throw DeepSeekException("AI 未返回内容，请重试")
        } catch (e: DeepSeekException) {
            throw e
        } catch (e: IOException) {
            throw DeepSeekException("网络连接失败，请检查网络后重试")
        } finally {
            conn.disconnect()
        }
    }

    /** SSE 流式对话：每收到一段增量文本回调 onDelta（IO 线程），返回完整回复。 */
    suspend fun chatStream(
        apiKey: String,
        messages: List<Message>,
        maxTokens: Int = 1024,
        temperature: Double = 0.8,
        onDelta: suspend (String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        val conn = connect(apiKey, Req(messages = messages, stream = true, maxTokens = maxTokens, temperature = temperature))
        val full = StringBuilder()
        try {
            val reader = conn.inputStream.bufferedReader()
            while (true) {
                ensureActive()
                val line = reader.readLine() ?: break
                if (!line.startsWith("data:")) continue
                val payload = line.removePrefix("data:").trim()
                if (payload == "[DONE]") break
                val chunk = runCatching { json.decodeFromString(Chunk.serializer(), payload) }.getOrNull() ?: continue
                val delta = chunk.choices.firstOrNull()?.delta?.content ?: continue
                if (delta.isNotEmpty()) {
                    full.append(delta)
                    onDelta(delta)
                }
            }
            full.toString().takeIf { it.isNotBlank() } ?: throw DeepSeekException("AI 未返回内容，请重试")
        } catch (e: DeepSeekException) {
            throw e
        } catch (e: IOException) {
            if (full.isNotEmpty()) full.toString() // 流中断但已有部分回复，降级返回
            else throw DeepSeekException("网络连接失败，请检查网络后重试")
        } finally {
            conn.disconnect()
        }
    }

    private fun connect(apiKey: String, req: Req): HttpURLConnection {
        val conn = (URL(API).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 120_000
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", if (req.stream) "text/event-stream" else "application/json")
            doOutput = true
        }
        try {
            conn.outputStream.use { it.write(json.encodeToString(Req.serializer(), req).toByteArray(Charsets.UTF_8)) }
            if (conn.responseCode !in 200..299) {
                val err = runCatching { conn.errorStream?.bufferedReader()?.readText() }.getOrNull()
                throw DeepSeekException(mapError(conn.responseCode, err))
            }
            return conn
        } catch (e: DeepSeekException) {
            conn.disconnect(); throw e
        } catch (e: IOException) {
            conn.disconnect(); throw DeepSeekException("网络连接失败，请检查网络后重试")
        }
    }

    private fun mapError(code: Int, body: String?): String {
        val detail = runCatching {
            json.decodeFromString(ErrBody.serializer(), body ?: "").error?.message
        }.getOrNull()
        return when {
            code == 401 -> "API key 无效，请在设置中检查"
            code == 402 -> "DeepSeek 账户余额不足，请充值后重试"
            code == 429 -> "请求过于频繁，请稍后重试"
            code == 400 -> detail ?: "请求被 AI 服务拒绝，请调整后重试"
            code >= 500 -> "AI 服务暂时不可用，请稍后重试"
            else -> detail ?: "AI 请求失败（$code），请重试"
        }
    }

    @Serializable
    private data class ErrBody(val error: ErrDetail? = null)

    @Serializable
    private data class ErrDetail(val message: String? = null)
}
