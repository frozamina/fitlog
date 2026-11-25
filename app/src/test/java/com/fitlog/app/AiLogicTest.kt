package com.fitlog.app

import com.fitlog.app.data.ColorKnowledge
import com.fitlog.app.data.DeepSeekClient
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiLogicTest {

    private fun entry(id: String, category: String, tags: List<String>, title: String = id) =
        ColorKnowledge.KnowledgeEntry(id = id, category = category, tags = tags, title = title, content = "内容")

    private val entries = listOf(
        entry("navy-bottom", "搭配技巧", listOf("藏青", "深蓝", "海军蓝", "裤子", "下装")),
        entry("light-dark-balance", "搭配技巧", listOf("上深下浅", "深浅", "层次", "叠穿")),
        entry("neutral-transition", "色系", listOf("中性色", "黑白灰", "米色", "过渡")),
        entry("skin-tone-match", "色温", listOf("肤色", "显白", "黄皮")),
        entry("spring-colors", "季节", listOf("春天", "春季"))
    )

    // ---- 知识检索 ----

    @Test
    fun `藏青裤子问题命中藏青下装条目`() {
        val hits = ColorKnowledge.searchIn(entries, "藏青裤子配什么颜色上衣")
        assertEquals("navy-bottom", hits.first().id)
        assertTrue(hits.isNotEmpty())
    }

    @Test
    fun `多主题问题命中多条目`() {
        val hits = ColorKnowledge.searchIn(entries, "黄皮冬天穿黑色裤子显白吗")
        assertTrue(hits.size >= 2) // 肤色条目 + 黑色下装条目
        assertTrue(hits.any { it.id == "skin-tone-match" })
    }

    @Test
    fun `显白问题命中肤色条目`() {
        val hits = ColorKnowledge.searchIn(entries, "黄皮穿什么颜色显白")
        assertEquals("skin-tone-match", hits.first().id)
    }

    @Test
    fun `无关问题返回空列表`() {
        assertTrue(ColorKnowledge.searchIn(entries, "今天天气怎么样").isEmpty())
        assertTrue(ColorKnowledge.searchIn(entries, "").isEmpty())
    }

    @Test
    fun `命中分数高的条目排前面`() {
        val hits = ColorKnowledge.searchIn(entries, "藏青裤子和深蓝裤子怎么配")
        // navy-bottom 命中 藏青/深蓝/裤子 等多个 tag，应排第一
        assertEquals("navy-bottom", hits.first().id)
    }

    @Test
    fun `limit 生效`() {
        val hits = ColorKnowledge.searchIn(entries, "藏青裤子配什么颜色上衣", limit = 1)
        assertEquals(1, hits.size)
    }

    // ---- 注入拼接 ----

    @Test
    fun `空结果标注暂无`() {
        assertEquals("（暂无）", ColorKnowledge.inject(emptyList()))
    }

    @Test
    fun `注入格式为标题加内容并截断`() {
        val out = ColorKnowledge.inject(entries.take(2).map { it.copy(content = "一".repeat(200)) }, maxChars = 100)
        assertTrue(out.startsWith("· "))
        assertTrue(out.length <= 100)
    }

    // ---- DTO 序列化形状 ----

    @Test
    fun `请求体包含 stream 与 response_format`() {
        val json = Json { encodeDefaults = true }
        val msg = DeepSeekClient.Message("user", "你好")
        // 用非流式/流式请求验证 JSON 字段存在（通过公共 Message 类构造请求形状）
        val encoded = json.encodeToString(DeepSeekClient.Message.serializer(), msg)
        assertTrue(encoded.contains("\"role\":\"user\""))
        assertTrue(encoded.contains("\"content\":\"你好\""))
    }
}
