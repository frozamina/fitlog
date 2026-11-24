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
    Column(Modifier.fillMaxWidth().padding(top=20.dp,bottom=24.dp)){
        if(eyebrow.isNotBlank()){
            Text(eyebrow,style=Typography2.caption,color=FitTheme.colors.textSub)
            Spacer(Modifier.height(8.dp))
        }
        Text(title,style=Typography2.pageTitle,color=FitTheme.colors.textMain,modifier=Modifier.semantics { heading() })
        if(description.isNotBlank()){Spacer(Modifier.height(10.dp));Text(description,style=Typography2.body,color=FitTheme.colors.textSub)}
    }
}
@Composable
fun StageReveal(index:Int,content:@Composable ()->Unit){
    val context=LocalContext.current
    val animate=remember { runCatching { Settings.Global.getFloat(context.contentResolver,Settings.Global.ANIMATOR_DURATION_SCALE,1f)>0f }.getOrDefault(true) }
    var visible by remember { mutableStateOf(!animate) }
    LaunchedEffect(Unit){if(animate)delay((index.coerceAtMost(4)*80).toLong());visible=true}
    AnimatedVisibility(visible,enter=fadeIn(tween(220))+slideInVertically(tween(300)){it/12}){content()}
}
fun garmentColor(g:Garment):Color?=Styling.hex(g.mainColor)?.let { Color(android.graphics.Color.parseColor(it)) }
@Composable
fun GarmentPhoto(g:Garment,path:String?,modifier:Modifier=Modifier){
    Box(modifier.background(FitTheme.colors.surfaceVariant),contentAlignment=Alignment.Center){
        if(path!=null&&File(path).isFile())AsyncImage(File(path),contentDescription=g.name,modifier=Modifier.fillMaxSize(),contentScale=ContentScale.Fit)
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
        if(onStyle!=null)TextButton(onClick=onStyle,contentPadding=PaddingValues(0.dp),modifier=Modifier.heightIn(min=44.dp)){Text("用这件搭配",color=MaterialTheme.colorScheme.primary)}
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
