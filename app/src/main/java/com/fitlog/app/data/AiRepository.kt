package com.fitlog.app.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** AI 生成色板的原始输出结构（与 UI 的 StudioPalette 解耦） */
@Serializable
data class AiPalette(val name: String = "", val description: String = "", val colors: List<String> = emptyList())

/** 聊天消息（UI 展示与请求历史共用） */
data class ChatMessage(val role: String, val content: String, val time: Long = 0, val isError: Boolean = false)

/**
 * AI 编排层：prompt 组装、AI 输出解析校验、衣橱摘要构建。
 * 网络调用统一走 DeepSeekClient；知识检索走 ColorKnowledge。
 */
class AiRepository(private val ctx: Context) {
    private val knowledge = ColorKnowledge(ctx)

    // ---- key 管理 ----
    fun hasKey() = AiSettings.hasKey(ctx)
    fun key() = AiSettings.apiKey(ctx)
    fun mask() = AiSettings.mask(AiSettings.apiKey(ctx))
    fun saveKey(key: String) = AiSettings.saveKey(ctx, key)
    fun clearKey() = AiSettings.clearKey(ctx)

    /** 色板生成的 messages（强制 JSON 输出） */
    suspend fun paletteMessages(keyword: String, digest: String?): List<DeepSeekClient.Message> {
        val user = buildString {
            append("主题：").append(keyword.trim())
            if (!digest.isNullOrBlank()) append("\n衣橱摘要：").append(digest)
        }
        return listOf(
            DeepSeekClient.Message("system", PALETTE_PROMPT),
            DeepSeekClient.Message("user", user)
        )
    }

    /** 聊天的 messages：system（含检索知识）+ 最近 8 条历史 + 当前问题 */
    suspend fun chatMessages(userText: String, history: List<ChatMessage>): List<DeepSeekClient.Message> {
        val ref = ColorKnowledge.inject(knowledge.search(userText))
        val msgs = listOf(DeepSeekClient.Message("system", CHAT_PROMPT.replace("@@REF@@", ref))) +
            history.filter { !it.isError && it.content.isNotBlank() }.takeLast(8)
                .map { DeepSeekClient.Message(it.role, it.content) } +
            DeepSeekClient.Message("user", userText)
        return msgs
    }

    companion object {

        private val PALETTE_PROMPT = """
            你是服装配色顾问。根据用户给出的主题关键词生成一套适合日常穿搭的配色方案。
            严格只输出 JSON，不要输出任何其他文字，格式：
            {"name":"方案名（不超过8字）","description":"一句话配色思路（不超过40字）","colors":["#RRGGBB"]}
            要求：
            1. colors 为 3~5 个颜色，必须是大写 #RRGGBB 格式；
            2. 颜色为低饱和、贴近真实面料的服装色，禁用荧光色；至少包含一个中性色（黑白灰米、牛仔蓝等）便于日常搭配；
            3. 若用户消息附有衣橱摘要（中文色名×类别统计），优先从中取色，或给出与其协调的新颜色；
            4. 名称与描述使用简体中文。
        """.trimIndent()

        private val CHAT_PROMPT = """
            你是「服集 FitLog」内置的穿搭配色顾问，用简洁友好的简体中文回答。
            规则：
            1. 只回答穿搭配色相关问题；无关话题礼貌引导回配色话题；
            2. 优先依据下方【参考资料】回答，不要编造色号；资料未覆盖时可用你的通用搭配常识；
            3. 回答不超过 150 字，可分点；涉及具体组合时给出可直接使用的颜色组合（如「藏青 + 奶白 + 浅驼」）；
            4. 不提供医疗、占卜等无关内容；不确定时如实说明。

            【参考资料】
            @@REF@@
        """.trimIndent()

        /**
         * 解析 AI 返回的色板 JSON；色值逐个经 Styling.hex 校验并去重，有效色 <2 视为失败返回 null。
         */
        fun parsePalette(content: String): AiPalette? {
            val cleaned = content.trim()
                .removePrefix("```json").removePrefix("```")
                .removeSuffix("```").trim()
            val p = runCatching {
                Json { ignoreUnknownKeys = true }.decodeFromString(AiPalette.serializer(), cleaned)
            }.getOrNull() ?: return null
            val colors = p.colors.mapNotNull { Styling.hex(it) }.distinct()
            if (colors.size < 2) return null
            return p.copy(
                name = p.name.trim().ifBlank { "AI 配色" },
                description = p.description.trim().ifBlank { "由 AI 生成的配色方案" },
                colors = colors
            )
        }

        /**
         * 衣橱摘要：中文色名×类别统计（按件数取前 topN 组，总长 ≤maxChars）。
         * 只包含颜色与类别聚合信息，绝不包含名称、品牌、备注等私人文字。
         */
        fun buildDigest(garments: List<Garment>, topN: Int = 15, maxChars: Int = 200): String {
            val usable = garments.filter { it.mainColor.isNotBlank() }
            if (usable.isEmpty()) return ""
            val counts = usable
                .groupingBy { "${ColorNames.nameOf(it.mainColor) ?: "其他色"} ${it.type}" }
                .eachCount()
            val text = counts.entries
                .sortedByDescending { it.value }
                .take(topN)
                .joinToString("、") { "${it.key}×${it.value}" }
            return text.take(maxChars)
        }
    }
}
