package io.github.devhyper.openvideoeditor.storage

import android.app.Application

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DataStoreManager.init(this)
    }
}