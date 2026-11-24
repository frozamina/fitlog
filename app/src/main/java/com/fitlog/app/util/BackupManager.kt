package com.fitlog.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.fitlog.app.data.BackupData
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object BackupManager {
    private val json=Json { prettyPrint=true;ignoreUnknownKeys=true }
    /** 同一张照片可能被多个衣物引用，打包时只写一次。 */
    suspend fun export(context:Context,data:BackupData):File=withContext(Dispatchers.IO){
        val images=data.images.map { it.path }.distinct().associateWith { "images/${UUID.randomUUID()}.jpg" }
        images.keys.forEach { require(File(it).isFile){"有照片文件缺失，请先检查衣物照片"} }
        val portable=data.copy(version=2,images=data.images.map { it.copy(path=images.getValue(it.path)) })
        val outDir=File(context.cacheDir,"export").apply { mkdirs() }
        val result=File(outDir,"fitlog_backup_${System.currentTimeMillis()}.zip")
        ZipOutputStream(result.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("data.json"));zip.write(json.encodeToString(portable).toByteArray());zip.closeEntry()
            images.forEach { (source,path) -> zip.putNextEntry(ZipEntry(path));File(source).inputStream().use { it.copyTo(zip) };zip.closeEntry() }
        };result
    }
    fun share(context:Context,file:File)=shareFile(context,file,"application/zip")
    fun shareFile(context:Context,file:File,mime:String="text/plain"){
        val uri=FileProvider.getUriForFile(context,"${context.packageName}.fileprovider",file)
        val intent=Intent(Intent.ACTION_SEND).apply { type=mime;putExtra(Intent.EXTRA_STREAM,uri);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        context.startActivity(Intent.createChooser(intent,"保存或分享文件").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
    /** 兼容 v1 绝对路径与 v2 可移植路径，在当前设备重新建立照片位置。 */
    suspend fun import(context:Context,uri:Uri):BackupData?=withContext(Dispatchers.IO){
        val created=mutableListOf<File>()
        try {
            val paths=mutableMapOf<String,String>();var text:String?=null;var total=0L;var count=0
            val input=context.contentResolver.openInputStream(uri)?:return@withContext null
            ZipInputStream(input).use { zip ->
                var entry=zip.nextEntry
                while(entry!=null){
                    require(++count<=5000){"备份条目过多"}
                    if(!entry.isDirectory){
                        val limit=if(entry.name=="data.json")8*1024*1024 else 25*1024*1024
                        val bytes=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192);var n=zip.read(buffer)
                        while(n>0){total+=n;require(total<=512L*1024*1024&&bytes.size()+n<=limit){"备份文件过大"};bytes.write(buffer,0,n);n=zip.read(buffer)}
                        if(entry.name=="data.json"){require(text==null);text=bytes.toString(Charsets.UTF_8.name())}
                        else if(Regex("images/[A-Za-z0-9._-]+\\.(jpg|jpeg|png|webp)",RegexOption.IGNORE_CASE).matches(entry.name)){
                            require(!paths.containsKey(entry.name));val file=File(ImageStore.dir(context),"restored_${UUID.randomUUID()}.jpg")
                            created.add(file);file.writeBytes(bytes.toByteArray());paths[entry.name]=file.absolutePath
                        }
                    };zip.closeEntry();entry=zip.nextEntry
                }
            }
            val data=json.decodeFromString<BackupData>(requireNotNull(text));require(data.version in 1..2)
            data.copy(images=data.images.map { img -> val key=if(img.path.startsWith("images/"))img.path else "images/${File(img.path).name}";img.copy(path=requireNotNull(paths[key]){"备份缺少照片：$key"}) })
        }catch(e:Exception){created.forEach { it.delete() };e.printStackTrace();null}
    }
}
