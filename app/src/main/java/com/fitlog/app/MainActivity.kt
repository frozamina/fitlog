package com.fitlog.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.fitlog.app.ui.screens.*
import com.fitlog.app.ui.theme.*
import com.fitlog.app.vm.AppViewModel

class MainActivity:ComponentActivity(){override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{FitLogTheme{AppNav()}}}}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNav(){
    val nav=rememberNavController();val vm:AppViewModel=viewModel();val entry by nav.currentBackStackEntryAsState();val route=entry?.destination?.route
    val top=route in listOf("closet?palette={palette}","outfits","inspiration")
    fun style(id:Long,edit:Long=0,copy:Long=0){nav.navigate("style/$id?edit=$edit&copy=$copy")}
    val error by vm.errors.collectAsState()
    Scaffold(containerColor=FitTheme.colors.bg,
        topBar={if(top)TopAppBar(title={Text("服集 FitLog",fontSize=27.sp,color=FitTheme.colors.textMain)},actions={TextButton(onClick={nav.navigate("settings")}){Icon(Icons.Outlined.Tune,contentDescription=null,modifier=Modifier.size(18.dp));Spacer(Modifier.width(6.dp));Text("我的",fontSize=14.sp)}},colors=TopAppBarDefaults.topAppBarColors(containerColor=FitTheme.colors.bg))},
        bottomBar={if(top)Row(
            Modifier.fillMaxWidth().background(FitTheme.colors.surface).navigationBarsPadding().height(58.dp),
            horizontalArrangement=Arrangement.SpaceEvenly,
            verticalAlignment=Alignment.CenterVertically
        ){
            listOf(Triple("closet","衣橱",Icons.Outlined.Checkroom),Triple("outfits","搭配",Icons.Outlined.Layers),Triple("inspiration","灵感",Icons.Outlined.Palette)).forEach { (path,label,icon) ->
                val selected=route?.startsWith(path)==true
                Column(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable{nav.navigate(path){popUpTo("closet?palette={palette}"){saveState=true};launchSingleTop=true;restoreState=true}}
                        .padding(horizontal=10.dp,vertical=4.dp),
                    horizontalAlignment=Alignment.CenterHorizontally
                ){
                    // 高亮胶囊直接作为图标背景，与图标严格对齐
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if(selected)PrimaryLight else Color.Transparent)
                            .padding(horizontal=16.dp,vertical=3.dp)
                    ){Icon(icon,contentDescription=null,tint=if(selected)Primary else FitTheme.colors.textSub,modifier=Modifier.size(20.dp))}
                    Spacer(Modifier.height(1.dp))
                    Text(label,fontSize=11.sp,color=if(selected)Primary else FitTheme.colors.textSub)
                }
            }
        }}
    ){padding->
        NavHost(nav,"closet?palette={palette}",Modifier.padding(padding)){
            composable("closet?palette={palette}",arguments=listOf(navArgument("palette"){type=NavType.StringType;defaultValue=""})){e->
                ListScreen(vm,onOpen={nav.navigate("detail/$it")},onAdd={nav.navigate("edit/-1")},onStyle={style(it)},paletteId=e.arguments?.getString("palette").orEmpty())
            }
            composable("outfits"){OutfitsScreen(vm,onNew={style(0)},onEdit={style(0,edit=it)},onCopy={style(0,copy=it)})}
            composable("inspiration"){InspirationScreen(vm,onPalette={nav.navigate("closet?palette=$it")},onStyle={style(it)})}
            composable("settings"){StudioSubPage("我的",onBack={nav.popBackStack()}){SettingsScreen(vm,onBody={nav.navigate("body")})}}
            composable("body"){StudioSubPage("身体与尺码",onBack={nav.popBackStack()}){HomeScreen(vm,onAdd={nav.navigate("edit/-1")},onOpenGarment={nav.navigate("detail/$it")})}}
            composable("detail/{id}",arguments=listOf(navArgument("id"){type=NavType.LongType})){e->val id=e.arguments!!.getLong("id");DetailScreen(vm,id,onBack={nav.popBackStack()},onEdit={nav.navigate("edit/$it")},onStyle={style(it)},onCompare={nav.navigate("compare/$it")})}
            composable("edit/{id}",arguments=listOf(navArgument("id"){type=NavType.LongType})){e->EditScreen(vm,e.arguments!!.getLong("id"),onBack={nav.popBackStack()})}
            composable("compare/{id}",arguments=listOf(navArgument("id"){type=NavType.LongType})){e->CompareScreen(vm,e.arguments!!.getLong("id"),onBack={nav.popBackStack()})}
            composable("style/{anchor}?edit={edit}&copy={copy}",arguments=listOf(navArgument("anchor"){type=NavType.LongType},navArgument("edit"){type=NavType.LongType;defaultValue=0L},navArgument("copy"){type=NavType.LongType;defaultValue=0L})){e->
                OutfitEditorScreen(vm,e.arguments!!.getLong("anchor"),e.arguments!!.getLong("edit"),e.arguments!!.getLong("copy"),onBack={nav.popBackStack()},onSaved={nav.navigate("outfits"){popUpTo("closet?palette={palette}");launchSingleTop=true}})
            }
        }
    }
    if(error!=null)AlertDialog(onDismissRequest=vm::clearError,title={Text("操作未完成")},text={Text(error.orEmpty())},confirmButton={TextButton(onClick=vm::clearError){Text("知道了")}})
}
