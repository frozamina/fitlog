package com.fitlog.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object ImageStore {
    private const val MAX_EDGE = 1600

    fun dir(context: Context): File = File(context.filesDir, "images").apply { mkdirs() }

    /** 把相册/相机得到的 uri 复制并压缩到应用私有目录，返回绝对路径 */
    fun import(context: Context, uri: Uri): String? {
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return null
            val raw = BitmapFactory.decodeStream(input, null, BitmapFactory.Options().apply {
                inSampleSize = 2
            }) ?: return null
            input.close()
            val scaled = scale(raw)
            val file = File(dir(context), "img_${System.currentTimeMillis()}_${(0..9999).random()}.jpg")
            FileOutputStream(file).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 88, out)
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun scale(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height
        val max = maxOf(w, h)
        if (max <= MAX_EDGE) return src
        val ratio = MAX_EDGE.toFloat() / max
        return Bitmap.createScaledBitmap(src, (w * ratio).toInt(), (h * ratio).toInt(), true)
    }

    fun delete(path: String) {
        runCatching { File(path).delete() }
    }
}
