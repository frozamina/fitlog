package com.fitlog.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 内置穿搭配色知识库（assets/color_knowledge.json）。
 * 六大维度：色系 / 搭配技巧 / 质感 / 氛围风格 / 季节 / 色温。
 * 检索策略：主题关键词映射 → 与条目 tags 交集打分 → 取 top N 拼进 AI prompt（无向量库，规则检索）。
 */
class ColorKnowledge(private val ctx: Context) {

    @Serializable
    data class KnowledgeEntry(
        val id: String,
        val category: String,
        val tags: List<String> = emptyList(),
        val title: String,
        val content: String
    )

    @Serializable
    data class KnowledgeFile(val version: Int = 1, val entries: List<KnowledgeEntry> = emptyList())

    private val json = Json { ignoreUnknownKeys = true }
    private var cache: KnowledgeFile? = null

    suspend fun load(): KnowledgeFile {
        cache?.let { return it }
        return withContext(Dispatchers.IO) {
            cache ?: run {
                val text = ctx.assets.open(FILE).bufferedReader().use { it.readText() }
                json.decodeFromString(KnowledgeFile.serializer(), text).also { cache = it }
            }
        }
    }

    /** 检索与问题最相关的条目（无命中返回空列表，由 prompt 标注「暂无」）。 */
    suspend fun search(query: String, limit: Int = 4): List<KnowledgeEntry> =
        searchIn(load().entries, query)

    companion object {
        private const val FILE = "color_knowledge.json"

        /** 主题关键词映射：口语问题 → 命中词集合（与 tags 做交集打分）。 */
        val TOPICS: List<List<String>> = listOf(
            listOf("藏青", "藏蓝", "海军蓝", "深蓝"),
            listOf("黑色", "黑裤子", "黑裙", "黑外套"),
            listOf("白色", "白裤子", "白衬衫", "奶白", "米白"),
            listOf("灰色", "浅灰", "深灰", "灰白"),
            listOf("驼色", "驼", "棕色", "咖啡", "焦糖", "卡其"),
            listOf("蓝色", "雾蓝", "浅蓝", "牛仔蓝", "克莱因蓝"),
            listOf("绿色", "墨绿", "军绿", "灰绿", "鼠尾草"),
            listOf("红色", "酒红", "砖红", "粉色", "浅粉"),
            listOf("黄色", "鹅黄", "芥末黄", "姜黄"),
            listOf("紫色", "香芋", "丁香"),
            listOf("大地色", "大地"),
            listOf("裤子", "牛仔裤", "牛仔裤", "阔腿裤", "直筒裤", "西裤", "下装", "半裙", "裙子", "长裙"),
            listOf("上衣", "衬衫", "T恤", "针织衫", "毛衣", "卫衣", "外套", "大衣", "风衣", "夹克", "西装"),
            listOf("鞋", "球鞋", "皮鞋", "靴子", "乐福鞋"),
            listOf("包", "包袋", "袋子"),
            listOf("配饰", "围巾", "帽子", "丝巾", "项链", "腰带"),
            listOf("条纹", "格纹", "印花", "碎花", "图案", "格子"),
            listOf("牛仔", "丹宁"),
            listOf("针织", "羊绒", "毛线"),
            listOf("棉麻", "亚麻"),
            listOf("皮革", "皮衣", "皮靴", "皮包"),
            listOf("真丝", "缎面", "光泽"),
            listOf("法式", "慵懒", "松弛"),
            listOf("老钱风", "静奢", "高级感", "显贵"),
            listOf("日系", "清冷", "冷淡"),
            listOf("街头", "运动风", "潮"),
            listOf("通勤", "职场", "上班", "工作", "面试"),
            listOf("约会", "温柔", "相亲"),
            listOf("复古", "怀旧", "学院风"),
            listOf("春天", "春季", "初春"),
            listOf("夏天", "夏季", "盛夏", "热天"),
            listOf("秋天", "秋季", "初秋", "深秋"),
            listOf("冬天", "冬季", "寒冬", "冷天"),
            listOf("换季", "过渡季", "早晚温差"),
            listOf("显白", "肤色", "黄皮", "气色", "衬肤色"),
            listOf("色温", "冷调", "暖调", "冷暖"),
            listOf("同色系", "套装", "渐变"),
            listOf("邻近色", "相近色"),
            listOf("对比色", "撞色", "点缀", "亮色"),
            listOf("中性色", "百搭", "基础色"),
            listOf("上宽下窄", "上紧下松", "宽松", "oversize", "阔腿", "轮廓", "版型", "显高", "显胖", "比例"),
            listOf("层次", "叠穿", "内搭", "叠搭"),
            listOf("显瘦", "显胖", "瘦")
        )

        /** 纯函数检索（便于单元测试）：query 命中主题词 → 与条目 tags 交集计分。 */
        fun searchIn(entries: List<KnowledgeEntry>, query: String, limit: Int = 4): List<KnowledgeEntry> {
            val hits = TOPICS.flatten().distinct().filter { query.contains(it) }
            if (hits.isEmpty()) return emptyList()
            return entries
                .map { e -> e to e.tags.count { tag -> hits.any { tag.contains(it) || it.contains(tag) } } }
                .filter { it.second > 0 }
                .sortedWith(compareByDescending<Pair<KnowledgeEntry, Int>> { it.second }.thenBy { it.first.id })
                .take(limit)
                .map { it.first }
        }

        /** 纯函数：拼接注入 prompt 的资料块（总长截断，第一条保底截断防超支）。 */
        fun inject(entries: List<KnowledgeEntry>, maxChars: Int = 1200): String {
            if (entries.isEmpty()) return "（暂无）"
            val sb = StringBuilder()
            for (e in entries) {
                val line = "· ${e.title}：${e.content}"
                if (sb.isNotEmpty() && sb.length + line.length > maxChars) break
                val cut = if (sb.isEmpty()) line.take(maxChars) else line // 第一条是最高分，必须注入
                if (sb.isNotEmpty()) sb.append('\n')
                sb.append(cut)
            }
            return sb.toString()
        }
    }
}
