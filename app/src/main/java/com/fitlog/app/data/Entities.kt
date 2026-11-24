package com.fitlog.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** 身体数据快照（每次保存一条，保留历史） */
@Entity(tableName = "body_profile")
data class BodyProfile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long = System.currentTimeMillis(),
    val height: Float? = null,
    val weight: Float? = null,
    val chest: Float? = null,
    val waist: Float? = null,
    val hip: Float? = null,
    val shoulder: Float? = null
)

/** 衣物 */
@Entity(tableName = "garment")
data class Garment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    val sizeLabel: String = "",
    val brand: String = "",
    val purchaseDate: Long? = null,
    val price: String = "",
    val channel: String = "",
    val fit: String = FitRating.FIT.label,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/** 衣物尺码属性（含自定义属性） */
@Entity(
    tableName = "garment_attr",
    indices = [Index("garmentId")]
)
data class GarmentAttr(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val garmentId: Long = 0,
    val name: String,
    val value: String,
    val unit: String = "cm",
    val isCustom: Boolean = false,
    val sortOrder: Int = 0
)

/** 衣物图片 */
@Entity(
    tableName = "garment_image",
    indices = [Index("garmentId")]
)
data class GarmentImage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val garmentId: Long = 0,
    val path: String,
    val isCover: Boolean = false,
    val sortOrder: Int = 0
)

/** 属性模板：某类型下默认带出的属性名 */
@Entity(
    tableName = "attr_template",
    indices = [Index(value = ["type", "name"], unique = true)]
)
data class AttrTemplate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val name: String,
    val isDefault: Boolean = false,
    val sortOrder: Int = 0
)
