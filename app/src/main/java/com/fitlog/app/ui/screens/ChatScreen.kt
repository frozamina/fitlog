package com.fitlog.app.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitlog.app.data.ChatMessage
import com.fitlog.app.ui.theme.*
import com.fitlog.app.vm.AppViewModel

/** 配色问答聊天页：SSE 流式回复逐字更新，首字前显示思考指示器。 */
@Composable
fun ChatScreen(vm: AppViewModel, onBack: () -> Unit) {
    val messages by vm.chatMessages.collectAsState()
    val busy by vm.aiBusy.collectAsState()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // 新消息或流式内容增长时滚动到底部（reverseLayout 下最新在 index 0）
    LaunchedEffect(messages.size, messages.lastOrNull()?.content?.length) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(0)
    }

    StudioSubPage("配色问答", onBack) {
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
                                onClick = { vm.sendChat(q) },
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
            // 输入行
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(FitTheme.colors.surface)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("输入配色问题…", fontSize = 14.sp) },
                    singleLine = false,
                    maxLines = 3,
                    shape = RoundedCornerShape(Radius.field),
                    modifier = Modifier.weight(1f).heightIn(min = 44.dp)
                )
                Spacer(Modifier.width(8.dp))
                FilledIconButton(
                    onClick = { val t = input; input = ""; vm.sendChat(t) },
                    enabled = !busy && input.isNotBlank(),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Primary, contentColor = FitTheme.colors.surface),
                    modifier = Modifier.size(44.dp)
                ) { Icon(Icons.Outlined.ArrowUpward, contentDescription = "发送") }
            }
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
                    m.content,
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
