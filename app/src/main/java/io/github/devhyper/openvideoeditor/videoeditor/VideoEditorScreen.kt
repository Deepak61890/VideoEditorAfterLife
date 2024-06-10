package io.github.devhyper.openvideoeditor.videoeditor

import android.app.Activity
import android.content.Intent
import android.os.Handler
import android.os.Looper.getMainLooper
import android.util.Log
import android.view.TextureView
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Replay5
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import androidx.media3.common.Player.COMMAND_GET_CURRENT_MEDIA_ITEM
import androidx.media3.common.Player.Commands
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.Composition
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer.Listener
import io.github.devhyper.openvideoeditor.R
import io.github.devhyper.openvideoeditor.misc.AcceptDeclineRow
import io.github.devhyper.openvideoeditor.misc.DropdownSetting
import io.github.devhyper.openvideoeditor.misc.ListDialog
import io.github.devhyper.openvideoeditor.misc.PLAYER_SEEK_BACK_INCREMENT
import io.github.devhyper.openvideoeditor.misc.PLAYER_SEEK_FORWARD_INCREMENT
import io.github.devhyper.openvideoeditor.misc.REFRESH_RATE
import io.github.devhyper.openvideoeditor.misc.TextfieldSetting
import io.github.devhyper.openvideoeditor.misc.formatMinSec
import io.github.devhyper.openvideoeditor.misc.getFileNameFromUri
import io.github.devhyper.openvideoeditor.misc.toLongPair
import io.github.devhyper.openvideoeditor.misc.validateUInt
import io.github.devhyper.openvideoeditor.settings.SettingsDataStore
import io.github.devhyper.openvideoeditor.ui.theme.OpenVideoEditorTheme
import io.github.devhyper.openvideoeditor.utility.MyUtilsVideo
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VideoEditorScreen(
    uri: String,
) {

    val viewModel = viewModel { VideoEditorViewModel() }

    val context = LocalContext.current

    val dataStore = SettingsDataStore(context)

    val controlsVisible by viewModel.controlsVisible.collectAsState()

    val player = remember {
        ExoPlayer.Builder(context)
            .apply {
                setSeekBackIncrementMs(PLAYER_SEEK_BACK_INCREMENT)
                setSeekForwardIncrementMs(PLAYER_SEEK_FORWARD_INCREMENT)
            }
            .build()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        player.pause()
    }

    var currentTime by rememberSaveable { mutableLongStateOf(0L) }

    val transformManager = remember {
        viewModel.transformManager.apply {
            init(player, uri, context, viewModel)
            player.seekTo(currentTime)
        }
    }

    var listenerRepeating by remember { mutableStateOf(false) }

    var isPlaying by remember { mutableStateOf(player.isPlaying) }

    var fpm by remember { mutableFloatStateOf(0F) }

    var totalDuration by remember { mutableLongStateOf(0L) }

    var totalDurationFrames by remember { mutableLongStateOf(0L) }

    var currentTimeFrames by remember { mutableLongStateOf(0L) }

    var playbackState by remember { mutableIntStateOf(player.playbackState) }

    var playerViewSet by remember { mutableStateOf(false) }

    val currentEditingEffect by viewModel.currentEditingEffect.collectAsState()

    val useUiCascadingEffect by dataStore.getUiCascadingEffectAsync()
        .collectAsState(dataStore.getUiCascadingEffectBlocking())

    var textureView: TextureView? = null

    OpenVideoEditorTheme(forceDarkTheme = true, forceBlackStatusBar = true) {
        Surface(
            modifier = Modifier
                .fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box {
                DisposableEffect(key1 = Unit) {
                    val listenerHandler = Handler(getMainLooper())
                    val listener =
                        object : Player.Listener {
                            override fun onAvailableCommandsChanged(
                                availableCommands: Commands
                            ) {
                                super.onAvailableCommandsChanged(availableCommands)

                                if (!playerViewSet && availableCommands.contains(Player.COMMAND_SET_VIDEO_SURFACE) && textureView != null) {
                                    player.setVideoTextureView(textureView)
                                    playerViewSet = true
                                }

                                if (availableCommands.contains(COMMAND_GET_CURRENT_MEDIA_ITEM)) {
                                    viewModel.setFilterDurationEditorSliderPosition(0f..player.duration.toFloat())
                                }
                            }

                            override fun onEvents(
                                regularPlayer: Player,
                                events: Player.Events
                            ) {
                                super.onEvents(player, events)

                                if (player.duration > 0L) {
                                    totalDuration = player.duration
                                    fpm = (player.videoFormat?.frameRate ?: 0F) / 1000F
                                    if (fpm > 0F) {
                                        totalDurationFrames = (totalDuration * fpm).toLong()
                                    }
                                }

                                isPlaying = player.isPlaying
                                playbackState = player.playbackState

                                if (isPlaying) {
                                    if (!listenerRepeating) {
                                        listenerRepeating = true
                                        listenerHandler.post(
                                            object : Runnable {
                                                override fun run() {
                                                    currentTime =
                                                        player.currentPosition.coerceAtLeast(0L)
                                                    currentTimeFrames =
                                                        ((currentTime * fpm).toLong()).coerceAtMost(
                                                            totalDurationFrames
                                                        )
                                                    if (listenerRepeating) {
                                                        listenerHandler.postDelayed(
                                                            this,
                                                            REFRESH_RATE
                                                        )
                                                    }
                                                }
                                            }
                                        )
                                    }
                                } else {
                                    listenerRepeating = false
                                    currentTime =
                                        player.currentPosition.coerceAtLeast(0L)
                                    currentTimeFrames =
                                        ((currentTime * fpm).toLong()).coerceAtMost(
                                            totalDurationFrames
                                        )
                                }
                            }
                        }

                    player.addListener(listener)

                    onDispose {
                        player.removeListener(listener)
                        player.release()
                    }
                }

                val androidViewModifier = if (useUiCascadingEffect) {
                    Modifier.clickable { viewModel.setControlsVisible(!controlsVisible) }
                } else {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { viewModel.setControlsVisible(!controlsVisible) }
                }

                Box(modifier = Modifier.windowInsetsPadding(WindowInsets.systemBarsIgnoringVisibility)) {
                    AndroidView(
                        modifier = androidViewModifier,
                        factory = {
                            textureView = TextureView(context).apply {
                                layoutParams =
                                    FrameLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                            }
                            textureView!!
                        }
                    )

                    val videoFormat = player.videoFormat
                    if (videoFormat != null) {
                        Box(
                            modifier = Modifier
                                .width(videoFormat.width.dp)
                                .height(videoFormat.height.dp)
                                .align(Alignment.Center)
                                .windowInsetsPadding(WindowInsets.systemBarsIgnoringVisibility)
                        ) {
                            currentEditingEffect?.Editor()
                        }
                    }
                }

                PlayerControls(
                    modifier = Modifier
                        .fillMaxSize(),
                    isVisible = { controlsVisible },
                    isPlaying = { isPlaying },
                    title = { getFileNameFromUri(context, uri.toUri()) },
                    transformManager = transformManager,
                    playbackState = { playbackState },
                    onReplayClick = { player.seekBack() },
                    onForwardClick = { player.seekForward() },
                    onPauseToggle = {
                        when {
                            player.isPlaying -> {
                                player.pause()
                            }

                            player.isPlaying.not() &&
                                    playbackState == Player.STATE_ENDED -> {
                                player.seekTo(0)
                                player.playWhenReady = true
                            }

                            else -> {
                                player.play()
                            }
                        }
                        isPlaying = isPlaying.not()
                    },
                    fpm = { fpm },
                    totalDuration = { totalDuration },
                    totalDurationFrames = { totalDurationFrames },
                    currentTime = { currentTime },
                    currentTimeFrames = { currentTimeFrames }
                ) { timeMs: Float ->
                    player.seekTo(timeMs.toLong())
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun PlayerControls(
    modifier: Modifier = Modifier,
    isVisible: () -> Boolean,
    isPlaying: () -> Boolean,
    title: () -> String,
    transformManager: TransformManager,
    onReplayClick: () -> Unit,
    onForwardClick: () -> Unit,
    onPauseToggle: () -> Unit,
    fpm: () -> Float,
    totalDuration: () -> Long,
    totalDurationFrames: () -> Long,
    currentTime: () -> Long,
    currentTimeFrames: () -> Long,
    playbackState: () -> Int,
    onSeekChanged: (timeMs: Float) -> Unit
) {

    val visible = remember(isVisible()) { isVisible() }

    AnimatedVisibility(
        modifier = modifier,
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = SolidColor(MaterialTheme.colorScheme.scrim),
                    alpha = 0.5F
                )
        ) {
            Box(
                modifier = Modifier
                    .safeContentPadding()
                    .fillMaxSize()
            ) {
                TopControls(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth(),
                    title = title,
                    transformManager = transformManager,
                )

                CenterControls(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(),
                    isPlaying = isPlaying,
                    onReplayClick = onReplayClick,
                    onForwardClick = onForwardClick,
                    onPauseToggle = onPauseToggle,
                    playbackState = playbackState
                )

                BottomControls(
                    modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .animateEnterExit(
                            enter =
                            slideInVertically(
                                initialOffsetY = { fullHeight: Int ->
                                    fullHeight
                                }
                            ),
                            exit =
                            slideOutVertically(
                                targetOffsetY = { fullHeight: Int ->
                                    fullHeight
                                }
                            )
                        ),
                    fpm = fpm,
                    totalDuration = totalDuration,
                    totalDurationFrames = totalDurationFrames,
                    currentTime = currentTime,
                    currentTimeFrames = currentTimeFrames,
                    onSeekChanged = onSeekChanged,
                    transformManager = transformManager
                )
            }
        }
    }
}

@Composable
private fun TopControls(
    modifier: Modifier = Modifier,
    title: () -> String,
    transformManager: TransformManager,
) {
    val activity = LocalContext.current as Activity
    val viewModel = viewModel { VideoEditorViewModel() }
    val projectOutputPath by viewModel.projectOutputPath.collectAsState()
    val projectSavingSupported by viewModel.projectSavingSupported.collectAsState()
    val videoTitle = remember(title()) { title() }
    var showThreeDotMenu by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.padding(top = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { activity.finish() }) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back)
            )
        }
        /* below text are used to show the video title*/
        Text(
            text = videoTitle,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            modifier = Modifier.weight(1f, false)
        )

        if (projectOutputPath.isNotEmpty()) {
            transformManager.projectData.write(projectOutputPath, activity)
            viewModel.setProjectOutputPath("")
        }

        IconButton(onClick = { showThreeDotMenu = !showThreeDotMenu }) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.more_vertical_options)
            )
            DropdownMenu(
                expanded = showThreeDotMenu,
                onDismissRequest = { showThreeDotMenu = false },
                content = {
                    /* DropdownMenuItem(
                         text = { Text(stringResource(R.string.settings)) },
                         onClick = {
                             showThreeDotMenu = false
                             val intent = Intent(activity, SettingsActivity::class.java)
                             activity.startActivity(intent)
                         })*/
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.export)) },
                        onClick = { showThreeDotMenu = false; showExportDialog = true })
                    /*DropdownMenuItem(
                        enabled = projectSavingSupported,
                        text = { Text(stringResource(R.string.save_project)) },
                        onClick = {
                            showThreeDotMenu = false
                            val dotIndex: Int = videoTitle.lastIndexOf('.')
                            val projectName: String =
                                videoTitle.substring(0, dotIndex) + "." + PROJECT_FILE_EXT
                            createProject.launch(projectName)
                        })*/
                })
        }
    }

    if (showExportDialog) {
        System.out.println("VIDEDITOR-> " + "526 " + showExportDialog)
        ExportDialog(transformManager, videoTitle, activity) {
            showExportDialog = false
        }
    }
}

