package com.fitlog.app

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.fitlog.app.data.*
import com.fitlog.app.util.BackupManager
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.zip.ZipFile

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
class NativeIntegrationTest {
    private val context:Context get()=ApplicationProvider.getApplicationContext()
    @Before fun reset(){closeDatabase();context.deleteDatabase("fitlog.db")}
    @After fun close(){closeDatabase()}
    private fun closeDatabase(){val field=FitLogDatabase::class.java.getDeclaredField("INSTANCE");field.isAccessible=true;(field.get(null) as? FitLogDatabase)?.close();field.set(null,null)}
    @Test fun migratesVersionOneWithoutDroppingClothesOrMeasurements()=runBlocking{
        val file=context.getDatabasePath("fitlog.db");file.parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file,null).use{old->
            old.execSQL("CREATE TABLE body_profile (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,date INTEGER NOT NULL,height REAL,weight REAL,chest REAL,waist REAL,hip REAL,shoulder REAL)")
            old.execSQL("CREATE TABLE garment (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,name TEXT NOT NULL,type TEXT NOT NULL,sizeLabel TEXT NOT NULL,brand TEXT NOT NULL,purchaseDate INTEGER,price TEXT NOT NULL,channel TEXT NOT NULL,fit TEXT NOT NULL,note TEXT NOT NULL,createdAt INTEGER NOT NULL,updatedAt INTEGER NOT NULL)")
            old.execSQL("CREATE TABLE garment_attr (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,garmentId INTEGER NOT NULL,name TEXT NOT NULL,value TEXT NOT NULL,unit TEXT NOT NULL,isCustom INTEGER NOT NULL,sortOrder INTEGER NOT NULL)")
            old.execSQL("CREATE INDEX index_garment_attr_garmentId ON garment_attr(garmentId)")
            old.execSQL("CREATE TABLE garment_image (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,garmentId INTEGER NOT NULL,path TEXT NOT NULL,isCover INTEGER NOT NULL,sortOrder INTEGER NOT NULL)")
            old.execSQL("CREATE INDEX index_garment_image_garmentId ON garment_image(garmentId)")
            old.execSQL("CREATE TABLE attr_template (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,type TEXT NOT NULL,name TEXT NOT NULL,isDefault INTEGER NOT NULL,sortOrder INTEGER NOT NULL)")
            old.execSQL("CREATE UNIQUE INDEX index_attr_template_type_name ON attr_template(type,name)")
            old.execSQL("INSERT INTO garment VALUES(41,'旧版衬衫','上衣','M','原品牌',NULL,'199','线下','合身','原备注',100,100)")
            old.execSQL("INSERT INTO garment_attr VALUES(1,41,'胸围','106','cm',0,0)")
            old.execSQL("INSERT INTO body_profile VALUES(1,100,175,68,96,78,94,45)")
            old.version=1
        }
        val data=Repository(context).dumpAll();assertEquals("旧版衬衫",data.garments.single().name);assertEquals("106",data.attrs.single().value);assertEquals(1,data.bodyProfiles.size);assertEquals("",data.attrs.single().basis);assertTrue(data.outfits.isEmpty())
    }
    @Test fun outfitsCanBeEditedAndCopiedAndBrokenReferencesAreRejected()=runBlocking{
        val repo=Repository(context);val a=repo.saveGarment(Garment(name="衬衫",type="上衣"),emptyList(),emptyList());val b=repo.saveGarment(Garment(name="长裤",type="裤子"),emptyList(),emptyList())
        val id=repo.saveOutfit(Outfit(name="通勤",anchorId=a),listOf(a,b));repo.saveOutfit(Outfit(id=id,name="通勤调整",anchorId=a),listOf(a,b));assertEquals("通勤调整",repo.loadOutfit(id)!!.first.name)
        val copy=repo.saveOutfit(Outfit(name="新版本",anchorId=a),listOf(a,b));assertNotEquals(id,copy)
        assertTrue(runCatching{repo.saveOutfit(Outfit(name="错误",anchorId=a),listOf(9999))}.isFailure)
        repo.deleteGarment(a);assertNull(repo.loadOutfit(id)!!.first.anchorId);assertEquals(listOf(b),repo.loadOutfit(id)!!.second.map{it.garmentId})
    }
    @Test fun sharedPhotosAndPortableBackupsSurviveRestore()=runBlocking{
        val repo=Repository(context);val photo=File(context.filesDir,"shared.jpg");photo.writeBytes(byteArrayOf(1,2,3,4))
        val id=repo.saveGarment(Garment(name="衬衫",type="上衣"),emptyList(),listOf(GarmentImage(path=photo.absolutePath,isCover=true)))
        val copied=repo.duplicateGarment(id)!!;val outfit=repo.saveOutfit(Outfit(name="收藏",anchorId=copied),listOf(copied))
        val archive=BackupManager.export(context,repo.dumpAll());ZipFile(archive).use{z->assertEquals(1,z.entries().asSequence().count{it.name.startsWith("images/")})}
        val restored=BackupManager.import(context,Uri.fromFile(archive))!!;assertTrue(restored.images.all{File(it.path).exists()&&it.path!=photo.absolutePath})
        repo.restore(restored);val data=repo.dumpAll();assertEquals(4,data.garments.size);assertEquals(2,data.outfits.size);val imported=data.outfits.first{it.id!=outfit};assertTrue(data.outfitPieces.any{it.outfitId==imported.id&&it.garmentId==imported.anchorId})
        assertTrue(repo.deleteGarment(id).isEmpty());assertTrue(photo.exists());assertEquals(listOf(photo.absolutePath),repo.deleteGarment(copied))
    }
}
