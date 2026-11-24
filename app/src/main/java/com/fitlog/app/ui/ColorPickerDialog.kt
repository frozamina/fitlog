package com.fitlog.app.ui

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitlog.app.ui.theme.FitTheme
import com.fitlog.app.ui.theme.Typography2
import kotlin.math.roundToInt

/** 服装常用低饱和色板（主色快捷选择，与配色推荐算法同色系） */
val GarmentSwatches = listOf(
    "#292D2C", // 黑
    "#555C5B", // 深灰
    "#A7ADB5", // 浅灰
    "#F5F3EE", // 米白
    "#E8E5DA", // 燕麦
    "#C4B9A4", // 奶咖
    "#B9A08E", // 驼色
    "#B07A4E", // 焦糖
    "#6E5A4B", // 深棕
    "#2C3E5D", // 藏蓝
    "#A9BEC7", // 雾蓝
    "#5B7B97", // 牛仔蓝
    "#47765D", // 军绿
    "#788674", // 灰绿
    "#A3B18A", // 豆绿
    "#7A3B3F", // 酒红
    "#D8A7A0", // 藕粉
    "#9B8AA6"  // 紫灰
)

private fun hexToHsv(hex: String): FloatArray = try {
    val c = AndroidColor.parseColor(hex.trim())
    val hsv = FloatArray(3)
    AndroidColor.RGBToHSV(c shr 16 and 0xFF, c shr 8 and 0xFF, c and 0xFF, hsv)
    hsv
} catch (e: Exception) {
    floatArrayOf(0f, 0.75f, 0.45f) // 无有效初始色时给一个中性的红棕
}

/**
 * 自定义颜色弹窗（仿美图秀秀/醒图交互）：
 * 二维渐变面板（横=饱和度，纵=明度）+ 底部色相条 + 当前色预览，纯 Compose 自绘。
 */
@Composable
fun ColorPickerDialog(initialHex: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    val init = remember { hexToHsv(initialHex) }
    var hue by remember { mutableFloatStateOf(init[0]) }
    var sat by remember { mutableFloatStateOf(init[1]) }
    var value by remember { mutableFloatStateOf(init[2]) }
    val argb = AndroidColor.HSVToColor(floatArrayOf(hue, sat, value))
    val currentColor = Color(argb)
    val hexText = "#%06X".format(argb and 0xFFFFFF)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自定义颜色", style = Typography2.cardTitle, color = FitTheme.colors.textMain) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SvPanel(hue, sat, value) { s, v -> sat = s; value = v }
                Spacer(Modifier.height(16.dp))
                HueBar(hue) { hue = it }
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(currentColor)
                            .border(1.dp, FitTheme.colors.line, CircleShape)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(hexText, style = Typography2.body, color = FitTheme.colors.textMain)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(hexText) }) {
                Text("确定", color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = FitTheme.colors.textSub) }
        }
    )
}

/** 饱和度/明度面板：底色为当前色相，横向白→透明（饱和度），纵向透明→黑（明度） */
@Composable
private fun SvPanel(hue: Float, sat: Float, value: Float, onChange: (Float, Float) -> Unit) {
    var panelSize by remember { mutableStateOf(IntSize.Zero) }
    fun apply(pos: Offset) {
        if (panelSize.width <= 0 || panelSize.height <= 0) return
        onChange(
            (pos.x / panelSize.width).coerceIn(0f, 1f),
            (1f - pos.y / panelSize.height).coerceIn(0f, 1f)
        )
    }
    Box(Modifier.fillMaxWidth().height(216.dp)) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .drawBehind {
                    drawRect(Color(AndroidColor.HSVToColor(floatArrayOf(hue, 1f, 1f))))
                    drawRect(Brush.horizontalGradient(listOf(Color.White, Color.Transparent)))
                    drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                }
                .onSizeChanged { panelSize = it }
                .pointerInput(Unit) { detectTapGestures { apply(it) } }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        apply(change.position)
                    }
                }
        )
        // 拾取圈
        Box(
            Modifier
                .offset {
                    IntOffset(
                        (sat * panelSize.width).roundToInt() - 11.dp.roundToPx(),
                        ((1 - value) * panelSize.height).roundToInt() - 11.dp.roundToPx()
                    )
                }
                .size(22.dp)
                .border(3.dp, Color.White, CircleShape)
        )
    }
}

/** 色相条（彩虹渐变滑块） */
@Composable
private fun HueBar(hue: Float, onChange: (Float) -> Unit) {
    var barSize by remember { mutableStateOf(IntSize.Zero) }
    val rainbow = listOf(
        Color(0xFFFF0000), Color(0xFFFFFF00), Color(0xFF00FF00),
        Color(0xFF00FFFF), Color(0xFF0000FF), Color(0xFFFF00FF), Color(0xFFFF0000)
    )
    fun apply(x: Float) {
        if (barSize.width <= 0) return
        onChange(((x / barSize.width) * 360f).coerceIn(0f, 360f))
    }
    Box(Modifier.fillMaxWidth().height(36.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(24.dp)
                .align(Alignment.Center)
                .clip(RoundedCornerShape(12.dp))
                .drawBehind { drawRect(Brush.horizontalGradient(rainbow)) }
                .onSizeChanged { barSize = it }
                .pointerInput(Unit) { detectTapGestures { apply(it.x) } }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        apply(change.position.x)
                    }
                }
        )
        // 滑块
        Box(
            Modifier
                .offset {
                    IntOffset(
                        ((hue / 360f) * barSize.width).roundToInt() - 11.dp.roundToPx(),
                        (6.dp).roundToPx()
                    )
                }
                .size(22.dp)
                .border(3.dp, Color.White, CircleShape)
        )
    }
}
