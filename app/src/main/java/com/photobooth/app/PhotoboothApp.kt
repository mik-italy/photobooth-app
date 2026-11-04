package com.photobooth.app

import android.app.Application
import android.util.Log
import java.io.File

class PhotoboothApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                Log.e("PhotoboothApp", "FATAL CRASH on thread ${thread.name}", throwable)
                val crashFile = File(cacheDir, "last_crash.txt")
                crashFile.writeText(Log.getStackTraceString(throwable))
            } catch (_: Exception) {}
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
