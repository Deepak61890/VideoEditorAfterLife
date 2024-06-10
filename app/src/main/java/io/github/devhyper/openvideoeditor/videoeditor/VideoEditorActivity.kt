package io.github.devhyper.openvideoeditor.videoeditor

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.devhyper.openvideoeditor.misc.PROJECT_MIME_TYPE
import io.github.devhyper.openvideoeditor.misc.setImmersiveMode
import io.github.devhyper.openvideoeditor.misc.setupSystemUi
import java.io.File


class VideoEditorActivity : ComponentActivity() {

    private lateinit var viewModel: VideoEditorViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupSystemUi()

        viewModel = VideoEditorViewModel()

        window.decorView.setOnSystemUiVisibilityChangeListener {
            viewModel.setControlsVisible(it == 0)
        }

        var uri: String? = null
        if (intent.action == Intent.ACTION_EDIT) {
            intent.dataString?.let {
                uri = it
            }
        }

        uri?.let {
            setContent {
                viewModel = viewModel { viewModel }
                val controlsVisible by viewModel.controlsVisible.collectAsState()
                setImmersiveMode(!controlsVisible)
                System.out.println("VIDEDITOR-> videoeditoractivity-> " + it)

                VideoEditorScreen(it)
            }
        } ?: finish()
    }
}
