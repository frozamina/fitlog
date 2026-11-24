package com.fitlog.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.fitlog.app.data.BodyProfile
import com.fitlog.app.data.FitRating
import com.fitlog.app.data.Garment
import com.fitlog.app.ui.CardBox
import com.fitlog.app.ui.EmptyState
import com.fitlog.app.ui.FitTag
import com.fitlog.app.ui.IconAction
import com.fitlog.app.ui.SectionTitle
import com.fitlog.app.ui.formatDate
import com.fitlog.app.ui.typeIcon
import com.fitlog.app.ui.typePlaceholderColor
import com.fitlog.app.ui.theme.*
import com.fitlog.app.vm.AppViewModel

@Composable
fun HomeScreen(vm: AppViewModel, onAdd: () -> Unit, onOpenGarment: (Long) -> Unit) {
    val history by vm.bodyHistory.collectAsState()
    val garments by vm.garments.collectAsState()
    var showEdit by remember { mutableStateOf(false) }
    var showAllHistory by remember { mutableStateOf(false) }

    val latest = history.firstOrNull()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.base)
    ) {
        Spacer(Modifier.height(Spacing.s))

        // ---- 身体数据主卡：页面唯一视觉焦点（渐变 + 大字号数字）----
        BodyHeroCard(latest = latest, onEdit = { showEdit = true })

        SectionTitle("我的衣物（${garments.size} 件）") {
            Text("全部 ▸", fontSize = 13.sp, color = Primary)
        }

        if (garments.isEmpty()) {
            CardBox {
                EmptyState(
                    icon = "👕",
                    title = "还没有衣物记录",
                    sub = "点右下角 ＋ 记录第一件衣服的尺码"
                )
            }
        } else {
            garments.take(5).forEachIndexed { index, g ->
                StaggerIn(index) { GarmentRow(vm, g, onOpenGarment) }
            }
        }

        SectionTitle("身体数据历史") {
            Text(
                if (showAllHistory) "收起" else "查看全部 ▸",
                fontSize = 13.sp, color = Primary,
                modifier = Modifier
                    .clickable { showAllHistory = !showAllHistory }
                    .padding(vertical = Spacing.s, horizontal = Spacing.xs)
            )
        }
        CardBox {
            val list = if (showAllHistory) history else history.take(3)
            if (list.isEmpty()) {
                Text("暂无记录", style = Typography2.body, color = FitTheme.colors.textSub)
            } else {
                list.forEachIndexed { i, p ->
                    if (i > 0) Divider(color = FitTheme.colors.line, thickness = 1.dp)
                    HistoryRow(p, onDelete = { vm.deleteBody(p.id) })
                }
            }
        }
        Spacer(Modifier.height(90.dp))
    }

    if (showEdit) {
        BodyEditDialog(
            latest = latest,
            onDismiss = { showEdit = false },
            onSave = { p ->
                vm.saveBody(p)
                showEdit = false
            }
        )
    }
}

/* ---------- 身体数据主卡 ---------- */

@Composable
private fun BodyHeroCard(latest: BodyProfile?, onEdit: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.hero))
            .background(FitTheme.colors.surface)
            .padding(Spacing.l)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("我的身体数据", fontSize = 13.sp, color = FitTheme.colors.textSub)
                    Spacer(Modifier.height(Spacing.xs))
                    // 身高体重合排一行：大数字 + 小单位，避免换行
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            fmt(latest?.height),
                            style = Typography2.numberBig.copy(fontSize = 26.sp, lineHeight = 34.sp),
                            color = FitTheme.colors.textMain
                        )
                        Text(
                            " cm",
                            fontSize = 14.sp, color = FitTheme.colors.textSub,
                            modifier = Modifier.padding(bottom = 5.dp)
                        )
                        Text(
                            "  ·  ",
                            fontSize = 14.sp, color = FitTheme.colors.textSub,
                            modifier = Modifier.padding(bottom = 5.dp)
                        )
                        Text(
                            fmt(latest?.weight),
                            style = Typography2.numberBig.copy(fontSize = 26.sp, lineHeight = 34.sp),
                            color = FitTheme.colors.textMain
                        )
                        Text(
                            " kg",
                            fontSize = 14.sp, color = FitTheme.colors.textSub,
                            modifier = Modifier.padding(bottom = 5.dp)
                        )
                    }
                }
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(Radius.field))
                        .background(FitTheme.colors.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) { Text("🧍", fontSize = 24.sp) }
            }
            Spacer(Modifier.height(Spacing.base))
            Row(Modifier.fillMaxWidth()) {
                BodyCell("胸围", fmt(latest?.chest), Modifier.weight(1f))
                BodyCell("腰围", fmt(latest?.waist), Modifier.weight(1f))
                BodyCell("臀围", fmt(latest?.hip), Modifier.weight(1f))
                BodyCell("肩宽", fmt(latest?.shoulder), Modifier.weight(1f))
            }
            Spacer(Modifier.height(Spacing.m))
            Divider(color = FitTheme.colors.line, thickness = 1.dp)
            Spacer(Modifier.height(Spacing.s))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "上次更新：${if (latest != null) formatDate(latest.date) else "暂无"}",
                    fontSize = 13.sp, color = FitTheme.colors.textSub,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(Radius.chip),
                    color = FitTheme.colors.surfaceVariant,
                    modifier = Modifier.clickable(onClick = onEdit)
                ) {
                    Text(
                        "更新数据", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        color = FitTheme.colors.textMain,
                        modifier = Modifier.padding(horizontal = Spacing.m, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BodyCell(label: String, value: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 13.sp, color = FitTheme.colors.textSub)
        Spacer(Modifier.height(2.dp))
        Text(value, style = Typography2.numberMid, color = FitTheme.colors.textMain)
    }
}

/* ---------- 交错入场（stagger fade-in）---------- */

@Composable
private fun StaggerIn(index: Int, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(staggerDelay(index).toLong())
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(Motion.enter(Motion.MEDIUM2)) +
            expandVertically(
                animationSpec = tween(Motion.MEDIUM2, easing = Motion.easeDecelerate)
            )
    ) { content() }
}

