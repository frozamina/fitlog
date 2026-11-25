package com.fitlog.app.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fitlog.app.data.BodyProfile
import com.fitlog.app.data.Garment
import com.fitlog.app.data.GarmentAttr
import com.fitlog.app.data.GarmentImage
import com.fitlog.app.data.Repository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository(app.applicationContext)

    val garments: StateFlow<List<Garment>> =
        repo.garments().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bodyHistory: StateFlow<List<BodyProfile>> =
        repo.bodyHistory().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val imageLibrary = repo.allImages().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val attributeLibrary = repo.allAttrs().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    suspend fun loadOutfit(id:Long)=repo.loadOutfit(id)
    val outfits = repo.outfits().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val outfitPieces = repo.outfitPieces().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val errors = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val saving = kotlinx.coroutines.flow.MutableStateFlow(false)
    fun clearError() { errors.value = null }
    fun favorite(id: Long) = viewModelScope.launch { runCatching { repo.favorite(id) }.onFailure { errors.value=it.message } }
    fun saveOutfit(outfit: com.fitlog.app.data.Outfit, ids: List<Long>, done: (Long)->Unit) = viewModelScope.launch {
        if(saving.value)return@launch
        saving.value=true
        try { done(repo.saveOutfit(outfit,ids)) } catch(e:Exception){errors.value=e.message?:"保存失败，请重试"} finally{saving.value=false}
    }
    val allTemplates =
        repo.allTemplates().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun attrsFlow(garmentId: Long): Flow<List<GarmentAttr>> = repo.attrs(garmentId)
    fun imagesFlow(garmentId: Long): Flow<List<GarmentImage>> = repo.images(garmentId)
    suspend fun templatesOf(type: String) = repo.templates(type)
    suspend fun garment(id: Long) = repo.garment(id)

    fun saveBody(p: BodyProfile, done: (Long) -> Unit = {}) = viewModelScope.launch {
        val id = repo.saveBody(p)
        done(id)
    }

    fun deleteBody(id: Long) = viewModelScope.launch { repo.deleteBody(id) }

    fun saveGarment(
        garment: Garment,
        attrs: List<GarmentAttr>,
        images: List<GarmentImage>,
        done: (Long) -> Unit = {}
    ) = viewModelScope.launch {
        if(saving.value)return@launch
        saving.value=true
        try { done(repo.saveGarment(garment,attrs,images)) } catch(e:Exception){errors.value=e.message?:"保存失败，请重试"} finally{saving.value=false}
    }

    fun deleteGarment(id: Long, done: () -> Unit = {}) = viewModelScope.launch {
        repo.deleteGarment(id).forEach { java.io.File(it).delete() }
        done()
    }

    fun duplicate(id: Long, done: (Long) -> Unit) = viewModelScope.launch {
        val newId = repo.duplicateGarment(id)
        if (newId != null) done(newId)
    }

    fun addTemplate(type: String, name: String) = viewModelScope.launch { repo.addTemplate(type, name) }
    fun removeTemplate(id: Long) = viewModelScope.launch { repo.removeTemplate(id) }

    suspend fun dump() = repo.dumpAll()
    suspend fun restore(data: com.fitlog.app.data.BackupData) = repo.restore(data)

    // ---- AI（DeepSeek 配色灵感 / 问答） ----
    private val ai = com.fitlog.app.data.AiRepository(app.applicationContext)
    val apiKeyState = kotlinx.coroutines.flow.MutableStateFlow(ai.hasKey())
    val apiKeyMask = kotlinx.coroutines.flow.MutableStateFlow(ai.mask())
    val aiPalettes = kotlinx.coroutines.flow.MutableStateFlow<List<com.fitlog.app.ui.screens.StudioPalette>>(emptyList())
    val aiBusy = kotlinx.coroutines.flow.MutableStateFlow(false)
    val chatMessages = kotlinx.coroutines.flow.MutableStateFlow<List<com.fitlog.app.data.ChatMessage>>(emptyList())

    fun saveApiKey(key: String) {
        ai.saveKey(key)
        apiKeyState.value = ai.hasKey()
        apiKeyMask.value = ai.mask()
    }

    fun clearApiKey() {
        ai.clearKey()
        apiKeyState.value = false
        apiKeyMask.value = ai.mask()
    }

    /** AI 生成配色色板（jsonMode，失败走 toast 错误，硬编码色板不受影响） */
    fun generatePalette(keyword: String, useWardrobe: Boolean) = viewModelScope.launch {
        if (aiBusy.value) return@launch
        val kw = keyword.trim()
        if (kw.isEmpty()) return@launch
        if (!ai.hasKey()) { errors.value = "请先在「我的 → AI 功能」中配置 API key"; return@launch }
        aiBusy.value = true
        try {
            val digest = if (useWardrobe) com.fitlog.app.data.AiRepository.buildDigest(garments.value) else null
            val content = com.fitlog.app.data.DeepSeekClient.chat(
                apiKey = ai.key(), messages = ai.paletteMessages(kw, digest),
                jsonMode = true, maxTokens = 300, temperature = 1.0
            )
            val p = com.fitlog.app.data.AiRepository.parsePalette(content)
                ?: throw com.fitlog.app.data.DeepSeekClient.DeepSeekException("AI 返回的配色无法解析，请重试")
            aiPalettes.value = listOf(
                com.fitlog.app.ui.screens.StudioPalette("ai:${System.currentTimeMillis()}", p.name, p.description, p.colors)
            ) + aiPalettes.value
        } catch (e: Exception) {
            errors.value = e.message ?: "AI 暂时不可用"
        } finally { aiBusy.value = false }
    }

    /** 配色问答（SSE 流式：AI 消息逐字更新，首字前 UI 显示思考指示器）；useWardrobe=true 时注入衣橱明细 */
    fun sendChat(text: String, useWardrobe: Boolean = false) {
        val msg = text.trim()
        if (msg.isEmpty() || aiBusy.value) return
        if (!ai.hasKey()) {
            chatMessages.value = chatMessages.value + com.fitlog.app.data.ChatMessage(
                "assistant", "请先在「我的 → AI 功能」中配置 DeepSeek API key，再开始配色问答。", System.currentTimeMillis(), isError = true
            )
            return
        }
        val now = System.currentTimeMillis()
        chatMessages.value = (chatMessages.value + com.fitlog.app.data.ChatMessage("user", msg, now)).takeLast(50)
        aiBusy.value = true
        viewModelScope.launch {
            val history = chatMessages.value.filter { !it.isError }.dropLast(1) // 最后一条是刚发的 user 消息
            val placeholder = com.fitlog.app.data.ChatMessage("assistant", "", now + 1)
            chatMessages.value = chatMessages.value + placeholder
            try {
                val wardrobe = if (useWardrobe) com.fitlog.app.data.AiRepository.buildWardrobeDetail(garments.value) else null
                val msgs = ai.chatMessages(msg, history, wardrobe)
                val reply = com.fitlog.app.data.DeepSeekClient.chatStream(
                    apiKey = ai.key(), messages = msgs, maxTokens = 500
                ) { delta ->
                    val list = chatMessages.value.toMutableList()
                    if (list.isNotEmpty() && list.last().time == placeholder.time) {
                        list[list.lastIndex] = list.last().copy(content = list.last().content + delta)
                        chatMessages.value = list
                    }
                }
                if (reply.isBlank()) throw com.fitlog.app.data.DeepSeekClient.DeepSeekException("AI 未返回内容，请重试")
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                val list = chatMessages.value.toMutableList()
                val err = com.fitlog.app.data.ChatMessage("assistant", e.message ?: "AI 暂时不可用", System.currentTimeMillis(), isError = true)
                if (list.isNotEmpty() && list.last().time == placeholder.time && list.last().content.isBlank()) {
                    list[list.lastIndex] = err
                    chatMessages.value = list
                } else chatMessages.value = chatMessages.value + err
            } finally { aiBusy.value = false }
        }
    }

    fun clearChat() { chatMessages.value = emptyList() }
}
