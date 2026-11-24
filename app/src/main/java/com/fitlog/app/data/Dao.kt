package com.fitlog.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyProfileDao {
    @Query("SELECT * FROM body_profile ORDER BY date DESC")
    fun observeAll(): Flow<List<BodyProfile>>

    @Query("SELECT * FROM body_profile ORDER BY date DESC LIMIT 1")
    suspend fun latest(): BodyProfile?

    @Insert
    suspend fun insert(profile: BodyProfile): Long

    @Query("DELETE FROM body_profile WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface GarmentDao {
    @Query("SELECT * FROM garment ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Garment>>

    @Query("SELECT * FROM garment WHERE id = :id")
    suspend fun get(id: Long): Garment?

    @Insert
    suspend fun insert(garment: Garment): Long

    @Update
    suspend fun update(garment: Garment)

    @Query("DELETE FROM garment WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM garment")
    fun countFlow(): Flow<Int>
}

@Dao
interface GarmentAttrDao {
    @Query("SELECT * FROM garment_attr WHERE garmentId = :garmentId ORDER BY sortOrder, id")
    fun observeByGarment(garmentId: Long): Flow<List<GarmentAttr>>

    @Query("SELECT * FROM garment_attr WHERE garmentId = :garmentId ORDER BY sortOrder, id")
    suspend fun listByGarment(garmentId: Long): List<GarmentAttr>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attr: GarmentAttr): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(attrs: List<GarmentAttr>)

    @Query("DELETE FROM garment_attr WHERE garmentId = :garmentId")
    suspend fun deleteByGarment(garmentId: Long)

    @Query("SELECT * FROM garment_attr ORDER BY garmentId, sortOrder, id")
    fun observeAllRaw(): Flow<List<GarmentAttr>>
}

@Dao
interface GarmentImageDao {
    @Query("SELECT * FROM garment_image WHERE garmentId = :garmentId ORDER BY isCover DESC, sortOrder, id")
    fun observeByGarment(garmentId: Long): Flow<List<GarmentImage>>

    @Query("SELECT * FROM garment_image WHERE garmentId = :garmentId ORDER BY isCover DESC, sortOrder, id")
    suspend fun listByGarment(garmentId: Long): List<GarmentImage>

    @Insert
    suspend fun insert(image: GarmentImage): Long

    @Update
    suspend fun update(image: GarmentImage)

    @Query("DELETE FROM garment_image WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM garment_image WHERE garmentId = :garmentId")
    suspend fun deleteByGarment(garmentId: Long)

    @Query("UPDATE garment_image SET isCover = 0 WHERE garmentId = :garmentId")
    suspend fun clearCover(garmentId: Long)

    @Query("SELECT * FROM garment_image ORDER BY garmentId, sortOrder, id")
    fun observeAllRaw(): Flow<List<GarmentImage>>
}

@Dao
interface AttrTemplateDao {
    @Query("SELECT * FROM attr_template WHERE type = :type ORDER BY sortOrder, id")
    fun observeByType(type: String): Flow<List<AttrTemplate>>

    @Query("SELECT * FROM attr_template ORDER BY type, sortOrder, id")
    fun observeAll(): Flow<List<AttrTemplate>>

    @Query("SELECT * FROM attr_template WHERE type = :type ORDER BY sortOrder, id")
    suspend fun listByType(type: String): List<AttrTemplate>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(t: AttrTemplate)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(t: AttrTemplate)

    @Query("DELETE FROM attr_template WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM attr_template")
    suspend fun clear()
}
