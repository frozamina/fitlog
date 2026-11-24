package com.fitlog.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitlog.app.data.GarmentType
import com.fitlog.app.ui.CardBox
import com.fitlog.app.ui.FitTextField
import com.fitlog.app.ui.FitToast
import com.fitlog.app.ui.SectionTitle
import com.fitlog.app.ui.theme.*
import com.fitlog.app.util.BackupManager
import com.fitlog.app.vm.AppViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(vm: AppViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val templates by vm.allTemplates.collectAsState()
    val hasCrashLog = remember { java.io.File(context.filesDir, "crash_log.txt").exists() }
    var toast by remember { mutableStateOf<String?>(null) }
    var addType by remember { mutableStateOf<String?>(null) }
    var newTplName by remember { mutableStateOf("") }
    var expandedType by remember { mutableStateOf<String?>(null) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val data = BackupManager.import(context, uri)
            if (data == null) toast = "导入失败：文件格式不正确"
            else {
                vm.restore(data)
                toast = "导入成功：${data.garments.size} 件衣物"
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.base)
        ) {
            Spacer(Modifier.height(Spacing.s))
            SectionTitle("数据管理")
            CardBox {
                RowItem("📤", "导出备份", "JSON + 图片，可分享保存") {
                    scope.launch {
                        toast = "正在打包…"
                        val file = BackupManager.export(context, vm.dump())
                        BackupManager.share(context, file)
                    }
                }
                Divider(color = FitTheme.colors.line, thickness = 1.dp)
                RowItem("📥", "导入恢复", "选择此前导出的 zip") { importLauncher.launch("application/zip") }
                Divider(color = FitTheme.colors.line, thickness = 1.dp)
                RowItem("🔒", "隐私说明", "所有数据仅存本机，不上传") { }
                Divider(color = FitTheme.colors.line, thickness = 1.dp)
                RowItem("📮", "错误日志", if (hasCrashLog) "点此分享给开发者排查" else "暂无崩溃记录") {
                    val f = java.io.File(context.filesDir, "crash_log.txt")
                    if (f.exists()) {
                        BackupManager.shareFile(context, f, "text/plain")
                    } else toast = "没有错误日志"
                }
            }

            SectionTitle("属性模板")
            CardBox {
                GarmentType.entries.forEachIndexed { i, t ->
                    val list = templates.filter { it.type == t.label }
                    val isOpen = expandedType == t.label
                    if (i > 0) Divider(color = FitTheme.colors.line, thickness = 1.dp)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { expandedType = if (isOpen) null else t.label }
                            .padding(vertical = Spacing.m),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${t.icon} ${t.label}", style = Typography2.cardTitle, color = FitTheme.colors.textMain)
                        Spacer(Modifier.width(Spacing.s))
                        Text("${list.size} 项", style = Typography2.caption, color = FitTheme.colors.textSub)
                        Spacer(Modifier.weight(1f))
                        // 箭头旋转 180°（规范：展开态同步）
                        Text(
                            "⌄", fontSize = 16.sp, color = FitTheme.colors.textSub,
                            modifier = Modifier.rotate(if (isOpen) 180f else 0f)
                        )
                    }
                    AnimatedVisibility(
                        visible = isOpen,
                        enter = fadeIn(Motion.enter(Motion.SHORT4)) + expandVertically(Motion.enter(Motion.SHORT4)),
                        exit = fadeOut(Motion.standard(Motion.SHORT4)) + shrinkVertically(Motion.standard(Motion.SHORT4))
                    ) {
                        Column(Modifier.padding(bottom = Spacing.m)) {
                            Text(
                                list.joinToString(" · ") { it.name },
                                style = Typography2.caption, color = FitTheme.colors.textSub
                            )
                            Spacer(Modifier.height(Spacing.s))
                            OutlinedButton(
                                onClick = { newTplName = ""; addType = t.label },
                                shape = RoundedCornerShape(Radius.field),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary),
                                modifier = Modifier.height(Touch.min)
                            ) { Text("＋ 添加属性", fontSize = 12.sp) }
                        }
                    }
                }
            }

            SectionTitle("其他")
            CardBox {
                RowItem("ℹ️", "关于 FitLog", "v0.3.0") { }
                Divider(color = FitTheme.colors.line, thickness = 1.dp)
                RowItem("🛠", "技术栈", "Kotlin · Compose · Room") { }
                Divider(color = FitTheme.colors.line, thickness = 1.dp)
                RowItem("🎨", "设计系统", "8pt 网格 · M3 动效") { }
            }
            Spacer(Modifier.height(90.dp))
        }

        Box(
            Modifier.fillMaxSize().padding(bottom = 120.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            FitToast(message = toast ?: "", visible = toast != null)
        }
    }

    addType?.let { type ->
        AlertDialog(
            onDismissRequest = { addType = null },
            shape = RoundedCornerShape(Radius.hero),
            containerColor = FitTheme.colors.surface,
            title = { Text("为「$type」添加属性模板") },
            text = {
                FitTextField(
                    value = newTplName,
                    onValueChange = { newTplName = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "属性名，如 袖长"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newTplName.isNotBlank()) vm.addTemplate(type, newTplName.trim())
                    addType = null
                }) { Text("添加") }
            },
            dismissButton = { TextButton(onClick = { addType = null }) { Text("取消") } }
        )
    }

    toast?.let { msg ->
        LaunchedEffect(msg) {
            delay(2200)
            toast = null
        }
    }
}

@Composable
private fun RowItem(icon: String, title: String, sub: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.m),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 16.sp)
        Spacer(Modifier.width(Spacing.m))
        Column(Modifier.weight(1f)) {
            Text(title, style = Typography2.cardTitle, color = FitTheme.colors.textMain)
            Spacer(Modifier.height(2.dp))
            Text(sub, style = Typography2.caption, color = FitTheme.colors.textSub)
        }
        Text("›", fontSize = 16.sp, color = FitTheme.colors.outline)
    }
}
