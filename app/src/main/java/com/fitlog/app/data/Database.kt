package com.fitlog.app.data

import android.content.Context
import androidx.room.migration.Migration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [BodyProfile::class, Garment::class, GarmentAttr::class, GarmentImage::class, AttrTemplate::class, Outfit::class, OutfitPiece::class],
    version = 2,
    exportSchema = false
)
abstract class FitLogDatabase : RoomDatabase() {
    abstract fun outfitDao(): OutfitDao
    abstract fun bodyProfileDao(): BodyProfileDao
    abstract fun garmentDao(): GarmentDao
    abstract fun attrDao(): GarmentAttrDao
    abstract fun imageDao(): GarmentImageDao
    abstract fun templateDao(): AttrTemplateDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE garment ADD COLUMN mainColor TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE garment ADD COLUMN pattern TEXT NOT NULL DEFAULT '未标注'")
                db.execSQL("ALTER TABLE garment ADD COLUMN silhouette TEXT NOT NULL DEFAULT '未标注'")
                db.execSQL("ALTER TABLE garment ADD COLUMN favorite INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE garment_attr ADD COLUMN basis TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE TABLE IF NOT EXISTS outfit (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, anchorId INTEGER, occasion TEXT NOT NULL, tags TEXT NOT NULL, note TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS outfit_piece (outfitId INTEGER NOT NULL, garmentId INTEGER NOT NULL, sortOrder INTEGER NOT NULL, PRIMARY KEY(outfitId, garmentId), FOREIGN KEY(outfitId) REFERENCES outfit(id) ON DELETE CASCADE, FOREIGN KEY(garmentId) REFERENCES garment(id) ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_outfit_piece_garmentId ON outfit_piece (garmentId)")
            }
        }
        @Volatile
        private var INSTANCE: FitLogDatabase? = null

        @Suppress("unused")
        fun get(context: Context): FitLogDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FitLogDatabase::class.java,
                    "fitlog.db"
                ).addMigrations(MIGRATION_1_2).addCallback(object : Callback() {
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
