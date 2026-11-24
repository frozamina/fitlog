package com.fitlog.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitlog.app.data.FitRating
import com.fitlog.app.data.GarmentType
import com.fitlog.app.ui.theme.*

/* ============================================================
 * 一级组件：卡片
 * 规范参考 —— Elevated 变体：两层克制阴影，按压时抬升
 * 同一页面只用一种变体，保持一致性
 * ============================================================ */

@Composable
fun CardBox(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(Radius.card),
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = FitTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    // 按压反馈：scale 0.985（微交互，150ms 标准曲线）
    val scale by animateFloatAsState(
        targetValue = if (pressed && onClick != null) 0.985f else 1f,
        animationSpec = Motion.standard(Motion.SHORT3),
        label = "cardScale"
    )
    // 阴影：默认 1 层精准，按压时下沉
    val elevation by animateDpAsState(
        targetValue = if (pressed && onClick != null) 0.dp else 1.dp,
        animationSpec = Motion.standard(Motion.SHORT4),
        label = "cardElev"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            ),
        shape = shape,
        color = colors.surface,
        shadowElevation = elevation,
        tonalElevation = if (colors.isDark) 2.dp else 0.dp
    ) {
        Column(Modifier.padding(Spacing.base), content = content)
    }
}

/** Filled 变体：无阴影填色卡（用于容器分组，避免阴影堆叠） */
@Composable
fun FilledCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = FitTheme.colors
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.card),
        color = colors.surface,
        tonalElevation = if (colors.isDark) 2.dp else 0.dp
    ) {
        Column(content = content)
    }
}

/* ============================================================
 * 二级组件：类型 Chip
 * 规范参考 —— Tab 指示条回弹（Spring），选中态位移 + 色彩过渡
 * ============================================================ */

@Composable
fun TypeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(
        targetValue = if (selected) Primary else FitTheme.colors.surfaceVariant,
        animationSpec = Motion.standard(Motion.SHORT4),
        label = "chipBg"
    )
    val fg by animateColorAsState(
        targetValue = if (selected) Color.White else FitTheme.colors.textSub,
        animationSpec = Motion.standard(Motion.SHORT4),
        label = "chipFg"
    )
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = Motion.springy(),
        label = "chipScale"
    )
    Box(
        Modifier
            .padding(end = Spacing.s, bottom = Spacing.s)
            .height(36.dp)
            .scale(scale)
            .background(bg, RoundedCornerShape(Radius.chip))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = Spacing.base),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = fg,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

/* ============================================================
 * 二级组件：合身标签
 * 规范参考 —— 不只靠颜色传达状态，配文字（色盲友好）
 * ============================================================ */

@Composable
fun FitTag(rating: FitRating) {
    val r = runCatching { FitRating.from(rating.label) }.getOrDefault(FitRating.FIT)
    val colors = FitTheme.colors
    val bg = if (colors.isDark) r.color.copy(alpha = 0.22f) else r.color.copy(alpha = 0.13f)
    val fg = if (colors.isDark) lighten(r.color) else r.color
    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = Spacing.s, vertical = 2.dp)
    ) {
        Text(r.label, color = fg, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun lighten(c: Color) = Color(
    red = (c.red * 0.55f + 0.45f),
    green = (c.green * 0.55f + 0.45f),
    blue = (c.blue * 0.55f + 0.45f),
    alpha = 1f
)

/* ============================================================
 * 二级组件：区块标题
 * ============================================================ */

@Composable
fun SectionTitle(text: String, action: (@Composable () -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xs, vertical = Spacing.s),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, style = Typography2.sectionTitle, color = FitTheme.colors.textMain, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

/* ============================================================
 * 二级组件：图标按钮 —— 保证 44dp 触控区（Apple HIG 底线）
 * ============================================================ */

@Composable
fun IconAction(
    text: String,
    color: Color,
    onClick: () -> Unit,
    fontSize: Int = 16
) {
    Box(
        Modifier
            .size(Touch.min)
            .clip(RoundedCornerShape(Spacing.m))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = color, fontSize = fontSize.sp)
    }
}

/* ============================================================
 * 三级组件：输入框（紧凑版，44dp 高，统一 Focus 态）
 * ============================================================ */

@Composable
fun FitTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    height: Dp? = null
) {
    val colors = FitTheme.colors
    Surface(
        modifier = modifier
            .heightIn(min = 36.dp)
            .then(if (height != null) Modifier.height(height) else Modifier),
        shape = RoundedCornerShape(Radius.field),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.outline)
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = singleLine,
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, color = colors.textMain),
                cursorBrush = SolidColor(Primary),
                modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty() && placeholder.isNotEmpty()) {
                            Text(placeholder, color = colors.textFaint, fontSize = 13.sp, maxLines = 1)
                        }
                        inner()
                    }
                }
            )
        }
    }
}

