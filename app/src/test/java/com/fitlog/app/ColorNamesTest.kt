package com.fitlog.app

import com.fitlog.app.data.ColorNames
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorNamesTest {

    @Test
    fun `标准色号原样通过并大写`() {
        assertEquals("#A9BEC7", ColorNames.resolve("#a9bec7"))
        assertEquals("#292D2C", ColorNames.resolve("#292D2C"))
    }

    @Test
    fun `不带井号的六位色号自动补全`() {
        assertEquals("#A9BEC7", ColorNames.resolve("a9bec7"))
        assertEquals("#A9BEC7", ColorNames.resolve("A9BEC7"))
    }

    @Test
    fun `常用中文色名可解析`() {
        assertEquals("#292D2C", ColorNames.resolve("黑色"))
        assertEquals("#F5F3EE", ColorNames.resolve("白色"))
        assertEquals("#A9BEC7", ColorNames.resolve("雾蓝"))
        assertEquals("#B9A08E", ColorNames.resolve("驼色"))
        assertEquals("#47765D", ColorNames.resolve("军绿"))
    }

    @Test
    fun `色名含空格或后缀容错`() {
        assertEquals("#292D2C", ColorNames.resolve(" 黑色 "))
        assertEquals("#47765D", ColorNames.resolve("军绿色"))
        assertEquals("#B9A08E", ColorNames.resolve("驼色的"))
        assertEquals("#A9BEC7", ColorNames.resolve("雾霾蓝"))
    }

    @Test
    fun `无「色」后缀的单字也支持`() {
        assertEquals("#292D2C", ColorNames.resolve("黑"))
        assertEquals("#5B7B97", ColorNames.resolve("蓝"))
        assertEquals("#D8A7A0", ColorNames.resolve("粉"))
    }

    @Test
    fun `无法识别的输入返回null`() {
        assertNull(ColorNames.resolve("好看的颜色"))
        assertNull(ColorNames.resolve("hello"))
        assertNull(ColorNames.resolve("#12345"))
        assertNull(ColorNames.resolve(""))
    }

    @Test
    fun `空白输入返回null`() {
        assertNull(ColorNames.resolve("   "))
    }

    @Test
    fun `isName判断色名类别`() {
        assertTrue(ColorNames.isName("黑色"))
        assertTrue(ColorNames.isName("雾蓝色"))
        assertFalse(ColorNames.isName("#A9BEC7"))
        assertFalse(ColorNames.isName("不知道"))
    }

    @Test
    fun `所有色号值均为合法RRGGBB`() {
        // 反射读取私有 map 校验所有值格式（防止后续维护时写错色值）
        val field = ColorNames::class.java.getDeclaredField("map")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val map = field.get(ColorNames) as Map<String, String>
        assertTrue("色名表不应为空", map.isNotEmpty())
        map.values.forEach { hex ->
            assertTrue("非法色值 $hex", Regex("#[0-9A-F]{6}").matches(hex))
        }
    }
}
