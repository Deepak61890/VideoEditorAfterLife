package io.github.devhyper.openvideoeditor.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import io.github.devhyper.openvideoeditor.R
import io.github.devhyper.openvideoeditor.camera_rec.MainActivityCamera
import io.github.devhyper.openvideoeditor.ui.theme.OpenVideoEditorTheme
import io.github.devhyper.openvideoeditor.utility.MyUtilsVideo
import io.github.devhyper.openvideoeditor.videoeditor.VideoEditorActivity
import java.io.File


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenVideo() {
    val activity = LocalContext.current as Activity
    val context = LocalContext.current
    var videoUri by remember { mutableStateOf<Uri?>(null) }

    // ActivityResultLauncher to pick a video
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        videoUri = uri
        val intent = Intent(context, VideoEditorActivity::class.java)
        intent.action = Intent.ACTION_EDIT
        intent.data = videoUri
        context.startActivity(intent)
    }

    OpenVideoEditorTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                stringResource(R.string.app_name),
                            )
                        },
                        /* actions = {
                             IconButton(onClick = {
                                 val intent = Intent(activity, SettingsActivity::class.java)
                                 activity.startActivity(intent)
                             }) {
                                 Icon(
                                     imageVector = Icons.Filled.Settings,
                                     contentDescription = stringResource(R.string.settings)
                                 )
                             }
                         }*/
                    )
                }, content = { innerPadding ->
                    Box(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                    ) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            verticalArrangement = Arrangement.spacedBy(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        )
                        {
                            Text(
                                stringResource(R.string.select_a_file_to_edit),
                                // style = MaterialTheme.typography.headlineLarge,
                                textAlign = TextAlign.Center
                            )
                            Button(onClick = {
                                System.out.println("VIDEDITOR-> MainScreen")
                                videoPickerLauncher.launch("video/*")


                            }, modifier = Modifier) {
                                Text(
                                    style = MaterialTheme.typography.titleLarge,
                                    text = stringResource(R.string.video)
                                )
                            }
                            Spacer(modifier = Modifier.padding(16.dp))
                            Button(onClick = {
                                val intent = Intent(
                                    context,
                                    MainActivityCamera::class.java
                                )
                                context.startActivity(intent)
                            }) {
                                Text(text = "Record Video")
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = {
                                /*val filePath =
                                    "/storage/emulated/0/Android/data/io.github.veditor.debug/files/afterlife/output_video.mp4"
                                val file = File(filePath)
                                val uri =
                                    FileProvider.getUriForFile(
                                        context,
                                        "io.github.veditor.debug.fileprovider",
                                        file
                                    )*/

                                //  "content://media/external/video/media/1000036094"

                                val galURI = MyUtilsVideo.saved_vid_uri
                                println("VIDEDITOR-> recorded_vid_uri-> $galURI")

                                val intent =
                                    Intent(context, VideoEditorActivity::class.java).apply {
                                        action = Intent.ACTION_EDIT
                                        data = galURI
                                       // flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    }
                                context.startActivity(intent)
                            }) {
                                Text(text = "Recorded Video")
                            }
                        }
                    }
                }
            )
        }
    }
}

fun getVideoFileUri(context: Context): Uri {

    val videoFileName = "output_video.mp4"
    val videoFile = File(context.filesDir, videoFileName)
    if (!videoFile.exists()) {
        System.out.println("VIDEDITOR-> " + videoFile)
    }
    // Generate URI for the file using FileProvider
    //return Uri.fromFile(videoFile)

    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        videoFile
    )
}