/** 紧凑搜索框（44dp 圆角胶囊，带搜索图标） */
@Composable
fun CompactSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "搜索名称、品牌"
) {
    val colors = FitTheme.colors
    Surface(
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(Radius.field),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.outline)
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.Search, contentDescription = null,
                tint = colors.textSub, modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, color = colors.textMain),
                cursorBrush = SolidColor(Primary),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty()) {
                            Text(placeholder, color = colors.textFaint, fontSize = 13.sp, maxLines = 1)
                        }
                        inner()
                    }
                }
            )
        }
    }
}

/** 小号复选框：视觉 18dp，外层保留 44dp 触控区 */
@Composable
fun CompactCheckbox(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = FitTheme.colors
    val boxShape = RoundedCornerShape(5.dp)
    Box(
        Modifier
            .size(44.dp)
            .clickable(onClick = { onCheckedChange(!checked) }),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(18.dp)
                .then(
                    if (checked) Modifier.background(Primary, boxShape)
                    else Modifier.border(1.5.dp, colors.outlineStrong, boxShape)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    Icons.Outlined.Check, contentDescription = null,
                    tint = Color.White, modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

@Composable
fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = ""
) {
    Column(Modifier.padding(bottom = Spacing.m)) {
        FieldLabel(label)
        FitTextField(value, onValueChange, Modifier.fillMaxWidth(), placeholder)
    }
}

@Composable
fun FieldLabel(text: String) {
    Text(
        text,
        style = Typography2.caption,
        color = FitTheme.colors.textSub,
        modifier = Modifier.padding(bottom = Spacing.xs, start = 2.dp)
    )
}

/* ============================================================
 * 三级组件：骨架屏 —— shimmer 脉冲（Loading 状态）
 * ============================================================ */

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(Radius.small)
) {
    val colors = FitTheme.colors
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = Motion.easeStandard),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )
    Box(
        modifier
            .clip(shape)
            .background(colors.surfaceVariant.copy(alpha = alpha))
    )
}

@Composable
fun GarmentCardSkeleton() {
    CardBox {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SkeletonBox(Modifier.size(56.dp), RoundedCornerShape(Radius.field))
            Spacer(Modifier.width(Spacing.m))
            Column(Modifier.weight(1f)) {
                SkeletonBox(Modifier.fillMaxWidth(0.6f).height(14.dp))
                Spacer(Modifier.height(Spacing.s))
                SkeletonBox(Modifier.fillMaxWidth(0.4f).height(11.dp))
            }
        }
    }
}

/* ============================================================
 * 三级组件：空状态（有温度的提示，而非冷冰冰一行字）
 * ============================================================ */

@Composable
fun EmptyState(icon: String, title: String, sub: String, action: (@Composable () -> Unit)? = null) {
    val colors = FitTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xxl, horizontal = Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(Radius.hero))
                .background(if (colors.isDark) colors.surfaceVariant else PrimaryLight),
            contentAlignment = Alignment.Center
        ) { Text(icon, fontSize = 30.sp) }
        Spacer(Modifier.height(Spacing.base))
        Text(title, style = Typography2.cardTitle, color = colors.textMain)
        Spacer(Modifier.height(Spacing.xs))
        Text(sub, style = Typography2.caption, color = colors.textSub)
        action?.let {
            Spacer(Modifier.height(Spacing.base))
            it()
        }
    }
}

/* ============================================================
 * 三级组件：Toast（底部滑入，自动消失）
 * ============================================================ */

@Composable
fun FitToast(message: String, visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(animationSpec = Motion.enter(Motion.MEDIUM2)) +
            fadeIn(Motion.standard(Motion.SHORT4)),
        exit = slideOutVertically(animationSpec = Motion.standard(Motion.SHORT4)) +
            fadeOut(Motion.standard(Motion.SHORT4))
    ) {
        Surface(
            shape = RoundedCornerShape(Radius.field),
            color = if (FitTheme.colors.isDark) DarkSurface3 else Color(0xE61F2430),
            shadowElevation = 6.dp
        ) {
            Text(
                message,
                color = Color.White,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = Spacing.base, vertical = Spacing.m)
            )
        }
    }
}

/* ============================================================
 * 工具方法
 * ============================================================ */

fun typeIcon(label: String): String =
    GarmentType.entries.firstOrNull { it.label == label }?.icon ?: "👕"

/** 按类型稳定分配占位色（同类型同色，色盲友好：始终配图标） */
fun typePlaceholderColor(label: String): Color {
    val idx = GarmentType.entries.indexOfFirst { it.label == label }
    return PlaceholderColors[(if (idx < 0) 0 else idx) % PlaceholderColors.size]
}

fun formatDate(ts: Long?): String {
    if (ts == null) return ""
    return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.CHINA).format(java.util.Date(ts))
}
