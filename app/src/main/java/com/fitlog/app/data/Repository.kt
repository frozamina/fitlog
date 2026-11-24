package com.fitlog.app.data

import android.content.Context
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.io.File

class Repository(context: Context) {
    private val db = FitLogDatabase.get(context)
    private val bodyDao = db.bodyProfileDao()
    private val garmentDao = db.garmentDao()
    private val attrDao = db.attrDao()
    private val imageDao = db.imageDao()
    private val templateDao = db.templateDao()
    private val outfitDao = db.outfitDao()
    fun bodyHistory(): Flow<List<BodyProfile>> = bodyDao.observeAll()
    suspend fun latestBody(): BodyProfile? = bodyDao.latest()
    suspend fun saveBody(p: BodyProfile) = bodyDao.insert(p)
    suspend fun deleteBody(id: Long) = bodyDao.delete(id)
    fun garments() = garmentDao.observeAll()
    suspend fun garment(id: Long) = garmentDao.get(id)
    fun attrs(garmentId: Long) = attrDao.observeByGarment(garmentId)
    fun images(garmentId: Long) = imageDao.observeByGarment(garmentId)
    fun allImages() = imageDao.observeAllRaw()
    fun allAttrs() = attrDao.observeAllRaw()
    suspend fun loadOutfit(id:Long):Pair<Outfit,List<OutfitPiece>>?=db.withTransaction { outfitDao.get(id)?.let { it to outfitDao.pieces(id) } }
    fun outfits() = outfitDao.observeAll()
    fun outfitPieces() = outfitDao.observePieces()
    suspend fun favorite(id: Long) { val g=garmentDao.get(id)?:error("衣物已不存在");garmentDao.update(g.copy(favorite=!g.favorite)) }

    suspend fun saveGarment(garment: Garment, attrs: List<GarmentAttr>, images: List<GarmentImage>): Long {
        require(garment.name.isNotBlank()) { "请填写衣物名称" }
        require(garment.mainColor.isBlank() || Styling.hex(garment.mainColor)!=null) { "主色请填写 #RRGGBB 格式，或暂时留空" }
        val oldPaths=if(garment.id>0)imageDao.listByGarment(garment.id).map { it.path } else emptyList()
        val id=db.withTransaction {
            val id = if (garment.id == 0L) garmentDao.insert(garment.copy(name=garment.name.trim())) else {
                checkNotNull(garmentDao.get(garment.id)) { "这件衣物已不存在，请返回衣橱" }
                garmentDao.update(garment.copy(name=garment.name.trim(),updatedAt=System.currentTimeMillis()));garment.id
            }
            attrDao.deleteByGarment(id)
            attrs.filter { it.name.isNotBlank() && it.value.isNotBlank() }.forEachIndexed { n,a -> attrDao.insert(a.copy(id=0,garmentId=id,sortOrder=n)) }
            imageDao.deleteByGarment(id)
            images.forEachIndexed { n,img -> imageDao.insert(img.copy(id=0,garmentId=id,sortOrder=n,isCover=if(images.none { it.isCover }) n==0 else img.isCover)) }
            attrs.filter { it.isCustom && it.name.isNotBlank() }.forEach { templateDao.insertIgnore(AttrTemplate(type=garment.type,name=it.name,isDefault=false,sortOrder=100)) }
            id
        }
        // 复制的衣物可能共享照片，只有最后一个引用移除后才清理文件。
        oldPaths.filter { path -> images.none { it.path==path } }.distinct().forEach { if(imageDao.references(it)==0)File(it).delete() }
        return id
    }
    suspend fun deleteGarment(id: Long): List<String> {
        val paths=imageDao.listByGarment(id).map { it.path }.distinct()
        db.withTransaction { outfitDao.clearAnchor(id);imageDao.deleteByGarment(id);attrDao.deleteByGarment(id);garmentDao.delete(id) }
        return paths.filter { imageDao.references(it)==0 }
    }
    suspend fun duplicateGarment(id: Long): Long? = db.withTransaction {
        val g=garmentDao.get(id)?:return@withTransaction null
        val newId=garmentDao.insert(g.copy(id=0,name=g.name+" 副本",createdAt=System.currentTimeMillis(),updatedAt=System.currentTimeMillis()))
        attrDao.listByGarment(id).forEach { attrDao.insert(it.copy(id=0,garmentId=newId)) }
        imageDao.listByGarment(id).forEach { imageDao.insert(it.copy(id=0,garmentId=newId)) };newId
    }
    suspend fun saveOutfit(outfit: Outfit, ids: List<Long>): Long = db.withTransaction {
        require(outfit.name.trim().isNotEmpty()) { "请给搭配起个名字" }
        val unique=ids.distinct();require(unique.isNotEmpty()&&unique.size<=12){"请选择 1 至 12 件衣物"}
        require(outfit.anchorId==null||outfit.anchorId in unique){"起搭单品必须包含在搭配中"}
        unique.forEach { id -> require(garmentDao.get(id)!=null){"有衣物已不存在，请重新选择"} }
        val old=if(outfit.id>0)checkNotNull(outfitDao.get(outfit.id)){"搭配已不存在"} else null
        val value=outfit.copy(name=outfit.name.trim(),createdAt=old?.createdAt?:outfit.createdAt,updatedAt=System.currentTimeMillis())
        val id=if(old==null)outfitDao.insert(value) else {outfitDao.update(value);value.id}
        outfitDao.clearPieces(id);outfitDao.insertPieces(unique.mapIndexed { n,g -> OutfitPiece(id,g,n) });id
    }
    fun templates(type:String)=templateDao.observeByType(type)
    fun allTemplates()=templateDao.observeAll()
    suspend fun addTemplate(type:String,name:String)=templateDao.insertIgnore(AttrTemplate(type=type,name=name,isDefault=false,sortOrder=100))
    suspend fun removeTemplate(id:Long)=templateDao.delete(id)
    suspend fun dumpAll():BackupData=BackupData(version=2,exportedAt=System.currentTimeMillis(),bodyProfiles=bodyDao.observeAll().first(),garments=garmentDao.observeAll().first(),attrs=attrDao.observeAllRaw().first(),images=imageDao.observeAllRaw().first(),templates=templateDao.observeAll().first(),outfits=outfitDao.observeAll().first(),outfitPieces=outfitDao.observePieces().first())
    /** 追加导入，不覆盖现有档案；所有旧 ID 与搭配关系在同一事务中重建。 */
    suspend fun restore(data: BackupData) = db.withTransaction {
        require(data.version in 1..2){"备份版本不受支持"}
        data.bodyProfiles.forEach { bodyDao.insert(it.copy(id=0)) }
        val garmentIds=data.garments.associate { g -> g.id to garmentDao.insert(g.copy(id=0)) }
        data.attrs.forEach { a -> garmentIds[a.garmentId]?.let { attrDao.insert(a.copy(id=0,garmentId=it)) } }
        data.images.forEach { i -> garmentIds[i.garmentId]?.let { imageDao.insert(i.copy(id=0,garmentId=it)) } }
        data.templates.forEach { templateDao.insertIgnore(it.copy(id=0)) }
        data.outfits.forEach { o ->
            val id=outfitDao.insert(o.copy(id=0,anchorId=garmentIds[o.anchorId]))
            val links=data.outfitPieces.filter { it.outfitId==o.id }.mapNotNull { p -> garmentIds[p.garmentId]?.let { p.copy(outfitId=id,garmentId=it) } }.distinctBy { it.garmentId }
            outfitDao.insertPieces(links)
        }
    }
}
