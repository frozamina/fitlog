package com.fitlog.app.data

/**
 * 中文颜色名 → 色号 对照表与解析。
 * 主色输入框支持直接输入「黑色」「雾蓝」「驼色」等常用色名，保存时自动转换为 #RRGGBB。
 * 色值与 GarmentSwatches 快捷色板保持同一色系（低饱和服装色）。
 */
object ColorNames {

    private val map: Map<String, String> = buildMap {
        // 黑白灰
        put("黑色", "#292D2C"); put("黑", "#292D2C"); put("炭黑", "#1F2322")
        put("白色", "#F5F3EE"); put("白", "#F5F3EE"); put("纯白", "#FAFAF7"); put("乳白", "#F7F2E8")
        put("米白", "#EFEAE0"); put("米色", "#E8E5DA"); put("燕麦", "#E8E5DA"); put("燕麦色", "#E8E5DA")
        put("灰色", "#A7ADB5"); put("灰", "#A7ADB5"); put("浅灰", "#C6CBCC"); put("深灰", "#555C5B")
        put("烟灰", "#8C9196"); put("炭灰", "#4A4F4E"); put("银灰", "#B9BEC2"); put("灰白", "#DDDCD6")
        // 大地色系
        put("奶咖", "#C4B9A4"); put("奶咖色", "#C4B9A4"); put("卡其", "#B9A08E"); put("卡其色", "#B9A08E")
        put("驼色", "#B9A08E"); put("驼", "#B9A08E"); put("浅驼", "#CBB49E")
        put("焦糖", "#B07A4E"); put("焦糖色", "#B07A4E"); put("焦糖棕", "#A9754B")
        put("棕色", "#6E5A4B"); put("棕", "#6E5A4B"); put("咖啡色", "#5C4A3D"); put("咖啡", "#5C4A3D")
        put("深棕", "#4E4036"); put("褐色", "#6B5647"); put("浅棕", "#A08B72"); put("栗色", "#5B4636")
        // 蓝色系
        put("蓝色", "#5B7B97"); put("蓝", "#5B7B97"); put("浅蓝", "#A9C4D4"); put("深蓝", "#2C3E5D")
        put("藏蓝", "#2C3E5D"); put("藏青", "#2C3A52"); put("藏蓝色", "#2C3E5D"); put("海军蓝", "#2C3E5D")
        put("雾蓝", "#A9BEC7"); put("雾霾蓝", "#A9BEC7"); put("雾霾蓝", "#A9BEC7")
        put("牛仔蓝", "#5B7B97"); put("天蓝", "#9FC0D4"); put("湖蓝", "#7FA8BC"); put("靛蓝", "#3D5270")
        // 绿色系
        put("绿色", "#47765D"); put("绿", "#47765D"); put("军绿", "#47765D"); put("军绿色", "#47765D")
        put("橄榄绿", "#6B7452"); put("豆绿", "#A3B18A"); put("灰绿", "#788674"); put("鼠尾草绿", "#788674")
        put("浅绿", "#B5C4A8"); put("墨绿", "#2E4638"); put("深绿", "#33513F"); put("薄荷绿", "#B9CDB9")
        put("苔藓绿", "#5F6E4E"); put("森林绿", "#3A5748")
        // 红色系
        put("红色", "#8C3A38"); put("红", "#8C3A38"); put("酒红", "#7A3B3F"); put("酒红色", "#7A3B3F")
        put("枣红", "#6E3134"); put("砖红", "#9E5544"); put("暗红", "#6B3336"); put("正红", "#A03030")
        put("砖红色", "#9E5544"); put("铁锈红", "#96513F")
        // 粉色系
        put("粉色", "#D8A7A0"); put("粉", "#D8A7A0"); put("浅粉", "#E7C2BC"); put("藕粉", "#D8A7A0")
        put("藕粉色", "#D8A7A0"); put("豆沙", "#B98284"); put("豆沙色", "#B98284"); put("裸粉", "#DDBAB1")
        put("玫瑰粉", "#C98F8C"); put("珊瑚粉", "#E0A898")
        // 紫色系
        put("紫色", "#9B8AA6"); put("紫", "#9B8AA6"); put("浅紫", "#B8A9C4"); put("深紫", "#5E4A6E")
        put("紫灰", "#9B8AA6"); put("香芋紫", "#A99BB8"); put("薰衣草紫", "#B4A6C6")
        // 黄橙系
        put("黄色", "#D9C07A"); put("黄", "#D9C07A"); put("鹅黄", "#E6D6A0"); put("芥末黄", "#C0A45A")
        put("姜黄", "#B98A3E"); put("奶黄", "#E9DDB8")
        put("橙色", "#C77E52"); put("橙", "#C77E52"); put("橘色", "#C77E52"); put("南瓜橙", "#BC7147")
        put("珊瑚橙", "#D08A6E")
        // 其他
        put("杏色", "#E4C9A8"); put("杏", "#E4C9A8"); put("奶茶色", "#CBB79B"); put("奶茶", "#CBB79B")
        put("香草", "#E3DCC8"); put("亚麻", "#D5C6AC"); put("亚麻色", "#D5C6AC")
    }

    /**
     * 解析用户输入：支持 #RRGGBB / RRGGBB / 常用中文色名。
     * 返回标准化 #RRGGBB（大写），无法识别返回 null。
     */
    fun resolve(input: String): String? {
        val s = input.trim().replace(" ", "").replace("　", "")
        if (s.isEmpty()) return null
        // 标准色号
        Styling.hex(s)?.let { return it }
        // 允许不带 # 的六位色号
        if (Regex("^[0-9a-fA-F]{6}$").matches(s)) return s.uppercase().let { "#$it" }
        // 中文名（含容错：去掉末尾「色」「的」「颜色」）
        map[s]?.let { return it }
        return map[s.removeSuffix("颜色").removeSuffix("的").removeSuffix("色")]
    }

    /** 是否为可识别的中文色名（用于判断输入类别） */
    fun isName(input: String): Boolean {
        val s = input.trim().replace(" ", "")
        return map.containsKey(s) || map.containsKey(s.removeSuffix("颜色").removeSuffix("的").removeSuffix("色"))
    }
}
