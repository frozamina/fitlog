package com.fitlog.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.fitlog.app.data.BackupData
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object BackupManager {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    /** 导出为 zip：data.json + 图片文件，返回 zip 文件 */
    fun export(context: Context, data: BackupData): File {
        val outDir = File(context.cacheDir, "export").apply { mkdirs() }
        val zipFile = File(outDir, "fitlog_backup_${stamp()}.zip")
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            zos.putNextEntry(ZipEntry("data.json"))
            zos.write(json.encodeToString(data).toByteArray())
            zos.closeEntry()
            data.images.forEach { img ->
                val f = File(img.path)
                if (f.exists()) {
                    zos.putNextEntry(ZipEntry("images/${f.name}"))
                    f.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }
        }
        return zipFile
    }

    /** 分享导出文件 */
    fun share(context: Context, file: File) = shareFile(context, file, "application/zip")

    /** 分享任意文件（如崩溃日志） */
    fun shareFile(context: Context, file: File, mime: String = "text/plain") {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "分享文件").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** 导入：解压 zip，恢复图片与 JSON */
    fun import(context: Context, uri: Uri): BackupData? {
        return try {
            val imageDir = ImageStore.dir(context)
            val zipTmp = File(context.cacheDir, "import_${System.currentTimeMillis()}.zip")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(zipTmp).use { input.copyTo(it) }
            } ?: return null

            var jsonText: String? = null
            ZipInputStream(zipTmp.inputStream()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    when {
                        entry.name == "data.json" -> jsonText = zis.readBytes().toString(Charsets.UTF_8)
                        entry.name.startsWith("images/") -> {
                            val target = File(imageDir, File(entry.name).name)
                            FileOutputStream(target).use { zis.copyTo(it) }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            zipTmp.delete()
            jsonText?.let { json.decodeFromString<BackupData>(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun stamp(): String =
        java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.CHINA).format(java.util.Date())
}
