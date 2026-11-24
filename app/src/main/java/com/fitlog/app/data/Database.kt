package com.fitlog.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [BodyProfile::class, Garment::class, GarmentAttr::class, GarmentImage::class, AttrTemplate::class],
    version = 1,
    exportSchema = false
)
abstract class FitLogDatabase : RoomDatabase() {
    abstract fun bodyProfileDao(): BodyProfileDao
    abstract fun garmentDao(): GarmentDao
    abstract fun attrDao(): GarmentAttrDao
    abstract fun imageDao(): GarmentImageDao
    abstract fun templateDao(): AttrTemplateDao

    companion object {
        @Volatile
        private var INSTANCE: FitLogDatabase? = null

        @Suppress("unused")
        fun get(context: Context): FitLogDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FitLogDatabase::class.java,
                    "fitlog.db"
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        seedTemplates(db)
                    }

                    override fun onOpen(db: SupportSQLiteDatabase) {
                        // 每次打开补种缺失的默认模板（兼容升级后新增的衣物类型）
                        seedTemplates(db)
                    }
                }).build().also { INSTANCE = it }
            }

        /** 预置各类型的默认属性模板（幂等，直接走 SQL） */
        private fun seedTemplates(db: SupportSQLiteDatabase) {
            GarmentType.entries.forEach { type ->
                type.defaults.forEachIndexed { i, name ->
                    db.execSQL(
                        "INSERT OR IGNORE INTO attr_template (type, name, isDefault, sortOrder) VALUES (?, ?, 1, ?)",
                        arrayOf(type.label, name, i)
                    )
                }
            }
        }
    }
}