/* below composable are used for 5 sec back play, play/pause and 10 sec forward*/
@Composable
private fun CenterControls(
    modifier: Modifier = Modifier,
    isPlaying: () -> Boolean,
    playbackState: () -> Int,
    onReplayClick: () -> Unit,
    onPauseToggle: () -> Unit,
    onForwardClick: () -> Unit
) {
    val isVideoPlaying = remember(isPlaying()) { isPlaying() }

    val playerState = remember(playbackState()) { playbackState() }

    Row(modifier = modifier, horizontalArrangement = Arrangement.SpaceEvenly) {
        IconButton(modifier = Modifier.size(40.dp), onClick = onReplayClick) {
            Icon(
                modifier = Modifier.fillMaxSize(),
                imageVector = Icons.Filled.Replay5,
                contentDescription = stringResource(R.string.replay_5_seconds),
            )
        }

        IconButton(modifier = Modifier.size(40.dp), onClick = onPauseToggle) {
            Icon(
                modifier = Modifier.fillMaxSize(),
                imageVector =
                when {
                    isVideoPlaying -> {
                        Icons.Filled.Pause
                    }

                    isVideoPlaying.not() && playerState == Player.STATE_ENDED -> {
                        Icons.Filled.Replay
                    }

                    else -> {
                        Icons.Filled.PlayArrow
                    }
                },
                contentDescription = stringResource(R.string.play_pause),
            )
        }

        IconButton(modifier = Modifier.size(40.dp), onClick = onForwardClick) {
            Icon(
                modifier = Modifier.fillMaxSize(),
                imageVector = Icons.Filled.Forward10,
                contentDescription = stringResource(R.string.forward_10_seconds),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomControls(
    modifier: Modifier = Modifier,
    fpm: () -> Float,
    totalDuration: () -> Long,
    totalDurationFrames: () -> Long,
    currentTime: () -> Long,
    currentTimeFrames: () -> Long,
    onSeekChanged: (timeMs: Float) -> Unit,
    transformManager: TransformManager
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val filterSheetState = rememberModalBottomSheetState()
    val layerSheetState = rememberModalBottomSheetState()
    var showFilterBottomSheet by remember { mutableStateOf(false) }
    var showLayerBottomSheet by remember { mutableStateOf(false) }
    var showFrameDialog by remember { mutableStateOf(false) }
    var isTrim by remember { mutableStateOf(false) }
    var startTime by remember { mutableStateOf(0L) }
    var endTime by remember { mutableStateOf(totalDuration()) }

    val videoFpm = remember(fpm()) { fpm() }
    val duration = remember(totalDuration()) { totalDuration() }
    val durationFrames = remember(totalDurationFrames()) { totalDurationFrames() }
    val videoTime = remember(currentTime()) { currentTime() }
    val videoTimeFrames = remember(currentTimeFrames()) { currentTimeFrames() }

    val viewModel = viewModel { VideoEditorViewModel() }

    val currentEditingEffect by viewModel.currentEditingEffect.collectAsState()

    val filterDurationEditorEnabled by viewModel.filterDurationEditorEnabled.collectAsState()
    val filterDurationCallback by viewModel.filterDurationCallback.collectAsState()
    val prevFilterDurationEditorSliderPosition by viewModel.prevFilterDurationEditorSliderPosition.collectAsState()
    val filterDurationEditorSliderPosition by viewModel.filterDurationEditorSliderPosition.collectAsState()

    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (filterDurationEditorEnabled) {
                RangeSlider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    value = filterDurationEditorSliderPosition,
                    onValueChange = { rangeArg ->
                        var range = rangeArg
                        if (range.endInclusive == 0f) {
                            range = range.start..1f
                        }
                        viewModel.setPrevFilterDurationEditorSliderPosition(
                            filterDurationEditorSliderPosition
                        )
                        viewModel.setFilterDurationEditorSliderPosition(range)
                        if (prevFilterDurationEditorSliderPosition.start != filterDurationEditorSliderPosition.start) {
                            //onSeekChanged(range.start)
                            if (range.start <= duration.toFloat() / 2) {
                                startTime = range.start.toLong()
                            } else {
                                endTime = range.start.toLong()
                            }
                        } else {
                           // onSeekChanged(range.endInclusive)
                            if (range.endInclusive <= duration.toFloat() / 2) {
                                startTime = range.endInclusive.toLong()
                            } else {
                                endTime = range.endInclusive.toLong()
                            }
                        }
                    },
                    colors = SliderDefaults.colors(
                        inactiveTrackColor = MaterialTheme.colorScheme.inversePrimary
                    ),
                    valueRange = 0f..duration.toFloat(),
                )
            } else {
                Slider(
                    modifier = Modifier
                        .fillMaxWidth(),
                    value = videoTime.toFloat(),
                    onValueChange = onSeekChanged,
                    colors = SliderDefaults.colors(
                        inactiveTrackColor = MaterialTheme.colorScheme.inversePrimary
                    ),
                    valueRange = 0f..duration.toFloat(),
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isTrim){
                if(endTime.toInt()==0){
                    endTime=duration
                }
                Text(
                    modifier = Modifier
                        .weight(2f, false),
                    text = "Start Time: " + startTime.formatMinSec()
                )
                Text(
                    modifier = Modifier
                        .weight(2f, false),
                    text = "End Time: " + endTime.formatMinSec()
                )
            }else{
                Text(
                    modifier = Modifier
                        .weight(2f, false),
                    text = videoTime.formatMinSec()+"/"+ duration.formatMinSec()
                )
            }
            /*Row(
                modifier = Modifier.weight(2f, false),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    modifier = Modifier
                        .weight(1f, false)
                        .repeatingClickable(remember { MutableInteractionSource() },
                            true,
                            onClick = {
                                onSeekChanged((videoTime.toFloat() - (1F / videoFpm)) + 1F)
                            }), onClick = {}) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.decrement_frame)
                    )
                }
                Text(
                    modifier = Modifier
                        .weight(1f, false)
                        .clickable { showFrameDialog = true },
                    text = "$videoTimeFrames/$durationFrames",
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
                IconButton(
                    modifier = Modifier
                        .weight(1f, false)
                        .repeatingClickable(remember { MutableInteractionSource() },
                            true,
                            onClick = {
                                onSeekChanged((videoTime.toFloat() + (1F / videoFpm)) + 1F)
                            }), onClick = {}) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.increment_frame)
                    )
                }
            }*/

            if (filterDurationEditorEnabled || currentEditingEffect != null) {
                AcceptDeclineRow(
                    modifier = Modifier.weight(1f),
                    acceptDescription = stringResource(R.string.accept_filter),
                    acceptOnClick = {
                        isTrim=false
                        val currentEditingEffectLocal = currentEditingEffect
                        if (currentEditingEffectLocal != null) {
                            currentEditingEffectLocal.runCallback()
                            viewModel.setCurrentEditingEffect(null)
                        } else {
                            viewModel.setFilterDurationEditorEnabled(false)
                            filterDurationCallback(
                                LongRange(
                                    filterDurationEditorSliderPosition.start.toLong(),
                                    filterDurationEditorSliderPosition.endInclusive.toLong()
                                )
                            )
                        }
                    },
                    declineDescription = stringResource(R.string.decline_filter),
                    declineOnClick = {
                        viewModel.setCurrentEditingEffect(null)
                        viewModel.setFilterDurationEditorEnabled(false)
                        isTrim=false
                    }
                )
            } else {
                Row(
                    modifier = Modifier.weight(1f),
                ) {
                    IconButton(modifier = Modifier.weight(1f), onClick = {
                        showLayerBottomSheet = true
                    }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Layers,
                            contentDescription = stringResource(R.string.open_layer_drawer)
                        )
                    }
                    IconButton(modifier = Modifier.weight(1f), onClick = {
                        showFilterBottomSheet = true
                        isTrim = true
                    }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.open_filter_drawer)
                        )
                    }
                }
            }
        }
    }
    if (showFilterBottomSheet) {
        ModalBottomSheet(
            modifier = Modifier.fillMaxSize(),
            onDismissRequest = {
                showFilterBottomSheet = false
            },
            sheetState = filterSheetState
        ) {
            FilterDrawer(transformManager) {
                scope.launch { filterSheetState.hide() }.invokeOnCompletion {
                    if (!filterSheetState.isVisible) {
                        showFilterBottomSheet = false
                    }
                }
            }
        }
    } else if (showLayerBottomSheet) {
        ModalBottomSheet(
            modifier = Modifier.fillMaxSize(),
            onDismissRequest = {
                showLayerBottomSheet = false
            },
            sheetState = layerSheetState
        ) {
            LayerDrawer(transformManager)
        }
    } else if (showFrameDialog) {
        var newFrame by remember { mutableLongStateOf(-1L) }
        ListDialog(
            title = stringResource(R.string.frames),
            dismissText = stringResource(R.string.dismiss),
            acceptText = stringResource(R.string.accept),
            onDismissRequest = { showFrameDialog = false },
            onAcceptRequest = {
                if (newFrame >= 0L) {
                    showFrameDialog = false; onSeekChanged((newFrame / videoFpm) + 1F)
                }
            },
            listItems = {
                item {
                    Text("$videoTimeFrames/$durationFrames")
                    TextfieldSetting(
                        name = stringResource(R.string.new_frame),
                        onValueChanged = {
                            val errorTxt = validateUInt(it)
                            if (errorTxt.isEmpty()) {
                                val newLongFrame = it.toLong()
                                if (newLongFrame <= durationFrames) {
                                    newFrame = newLongFrame
                                } else {
                                    newFrame = -1L
                                    return@TextfieldSetting context.getString(R.string.input_frame_must_less_or_equal) + "$durationFrames"
                                }
                            }
                            errorTxt
                        })
                }
            }
        )
    }
}

