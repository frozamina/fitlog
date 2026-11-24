package com.fitlog.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.fitlog.app.data.FitRating
import com.fitlog.app.data.Garment
import com.fitlog.app.data.GarmentType
import com.fitlog.app.ui.CardBox
import com.fitlog.app.ui.EmptyState
import com.fitlog.app.ui.FitTag
import com.fitlog.app.ui.FitTextField
import com.fitlog.app.ui.GarmentCardSkeleton
import com.fitlog.app.ui.TypeChip
import com.fitlog.app.ui.formatDate
import com.fitlog.app.ui.typeIcon
import com.fitlog.app.ui.typePlaceholderColor
import com.fitlog.app.ui.theme.*
import com.fitlog.app.vm.AppViewModel
import java.io.File

@Composable
fun ListScreen(vm: AppViewModel, onOpen: (Long) -> Unit) {
    val all by vm.garments.collectAsState()
    var query by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("全部") }

    // 数据到位前的骨架屏（Loading 态）
    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(all) { if (all.isNotEmpty()) ready = true }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(700)
        ready = true
    }

    val list = all.filter {
        (type == "全部" || it.type == type) &&
            (query.isBlank() || it.name.contains(query, true) || it.brand.contains(query, true))
    }

    Column(Modifier.fillMaxSize()) {
        // 搜索框（Focus 态清晰，圆角统一）
        Box(Modifier.padding(horizontal = Spacing.base, vertical = Spacing.s)) {
            FitTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = "搜索名称 / 品牌…"
            )
        }

        // 类型筛选：FlowRow 自动换行
        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(
            Modifier
                .padding(horizontal = Spacing.base, vertical = Spacing.xs)
                .fillMaxWidth()
        ) {
            TypeChip("全部 ${all.size}", type == "全部") { type = "全部" }
            GarmentType.entries.forEach { t ->
                val n = all.count { it.type == t.label }
                TypeChip("${t.label} $n", type == t.label) { type = t.label }
            }
        }

        when {
            !ready -> {
                Column(
                    Modifier.padding(horizontal = Spacing.base),
                    verticalArrangement = Arrangement.spacedBy(Spacing.m)
                ) {
                    repeat(4) { GarmentCardSkeleton() }
                }
            }

            list.isEmpty() -> {
                EmptyState(
                    icon = if (all.isEmpty()) "👔" else "🔍",
                    title = if (all.isEmpty()) "衣柜还是空的" else "没有匹配的衣物",
                    sub = if (all.isEmpty()) "点右下角 ＋ 添加第一件" else "换个类型或关键字试试"
                )
            }

            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = Spacing.base, vertical = Spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(Spacing.m)
                ) {
                    items(list, key = { it.id }) { g ->
                        GarmentCard(vm, g) { onOpen(g.id) }
                    }
                    item { Spacer(Modifier.height(90.dp)) }
                }
            }
        }
    }
}

@Composable
private fun GarmentCard(vm: AppViewModel, g: Garment, onClick: () -> Unit) {
    val images by vm.imagesFlow(g.id).collectAsState(initial = emptyList())
    val attrs by vm.attrsFlow(g.id).collectAsState(initial = emptyList())
    val cover = images.firstOrNull()

    CardBox(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (cover != null && File(cover.path).exists()) {
                AsyncImage(
                    model = File(cover.path),
                    contentDescription = null,
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
                if (attrs.isNotEmpty()) {
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        attrs.take(4).joinToString(" · ") { "${it.name}${it.value}${it.unit}" },
                        style = Typography2.caption, color = FitTheme.colors.textSub, maxLines = 1
                    )
                }
            }
            Text("›", fontSize = 18.sp, color = FitTheme.colors.outline)
        }
    }
}
