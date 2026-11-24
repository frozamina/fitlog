package com.fitlog.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fitlog.app.data.*
import com.fitlog.app.ui.*
import com.fitlog.app.ui.theme.*
import com.fitlog.app.vm.AppViewModel

@Composable
fun DetailScreen(vm:AppViewModel,garmentId:Long,onBack:()->Unit,onEdit:(Long)->Unit,onStyle:(Long)->Unit={},onCompare:(Long)->Unit={}){
    val garments by vm.garments.collectAsState();val g=garments.find { it.id==garmentId }
    val attrs by vm.attrsFlow(garmentId).collectAsState(initial=emptyList());val images by vm.imagesFlow(garmentId).collectAsState(initial=emptyList())
    val outfits by vm.outfits.collectAsState();val links by vm.outfitPieces.collectAsState();val allImages by vm.imageLibrary.collectAsState()
    var cover by remember { mutableStateOf<String?>(null) };var expanded by remember { mutableStateOf(false) };var showDelete by remember { mutableStateOf(false) }
    StudioSubPage("衣物详情",onBack){
        if(g==null){Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text("正在查找衣物…",style=Typography2.body)};return@StudioSubPage}
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=20.dp)){
            StageReveal(0){GarmentPhoto(g,cover?:images.firstOrNull { it.isCover }?.path?:images.firstOrNull()?.path,Modifier.fillMaxWidth().heightIn(max=420.dp).aspectRatio(1f))}
            if(images.size>1)Row(Modifier.padding(vertical=8.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){images.take(9).forEach { image->Box(Modifier.size(48.dp).clickable{cover=image.path}){GarmentPhoto(g,image.path,Modifier.fillMaxSize())}}}
            StageReveal(1){Column{
                StudioHeading(g.brand.ifBlank { "YOUR PIECE" },g.name,"${g.type} · 标码 ${g.sizeLabel.ifBlank { "未记录" }}")
                if(Styling.slot(g.type)!=Styling.Slot.NONE)Button(onClick={onStyle(g.id)},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text("用这件搭配")}
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){TextButton(onClick={vm.favorite(g.id)}){Icon(if(g.favorite)Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,contentDescription=null);Spacer(Modifier.width(6.dp));Text(if(g.favorite)"已收藏" else "收藏")};TextButton(onClick={onEdit(g.id)}){Text("编辑单品")}}
            }}
            StageReveal(2){Column{
                CardBox{Text("颜色与版型",style=Typography2.sectionTitle);Spacer(Modifier.height(12.dp));Text("主色 ${g.mainColor.ifBlank { "未标注" }} · ${g.pattern}",style=Typography2.body,color=FitTheme.colors.textSub);Text("版型：${g.silhouette}　实穿：${g.fit}",style=Typography2.body,color=FitTheme.colors.textSub)}
                Spacer(Modifier.height(20.dp))
                CardBox{
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("尺码档案",style=Typography2.sectionTitle,modifier=Modifier.weight(1f));TextButton(onClick={onCompare(g.id)}){Text("对比尺码")}}
                    if(attrs.isEmpty())Text("尺寸尚未填写，之后可以补充。",style=Typography2.body,color=FitTheme.colors.textSub)
                    (if(expanded)attrs else attrs.take(3)).forEach { a->Row(Modifier.fillMaxWidth().padding(vertical=10.dp),horizontalArrangement=Arrangement.SpaceBetween){Column(Modifier.weight(1f)){Text(a.name,style=Typography2.body);Text(a.basis.ifBlank { "测量口径未标注" },style=Typography2.caption,color=FitTheme.colors.textSub)};Text("${a.value} ${a.unit}",style=Typography2.body)} }
                    if(attrs.size>3)TextButton(onClick={expanded=!expanded}){Text(if(expanded)"收起尺码" else "查看全部 ${attrs.size} 项")}
                }
                if(g.note.isNotBlank()){Spacer(Modifier.height(20.dp));StudioNote(g.note)}
            }}
            val related=outfits.filter { o->links.any { it.outfitId==o.id&&it.garmentId==g.id } }
            if(related.isNotEmpty()){Spacer(Modifier.height(28.dp));Text("包含它的搭配",style=Typography2.sectionTitle);related.forEach { o->Spacer(Modifier.height(16.dp));OutfitBoard(links.filter { it.outfitId==o.id }.sortedBy { it.sortOrder }.mapNotNull { p->garments.find { it.id==p.garmentId } },allImages,o.anchorId);Text(o.name,style=Typography2.cardTitle) }}
            Row(Modifier.fillMaxWidth().padding(vertical=24.dp),horizontalArrangement=Arrangement.SpaceBetween){TextButton(onClick={vm.duplicate(g.id){onEdit(it)}}){Text("复制为新衣物")};TextButton(onClick={showDelete=true}){Text("删除衣物",color=SemanticError)}}
        }
    }
    if(showDelete)AlertDialog(onDismissRequest={showDelete=false},title={Text("删除这件衣物？")},text={Text("衣物和不再被其他记录引用的照片会被移除。搭配中将不再包含它。")},confirmButton={TextButton(onClick={showDelete=false;vm.deleteGarment(garmentId,onBack)}){Text("删除",color=SemanticError)}},dismissButton={TextButton(onClick={showDelete=false}){Text("取消")}})
}
