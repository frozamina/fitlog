package com.fitlog.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitlog.app.data.*
import com.fitlog.app.ui.*
import com.fitlog.app.ui.theme.*
import com.fitlog.app.vm.AppViewModel
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioSubPage(title:String,onBack:()->Unit,content:@Composable ()->Unit){
    Scaffold(containerColor=FitTheme.colors.bg,topBar={TopAppBar(title={Text(title,style=Typography2.cardTitle)},navigationIcon={IconButton(onClick=onBack){Icon(Icons.Outlined.ArrowBack,contentDescription="返回")}},colors=TopAppBarDefaults.topAppBarColors(containerColor=FitTheme.colors.bg))}){p->Box(Modifier.padding(p)){content()}}
}
@Composable
fun OutfitsScreen(vm:AppViewModel,onNew:()->Unit,onEdit:(Long)->Unit,onCopy:(Long)->Unit){
    val outfits by vm.outfits.collectAsState();val links by vm.outfitPieces.collectAsState();val garments by vm.garments.collectAsState();val images by vm.imageLibrary.collectAsState()
    var query by remember { mutableStateOf("") }
    val shown=outfits.filter { (it.name+it.tags+it.occasion).contains(query,true) }
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
        item {StageReveal(0){Column{StudioHeading("","我的搭配","把喜欢的组合留下，下次直接拿来穿。");Button(onClick=onNew){Icon(Icons.Outlined.Add,contentDescription=null);Text("新建搭配")}}}}
        if(outfits.isNotEmpty())item {OutlinedTextField(query,{query=it},placeholder={Text("搜索名称、风格、场合")},singleLine=true,modifier=Modifier.fillMaxWidth())}
        if(shown.isEmpty())item {Column(Modifier.padding(vertical=8.dp)){Text(if(outfits.isEmpty())"还没有保存搭配" else "没有找到匹配的搭配",style=Typography2.sectionTitle);Spacer(Modifier.height(8.dp));Text("先选一件想穿的，其他部分可以慢慢调整。",style=Typography2.body,color=FitTheme.colors.textSub)}}
        items(shown,key={it.id}){o->StageReveal(1){Column{
            val pieces=links.filter { it.outfitId==o.id }.sortedBy { it.sortOrder }.mapNotNull { p->garments.find { it.id==p.garmentId } }
            Box(Modifier.clickable{onEdit(o.id)}){OutfitBoard(pieces,images,o.anchorId)}
            Spacer(Modifier.height(14.dp));Text(o.name,style=Typography2.sectionTitle,color=FitTheme.colors.textMain)
            Text(listOf(o.occasion,o.tags).filter { it.isNotBlank() }.joinToString(" · "),style=Typography2.caption,color=FitTheme.colors.textSub)
            Row{TextButton(onClick={onEdit(o.id)}){Text("编辑搭配")};TextButton(onClick={onCopy(o.id)}){Text("另存新搭配")}}
        }}}
    }
}

