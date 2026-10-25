package dev.swipetune.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<MusicViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SwipeTuneTheme {
                val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
                    uri ?: return@rememberLauncherForActivityResult
                    runCatching {
                        contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                        )
                    }
                    viewModel.loadFolder(uri)
                }
                SwipeTuneApp(viewModel, onChooseFolder = { folderPicker.launch(null) })
            }
        }
    }
}

private enum class Screen { LIBRARY, REVIEW, REJECTED }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeTuneApp(viewModel: MusicViewModel, onChooseFolder: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var screen by remember { mutableStateOf(Screen.LIBRARY) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.statusMessage, state.errorMessage) {
        (state.errorMessage ?: state.statusMessage)?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    if (state.folderUri == null && !state.isScanning) {
        Onboarding(onChooseFolder)
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("SwipeTune", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onChooseFolder) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "Choose another folder")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = screen == Screen.LIBRARY,
                    onClick = { screen = Screen.LIBRARY },
                    icon = { Icon(Icons.Default.LibraryMusic, null) },
                    label = { Text("Library") },
                )
                NavigationBarItem(
                    selected = screen == Screen.REVIEW,
                    onClick = { screen = Screen.REVIEW },
                    icon = { Icon(Icons.Default.Style, null) },
                    label = { Text("Swipe") },
                )
                NavigationBarItem(
                    selected = screen == Screen.REJECTED,
                    onClick = { screen = Screen.REJECTED },
                    icon = { Icon(Icons.Default.Delete, null) },
                    label = { Text("Rejected (${state.rejectedSongs.size})") },
                )
            }
        },
    ) { padding ->
        if (state.isScanning) {
            LoadingScreen(Modifier.padding(padding))
        } else when (screen) {
            Screen.LIBRARY -> LibraryScreen(state, viewModel, Modifier.padding(padding))
            Screen.REVIEW -> ReviewScreen(state, viewModel, Modifier.padding(padding))
            Screen.REJECTED -> RejectedScreen(state, viewModel, Modifier.padding(padding))
        }
    }
}

@Composable
private fun Onboarding(onChooseFolder: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF171329), Color(0xFF0B0B10))))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Box(
                Modifier.size(104.dp).clip(RoundedCornerShape(32.dp)).background(Color(0xFF8B5CF6)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Default.MusicNote, null, tint = Color.White, modifier = Modifier.size(54.dp)) }
            Text("Your music. Your call.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "Play your local library normally, or review it one random song at a time. Your files and decisions stay on this device.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onChooseFolder, contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp)) {
                Icon(Icons.Default.FolderOpen, null)
                Spacer(Modifier.size(10.dp))
                Text("Choose music folder")
            }
        }
    }
}

@Composable
private fun LoadingScreen(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator()
            Text("Scanning audio files…")
        }
    }
}

@Composable
private fun LibraryScreen(state: AppUiState, viewModel: MusicViewModel, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Music library", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("${state.visibleSongs.size} songs · rejected tracks hidden", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FilledIconButton(onClick = viewModel::shuffleLibrary) { Icon(Icons.Default.Shuffle, "Play random song") }
        }
        LazyColumn(Modifier.weight(1f)) {
            items(state.visibleSongs, key = SongRef::uri) { song ->
                SongRow(song, selected = song.uri == state.nowPlayingUri, onClick = { viewModel.play(song) })
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .4f))
            }
        }
        AnimatedVisibility(state.nowPlayingUri != null) {
            CompactPlayer(state, viewModel)
        }
    }
}

@Composable
private fun SongRow(song: SongRef, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier.size(46.dp).clip(RoundedCornerShape(12.dp))
                .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Default.MusicNote, null, tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant) }
        Text(song.fileName.substringBeforeLast('.'), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        if (selected) Icon(Icons.Default.PlayArrow, null, tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun CompactPlayer(state: AppUiState, viewModel: MusicViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Artwork(state, Modifier.size(52.dp))
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(state.metadata?.title ?: "Loading…", maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                    Text(state.metadata?.artist ?: "", maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = viewModel::previousVisibleSong) { Icon(Icons.Default.SkipPrevious, "Previous") }
                FilledIconButton(onClick = viewModel::togglePlayPause) {
                    Icon(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, if (state.isPlaying) "Pause" else "Play")
                }
                IconButton(onClick = viewModel::nextVisibleSong) { Icon(Icons.Default.SkipNext, "Next") }
            }
            SeekBar(state, viewModel)
        }
    }
}