/* ---------- 衣物行 ---------- */

@Composable
private fun GarmentRow(vm: AppViewModel, g: Garment, onClick: (Long) -> Unit) {
    val images by vm.imagesFlow(g.id).collectAsState(initial = emptyList())
    val cover = images.firstOrNull()

    CardBox(
        modifier = Modifier.padding(bottom = Spacing.m),
        onClick = { onClick(g.id) }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (cover != null && java.io.File(cover.path).exists()) {
                AsyncImage(
                    model = java.io.File(cover.path),
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(Radius.field))
                )
            } else {
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(Radius.field))
                        .background(typePlaceholderColor(g.type)),
                    contentAlignment = Alignment.Center
                ) { Text(typeIcon(g.type), fontSize = 26.sp) }
            }
            Spacer(Modifier.width(Spacing.m))
            Column(Modifier.weight(1f)) {
                Text(
                    g.name, style = Typography2.cardTitle,
                    color = FitTheme.colors.textMain, maxLines = 1
                )
                Spacer(Modifier.height(Spacing.xs))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${g.type} · ${g.sizeLabel.ifBlank { "未填标码" }} · ${formatDate(g.createdAt)}",
                        style = Typography2.caption, color = FitTheme.colors.textSub
                    )
                    Spacer(Modifier.width(Spacing.s))
                    FitTag(FitRating.from(g.fit))
                }
            }
            Text("›", fontSize = 18.sp, color = FitTheme.colors.outline)
        }
    }
}

/* ---------- 历史行：日期一行，六项数据等宽对齐排列 ---------- */

@Composable
private fun HistoryRow(p: BodyProfile, onDelete: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.s)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                formatDate(p.date),
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                color = FitTheme.colors.textMain,
                modifier = Modifier.weight(1f)
            )
            IconAction("✕", FitRed, onDelete, fontSize = 13)
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth()) {
            HistoryCell("身高", fmt(p.height), Modifier.weight(1f))
            HistoryCell("体重", fmt(p.weight), Modifier.weight(1f))
            HistoryCell("胸围", fmt(p.chest), Modifier.weight(1f))
            HistoryCell("腰围", fmt(p.waist), Modifier.weight(1f))
            HistoryCell("臀围", fmt(p.hip), Modifier.weight(1f))
            HistoryCell("肩宽", fmt(p.shoulder), Modifier.weight(1f))
        }
    }
}

@Composable
private fun HistoryCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, fontSize = 10.sp, color = FitTheme.colors.textSub)
        Spacer(Modifier.height(1.dp))
        Text(value, fontSize = 13.sp, color = FitTheme.colors.textMain)
    }
}

/* ---------- 编辑弹窗 ---------- */

@Composable
private fun BodyEditDialog(
    latest: BodyProfile?,
    onDismiss: () -> Unit,
    onSave: (BodyProfile) -> Unit
) {
    var height by remember { mutableStateOf(latest?.height?.let { fmt(it) } ?: "") }
    var weight by remember { mutableStateOf(latest?.weight?.let { fmt(it) } ?: "") }
    var chest by remember { mutableStateOf(latest?.chest?.let { fmt(it) } ?: "") }
    var waist by remember { mutableStateOf(latest?.waist?.let { fmt(it) } ?: "") }
    var hip by remember { mutableStateOf(latest?.hip?.let { fmt(it) } ?: "") }
    var shoulder by remember { mutableStateOf(latest?.shoulder?.let { fmt(it) } ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(Radius.hero),
        containerColor = FitTheme.colors.surface,
        title = { Text("更新身体数据", style = Typography2.pageTitle) },
        text = {
            Column {
                NumRow("身高 (cm)", height) { height = it }
                NumRow("体重 (kg)", weight) { weight = it }
                NumRow("胸围 (cm)", chest) { chest = it }
                NumRow("腰围 (cm)", waist) { waist = it }
                NumRow("臀围 (cm)", hip) { hip = it }
                NumRow("肩宽 (cm)", shoulder) { shoulder = it }
                Spacer(Modifier.height(Spacing.xs))
                Text("每次保存会生成一条历史快照", style = Typography2.caption, color = FitTheme.colors.textSub)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    BodyProfile(
                        height = height.toFloatOrNull(),
                        weight = weight.toFloatOrNull(),
                        chest = chest.toFloatOrNull(),
                        waist = waist.toFloatOrNull(),
                        hip = hip.toFloatOrNull(),
                        shoulder = shoulder.toFloatOrNull()
                    )
                )
            }) { Text("保存", fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun NumRow(label: String, value: String, onChange: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = Typography2.body, color = FitTheme.colors.textMain, modifier = Modifier.weight(1f))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.width(110.dp),
            singleLine = true,
            shape = RoundedCornerShape(Radius.field),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = FitTheme.colors.outline,
                focusedContainerColor = FitTheme.colors.surface,
                unfocusedContainerColor = FitTheme.colors.surface
            )
        )
    }
}

private fun fmt(v: Float?): String =
    if (v == null) "--" else (Math.round(v * 10) / 10f).toString()