@OptIn(ExperimentalLayoutApi::class,ExperimentalMaterial3Api::class)
@Composable
fun OutfitEditorScreen(vm:AppViewModel,initialAnchor:Long,editId:Long,copyId:Long,onBack:()->Unit,onSaved:()->Unit){
    val garments by vm.garments.collectAsState();val images by vm.imageLibrary.collectAsState();val outfits by vm.outfits.collectAsState();val links by vm.outfitPieces.collectAsState();val busy by vm.saving.collectAsState()
    val available=garments.filter { Styling.slot(it.type)!=Styling.Slot.NONE }
    var anchor by remember { mutableStateOf<Long?>(initialAnchor.takeIf { it>0 }) };var selected by remember { mutableStateOf<List<Long>>(emptyList()) }
    var name by remember { mutableStateOf("") };var occasion by remember { mutableStateOf("") };var tags by remember { mutableStateOf("") };var note by remember { mutableStateOf("") }
    var initialized by remember { mutableStateOf(false) };var picker by remember { mutableStateOf<String?>(null) };var pickerQuery by remember { mutableStateOf("") };var replaceId by remember { mutableStateOf<Long?>(null) }
    var asNew by remember { mutableStateOf(false) }
    LaunchedEffect(garments,outfits,links){if(!initialized){
        val loaded=if(editId>0||copyId>0)vm.loadOutfit(editId.takeIf{it>0}?:copyId) else null
        val old=loaded?.first
        if(editId>0||copyId>0){if(old!=null){anchor=old.anchorId;selected=loaded?.second.orEmpty().sortedBy { it.sortOrder }.map { it.garmentId };name=old.name+if(copyId>0)" · 新版本" else "";occasion=old.occasion;tags=old.tags;note=old.note;initialized=true}}
        else if(initialAnchor>0&&garments.any { it.id==initialAnchor }){selected=Styling.suggest(garments,initialAnchor).firstOrNull()?.ids?:listOf(initialAnchor);initialized=true}
        else if(initialAnchor==0L)initialized=true
    }}
    val suggestions=remember(garments,anchor){anchor?.let { Styling.suggest(garments,it) }.orEmpty()}
    fun choose(g:Garment){
        if(picker=="anchor"||anchor==null){anchor=g.id;selected=Styling.suggest(garments,g.id).firstOrNull()?.ids?:listOf(g.id)}
        else if(picker=="replace")selected=selected.map { if(it==replaceId)g.id else it }
        else if(g.id !in selected)selected=selected+g.id
        picker=null;pickerQuery=""
    }
    StudioSubPage(if(editId>0&&!asNew)"调整这套搭配" else "从这一件开始",onBack){
        if(anchor==null&&selected.isEmpty()){
            LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
                item{StudioHeading("","先选一件想穿的","推荐只会使用你已有的衣物。")}
                if(available.isEmpty())item{StudioNote("衣橱还没有可参与搭配的衣物，请先返回录入。")}
                items(available,key={it.id}){g->TextButton(onClick={choose(g)},modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)){Text(g.name,style=Typography2.body)}}
            };return@StudioSubPage
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)){
            StudioHeading("","保留这一件，换出新的可能","搭配不绑定日期，可以反复修改和使用。")
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.Lock,contentDescription=null,tint=MaterialTheme.colorScheme.primary);Spacer(Modifier.width(8.dp));Text(garments.find { it.id==anchor }?.name?:"尚未选择起搭单品",style=Typography2.cardTitle,modifier=Modifier.weight(1f));TextButton(onClick={picker="anchor"}){Text("更换")}}
            if(suggestions.size>1)FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){suggestions.forEachIndexed { n,s -> FilterChip(colors=FilterChipDefaults.filterChipColors(selectedContainerColor=if(FitTheme.colors.isDark) FitTheme.colors.surfaceVariant else PrimaryLight, selectedLabelColor=if(FitTheme.colors.isDark) FitTheme.colors.textMain else PrimaryDark),selected=selected==s.ids,onClick={selected=s.ids},label={Text("方案 ${n+1}")},modifier=Modifier.heightIn(min=44.dp)) }}
            Spacer(Modifier.height(12.dp));val pieces=selected.mapNotNull { id->garments.find { it.id==id } }
            OutfitBoard(pieces,images,anchor)
            selected.forEach { id -> val g=garments.find { it.id==id };Row(Modifier.fillMaxWidth().padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically){Text(g?.name?:"已移除的衣物",style=Typography2.body,modifier=Modifier.weight(1f));if(id==anchor)Text("起搭",style=Typography2.caption,color=MaterialTheme.colorScheme.primary) else {TextButton(onClick={replaceId=id;picker="replace"}){Text("替换")};IconButton(onClick={selected=selected-id}){Icon(Icons.Outlined.Close,contentDescription="移除 ${g?.name.orEmpty()}")}}}}
            OutlinedButton(onClick={picker="add"},enabled=selected.size<12,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Icon(Icons.Outlined.Add,contentDescription=null);Text("添加单品")}
            Spacer(Modifier.height(24.dp));Text("配色思路",style=Typography2.sectionTitle)
            val base=garments.find { it.id==anchor }
            val reasons=pieces.filter { it.id!=anchor }.mapNotNull { g->base?.let { Styling.relation(it,g).reason } }.distinct()
            reasons.forEach { Text(it,style=Typography2.body,color=FitTheme.colors.textSub,modifier=Modifier.padding(top=10.dp)) }
            if(pieces.size<2)Text("加入另一件单品后，就能比较配色关系。",style=Typography2.body,color=FitTheme.colors.textSub)
            suggestions.firstOrNull()?.missing?.takeIf { it.isNotEmpty() }?.let{Text("当前衣橱缺少可选的${it.joinToString("、"){s->s.label}}，可以先保存已有组合。",style=Typography2.caption,color=FitTheme.colors.textSub,modifier=Modifier.padding(top=12.dp))}
            StudioNote("根据颜色与图案提供规则建议，实际效果还取决于版型、材质和你的喜好。")
            Spacer(Modifier.height(24.dp));LabeledField("搭配名称",name,{name=it},"例如：蓝灰通勤")
            LabeledField("适合场合（可选）",occasion,{occasion=it},"日常通勤、朋友聚会…")
            LabeledField("风格标签（可选）",tags,{tags=it},"清爽、简约、春秋")
            LabeledField("搭配备注（可选）",note,{note=it},"领口、塞衣角、想替换的鞋子…")
            Button(onClick={vm.saveOutfit(Outfit(id=if(asNew)0 else editId,name=name,anchorId=anchor,occasion=occasion,tags=tags,note=note),selected){onSaved()}},enabled=!busy,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text(if(busy)"正在保存…" else if(editId>0&&!asNew)"保存修改" else "保存这套搭配")}
            if(editId>0&&!asNew)TextButton(onClick={asNew=true;name+=" · 新版本"},modifier=Modifier.fillMaxWidth().heightIn(min=44.dp)){Text("另存为新搭配")}
            Spacer(Modifier.height(32.dp))
        }
    }
    if(picker!=null){val current=garments.find { it.id==replaceId };val candidates=available.filter { g->(if(picker=="anchor")g.id!=anchor else g.id !in selected)&&(picker!="replace"||current==null||Styling.slot(g.type)==Styling.slot(current.type))&&g.name.contains(pickerQuery,true) }
        AlertDialog(onDismissRequest={picker=null},title={Text(when(picker){"anchor"->"选择起搭单品";"replace"->"替换单品";else->"添加单品"})},text={Column{OutlinedTextField(pickerQuery,{pickerQuery=it},placeholder={Text("搜索可选衣物")},singleLine=true,modifier=Modifier.fillMaxWidth());LazyColumn(Modifier.heightIn(max=360.dp)){if(candidates.isEmpty())item{Text("暂无其他可选衣物。",style=Typography2.body,modifier=Modifier.padding(vertical=24.dp))};items(candidates,key={it.id}){g->TextButton(onClick={choose(g)},modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)){Text(g.name,style=Typography2.body)}}}}},confirmButton={TextButton(onClick={picker=null}){Text("关闭")}})
    }
}

