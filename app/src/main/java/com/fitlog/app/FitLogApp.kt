package com.fitlog.app

import android.app.Application
import java.io.File

class FitLogApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // 全局崩溃捕获：把堆栈写到本地文件，便于用户导出反馈
        val default = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try {
                File(filesDir, "crash_log.txt").writeText(
                    buildString {
                        appendLine("time: ${System.currentTimeMillis()}")
                        appendLine("thread: ${t.name}")
                        appendLine(android.util.Log.getStackTraceString(e))
                    }
                )
            } catch (_: Throwable) {
            }
            default?.uncaughtException(t, e)
        }
    }
}
