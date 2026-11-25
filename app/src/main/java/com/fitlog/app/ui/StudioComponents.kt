package com.fitlog.app.ui

import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.fitlog.app.data.*
import com.fitlog.app.ui.theme.*
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun StudioHeading(eyebrow:String,title:String,description:String){
    Column(Modifier.fillMaxWidth().padding(top=8.dp,bottom=16.dp)){
        if(eyebrow.isNotBlank()){
            Text(eyebrow,style=Typography2.caption,color=FitTheme.colors.textSub)
            Spacer(Modifier.height(6.dp))
        }
        Text(title,style=Typography2.pageTitle,color=FitTheme.colors.textMain,modifier=Modifier.semantics { heading() })
        if(description.isNotBlank()){Spacer(Modifier.height(8.dp));Text(description,style=Typography2.body,color=FitTheme.colors.textSub)}
    }
}
/**
 * 入场动画已移除：LazyColumn 滑动时每个新组合的条目都会触发
 * 「延迟+淡入+上滑」动画，导致连续掉帧（实测约 1/3 帧被丢弃）。
 * 保留函数与调用点，内容直接渲染，保证所有列表页滑动流畅。
 */
@Composable
fun StageReveal(index:Int,content:@Composable ()->Unit){
    content()
}
fun garmentColor(g:Garment):Color?=Styling.hex(g.mainColor)?.let { Color(android.graphics.Color.parseColor(it)) }
@Composable
fun GarmentPhoto(g:Garment,path:String?,modifier:Modifier=Modifier){
    val context=LocalContext.current
    Box(modifier.background(FitTheme.colors.surfaceVariant),contentAlignment=Alignment.Center){
        if(path!=null&&File(path).isFile())AsyncImage(
            model=coil.request.ImageRequest.Builder(context).data(File(path))
                // 网格卡片限定 400px 解码 + RGB_565，滑动进入视口时避免大纹理解码上传
                .size(400).bitmapConfig(android.graphics.Bitmap.Config.RGB_565).build(),
            contentDescription=g.name,modifier=Modifier.fillMaxSize(),contentScale=ContentScale.Fit
        )
        else Column(horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Outlined.Checkroom,contentDescription=null,tint=garmentColor(g)?:FitTheme.colors.textSub,modifier=Modifier.size(40.dp));Spacer(Modifier.height(12.dp));Text(g.type,style=Typography2.caption,color=FitTheme.colors.textSub)}
    }
}
@Composable
fun GarmentTile(g:Garment,photo:String?,onOpen:()->Unit,onStyle:(()->Unit)?=null){
    Column(Modifier.fillMaxWidth()){
        Surface(onClick=onOpen,shape=RoundedCornerShape(12.dp),color=FitTheme.colors.surface){GarmentPhoto(g,photo,Modifier.fillMaxWidth().aspectRatio(.85f))}
        Spacer(Modifier.height(12.dp));Text(g.brand.ifBlank { g.type },style=Typography2.caption,color=FitTheme.colors.textSub)
        Text(g.name,style=Typography2.cardTitle,color=FitTheme.colors.textMain,maxLines=2)
        Row(verticalAlignment=Alignment.CenterVertically){garmentColor(g)?.let { Box(Modifier.padding(end=6.dp).size(12.dp).background(it,RoundedCornerShape(6.dp)).border(1.dp,FitTheme.colors.line,RoundedCornerShape(6.dp))) };Text(g.sizeLabel.ifBlank { "未记录标码" },style=Typography2.caption,color=FitTheme.colors.textSub)}
        if(onStyle!=null)TextButton(onClick=onStyle,contentPadding=PaddingValues(0.dp),modifier=Modifier.heightIn(min=40.dp)){Text("用这件搭配",color=MaterialTheme.colorScheme.primary)}
    }
}
@Composable
fun OutfitBoard(pieces:List<Garment>,images:List<GarmentImage>,anchorId:Long?){
    Surface(shape=RoundedCornerShape(16.dp),color=FitTheme.colors.surface){
        Column(Modifier.padding(16.dp)){
            if(pieces.isEmpty())Text("部分衣物已移除，编辑时可重新选择。",style=Typography2.body,color=FitTheme.colors.textSub)
            pieces.chunked(3).forEach { row -> Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
                row.forEach { g -> Column(Modifier.weight(1f)){
                    GarmentPhoto(g,images.firstOrNull { it.garmentId==g.id&&it.isCover }?.path?:images.firstOrNull { it.garmentId==g.id }?.path,Modifier.fillMaxWidth().aspectRatio(.8f))
                    Spacer(Modifier.height(8.dp));Text(g.name,style=Typography2.caption,color=FitTheme.colors.textMain,maxLines=2)
                    if(g.id==anchorId)Text("起搭单品",style=Typography2.caption,color=MaterialTheme.colorScheme.primary)
                } };repeat(3-row.size){Spacer(Modifier.weight(1f))}
            };Spacer(Modifier.height(12.dp)) }
        }
    }
}
@Composable
fun StudioNote(text:String){Surface(color=FitTheme.colors.surfaceVariant,shape=RoundedCornerShape(8.dp),modifier=Modifier.fillMaxWidth()){Text(text,style=Typography2.caption,color=FitTheme.colors.textSub,modifier=Modifier.padding(16.dp))}}
