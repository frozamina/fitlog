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
        val id = repo.saveGarment(garment, attrs, images)
        done(id)
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
}
