package com.fitlog.app.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitlog.app.data.ChatMessage
import com.fitlog.app.data.ChatSession
import com.fitlog.app.ui.FitTextField
import com.fitlog.app.ui.theme.*
import com.fitlog.app.vm.AppViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 配色问答聊天页：SSE 流式回复逐字更新，首字前显示思考指示器；右上角历史对话（长按删除）。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(vm: AppViewModel, onBack: () -> Unit) {
    val messages by vm.chatMessages.collectAsState()
    val busy by vm.aiBusy.collectAsState()
    var input by remember { mutableStateOf("") }
    var useWardrobe by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<ChatSession?>(null) }
    val listState = rememberLazyListState()

    // 新消息或流式内容增长时滚动到底部（reverseLayout 下最新在 index 0）
    LaunchedEffect(messages.size, messages.lastOrNull()?.content?.length) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(0)
    }

    StudioSubPage("配色问答", onBack, actions = {
        IconButton(onClick = { showHistory = true }) {
            Icon(Icons.Outlined.History, contentDescription = "历史对话", tint = FitTheme.colors.textSub)
        }
    }) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (messages.isEmpty()) {
                    Column(
                        Modifier.fillMaxSize().padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("问点什么", style = Typography2.sectionTitle, color = FitTheme.colors.textMain, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                        Text("AI 结合内置配色知识回答，例如：", style = Typography2.caption, color = FitTheme.colors.textSub, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(20.dp))
                        listOf("藏青裤子配什么颜色上衣", "秋冬怎么穿大地色", "黑白灰怎么穿不无聊").forEach { q ->
                            OutlinedButton(
                                onClick = { vm.sendChat(q, useWardrobe) },
                                enabled = !busy,
                                shape = RoundedCornerShape(Radius.field),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp).padding(vertical = 4.dp)
                            ) { Text(q, fontSize = 13.sp) }
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        reverseLayout = true,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(messages.asReversed(), key = { it.time }) { m ->
                            MessageBubble(m, thinking = busy && m == messages.lastOrNull() && !m.isError)
                        }
                    }
                }
            }
            // 衣橱开关 + 输入行
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(FitTheme.colors.surface)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = useWardrobe,
                    onCheckedChange = { useWardrobe = it },
                    enabled = !busy,
                    modifier = Modifier.size(36.dp)
                )
                Text("结合我的衣橱", fontSize = 13.sp, color = FitTheme.colors.textMain)
                Spacer(Modifier.weight(1f))
                Text("开启后 AI 可读取衣物名称与颜色", fontSize = 11.sp, color = FitTheme.colors.textSub)
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(FitTheme.colors.surface)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FitTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = "输入配色问题…"
                )
                Spacer(Modifier.width(8.dp))
                FilledIconButton(
                    onClick = { val t = input; input = ""; vm.sendChat(t, useWardrobe) },
                    enabled = !busy && input.isNotBlank(),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Primary, contentColor = FitTheme.colors.surface),
                    modifier = Modifier.size(40.dp)
                ) { Icon(Icons.Outlined.ArrowUpward, contentDescription = "发送", modifier = Modifier.size(20.dp)) }
            }
        }
    }

    // 历史对话弹层：点击载入，长按删除
    if (showHistory) {
        val sessions by vm.chatSessions.collectAsState()
        ModalBottomSheet(onDismissRequest = { showHistory = false }, containerColor = FitTheme.colors.surface) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 20.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("历史对话", style = Typography2.sectionTitle, color = FitTheme.colors.textMain)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { vm.clearChat(); showHistory = false }, enabled = !busy) {
                        Text("新建对话", fontSize = 13.sp)
                    }
                }
                if (sessions.isEmpty()) {
                    Text(
                        "还没有历史对话",
                        style = Typography2.caption,
                        color = FitTheme.colors.textSub,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)
                    )
                } else {
                    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                        items(sessions, key = { it.id }) { s ->
                            HistoryRow(
                                s,
                                onClick = { vm.loadChatSession(s.id); showHistory = false },
                                onLongClick = { deleteTarget = s }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("点击继续对话 · 长按删除", fontSize = 11.sp, color = FitTheme.colors.textSub, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                }
            }
        }
    }

    // 长按删除确认
    deleteTarget?.let { s ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除这条对话？") },
            text = { Text("「${s.title}」将被删除，无法恢复。") },
            confirmButton = {
                TextButton(onClick = { vm.deleteChatSession(s.id); deleteTarget = null }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("取消") } }
        )
    }
}

/** 历史会话行：标题 + 时间与条数；长按触发删除 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryRow(s: ChatSession, onClick: () -> Unit, onLongClick: () -> Unit) {
    val time = remember(s.updatedAt) { SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(Date(s.updatedAt)) }
    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(s.title, style = Typography2.body, color = FitTheme.colors.textMain, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text("$time · ${s.messages.size} 条", style = Typography2.caption, color = FitTheme.colors.textSub)
        }
    }
}

@Composable
private fun MessageBubble(m: ChatMessage, thinking: Boolean) {
    val isUser = m.role == "user"
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp
            ),
            color = when {
                m.isError -> MaterialTheme.colorScheme.errorContainer
                isUser -> PrimaryLight
                else -> FitTheme.colors.surfaceVariant
            },
            border = if (!isUser && !m.isError) BorderStroke(1.dp, FitTheme.colors.line) else null,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            if (thinking && m.content.isBlank()) {
                ThinkingDots(Modifier.padding(horizontal = 18.dp, vertical = 14.dp))
            } else {
                Text(
                    md(m.content),
                    style = Typography2.body,
                    color = if (m.isError) MaterialTheme.colorScheme.onErrorContainer else FitTheme.colors.textMain,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }
    }
}

/** 思考指示器：三点脉冲动画（AI 首字返回前显示） */
@Composable
private fun ThinkingDots(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "thinking")
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { i ->
            val alpha by transition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = i * 180, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot$i"
            )
            Box(Modifier.size(7.dp).clip(CircleShape).background(Primary.copy(alpha = alpha)))
        }
    }
}

/** 轻量 Markdown 渲染：**加粗** → 半粗体（星号不显示）；列表序号、破折号等保持原样 */
private fun md(text: String): AnnotatedString = buildAnnotatedString {
    text.split("**").forEachIndexed { i, part ->
        if (part.isEmpty()) return@forEachIndexed
        if (i % 2 == 1) withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(part) }
        else append(part)
    }
}