@Composable
private fun ReviewScreen(state: AppUiState, viewModel: MusicViewModel, modifier: Modifier = Modifier) {
    LaunchedEffect(state.currentSwipeSong?.uri) {
        val current = state.currentSwipeSong
        if (current != null && current.uri != state.nowPlayingUri) viewModel.play(current)
    }

    Column(
        modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Swipe review", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("${state.swipeCursor} of ${state.swipeOrder.size} · ${state.rejectedSongs.size} rejected", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = viewModel::undo, enabled = state.history.isNotEmpty()) { Icon(Icons.AutoMirrored.Filled.Undo, "Undo last swipe") }
        }
        Spacer(Modifier.height(10.dp))
        val song = state.currentSwipeSong
        if (song == null) {
            ReviewComplete(state, viewModel)
        } else {
            SwipeCard(
                state = state,
                song = song,
                onReject = { viewModel.decide(ReviewDecision.REJECTED) },
                onKeep = { viewModel.decide(ReviewDecision.KEPT) },
                modifier = Modifier.weight(1f),
            )
            PlayerControls(state, viewModel)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                FilledIconButton(
                    onClick = { viewModel.decide(ReviewDecision.REJECTED) },
                    modifier = Modifier.size(64.dp),
                ) { Icon(Icons.Default.Close, "Reject", tint = Color(0xFFFF8A80), modifier = Modifier.size(32.dp)) }
                OutlinedButton(onClick = viewModel::undo, enabled = state.history.isNotEmpty()) {
                    Icon(Icons.AutoMirrored.Filled.Undo, null)
                    Spacer(Modifier.size(8.dp))
                    Text("Undo")
                }
                FilledIconButton(
                    onClick = { viewModel.decide(ReviewDecision.KEPT) },
                    modifier = Modifier.size(64.dp),
                ) { Icon(Icons.Default.Check, "Keep", tint = Color(0xFF69F0AE), modifier = Modifier.size(32.dp)) }
            }
            Text("Swipe left to reject · right to keep", modifier = Modifier.padding(top = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SwipeCard(
    state: AppUiState,
    song: SongRef,
    onReject: () -> Unit,
    onKeep: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var offsetX by remember(song.uri) { mutableFloatStateOf(0f) }
    val threshold = 180f
    Card(
        modifier = modifier.fillMaxWidth().graphicsLayer {
            translationX = offsetX
            rotationZ = offsetX / 45f
        }.pointerInput(song.uri) {
            detectDragGestures(
                onDragEnd = {
                    when {
                        offsetX > threshold -> onKeep()
                        offsetX < -threshold -> onReject()
                    }
                    offsetX = 0f
                },
                onDragCancel = { offsetX = 0f },
            ) { change, dragAmount ->
                change.consume()
                offsetX += dragAmount.x
            }
        },
        colors = CardDefaults.cardColors(
            containerColor = when {
                offsetX > 50 -> Color(0xFF123B2D)
                offsetX < -50 -> Color(0xFF481D25)
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
        ),
        shape = RoundedCornerShape(28.dp),
    ) {
        Box(Modifier.fillMaxSize().padding(18.dp)) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Artwork(state, Modifier.fillMaxWidth().weight(1f))
                Spacer(Modifier.height(18.dp))
                Text(
                    state.metadata?.title ?: song.fileName.substringBeforeLast('.'),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    state.metadata?.artist ?: "Reading metadata…",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Text(state.metadata?.album ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            if (offsetX > 50) DecisionBadge("KEEP", Color(0xFF69F0AE), Modifier.align(Alignment.TopStart))
            if (offsetX < -50) DecisionBadge("REJECT", Color(0xFFFF8A80), Modifier.align(Alignment.TopEnd))
        }
    }
}

@Composable
private fun DecisionBadge(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(text, modifier.background(color.copy(alpha = .2f), RoundedCornerShape(10.dp)).padding(horizontal = 14.dp, vertical = 8.dp), color = color, fontWeight = FontWeight.Black)
}

@Composable
private fun Artwork(state: AppUiState, modifier: Modifier = Modifier) {
    val art = state.metadata?.artwork
    Box(modifier.clip(RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
        if (art != null) {
            Image(art.asImageBitmap(), "Album art", modifier = Modifier.fillMaxSize())
        } else {
            Icon(Icons.Default.MusicNote, null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun PlayerControls(state: AppUiState, viewModel: MusicViewModel) {
    Column(Modifier.fillMaxWidth()) {
        SeekBar(state, viewModel)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.seekBy(-10_000) }) { Icon(Icons.Default.FastRewind, "Back 10 seconds") }
            FilledIconButton(onClick = viewModel::togglePlayPause, modifier = Modifier.size(58.dp)) {
                Icon(if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, if (state.isPlaying) "Pause" else "Play", modifier = Modifier.size(30.dp))
            }
            IconButton(onClick = { viewModel.seekBy(10_000) }) { Icon(Icons.Default.FastForward, "Forward 10 seconds") }
        }
    }
}

@Composable
private fun SeekBar(state: AppUiState, viewModel: MusicViewModel) {
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    val max = state.durationMs.coerceAtLeast(1).toFloat()
    val shown = if (dragging) dragValue else state.positionMs.coerceAtMost(state.durationMs).toFloat()
    Column {
        Slider(
            value = shown.coerceIn(0f, max),
            onValueChange = { dragging = true; dragValue = it },
            onValueChangeFinished = { viewModel.seekTo(dragValue.toLong()); dragging = false },
            valueRange = 0f..max,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(shown.toLong()), style = MaterialTheme.typography.labelSmall)
            Text(formatTime(state.durationMs), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun ReviewComplete(state: AppUiState, viewModel: MusicViewModel) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.Check, null, modifier = Modifier.size(72.dp), tint = Color(0xFF69F0AE))
        Text("Review complete", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("${state.reviewedCount} decisions saved · ${state.rejectedSongs.size} rejected", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(18.dp))
        OutlinedButton(onClick = viewModel::undo, enabled = state.history.isNotEmpty()) {
            Icon(Icons.AutoMirrored.Filled.Undo, null)
            Spacer(Modifier.size(8.dp))
            Text("Undo last decision")
        }
        TextButton(onClick = viewModel::restartReview) {
            Icon(Icons.Default.Refresh, null)
            Spacer(Modifier.size(8.dp))
            Text("Review remaining undecided songs")
        }
    }
}

@Composable
private fun RejectedScreen(state: AppUiState, viewModel: MusicViewModel, modifier: Modifier = Modifier) {
    var confirmDelete by remember { mutableStateOf(false) }
    Column(modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Rejected", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Hidden from your normal library", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = { confirmDelete = true }, enabled = state.rejectedSongs.isNotEmpty() && !state.isDeleting) {
                Icon(Icons.Default.Delete, null)
                Spacer(Modifier.size(8.dp))
                Text("Delete files")
            }
        }
        if (state.rejectedSongs.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No rejected songs", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn {
                items(state.rejectedSongs, key = SongRef::uri) { song ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.MusicNote, null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(song.fileName.substringBeforeLast('.'), modifier = Modifier.weight(1f).padding(horizontal = 14.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        IconButton(onClick = { viewModel.restore(song) }) { Icon(Icons.Default.Restore, "Restore to library") }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            icon = { Icon(Icons.Default.Warning, null) },
            title = { Text("Permanently delete rejected files?") },
            text = { Text("This will permanently remove ${state.rejectedSongs.size} audio files from the selected folder. This cannot be undone.") },
            confirmButton = {
                Button(onClick = { confirmDelete = false; viewModel.deleteRejected() }) { Text("Delete permanently") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0) / 1000)
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

@Composable
private fun SwipeTuneTheme(content: @Composable () -> Unit) {
    val colors = androidx.compose.material3.darkColorScheme(
        primary = Color(0xFF9F7AEA),
        secondary = Color(0xFF2DD4BF),
        background = Color(0xFF0E0E12),
        surface = Color(0xFF18181F),
        surfaceVariant = Color(0xFF24242D),
    )
    MaterialTheme(colorScheme = colors, content = content)
}

