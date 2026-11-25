package com.fitlog.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitlog.app.data.*
import com.fitlog.app.ui.*
import com.fitlog.app.ui.theme.*
import com.fitlog.app.vm.AppViewModel

@OptIn(ExperimentalLayoutApi::class,ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(vm:AppViewModel,onOpen:(Long)->Unit,onAdd:()->Unit={},onStyle:(Long)->Unit={},paletteId:String=""){
    val garments by vm.garments.collectAsState();val images by vm.imageLibrary.collectAsState()
    var query by remember { mutableStateOf("") };var category by remember { mutableStateOf("全部") };var favorite by remember { mutableStateOf(false) };var palette by remember(paletteId){mutableStateOf(paletteId)}
    val aiPalettes by vm.aiPalettes.collectAsState()
    val group=StudioPalettes.all.find { it.id==palette } ?: aiPalettes.find { it.id==palette }
    val shown=garments.filter { g -> (category=="全部"||g.type==category)&&(!favorite||g.favorite)&&(query.isBlank()||(g.name+" "+g.brand).contains(query,true))&&(group==null||Styling.matchesPalette(g.mainColor,group.colors)) }
    LazyVerticalGrid(columns=GridCells.Adaptive(156.dp),modifier=Modifier.fillMaxSize(),contentPadding=PaddingValues(start=20.dp,end=20.dp,bottom=88.dp),horizontalArrangement=Arrangement.spacedBy(16.dp),verticalArrangement=Arrangement.spacedBy(20.dp)){
        item(span={GridItemSpan(maxLineSpan)}){StageReveal(0){Column{
            StudioHeading("", "我的衣橱", "先选一件想穿的，再找到它的搭配。")
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text("${garments.size} 件衣物",style=Typography2.caption,color=FitTheme.colors.textSub);Button(onClick=onAdd){Icon(Icons.Outlined.Add,contentDescription=null);Text("录入衣物")}}
        }}}
        item(span={GridItemSpan(maxLineSpan)}){StageReveal(1){Column{
            CompactSearchField(query, { query = it }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp));FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){(listOf("全部")+garments.map { it.type }.distinct()).forEach { t -> FilterChip(colors=FilterChipDefaults.filterChipColors(selectedContainerColor=if(FitTheme.colors.isDark) FitTheme.colors.surfaceVariant else PrimaryLight, selectedLabelColor=if(FitTheme.colors.isDark) FitTheme.colors.textMain else PrimaryDark),selected=t==category,onClick={category=t},label={Text(t,fontSize=13.sp)},modifier=Modifier.height(34.dp)) }}
            Row(verticalAlignment=Alignment.CenterVertically){CompactCheckbox(favorite,{favorite=it});Text("只看收藏",style=Typography2.caption,color=FitTheme.colors.textSub)}
            if(group!=null)StudioNote("灵感色组：${group.name}。只显示主色相近的衣物。")
            if(group!=null)TextButton(onClick={palette=""}){Text("清除色组筛选")}
        }}}
        if(shown.isEmpty())item(span={GridItemSpan(maxLineSpan)}){Column(Modifier.fillMaxWidth().padding(vertical=32.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(if(garments.isEmpty())"从想穿的那件开始" else "没有找到匹配衣物",style=Typography2.sectionTitle);Spacer(Modifier.height(12.dp));Text(if(garments.isEmpty())"照片、名称和类别先记下来，尺码之后再补。" else "可以调整搜索、分类或收藏条件。",style=Typography2.body,color=FitTheme.colors.textSub);Spacer(Modifier.height(20.dp));if(garments.isEmpty())Button(onClick=onAdd){Text("录入第一件衣物")}}}
        items(shown,key={it.id}){g->StageReveal(2){GarmentTile(g,images.firstOrNull { it.garmentId==g.id&&it.isCover }?.path?:images.firstOrNull { it.garmentId==g.id }?.path,onOpen={onOpen(g.id)},onStyle=if(Styling.slot(g.type)!=Styling.Slot.NONE){{onStyle(g.id)}}else null)}}
    }
}
