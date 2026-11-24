package com.fitlog.app.data

import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import kotlinx.serialization.Serializable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** 身体数据快照（每次保存一条，保留历史） */
@Serializable
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
@Serializable
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
    val fit: String = FitRating.UNRATED.label,
    val note: String = "",
    @ColumnInfo(defaultValue = "''") val mainColor: String = "",
    @ColumnInfo(defaultValue = "'未标注'") val pattern: String = "未标注",
    @ColumnInfo(defaultValue = "'未标注'") val silhouette: String = "未标注",
    @ColumnInfo(defaultValue = "0") val favorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/** 衣物尺码属性（含自定义属性） */
@Serializable
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
    @ColumnInfo(defaultValue = "''") val basis: String = "",
    val isCustom: Boolean = false,
    val sortOrder: Int = 0
)

/** 衣物图片 */
@Serializable
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
@Serializable
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

/** 长期保存的搭配；创建时间仅用于排序，不是穿着日期。 */
@Serializable
@Entity(tableName = "outfit")
data class Outfit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val anchorId: Long? = null,
    val occasion: String = "",
    val tags: String = "",
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "outfit_piece", primaryKeys = ["outfitId", "garmentId"],
    foreignKeys = [ForeignKey(entity = Outfit::class, parentColumns = ["id"], childColumns = ["outfitId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Garment::class, parentColumns = ["id"], childColumns = ["garmentId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("garmentId")])
data class OutfitPiece(val outfitId: Long, val garmentId: Long, val sortOrder: Int = 0)