@Composable
private fun LayerDrawer(transformManager: TransformManager) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stringResource(R.string.video_layers),
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.headlineMedium
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
        ) {
            items(transformManager.projectData.videoEffects)
            { effect ->
                LayerDrawerItem(
                    stringResId = effect.stringResId,
                    icon = effect.icon(),
                    range = 0L..transformManager.player.duration,
                    onClick = {
                        transformManager.removeVideoEffect(effect)
                    }
                )
            }
            /*
            items(transformManager.projectData.audioProcessors)
            { processor ->
                LayerDrawerItem(
                    stringResId = processor.toString(),
                    icon = Icons.Filled.Audiotrack,
                    range = 0L..transformManager.player.duration,
                    onClick = {
                        transformManager.removeAudioProcessor(processor)
                    }
                )
            }
            */
            val trim = transformManager.getMergedTrim()
            if (trim != null) {
                item()
                {
                    LayerDrawerItem(
                        stringResId = R.string.trim,
                        icon = Icons.Filled.ContentCut,
                        range = trim.first..trim.second,
                        onClick = {
                            transformManager.clearMediaTrims()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LayerDrawerItem(
    stringResId: Int,
    icon: ImageVector,
    range: LongRange,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(imageVector = icon, contentDescription = stringResource(R.string.layer_icon))
            Column(
                modifier = Modifier
                    .padding(start = 16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = stringResource(stringResId))
                Text(
                    text = "${range.first.formatMinSec()}:${range.last.formatMinSec()}",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = stringResource(R.string.remove_filter)
            )
        }
    }
}

@Composable
private fun FilterDrawer(transformManager: TransformManager, onDismissRequest: () -> Unit) {
    val viewModel = viewModel { VideoEditorViewModel() }
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stringResource(R.string.video_filters),
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.headlineMedium
        )
        LazyVerticalGrid(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            columns = GridCells.Adaptive(120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                FilterDrawerItem(R.string.trim, Icons.Filled.ContentCut, onClick = {
                    viewModel.setFilterDurationEditorEnabled(true)
                    viewModel.setFilterDurationCallback { range ->
                        transformManager.addMediaTrim(
                            range.toLongPair()
                        )
                    }
                    onDismissRequest()
                })
            }
            items(userEffectsArray) { userEffect ->
                userEffect.run {
                    FilterDrawerItem(
                        stringResId,
                        icon(),
                        onClick = { transformManager.addVideoEffect(this) }
                    )
                }
            }
            items(dialogUserEffectsArray) { dialogUserEffect ->
                dialogUserEffect.run {
                    DialogFilterDrawerItem(
                        stringResId,
                        icon,
                        args,
                        transformManager,
                        callback
                    )
                }
            }
            items(onVideoUserEffectsArray) { onVideoUserEffect ->
                onVideoUserEffect.run {
                    FilterDrawerItem(stringResId, icon()) {
                        callback = {
                            transformManager.addVideoEffect(UserEffect(stringResId, icon, it))
                        }
                        viewModel.setCurrentEditingEffect(this)
                        onDismissRequest()
                        viewModel.setControlsVisible(false)
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogFilterDrawerItem(
    stringResId: Int,
    icon: ImageConstructor,
    args: PersistentList<EffectDialogSetting>,
    transformManager: TransformManager,
    callback: (Map<String, String>) -> EffectConstructor
) {
    val viewModel = viewModel { VideoEditorViewModel() }
    var showFilterDialog by remember { mutableStateOf(false) }
    FilterDrawerItem(
        stringResId,
        icon(),
        onClick = { showFilterDialog = true; viewModel.setFilterDialogArgs(args) })
    if (showFilterDialog) {
        FilterDialog(stringResId = stringResId, { argMap ->
            val effect = callback(argMap)
            UserEffect(stringResId, icon, effect)
        }, transformManager) {
            showFilterDialog = false
        }
    }
}

@Composable
private fun FilterDrawerItem(
    stringResId: Int,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = stringResource(stringResId)
            )
        }
        Text(
            textAlign = TextAlign.Center,
            softWrap = false,
            text = stringResource(stringResId)
        )
    }
}

@Composable
private fun FilterDialog(
    stringResId: Int,
    callback: (Map<String, String>) -> UserEffect,
    transformManager: TransformManager,
    onDismissRequest: () -> Unit
) {
    val viewModel = viewModel { VideoEditorViewModel() }
    val args by viewModel.filterDialogArgs.collectAsState()
    ListDialog(
        title = stringResource(stringResId),
        dismissText = stringResource(R.string.cancel),
        acceptText = stringResource(R.string.add),
        onDismissRequest = onDismissRequest,
        onAcceptRequest = {
            var error = false
            val callbackArgsMap = mutableMapOf<String, String>()
            for (arg in args) {
                val string = arg.selection
                if (string.isEmpty()) {
                    error = true
                    break
                }
                callbackArgsMap[arg.key] = string
            }
            if (!error) {
                val userEffect = callback(callbackArgsMap.toMap())
                transformManager.addVideoEffect(userEffect)
                onDismissRequest()
            }
        },
    ) {
        for (arg in args) {

            val textfield = arg.textfieldValidation
            println("ROTATE-> txt" + arg.textfieldValidation)
            println("ROTATE-> drop" + arg.dropdownOptions)
            val dropdown = arg.dropdownOptions
            /*if (textfield != null) {
                item {
                    TextfieldSetting(
                        name = stringResource(arg.stringResId),
                        onValueChanged = {
                            val error = textfield(it)
                            if (error.isEmpty()) {
                                arg.selection = it
                            } else {
                                arg.selection = ""
                            }
                            viewModel.setFilterDialogArgs(args)
                            error
                        })
                }
            } else*/ if (dropdown != null) {
                item {
                    DropdownSetting(
                        name = stringResource(arg.stringResId),
                        options = dropdown.toImmutableList()
                    ) {
                        arg.selection = it
                        viewModel.setFilterDialogArgs(args)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportDialog(
    transformManager: TransformManager,
    title: String,
    activity: Activity,
    onDismissRequest: () -> Unit
) {
    println("VIDEDITOR-> 1109 $title")
    val context = LocalContext.current
    val exportSettings: ExportSettings by remember { mutableStateOf(ExportSettings()) }
    val viewModel = viewModel { VideoEditorViewModel() }
    val outputPath by viewModel.outputPath.collectAsState()
    var exportString: String? by remember { mutableStateOf(null) }
    var showExportProgress by remember { mutableStateOf(false) }
    exportSettings.setMediaToExportString("Video and Audio")
    exportSettings.setHdrModeString("Keep HDR")
    exportSettings.setAudioMimeTypeString("Original")
    exportSettings.setVideoMimeTypeString("Original")

    val transformerListener: Listener =
        object : Listener {
            override fun onError(
                composition: Composition, result: ExportResult,
                exception: ExportException
            ) {
                exportString = exception.toString()
              //  showExportProgress = false  // Hide the dialog on error

                Log.e("open-video-editor", "Export exception: ", exception)
            }
        }
    val onFFmpegError: () -> Unit = {
      //  showExportProgress = false
        exportString = context.getString(R.string.ffmpeg_error)
    }
    transformManager.export(
        context,
        exportSettings,
        transformerListener,
    )
    ExportProgressDialog(transformManager) {
     //   showExportProgress = false
        onDismissRequest()
        val galURI = MyUtilsVideo.saved_vid_uri
        println("VIDEDITOR-> recorded_vid_uri-> $galURI")

        /*val intent =
            Intent(context, VideoEditorActivity::class.java).apply {
                action = Intent.ACTION_EDIT
                data = galURI
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
        context.startActivity(intent)*/
    }
}

@Composable
fun ExportProgressDialog(
    transformManager: TransformManager,
    onDismissRequest: () -> Unit
) {
    System.out.println("VIDEDITOR-> ExportProgressDialog" + "1208")

    var exportProgress by remember { mutableFloatStateOf(0F) }
    val animatedProgress = animateFloatAsState(
        targetValue = exportProgress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "export_progress_animation"
    ).value
    val exportComplete = exportProgress == 1F
    val progressHandler = Handler(getMainLooper())
    progressHandler.postDelayed(
        object : Runnable {
            override fun run() {
                exportProgress = transformManager.getProgress()
                if (exportProgress != 1F && exportProgress != -1F) {
                    progressHandler.postDelayed(this, REFRESH_RATE)
                }
            }
        }, REFRESH_RATE
    )
    Dialog(onDismissRequest = {
        if (exportComplete) {
            onDismissRequest()
        }
    }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(215.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (exportComplete) stringResource(R.string.exported) else stringResource(
                        R.string.exporting
                    ),
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(16.dp)
                )
                Column(verticalArrangement = Arrangement.SpaceBetween) {
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.padding(vertical = 4.dp),
                        trackColor = MaterialTheme.colorScheme.inversePrimary,
                    )
                    Text(
                        modifier = Modifier.padding(vertical = 4.dp),
                        text = "${(exportProgress * 100).toInt()}%"
                    )
                }
                TextButton(
                    onClick = {
                        if (!exportComplete) {
                            transformManager.cancel()
                        }
                        onDismissRequest()
                    },
                ) {
                    Text(if (exportComplete) stringResource(R.string.dismiss) else stringResource(R.string.cancel))
                }
            }
        }
    }
}


/*@Composable
fun ExportProgressDialog( //
    transformManager: TransformManager,
    onDismissRequest: () -> Unit
) {
    var exportProgress by remember { mutableFloatStateOf(0F) }
    val animatedProgress = animateFloatAsState(
        targetValue = exportProgress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "export_progress_animation"
    ).value
    val exportComplete = exportProgress == 1F
    val progressHandler = Handler(getMainLooper())
    progressHandler.postDelayed(
        object : Runnable {
            override fun run() {
                exportProgress = transformManager.getProgress()
                if (exportProgress != 1F && exportProgress != -1F) {
                    progressHandler.postDelayed(this, REFRESH_RATE)
                }
            }
        }, REFRESH_RATE
    )
    Dialog(onDismissRequest = {
        if (exportComplete) {
            onDismissRequest()
        }
    }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(215.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (exportComplete) stringResource(R.string.exported) else stringResource(
                        R.string.exporting
                    ),
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(16.dp)
                )
                Column(verticalArrangement = Arrangement.SpaceBetween) {
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.padding(vertical = 4.dp),
                        trackColor = MaterialTheme.colorScheme.inversePrimary,
                    )
                    Text(
                        modifier = Modifier.padding(vertical = 4.dp),
                        text = "${(exportProgress * 100).toInt()}%"
                    )
                }
                TextButton(
                    onClick = {
                        if (!exportComplete) {
                            transformManager.cancel()
                        }
                        onDismissRequest()
                    },
                ) {
                    Text(if (exportComplete) stringResource(R.string.dismiss) else stringResource(R.string.cancel))
                }
            }
        }
    }
}*/

@Composable
fun ExportFailedAlertDialog(exceptionString: String, onDismissRequest: () -> Unit) {
    AlertDialog(
        title = {
            Text(text = stringResource(R.string.error))
        },
        text = {
            Text(text = exceptionString)
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {

        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text(stringResource(R.string.dismiss))
            }
        }
    )
}