data class StudioPalette(val id:String,val name:String,val description:String,val colors:List<String>)
object StudioPalettes{val all=listOf(StudioPalette("blue","蓝灰之间","柔和的蓝色做主角，灰色稳定整体，浅色留出呼吸感。",listOf("#A9BEC7","#555C5B","#E8E5DA")),StudioPalette("earth","柔和大地色","灰绿与燕麦色相互衔接，奶白提亮。不同材质让近似色更有层次。",listOf("#788674","#C4B9A4","#E8E5DA")),StudioPalette("mono","黑白有层次","把注意力留给剪裁与面料，在黑白之间加入灰色，让过渡更柔和。",listOf("#292D2C","#A7ADB5","#F5F3EE")))}
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InspirationScreen(vm:AppViewModel,onPalette:(String)->Unit,onStyle:(Long)->Unit,onChat:()->Unit={}){
    val garments by vm.garments.collectAsState()
    val aiPalettes by vm.aiPalettes.collectAsState()
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
        item{StageReveal(0){StudioHeading("","配色灵感","好看的颜色，最终落到你自己的衣物上。")}}
        item{AiPaletteCard(vm)}
        item{
            // 独立入口：配色问答（与 AI 色板生成分离）
            Surface(onClick=onChat,shape=RoundedCornerShape(16.dp),color=FitTheme.colors.surface,border=BorderStroke(1.dp,FitTheme.colors.line),modifier=Modifier.fillMaxWidth()){
                Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){
                    Column(Modifier.weight(1f)){
                        Text("配色问答",style=Typography2.cardTitle,color=FitTheme.colors.textMain)
                        Spacer(Modifier.height(4.dp));Text("和 AI 聊聊你的配色问题",style=Typography2.caption,color=FitTheme.colors.textSub)
                    }
                    Text("›",fontSize=18.sp,color=FitTheme.colors.outline)
                }
            }
        }
        items(aiPalettes,key={it.id}){p->PaletteCard(p,badge="AI 生成",onPalette=onPalette)}
        items(StudioPalettes.all,key={it.id}){p->PaletteCard(p,onPalette=onPalette)}
        item{StageReveal(2){StudioNote("这是配色灵感示例，不代表你的个人四季色彩结论。主色可以在衣物编辑中修正。")}}
        if(garments.any{Styling.slot(it.type)!=Styling.Slot.NONE})item{TextButton(onClick={onStyle(garments.first{Styling.slot(it.type)!=Styling.Slot.NONE}.id)}){Text("从一件已有单品开始搭配")}}
    }
}
@Composable
private fun PaletteCard(p:StudioPalette,badge:String?=null,onPalette:(String)->Unit){
    // 预解析颜色，避免每次重组都做字符串解析
    val swatches=remember(p.id){p.colors.map{c->Color(android.graphics.Color.parseColor(c))}}
    Surface(
        shape=RoundedCornerShape(16.dp),
        color=FitTheme.colors.surface,
        border=BorderStroke(1.dp,FitTheme.colors.line),
        modifier=Modifier.fillMaxWidth()
    ){
        Column(Modifier.padding(16.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){
                Text(p.name,style=Typography2.cardTitle,color=FitTheme.colors.textMain)
                if(badge!=null){Spacer(Modifier.width(8.dp));Surface(shape=RoundedCornerShape(999.dp),color=PrimaryLight){Text(badge,fontSize=10.sp,color=PrimaryDark,modifier=Modifier.padding(horizontal=8.dp,vertical=2.dp))}}
            }
            Spacer(Modifier.height(6.dp));Text(p.description,style=Typography2.caption,color=FitTheme.colors.textSub)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().height(90.dp).clip(RoundedCornerShape(12.dp))){swatches.forEach { c->Box(Modifier.weight(1f).fillMaxHeight().background(c))}}
            TextButton(onClick={onPalette(p.id)},contentPadding=PaddingValues(horizontal=0.dp),modifier=Modifier.heightIn(min=44.dp)){Text("在衣橱中寻找相近颜色",fontSize=13.sp,color=MaterialTheme.colorScheme.primary)}
        }
    }
}
@OptIn(ExperimentalLayoutApi::class,ExperimentalMaterial3Api::class)
@Composable
private fun AiPaletteCard(vm:AppViewModel){
    val hasKey by vm.apiKeyState.collectAsState()
    val busy by vm.aiBusy.collectAsState()
    var keyword by remember { mutableStateOf("") }
    var useWardrobe by remember { mutableStateOf(true) }
    Surface(shape=RoundedCornerShape(16.dp),color=FitTheme.colors.surface,border=BorderStroke(1.dp,FitTheme.colors.line),modifier=Modifier.fillMaxWidth()){
        Column(Modifier.padding(16.dp)){
            Text("AI 配色",style=Typography2.cardTitle,color=FitTheme.colors.textMain)
            Spacer(Modifier.height(6.dp))
            Text(if(hasKey)"输入一个主题，AI 结合配色知识为你生成专属配色。" else "配置 DeepSeek API Key 后可用（我的 → AI 功能）。",style=Typography2.caption,color=FitTheme.colors.textSub)
            if(hasKey){
                Spacer(Modifier.height(10.dp))
                FitTextField(keyword,{keyword=it},Modifier.fillMaxWidth(),placeholder="如：雾蓝通勤、秋日大地色")
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    listOf("雾蓝通勤","秋日大地色","黑白极简").forEach { t->FilterChip(colors=FilterChipDefaults.filterChipColors(selectedContainerColor=if(FitTheme.colors.isDark) FitTheme.colors.surfaceVariant else PrimaryLight, selectedLabelColor=if(FitTheme.colors.isDark) FitTheme.colors.textMain else PrimaryDark),selected=false,onClick={keyword=t},label={Text(t,fontSize=13.sp)},modifier=Modifier.heightIn(min=34.dp))}
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment=Alignment.CenterVertically){
                    CompactCheckbox(useWardrobe,{useWardrobe=it})
                    Text("结合我的衣橱（仅发送颜色×类别统计）",style=Typography2.caption,color=FitTheme.colors.textSub)
                }
                Spacer(Modifier.height(10.dp))
                Button(onClick={vm.generatePalette(keyword,useWardrobe)},enabled=!busy&&keyword.isNotBlank(),modifier=Modifier.fillMaxWidth().heightIn(min=44.dp)){
                    Text(if(busy)"生成中…" else "生成配色")
                }
            }
        }
    }
}
@Composable
fun CompareScreen(vm:AppViewModel,candidateId:Long,onBack:()->Unit){
    val garments by vm.garments.collectAsState();val attrs by vm.attributeLibrary.collectAsState();val candidate=garments.find { it.id==candidateId }
    var referenceId by remember { mutableStateOf<Long?>(null) }
    val candidates=garments.filter { it.id!=candidateId&&it.type==candidate?.type }.sortedByDescending { FitRating.from(it.fit)==FitRating.FIT }
    StudioSubPage("对比尺码",onBack){Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)){
        StudioHeading("","用熟悉的衣物作参照","选择一件同类型、你了解实穿感受的衣物。")
        Text(candidate?.name?:"当前衣物已不存在",style=Typography2.sectionTitle)
        if(candidates.isEmpty())StudioNote("还没有其他同类型衣物可对照。录入后即可并排查看。")
        candidates.forEach { g->Row(Modifier.fillMaxWidth().clickable{referenceId=g.id}.heightIn(min=56.dp),verticalAlignment=Alignment.CenterVertically){RadioButton(selected=g.id==referenceId,onClick={referenceId=g.id});Column{Text(g.name,style=Typography2.body);Text("标码 ${g.sizeLabel.ifBlank{"未记录"}} · ${g.fit}",style=Typography2.caption,color=FitTheme.colors.textSub)}}}
        if(referenceId!=null){
            Spacer(Modifier.height(24.dp));Text("参照衣物 / 当前衣物",style=Typography2.sectionTitle)
            val rows=MeasurementComparison.rows(attrs.filter{it.garmentId==referenceId},attrs.filter{it.garmentId==candidateId})
            if(rows.isEmpty())StudioNote("两件衣物尚未填写尺寸。")
            rows.forEach { r->CardBox(Modifier.padding(top=12.dp)){Text(r.name,style=Typography2.cardTitle);Text("${r.reference}  /  ${r.candidate}",style=Typography2.body);r.deltaCm?.let{Text("当前比参照${if(it>=0)"多" else "少"} ${"%.2f".format(abs(it))} cm",style=Typography2.caption,color=MaterialTheme.colorScheme.primary)};Text(r.explanation,style=Typography2.caption,color=FitTheme.colors.textSub)} }
            Spacer(Modifier.height(20.dp));StudioNote("差值仅在测量口径相同、单位可换算时显示。弹性与版型也会影响穿着效果。")
        }
    }}
}
