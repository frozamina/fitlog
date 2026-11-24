package com.fitlog.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.provider.Settings
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.fitlog.app.data.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk=[34],qualifiers="w375dp-h812dp-mdpi")
class StudioUiTest {
    @get:Rule(order=0) val seed=object:ExternalResource(){override fun before(){
        val context=ApplicationProvider.getApplicationContext<Context>();val field=FitLogDatabase::class.java.getDeclaredField("INSTANCE");field.isAccessible=true;(field.get(null) as? FitLogDatabase)?.close();field.set(null,null);context.deleteDatabase("fitlog.db")
        Settings.Global.putFloat(context.contentResolver,Settings.Global.ANIMATOR_DURATION_SCALE,0f)
        runBlocking { val repo=Repository(context)
            listOf(Triple("雾蓝衬衫","上衣","#A9BEC7"),Triple("炭灰长裤","裤子","#555C5B"),Triple("米白帆布鞋","鞋履","#E3E0D5")).forEachIndexed { n,(name,type,color)->
                val bitmap=Bitmap.createBitmap(180,200,Bitmap.Config.ARGB_8888);val canvas=Canvas(bitmap);canvas.drawColor(android.graphics.Color.parseColor("#F0F3EC"));val p=Paint(Paint.ANTI_ALIAS_FLAG);p.color=android.graphics.Color.parseColor(color)
                if(n==0){val path=Path();path.moveTo(60f,30f);path.lineTo(25f,45f);path.lineTo(14f,90f);path.lineTo(45f,100f);path.lineTo(52f,76f);path.lineTo(50f,170f);path.lineTo(130f,170f);path.lineTo(128f,76f);path.lineTo(135f,100f);path.lineTo(166f,90f);path.lineTo(155f,45f);path.lineTo(120f,30f);path.close();canvas.drawPath(path,p)}else if(n==1){canvas.drawRect(50f,30f,130f,70f,p);canvas.drawRect(50f,65f,83f,175f,p);canvas.drawRect(97f,65f,130f,175f,p)}else canvas.drawRoundRect(25f,90f,155f,135f,12f,12f,p)
                val file=File(context.filesDir,"fixture-$n.png");file.outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
                repo.saveGarment(Garment(name=name,type=type,brand=if(n==0)"UNIQLO" else "我的单品",sizeLabel="M",fit="合身",mainColor=color,pattern="纯色",silhouette="常规",createdAt=1000L-n),listOf(GarmentAttr(name=if(n==1)"腰围" else "衣长",value="68",basis="长度")),listOf(GarmentImage(path=file.absolutePath,isCover=true)))
            }
        }
    }}
    @get:Rule(order=1) val compose=createAndroidComposeRule<MainActivity>()
    private fun waitForWardrobe(){compose.waitUntil(15000){compose.onAllNodesWithText("雾蓝衬衫").fetchSemanticsNodes().isNotEmpty()};compose.waitForIdle()}
    private fun capture(name:String){compose.runOnIdle {val v=compose.activity.window.decorView;val b=Bitmap.createBitmap(v.width,v.height,Bitmap.Config.ARGB_8888);v.draw(Canvas(b));val dest=File("build/reports/ui/$name.png");dest.parentFile.mkdirs();dest.outputStream().use{b.compress(Bitmap.CompressFormat.PNG,100,it)}}}
    @Test fun phoneWardrobeAndSavedOutfitFlow(){waitForWardrobe();capture("wardrobe-375");compose.onAllNodesWithText("用这件搭配").onFirst().performScrollTo().performClick();compose.onAllNodes(hasSetTextAction()).onFirst().performScrollTo().performTextInput("蓝灰通勤");compose.onNodeWithText("保存这套搭配").performScrollTo().performClick();compose.waitUntil(15000){compose.onAllNodesWithText("蓝灰通勤").fetchSemanticsNodes().isNotEmpty()};capture("outfits-375")}
    @Test @Config(qualifiers="w768dp-h1024dp-mdpi") fun tabletWardrobe(){waitForWardrobe();capture("wardrobe-768")}
    @Test @Config(qualifiers="w1440dp-h1000dp-mdpi") fun wideWardrobe(){waitForWardrobe();capture("wardrobe-1440")}
}
