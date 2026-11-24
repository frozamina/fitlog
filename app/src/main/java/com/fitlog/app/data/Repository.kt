package com.fitlog.app.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class Repository(context: Context) {
    private val db = FitLogDatabase.get(context)
    private val bodyDao = db.bodyProfileDao()
    private val garmentDao = db.garmentDao()
    private val attrDao = db.attrDao()
    private val imageDao = db.imageDao()
    private val templateDao = db.templateDao()

    // ---- 身体数据 ----
    fun bodyHistory(): Flow<List<BodyProfile>> = bodyDao.observeAll()
    suspend fun latestBody(): BodyProfile? = bodyDao.latest()
    suspend fun saveBody(p: BodyProfile) = bodyDao.insert(p)
    suspend fun deleteBody(id: Long) = bodyDao.delete(id)

    // ---- 衣物 ----
    fun garments(): Flow<List<Garment>> = garmentDao.observeAll()
    suspend fun garment(id: Long): Garment? = garmentDao.get(id)
    fun attrs(garmentId: Long): Flow<List<GarmentAttr>> = attrDao.observeByGarment(garmentId)
    fun images(garmentId: Long): Flow<List<GarmentImage>> = imageDao.observeByGarment(garmentId)

    /** 保存衣物（新增或更新），连同属性一起 */
    suspend fun saveGarment(garment: Garment, attrs: List<GarmentAttr>, images: List<GarmentImage>): Long {
        val id = if (garment.id == 0L) {
            garmentDao.insert(garment.copy(createdAt = System.currentTimeMillis()))
        } else {
            garmentDao.update(garment.copy(updatedAt = System.currentTimeMillis()))
            garment.id
        }
        attrDao.deleteByGarment(id)
        attrs.filter { it.name.isNotBlank() }.forEachIndexed { i, a ->
            attrDao.insert(a.copy(id = 0, garmentId = id, sortOrder = i))
        }
        // 清理被移除的图片文件交给调用方处理，这里仅更新数据库
        imageDao.deleteByGarment(id)
        images.forEachIndexed { i, img ->
            imageDao.insert(img.copy(id = 0, garmentId = id, sortOrder = i))
        }
        // 把用到的自定义属性记为模板
        attrs.filter { it.isCustom && it.name.isNotBlank() }.forEach { a ->
            templateDao.insertIgnore(
                AttrTemplate(type = garment.type, name = a.name, isDefault = false, sortOrder = 100)
            )
        }
        return id
    }

    suspend fun deleteGarment(id: Long): List<String> {
        val paths = imageDao.listByGarment(id).map { it.path }
        imageDao.deleteByGarment(id)
        attrDao.deleteByGarment(id)
        garmentDao.delete(id)
        return paths
    }

    suspend fun duplicateGarment(id: Long): Long? {
        val g = garmentDao.get(id) ?: return null
        val newId = garmentDao.insert(g.copy(id = 0, name = g.name + " 副本", createdAt = System.currentTimeMillis()))
        attrDao.listByGarment(id).forEachIndexed { i, a ->
            attrDao.insert(a.copy(id = 0, garmentId = newId, sortOrder = i))
        }
        imageDao.listByGarment(id).forEachIndexed { i, img ->
            imageDao.insert(img.copy(id = 0, garmentId = newId, sortOrder = i))
        }
        return newId
    }

    // ---- 模板 ----
    fun templates(type: String): Flow<List<AttrTemplate>> = templateDao.observeByType(type)
    fun allTemplates(): Flow<List<AttrTemplate>> = templateDao.observeAll()
    suspend fun addTemplate(type: String, name: String) =
        templateDao.insertIgnore(AttrTemplate(type = type, name = name, isDefault = false, sortOrder = 100))
    suspend fun removeTemplate(id: Long) = templateDao.delete(id)

    // ---- 备份导出 ----
    suspend fun dumpAll(): BackupData {
        return BackupData(
            version = 1,
            exportedAt = System.currentTimeMillis(),
            bodyProfiles = bodyDao.observeAll().first(),
            garments = garmentDao.observeAll().first(),
            attrs = attrDao.observeAllRaw().first(),
            images = imageDao.observeAllRaw().first(),
            templates = templateDao.observeAll().first()
        )
    }

    suspend fun restore(data: BackupData) {
        data.bodyProfiles.forEach { bodyDao.insert(it.copy(id = 0)) }
        val idMap = mutableMapOf<Long, Long>()
        data.garments.forEach { g ->
            val old = g.id
            val newId = garmentDao.insert(g.copy(id = 0))
            idMap[old] = newId
        }
        data.attrs.forEach { a ->
            val newGid = idMap[a.garmentId] ?: return@forEach
            attrDao.insert(a.copy(id = 0, garmentId = newGid))
        }
        data.images.forEach { img ->
            val newGid = idMap[img.garmentId] ?: return@forEach
            imageDao.insert(img.copy(id = 0, garmentId = newGid))
        }
        data.templates.forEach { t -> templateDao.insertIgnore(t.copy(id = 0)) }
    }
}
