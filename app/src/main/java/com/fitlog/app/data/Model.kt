package com.fitlog.app.data

import androidx.compose.ui.graphics.Color
import com.fitlog.app.ui.theme.FitGreen
import com.fitlog.app.ui.theme.FitOrange
import com.fitlog.app.ui.theme.FitRed

/** 合身评价：三档 */
enum class FitRating(val label: String, val color: Color) {
    UNRATED("未评价", FitGreen),
    TIGHT("紧身", FitRed),
    FIT("合身", FitGreen),
    LOOSE("宽松", FitOrange);

    companion object {
        fun from(label: String): FitRating = entries.firstOrNull { it.label == label } ?: UNRATED
    }
}

/** 衣物类型：选择后自动带出默认属性 */
enum class GarmentType(val label: String, val icon: String, val defaults: List<String>) {
    TOP("上衣", "👕", listOf("衣长", "胸围", "肩宽")),
    VEST("背心", "🦺", listOf("衣长", "胸围", "肩宽")),
    PANTS("裤子", "👖", listOf("裤长", "腰围", "臀围")),
    SHORTS("高弹短裤", "🩳", listOf("裤长", "腰围", "臀围")),
    OUTER("外套", "🧥", listOf("衣长", "胸围", "肩宽")),
    SHOES("鞋履", "👟", listOf("鞋内长")),
    BAG("包袋", "👜", listOf("包宽", "包高")),
    KNIT("针织衫", "👕", listOf("衣长", "胸围", "肩宽")),
    ACCESSORY("配饰", "🧣", emptyList()),
    UNDERWEAR("内裤", "🩲", listOf("腰围", "臀围")),
    HAT("帽子", "🧢", listOf("头围", "帽檐长"));

    companion object {
        fun from(label: String): GarmentType = entries.firstOrNull { it.label == label } ?: TOP
        fun labels(): List<String> = entries.map { it.label }
    }
}
