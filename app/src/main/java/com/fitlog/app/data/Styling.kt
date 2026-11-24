package com.fitlog.app.data

import kotlin.math.*

/** 推荐只使用真实存在的衣物；颜色未知时保留不确定性，不生成合身结论。 */
object Styling {
    enum class Slot(val label: String) { TOP("上装"), BOTTOM("下装"), OUTER("外套"), SHOES("鞋履"), BAG("包袋"), ACCESSORY("配饰"), NONE("不参与日常搭配") }
    fun slot(type: String): Slot = when (type) {
        "上衣", "背心", "针织衫" -> Slot.TOP
        "裤子", "高弹短裤" -> Slot.BOTTOM
        "外套" -> Slot.OUTER
        "鞋履" -> Slot.SHOES
        "包袋" -> Slot.BAG
        "帽子", "配饰" -> Slot.ACCESSORY
        else -> Slot.NONE
    }
    fun hex(value: String): String? = value.trim().takeIf { Regex("#[0-9a-fA-F]{6}").matches(it) }?.uppercase()
    private fun hsl(hex: String): Triple<Double, Double, Double> {
        val rgb = listOf(1, 3, 5).map { hex.substring(it, it + 2).toInt(16) / 255.0 }
        val (r, g, b) = rgb; val max = rgb.max(); val min = rgb.min(); val d = max - min; val l = (max + min) / 2
        var h = when { d == 0.0 -> 0.0; max == r -> ((g - b) / d) % 6; max == g -> (b - r) / d + 2; else -> (r - g) / d + 4 }
        h = (h * 60 + 360) % 360
        return Triple(h, if (d == 0.0) 0.0 else d / (1 - abs(2 * l - 1)), l)
    }
    data class Relation(val score: Double, val reason: String)
    fun relation(a: Garment, b: Garment): Relation {
        val ac = hex(a.mainColor); val bc = hex(b.mainColor)
        if (ac == null || bc == null) return Relation(.35, "有单品尚未标注主色，补充后能更好地比较配色。")
        val (ha, sa, la) = hsl(ac); val (hb, sb, lb) = hsl(bc)
        val diff = min(abs(ha - hb), 360 - abs(ha - hb))
        return when {
            sa < .12 || sb < .12 || la < .12 || lb < .12 || la > .9 || lb > .9 -> Relation(.9, "中性色连接主色，明暗差异带来层次。")
            diff < 35 -> Relation(.85, "相近色相互呼应，可以用面料和明暗增加层次。")
            diff > 150 -> Relation(.72, "接近互补色的组合更醒目，建议让一种颜色占较小面积。")
            else -> Relation(.55, "颜色有一定差异，可以用白、灰或黑色过渡。")
        }
    }
    data class Suggestion(val ids: List<Long>, val reasons: List<String>, val missing: List<Slot>)
    fun suggest(items: List<Garment>, anchorId: Long): List<Suggestion> {
        val available = items.filter { slot(it.type) != Slot.NONE }
        val anchor = available.find { it.id == anchorId } ?: return emptyList()
        val required = listOf(Slot.TOP, Slot.BOTTOM, Slot.SHOES).filter { it != slot(anchor.type) }
        val missing = required.filter { s -> available.none { slot(it.type) == s } }
        var combinations = listOf(listOf(anchor))
        for (s in required - missing.toSet()) {
            val candidates = available.filter { it.id != anchorId && slot(it.type) == s }.sortedWith(compareByDescending<Garment> { relation(anchor, it).score }.thenBy { it.id }).take(5)
            combinations = combinations.flatMap { c -> candidates.map { c + it } }
        }
        return combinations.map { pieces ->
            val relations = pieces.flatMapIndexed { i, a -> pieces.drop(i + 1).map { b -> relation(a, b) } }
            val patterned = pieces.count { it.pattern !in listOf("未标注", "纯色", "") }
            val score = relations.map { it.score }.average().takeUnless { it.isNaN() } ?: 0.0
            Pair(score - if (patterned > 1) .15 else 0.0, Suggestion(pieces.map { it.id }, (pieces.drop(1).map { relation(anchor, it).reason } + if (patterned > 1) listOf("多件单品带图案，请再确认图案大小与视觉重点。") else emptyList()).distinct(), missing))
        }.sortedByDescending { it.first }.take(3).map { it.second }
    }
    fun matchesPalette(value: String, palette: List<String>): Boolean {
        val c = hex(value) ?: return false; val (h,s,l)=hsl(c)
        fun neutral(s: Double,l: Double)=s<.06 || (l>.82 && s<.35) || l<.13
        return palette.any { p -> val (ph,ps,pl)=hsl(p); if(neutral(ps,pl)) neutral(s,l)&&abs(l-pl)<.22 else !neutral(s,l)&&min(abs(h-ph),360-abs(h-ph))<35&&abs(l-pl)<.3 }
    }
}

object MeasurementComparison {
    data class Row(val name: String, val reference: String, val candidate: String, val deltaCm: Double?, val explanation: String)
    private fun cm(a: GarmentAttr): Double? {
        val n=a.value.trim().toDoubleOrNull()?.takeIf { it.isFinite() && it>=0 }?:return null
        return when(a.unit.trim().lowercase()){ "cm" -> n; "mm" -> n/10; "m" -> n*100; else -> null }
    }
    fun rows(reference: List<GarmentAttr>, candidate: List<GarmentAttr>): List<Row> = (reference.map { it.name }+candidate.map { it.name }).distinct().map { name ->
        val a=reference.find { it.name==name };val b=candidate.find { it.name==name }
        fun text(v:GarmentAttr?)=v?.let { "${it.value} ${it.unit}" }?:"未记录"
        val compatible=a!=null&&b!=null&&a.basis.isNotBlank()&&a.basis==b.basis
        val x=a?.let(::cm);val y=b?.let(::cm)
        Row(name,text(a),text(b),if(compatible&&x!=null&&y!=null) ((y-x)*100.0).roundToInt()/100.0 else null,
            when{a==null||b==null->"一侧未记录";a.basis.isBlank()||b.basis.isBlank()->"测量口径未注明，仅并排参考";a.basis!=b.basis->"测量口径不同，不计算差值";x==null||y==null->"数值或单位无法比较";else->a.basis})
    }
}
