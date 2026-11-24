package com.fitlog.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.fitlog.app.data.FitRating
import com.fitlog.app.ui.CardBox
import com.fitlog.app.ui.FitTag
import com.fitlog.app.ui.theme.*
import com.fitlog.app.vm.AppViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(vm: AppViewModel, garmentId: Long, onBack: () -> Unit, onEdit: (Long) -> Unit) {
    var garment by remember { mutableStateOf<com.fitlog.app.data.Garment?>(null) }
    val attrs by vm.attrsFlow(garmentId).collectAsState(initial = emptyList())
    val images by vm.imagesFlow(garmentId).collectAsState(initial = emptyList())
    var showDelete by remember { mutableStateOf(false) }
    var cover by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(garmentId) {
        garment = vm.garment(garmentId)
    }
    LaunchedEffect(images) { if (cover == null) cover = images.firstOrNull()?.path }

    if (garment == null) {
        Box(Modifier.fillMaxSize().background(FitTheme.colors.bg), contentAlignment = Alignment.Center) {
            Text("加载中…", style = Typography2.body, color = FitTheme.colors.textSub)
        }
        return
    }
    val g = garment!!

    Scaffold(
        containerColor = FitTheme.colors.bg,
        topBar = {
            TopAppBar(
                title = { Text("衣物详情", style = Typography2.pageTitle) },
                navigationIcon = { TextButton(onClick = onBack) { Text("‹ 返回", fontSize = 14.sp) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FitTheme.colors.surface)
            )
        }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 大图区
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(typePlaceholderColorLocal(g.type)),
                contentAlignment = Alignment.Center
            ) {
                val c = cover
                if (c != null && File(c).exists()) {
                    AsyncImage(model = File(c), contentDescription = null, modifier = Modifier.fillMaxSize())
                } else {
                    Text(com.fitlog.app.ui.typeIcon(g.type), fontSize = 64.sp)
                }
            }

            Column(Modifier.padding(Spacing.base)) {
                // 标题区
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(g.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = FitTheme.colors.textMain)
                        Spacer(Modifier.height(Spacing.xs))
                        Text(
                            "${g.type} · 标码 ${g.sizeLabel.ifBlank { "未填" }} · ${com.fitlog.app.ui.formatDate(g.createdAt)} 添加",
                            style = Typography2.caption, color = FitTheme.colors.textSub
                        )
                    }
                    FitTag(FitRating.from(g.fit))
                }

                if (listOf(g.brand, g.price, g.channel).any { it.isNotBlank() }) {
                    Spacer(Modifier.height(Spacing.s))
                    Text(
                        listOf(g.brand, g.price, g.channel).filter { it.isNotBlank() }.joinToString(" · "),
                        style = Typography2.caption, color = FitTheme.colors.textSub
                    )
                }

                Spacer(Modifier.height(Spacing.base))

                // 尺码数据卡
                CardBox {
                    Text("尺码数据", style = Typography2.sectionTitle, color = FitTheme.colors.textMain)
                    Spacer(Modifier.height(Spacing.xs))
                    if (attrs.isEmpty()) {
                        Text("暂无尺码数据", style = Typography2.body, color = FitTheme.colors.textSub)
                    } else {
                        attrs.forEachIndexed { i, a ->
                            if (i > 0) Divider(color = FitTheme.colors.line, thickness = 1.dp)
                            Row(Modifier.fillMaxWidth().padding(vertical = Spacing.m)) {
                                Text(
                                    if (a.isCustom) "${a.name}（自定义）" else a.name,
                                    style = Typography2.body, color = FitTheme.colors.textSub,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    "${a.value} ${a.unit}",
                                    style = Typography2.body, fontWeight = FontWeight.Bold,
                                    color = FitTheme.colors.textMain
                                )
                            }
                        }
                    }
                }

                // 备注
                if (g.note.isNotBlank()) {
                    Spacer(Modifier.height(Spacing.m))
                    CardBox {
                        Text("💬 ${g.note}", style = Typography2.body, color = FitTheme.colors.textSub)
                    }
                }

                // 图片切换
                if (images.size > 1) {
                    Spacer(Modifier.height(Spacing.m))
                    Row(Modifier.fillMaxWidth()) {
                        images.forEach { img ->
                            val selected = cover == img.path
                            AsyncImage(
                                model = File(img.path),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(72.dp)
                                    .padding(end = Spacing.s)
                                    .clip(RoundedCornerShape(Radius.field))
                                    .then(
                                        if (selected) Modifier.border(2.dp, Primary, RoundedCornerShape(Radius.field))
                                        else Modifier
                                    )
                                    .clickable { cover = img.path }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(Spacing.l))

                // 操作区：一屏只有一个 Filled 主 CTA
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    OutlinedButton(
                        onClick = { showDelete = true },
                        modifier = Modifier.weight(1f).height(Touch.button),
                        shape = RoundedCornerShape(Radius.field),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = FitRed)
                    ) { Text("删除", fontSize = 13.sp) }
                    OutlinedButton(
                        onClick = { vm.duplicate(g.id) { onEdit(it) } },
                        modifier = Modifier.weight(1f).height(Touch.button),
                        shape = RoundedCornerShape(Radius.field),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary)
                    ) { Text("复制", fontSize = 13.sp) }
                    Button(
                        onClick = { onEdit(g.id) },
                        modifier = Modifier.weight(1f).height(Touch.button),
                        shape = RoundedCornerShape(Radius.field),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) { Text("编辑", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                }
                Spacer(Modifier.height(Spacing.xl))
            }
        }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            shape = RoundedCornerShape(Radius.hero),
            containerColor = FitTheme.colors.surface,
            title = { Text("确认删除？") },
            text = { Text("该衣物及其图片将被删除，且不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    vm.deleteGarment(g.id) { onBack() }
                }) { Text("删除", color = FitRed) }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("取消") } }
        )
    }
}

private fun typePlaceholderColorLocal(type: String): Color =
    com.fitlog.app.ui.typePlaceholderColor(type)
