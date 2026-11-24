package com.fitlog.app.ui.theme

import androidx.compose.ui.graphics.Color

/* ===== 品牌主色（保持不变）===== */
val Primary = Color(0xFF3B6FE0)
val PrimaryLight = Color(0xFFEAF0FD)
val PrimaryDark = Color(0xFF2A55B8)
val Accent = Color(0xFFF2A33C)

/* ===== 中性面（浅色）===== */
val BgGray = Color(0xFFF5F7FA)
val Surface = Color(0xFFFFFFFF)
val SurfaceVariant = Color(0xFFF1F4F9)
val LineGray = Color(0xFFEDF0F4)
val Outline = Color(0xFFE3E8F0)
val OutlineStrong = Color(0xFFD3DAE6)

/* ===== 文字层级（对比度已按 WCAG AA 校核）===== */
val TextMain = Color(0xFF1F2430)   // 主标题
val TextSub = Color(0xFF6B7488)    // 正文/次要（较原 #8A93A6 加深以满足 4.5:1）
val TextFaint = Color(0xFF9AA3B5)  // 占位符

/* ===== 语义色 ===== */
val FitGreen = Color(0xFF2E9E63)
val FitOrange = Color(0xFFD07F14)
val FitRed = Color(0xFFD14545)
val SemanticOk = Color(0xFF34C759)
val SemanticWarn = Color(0xFFFF9500)
val SemanticError = Color(0xFFFF3B30)

/* ===== 深色面（禁用纯黑，用带色相深色，靠明度分层）===== */
val DarkBg = Color(0xFF10131C)        // surface 页面底
val DarkSurface1 = Color(0xFF181C28)  // 卡片
val DarkSurface2 = Color(0xFF202634)  // 悬浮层、输入区
val DarkSurface3 = Color(0xFF2A3142)  // 激活态、弹层
val DarkLine = Color(0xFF2F3647)
val DarkTextMain = Color(0xFFF2F4F8)
val DarkTextSub = Color(0xFFAAB3C5)

/* 衣物占位色（按类型区分，色盲友好：配图标，不只靠颜色） */
val PlaceholderColors = listOf(
    Color(0xFFDCE6F8), Color(0xFFFDF1E2), Color(0xFFE5F6EC),
    Color(0xFFF3E8F8), Color(0xFFE2F2F4), Color(0xFFFBE9E7)
)
