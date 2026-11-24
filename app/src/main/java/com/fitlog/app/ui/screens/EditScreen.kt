package com.fitlog.app.ui.screens

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.fitlog.app.data.FitRating
import com.fitlog.app.data.Garment
import com.fitlog.app.data.GarmentAttr
import com.fitlog.app.data.GarmentImage
import com.fitlog.app.data.GarmentType
import com.fitlog.app.ui.SectionTitle
import com.fitlog.app.ui.FieldLabel
import com.fitlog.app.ui.FitTextField
import com.fitlog.app.ui.FitToast
import com.fitlog.app.ui.IconAction
import com.fitlog.app.ui.LabeledField
import com.fitlog.app.ui.TypeChip
import com.fitlog.app.ui.theme.*
import com.fitlog.app.util.ImageStore
import com.fitlog.app.vm.AppViewModel
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

private data class AttrRow(var name: String, var value: String, var isCustom: Boolean = false, val unit: String = "cm", val basis: String = "")
private data class ImgPick(val path: String, var isCover: Boolean = false)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditScreen(vm: AppViewModel, garmentId: Long, onBack: () -> Unit) {
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var sizeLabel by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(GarmentType.TOP.label) }
    var fit by remember { mutableStateOf(FitRating.UNRATED) }
    var original by remember { mutableStateOf<Garment?>(null) }
    var mainColor by remember { mutableStateOf("") }
    var pattern by remember { mutableStateOf("未标注") }
    var silhouette by remember { mutableStateOf("未标注") }
    val saving by vm.saving.collectAsState()

    val attrs = remember { mutableStateListOf<AttrRow>() }
    val images = remember { mutableStateListOf<ImgPick>() }
    var showAddAttr by remember { mutableStateOf(false) }
    var newAttrName by remember { mutableStateOf("") }
    var toast by remember { mutableStateOf<String?>(null) }

    // 载入已有记录（属性与图片分开加载，避免一个 collect 阻塞另一个）
    LaunchedEffect(garmentId) {
        if (garmentId > 0) {
            val g = vm.garment(garmentId)
            if (g != null) {
                original=g;mainColor=g.mainColor;pattern=g.pattern;silhouette=g.silhouette
                name = g.name; brand = g.brand; sizeLabel = g.sizeLabel
                price = g.price; note = g.note; type = g.type
                fit = FitRating.from(g.fit)
            }
        }
    }
    LaunchedEffect(garmentId) {
        if (garmentId > 0) {
            val list = vm.attrsFlow(garmentId).first()
            attrs.clear()
            attrs.addAll(list.map { AttrRow(it.name, it.value, it.isCustom, it.unit, it.basis) })
        }
    }
    LaunchedEffect(garmentId) {
        if (garmentId > 0) {
            val list = vm.imagesFlow(garmentId).first()
            images.clear()
            images.addAll(list.map { ImgPick(it.path, it.isCover) })
        }
    }

    // 新建时按类型带出模板属性
    LaunchedEffect(type, garmentId) {
        if (garmentId <= 0L) {
            val list = vm.templatesOf(type).first()
            attrs.clear()
            attrs.addAll(list.map { AttrRow(it.name, "", !it.isDefault) })
        }
    }

    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val path = ImageStore.import(context, it)
            if (path != null && images.size<9) images.add(ImgPick(path, images.isEmpty()))
            else toast = "图片导入失败"
        }
    }

    val cameraUriState = remember { mutableStateOf<Uri?>(null) }
    val takeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) {
            cameraUriState.value?.let { uri ->
                val path = ImageStore.import(context, uri)
                if (path != null && images.size<9) images.add(ImgPick(path, images.isEmpty()))
            }
        }
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera(context, cameraUriState) { takeLauncher.launch(it) }
        else toast = "未获得相机权限"
    }

    // 校验：名称 + 至少一个尺码属性
    fun doSave() {
        if (name.isBlank()) { toast = "请填写衣物名称"; return }
        if(mainColor.isNotBlank() && com.fitlog.app.data.Styling.hex(mainColor)==null){toast="主色请填写 #RRGGBB，或暂时留空";return}
        vm.saveGarment(
            garment = (original ?: Garment(name="",type=type)).copy(
                id = if (garmentId > 0) garmentId else 0,
                name = name.trim(),
                type = type,
                sizeLabel = sizeLabel,
                brand = brand,
                price = price,
                fit = fit.label,
                mainColor=mainColor.trim().uppercase(),pattern=pattern,silhouette=silhouette,
                note = note
            ),
            attrs = attrs.mapIndexed { i, a -> GarmentAttr(name = a.name, value = a.value, unit=a.unit,basis=a.basis, isCustom = a.isCustom, sortOrder = i) },
            images = images.mapIndexed { i, p -> GarmentImage(path = p.path, isCover = p.isCover, sortOrder = i) }
        ) { onBack() }
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = FitTheme.colors.bg,
            topBar = {
                TopAppBar(
                    title = { Text(if (garmentId > 0) "编辑衣物" else "新增衣物", style = Typography2.pageTitle) },
                    navigationIcon = {
                        TextButton(onClick = onBack) { Text("‹ 返回", fontSize = 14.sp) }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = FitTheme.colors.surface)
                )
            },
            bottomBar = {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(FitTheme.colors.surface)
                        .padding(Spacing.base),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.m)
                ) {
                    Button(
                        onClick = onBack,
                        modifier = Modifier.weight(1f).height(Touch.button),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FitTheme.colors.surfaceVariant,
                            contentColor = FitTheme.colors.textSub
                        ),
                        shape = RoundedCornerShape(Radius.field)
                    ) { Text("取消") }
                    Button(
                        onClick = { doSave() },
                        enabled=!saving,
                        modifier = Modifier.weight(2f).height(Touch.button),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        shape = RoundedCornerShape(Radius.field)
                    ) { Text(if(saving)"正在保存…" else "保存", fontWeight = FontWeight.SemiBold) }
                }
            }
        ) { pad ->
            Column(
                Modifier
                    .padding(pad)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.base)
            ) {
                Spacer(Modifier.height(Spacing.s))

                // ---- 分组一：基本信息 ----
                FieldLabel("衣物类型 *")
                FlowRow(Modifier.fillMaxWidth().padding(bottom = Spacing.s)) {
                    GarmentType.entries.forEach { t ->
                        TypeChip(t.label, type == t.label) { type = t.label }
                    }
                }
                LabeledField("衣物名称 *", name, { name = it }, "如：优衣库 摇粒绒外套")
                Row(Modifier.fillMaxWidth()) {
                    Box(Modifier.weight(1f)) {
                        LabeledField("尺码标签（可选）", sizeLabel, { sizeLabel = it }, "如 XL / 31")
                    }
                    Spacer(Modifier.width(Spacing.m))
                    Box(Modifier.weight(1f)) {
                        LabeledField("品牌", brand, { brand = it }, "如 优衣库")
                    }
                }
                LabeledField("价格（可选）", price, { price = it }, "如 199")

                Spacer(Modifier.height(Spacing.s))

                SectionTitle("颜色与版型")
                LabeledField("主色（可选）",mainColor,{mainColor=it},"#RRGGBB，例如 #A9BEC7")
                Row(Modifier.fillMaxWidth().padding(bottom=16.dp),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                    listOf("#A9BEC7","#555C5B","#E8E5DA","#788674","#C4B9A4","#292D2C").forEach { hex ->
                        Box(Modifier.size(44.dp).clickable { mainColor=hex },contentAlignment=Alignment.Center) {
                            Box(Modifier.size(22.dp).background(Color(android.graphics.Color.parseColor(hex)),RoundedCornerShape(11.dp)).border(if(mainColor.equals(hex,true))2.dp else 1.dp,FitTheme.colors.textSub,RoundedCornerShape(11.dp)))
                        }
                    }
                }
                FieldLabel("图案")
                FlowRow { listOf("未标注","纯色","条纹","格纹","印花").forEach { value -> TypeChip(value,pattern==value){pattern=value} } }
                FieldLabel("衣服本身的版型")
                FlowRow { listOf("未标注","修身","常规","宽松").forEach { value -> TypeChip(value,silhouette==value){silhouette=value} } }
                Spacer(Modifier.height(20.dp))
                // ---- 分组二：尺码属性 ----
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "尺码属性（选填）",
                        style = Typography2.caption,
                        color = FitTheme.colors.textSub,
                        modifier = Modifier.weight(1f).padding(start = 2.dp, bottom = Spacing.xs)
                    )
                    Text(
                        "共 ${attrs.count { it.value.isNotBlank() }} 项已填",
                        style = Typography2.caption,
                        color = Primary,
                        modifier = Modifier.padding(bottom = Spacing.xs)
                    )
                }

                attrs.forEachIndexed { i, a ->
                    key(i) {
                        var basisMenu by remember { mutableStateOf(false) }
                        Row(
                            Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FitTextField(a.name, { attrs[i] = a.copy(name = it) }, Modifier.weight(1.2f), "属性名")
                            FitTextField(a.value, { attrs[i] = a.copy(value = it) }, Modifier.weight(0.9f), "数值")
                            FitTextField(a.unit, { attrs[i] = a.copy(unit = it) }, Modifier.width(52.dp), "单位")
                            Box {
                                TextButton(
                                    onClick = { basisMenu = true },
                                    contentPadding = PaddingValues(horizontal = 6.dp),
                                    modifier = Modifier.heightIn(min = 44.dp)
                                ) {
                                    Text(
                                        a.basis.ifBlank { "口径" },
                                        fontSize = 12.sp, maxLines = 1,
                                        color = FitTheme.colors.textSub
                                    )
                                }
                                DropdownMenu(basisMenu, { basisMenu = false }) {
                                    listOf("未注明", "长度", "周长", "平铺单面", "未拉伸", "拉伸后").forEach { value ->
                                        DropdownMenuItem(text = { Text(value) }, onClick = { attrs[i] = a.copy(basis = if (value == "未注明") "" else value); basisMenu = false })
                                    }
                                }
                            }
                            IconAction("×", FitRed, { attrs.removeAt(i) }, fontSize = 14)
                        }
                    }
                }

                // 添加自定义属性（原地展开，避免跳页）
                AnimatedVisibility(
                    visible = showAddAttr,
                    enter = fadeIn(Motion.enter(Motion.SHORT4)) + expandVertically(Motion.enter(Motion.SHORT4)),
                    exit = fadeOut(Motion.standard(Motion.SHORT4)) + shrinkVertically(Motion.standard(Motion.SHORT4))
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = Spacing.s),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FitTextField(
                            value = newAttrName,
                            onValueChange = { newAttrName = it },
                            modifier = Modifier.weight(1f),
                            placeholder = "属性名，如 袖长"
                        )
                        Spacer(Modifier.width(Spacing.s))
                        Button(
                            onClick = {
                                if (newAttrName.isNotBlank()) {
                                    attrs.add(AttrRow(newAttrName.trim(), "", true))
                                    newAttrName = ""
                                    showAddAttr = false
                                }
                            },
                            modifier = Modifier.height(Touch.button),
                            shape = RoundedCornerShape(Radius.field),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) { Text("添加") }
                    }
                }

                if (!showAddAttr) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(Touch.button)
                            .clip(RoundedCornerShape(Radius.field))
                            .border(1.5.dp, FitTheme.colors.outlineStrong, RoundedCornerShape(Radius.field))
                            .clickable { showAddAttr = true },
                        contentAlignment = Alignment.Center
                    ) { Text("＋ 添加自定义属性", fontSize = 14.sp, color = Primary) }
                }

                Spacer(Modifier.height(Spacing.base))

                // ---- 分组三：合身评价 ----
                FieldLabel("实穿感受（与版型分开记录）")
                Row(Modifier.fillMaxWidth().padding(bottom = Spacing.base)) {
                    FitRating.entries.forEach { r ->
                        val selected = fit == r
                        Box(
                            Modifier
                                .weight(1f)
                                .height(Touch.button)
                                .padding(end = Spacing.s)
                                .clip(RoundedCornerShape(Radius.field))
                                .background(if (selected) r.color else FitTheme.colors.surfaceVariant)
                                .clickable { fit = r },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                r.label, fontSize = 13.sp,
                                color = if (selected) Color.White else FitTheme.colors.textSub,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }

                // ---- 分组四：图片 ----
                FieldLabel("图片（最多 9 张，点击设封面）")
                Row(Modifier.fillMaxWidth().padding(bottom = Spacing.m)) {
                    OutlinedButton(
                        onClick = { if (images.size < 9) pickLauncher.launch("image/*") else toast = "最多 9 张" },
                        modifier = Modifier.weight(1f).height(Touch.button),
                        shape = RoundedCornerShape(Radius.field),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary)
                    ) { Text("从相册选择", fontSize = 14.sp) }
                    Spacer(Modifier.width(Spacing.s))
                    OutlinedButton(
                        onClick = { if(images.size<9)permLauncher.launch(Manifest.permission.CAMERA) else toast="最多 9 张" },
                        modifier = Modifier.weight(1f).height(Touch.button),
                        shape = RoundedCornerShape(Radius.field),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary)
                    ) { Text("拍照", fontSize = 14.sp) }
                }

                val columns = 3
                val rows = (images.size + columns - 1) / columns
                Column {
                    repeat(if (rows == 0) 1 else rows) { r ->
                        Row(Modifier.fillMaxWidth().padding(bottom = Spacing.s)) {
                            repeat(columns) { c ->
                                val idx = r * columns + c
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(end = if (c < columns - 1) Spacing.s else 0.dp)
                                ) {
                                    if (idx < images.size) {
                                        val item = images[idx]
                                        Box(
                                            Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(Radius.field))
                                                .clickable {
                                                    images.indices.forEach { i ->
                                                        images[i] = images[i].copy(isCover = i == idx)
                                                    }
                                                }
                                        ) {
                                            AsyncImage(
                                                model = File(item.path),
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            if (item.isCover) {
                                                Text(
                                                    "封面", fontSize = 14.sp, color = Color.White,
                                                    modifier = Modifier
                                                        .align(Alignment.BottomStart)
                                                        .padding(Spacing.xs)
                                                        .clip(RoundedCornerShape(Spacing.xs))
                                                        .background(Color(0x99000000))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            // 删除：44dp 触控区
                                            Box(
                                                Modifier
                                                    .align(Alignment.TopEnd)
                                                    .size(Touch.min)
                                                    .clickable { images.removeAt(idx) },
                                                contentAlignment = Alignment.TopEnd
                                            ) {
                                                Box(
                                                    Modifier
                                                        .padding(Spacing.xs)
                                                        .size(20.dp)
                                                        .clip(RoundedCornerShape(Spacing.xs))
                                                        .background(Color(0x99000000)),
                                                    contentAlignment = Alignment.Center
                                                ) { Text("✕", fontSize = 13.sp, color = Color.White) }
                                            }
                                        }
                                    } else if (idx == images.size) {
                                        Box(
                                            Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(Radius.field))
                                                .border(1.5.dp, FitTheme.colors.outlineStrong, RoundedCornerShape(Radius.field))
                                                .clickable { if (images.size < 9) pickLauncher.launch("image/*") else toast = "最多 9 张" },
                                            contentAlignment = Alignment.Center
                                        ) { Text("＋", fontSize = 20.sp, color = FitTheme.colors.textFaint) }
                                    }
                                }
                            }
                        }
                    }
                }

                LabeledField("备注（可选）", note, { note = it }, "如：袖子偏长，下次买 L")
                Spacer(Modifier.height(Spacing.xl))
            }
        }

        // Toast 覆盖层
        Box(
            Modifier
                .fillMaxSize()
                .padding(bottom = 120.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            FitToast(message = toast ?: "", visible = toast != null)
        }
    }

    toast?.let { msg ->
        LaunchedEffect(msg) {
            delay(2200)
            toast = null
        }
    }
}

private fun launchCamera(context: android.content.Context, state: MutableState<Uri?>, launch: (Uri) -> Unit) {
    val file = File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    state.value = uri
    launch(uri)
}
