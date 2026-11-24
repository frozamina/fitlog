package com.fitlog.app.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.dp

/**
 * 设计 Token 体系
 * 来源：《UI 设计系统》规范 —— Apple 8pt 网格 + Material 3 动效原则
 * Sage & Ivory 语义令牌；装饰克制、交互可达
 */

/** 8pt 间距网格（严格 4 的倍数，主节奏 8） */
object Spacing {
    val xs = 4.dp    // space-1
    val s = 8.dp     // space-2
    val m = 12.dp    // space-3
    val base = 16.dp // space-4 标准间距
    val l = 20.dp    // space-5
    val xl = 24.dp   // space-6 组件内间距
    val xxl = 32.dp  // space-8 区块间距
    val huge = 48.dp // space-12 大区块间距
}

/** 圆角 —— 统一半径，不乱用 */
object Radius {
    val chip = 999.dp
    val small = 8.dp
    val field = 12.dp
    val card = 16.dp
    val hero = 20.dp
}

/** 触控尺寸 —— Apple HIG 最小 44dp */
object Touch {
    val min = 44.dp
    val button = 48.dp
    val fab = 56.dp
}

/** 动效曲线：位移/缩放/形变用 spring，尺寸变化用 emphasized，淡入淡出用 decelerate */
object Motion {
    val easeSpring = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
    val easeStandard = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val easeEmphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val easeDecelerate = CubicBezierEasing(0f, 0f, 0.2f, 1f)
    val easeAccelerate = CubicBezierEasing(0.3f, 0f, 1f, 1f)

    /** 时长档位 */
    const val SHORT2 = 100
    const val SHORT3 = 150
    const val SHORT4 = 200   // 微交互
    const val MEDIUM2 = 300  // 标准转场
    const val LONG2 = 400    // 复杂动画

    /** 回弹型：适合位移、缩放 */
    fun <T> springy(): FiniteAnimationSpec<T> = spring(
        dampingRatio = 0.62f,
        stiffness = Spring.StiffnessMediumLow
    )

    /** 标准型：适合淡入淡出、颜色 */
    fun <T> standard(duration: Int = MEDIUM2): FiniteAnimationSpec<T> =
        tween(durationMillis = duration, easing = easeStandard)

    /** 入场型 */
    fun <T> enter(duration: Int = MEDIUM2): FiniteAnimationSpec<T> =
        tween(durationMillis = duration, easing = easeDecelerate)

    /** 尺寸/布局变化 */
    fun <T> emphasized(duration: Int = LONG2): FiniteAnimationSpec<T> =
        tween(durationMillis = duration, easing = easeEmphasized)
}

/** 交错入场延迟（列表项 stagger） */
fun staggerDelay(index: Int, step: Int = 80, max: Int = 320): Int =
    (index * step).coerceAtMost(max)
