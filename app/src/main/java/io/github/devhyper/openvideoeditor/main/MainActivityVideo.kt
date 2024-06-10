package io.github.devhyper.openvideoeditor.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import io.github.devhyper.openvideoeditor.misc.setImmersiveMode
import io.github.devhyper.openvideoeditor.misc.setupSystemUi

class MainActivityVideo : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupSystemUi()

       // val dataStore = SettingsDataStore(this)

        setContent {
            setImmersiveMode(false)
            // MainScreen(pickMedia)
            MainScreenVideo()
        }
    }

//    private fun launchVideoEditor(uri: Uri) {
//        println("VIDEDITOR-> mainactivity " + uri)
//
//        val intent = Intent(this, VideoEditorActivity::class.java)
//        intent.action = Intent.ACTION_EDIT
//        intent.data = uri
//        startActivity(intent)
//    }
}
