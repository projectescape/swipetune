package dev.swipetune.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlin.math.abs

private enum class ExpressiveScreen { LIBRARY, REVIEW, REJECTED }

@Composable
internal fun ExpressiveSwipeTuneApp(viewModel: MusicViewModel, onChooseFolder: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var screen by remember { mutableStateOf(ExpressiveScreen.LIBRARY) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.statusMessage, state.errorMessage) {
        (state.errorMessage ?: state.statusMessage)?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    if (state.folderUri == null && !state.isScanning) {
        ExpressiveOnboarding(onChooseFolder)
        return
    }

    Box(Modifier.fillMaxSize()) {
        LivingBackdrop(state)
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = { ExpressiveHeader(onChooseFolder) },
            bottomBar = {
                FloatingNavigation(
                    screen = screen,
                    onScreen = { screen = it },
                )
            },
        ) { padding ->
            if (state.isScanning) {
                ExpressiveLoading(Modifier.padding(padding))
            } else {
                AnimatedContent(
                    targetState = screen,
                    modifier = Modifier.padding(padding),
                    transitionSpec = {
                        (fadeIn(tween(260)) + scaleIn(tween(320), initialScale = .985f)) togetherWith
                            (fadeOut(tween(170)) + scaleOut(tween(170), targetScale = 1.015f))
                    },
                    label = "screen",
                ) { destination ->
                    when (destination) {
                        ExpressiveScreen.LIBRARY -> ExpressiveLibrary(state, viewModel)
                        ExpressiveScreen.REVIEW -> ExpressiveReview(state, viewModel)
                        ExpressiveScreen.REJECTED -> ExpressiveRejected(state, viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun LivingBackdrop(state: AppUiState) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(colors.surface, colors.background))),
    ) {
        state.metadata?.artwork?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { scaleX = 1.28f; scaleY = 1.28f }
                    .blur(72.dp)
                    .alpha(.24f),
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            colors.background.copy(alpha = .20f),
                            colors.background.copy(alpha = .78f),
                            colors.background.copy(alpha = .96f),
                        ),
                    ),
                ),
        )
    }
}

@Composable
private fun FrostedSurface(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(28.dp),
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = .78f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .38f)),
        content = content,
    )
}

@Composable
private fun ExpressiveHeader(onChooseFolder: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 22.dp, end = 14.dp, top = 10.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "SwipeTune",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                "Listen freely. Keep deliberately.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FrostedSurface(shape = RoundedCornerShape(20.dp)) {
            IconButton(onClick = onChooseFolder, modifier = Modifier.size(52.dp)) {
                Icon(Icons.Default.FolderOpen, contentDescription = "Choose music folder")
            }
        }
    }
}

@Composable
private fun FloatingNavigation(
    screen: ExpressiveScreen,
    onScreen: (ExpressiveScreen) -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        FrostedSurface(shape = RoundedCornerShape(34.dp)) {
            Row(
                Modifier.padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                NavigationDestination(
                    selected = screen == ExpressiveScreen.LIBRARY,
                    icon = { Icon(Icons.Default.LibraryMusic, null) },
                    label = "Library",
                    onClick = { onScreen(ExpressiveScreen.LIBRARY) },
                )
                NavigationDestination(
                    selected = screen == ExpressiveScreen.REVIEW,
                    icon = { Icon(Icons.Default.Style, null) },
                    label = "Review",
                    onClick = { onScreen(ExpressiveScreen.REVIEW) },
                )
                NavigationDestination(
                    selected = screen == ExpressiveScreen.REJECTED,
                    icon = { Icon(Icons.Default.DeleteOutline, null) },
                    label = "Rejected",
                    onClick = { onScreen(ExpressiveScreen.REJECTED) },
                )
            }
        }
    }
}

@Composable
private fun NavigationDestination(
    selected: Boolean,
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
) {
    val background = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    Row(
        Modifier
            .width(94.dp)
            .height(54.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(background)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        icon()
        Text(
            label,
            modifier = Modifier.padding(start = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
        )
    }
}

@Composable
private fun ExpressiveOnboarding(onChooseFolder: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        LivingBackdrop(AppUiState())
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("SwipeTune", style = MaterialTheme.typography.titleLarge)
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Surface(
                    modifier = Modifier.size(108.dp),
                    shape = RoundedCornerShape(topStart = 40.dp, topEnd = 24.dp, bottomEnd = 40.dp, bottomStart = 24.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.GraphicEq,
                            contentDescription = null,
                            modifier = Modifier.size(54.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
                Text("Hear everything.\nChoose what stays.", style = MaterialTheme.typography.displayLarge)
                Text(
                    "Pick any folder. SwipeTune plays the music already on your phone, then gives you a fast, private way to shape the library you actually want.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = onChooseFolder,
                    modifier = Modifier.height(58.dp),
                    shape = RoundedCornerShape(22.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp),
                ) {
                    Icon(Icons.Default.FolderOpen, null)
                    Spacer(Modifier.width(10.dp))
                    Text("Choose music folder")
                }
            }
            Text(
                "Your music and decisions stay on this device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ExpressiveLoading(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        FrostedSurface {
            Row(
                Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
                Column {
                    Text("Reading your library", style = MaterialTheme.typography.titleMedium)
                    Text("Finding audio and artwork…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun ExpressiveLibrary(state: AppUiState, viewModel: MusicViewModel) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 22.dp, end = 16.dp, top = 18.dp, bottom = 12.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Your library", style = MaterialTheme.typography.headlineLarge)
                Text(
                    "${state.visibleSongs.size} songs · rejected tracks stay hidden",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            FilledIconButton(
                onClick = viewModel::shuffleLibrary,
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(20.dp),
            ) { Icon(Icons.Default.Shuffle, "Play a random song") }
        }

        if (state.visibleSongs.isEmpty()) {
            EmptyLibrary(Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(state.visibleSongs, key = SongRef::uri) { song ->
                    ExpressiveSongRow(song, song.uri == state.nowPlayingUri) { viewModel.play(song) }
                }
            }
        }

        AnimatedVisibility(
            visible = state.nowPlayingUri != null,
            enter = fadeIn(tween(220)) + scaleIn(tween(260), initialScale = .98f),
            exit = fadeOut(tween(160)),
        ) {
            CompactGlassPlayer(state, viewModel)
        }
    }
}

@Composable
private fun EmptyLibrary(modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.LibraryMusic, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(14.dp))
        Text("No playable songs found", style = MaterialTheme.typography.headlineSmall)
        Text("Choose a folder that contains audio files.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ExpressiveSongRow(song: SongRef, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(if (selected) 22.dp else 14.dp)
    val container = if (selected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .82f) else Color.Transparent
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(container)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(if (selected) 18.dp else 14.dp),
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    if (selected) Icons.Default.GraphicEq else Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            song.fileName.substringBeforeLast('.'),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
        if (selected) Icon(Icons.Default.PlayArrow, null, tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun CompactGlassPlayer(state: AppUiState, viewModel: MusicViewModel) {
    FrostedSurface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp, bottomEnd = 22.dp, bottomStart = 22.dp),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ExpressiveArtwork(state, Modifier.size(58.dp), RoundedCornerShape(18.dp))
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(
                        state.metadata?.title ?: "Loading…",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        state.metadata?.artist.orEmpty(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = viewModel::previousVisibleSong) { Icon(Icons.Default.SkipPrevious, "Previous song") }
                MorphingPlayButton(state.isPlaying, viewModel::togglePlayPause, Modifier.size(54.dp))
                IconButton(onClick = viewModel::nextVisibleSong) { Icon(Icons.Default.SkipNext, "Next song") }
            }
            ExpressiveSeekBar(state, viewModel, showTime = false)
        }
    }
}

@Composable
private fun ExpressiveReview(state: AppUiState, viewModel: MusicViewModel) {
    LaunchedEffect(state.currentSwipeSong?.uri) {
        val current = state.currentSwipeSong
        if (current != null && current.uri != state.nowPlayingUri) viewModel.play(current)
    }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Decide by listening", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "${state.swipeCursor} of ${state.swipeOrder.size} reviewed · ${state.rejectedSongs.size} rejected",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = viewModel::undo, enabled = state.history.isNotEmpty(), modifier = Modifier.size(52.dp)) {
                Icon(Icons.AutoMirrored.Filled.Undo, "Undo last decision")
            }
        }
        Spacer(Modifier.height(12.dp))

        val song = state.currentSwipeSong
        if (song == null) {
            ExpressiveReviewComplete(state, viewModel)
        } else {
            ExpressiveSwipeCard(
                state = state,
                song = song,
                onReject = { viewModel.decide(ReviewDecision.REJECTED) },
                onKeep = { viewModel.decide(ReviewDecision.KEPT) },
                modifier = Modifier.weight(1f),
            )
            ReviewTransport(state, viewModel)
            DecisionControls(state, viewModel)
            Text(
                "Swipe left to reject · right to keep",
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun ExpressiveSwipeCard(
    state: AppUiState,
    song: SongRef,
    onReject: () -> Unit,
    onKeep: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val offset = remember(song.uri) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val threshold = with(density) { 104.dp.toPx() }
    val exitDistance = with(density) { 560.dp.toPx() }
    val progress = (abs(offset.value) / threshold).coerceIn(0f, 1f)
    val haptics = LocalHapticFeedback.current
    val neutral = MaterialTheme.colorScheme.surfaceContainerHigh
    val tint = when {
        offset.value > 0f -> lerp(neutral, SwipeTuneTokens.KeepContainer, progress * .78f)
        offset.value < 0f -> lerp(neutral, SwipeTuneTokens.RejectContainer, progress * .78f)
        else -> neutral
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationX = offset.value
                rotationZ = offset.value / 52f
                scaleX = 1f - progress * .025f
                scaleY = 1f - progress * .025f
            }
            .pointerInput(song.uri) {
                detectDragGestures(
                    onDragEnd = {
                        val direction = when {
                            offset.value > threshold -> 1
                            offset.value < -threshold -> -1
                            else -> 0
                        }
                        scope.launch {
                            if (direction == 0) {
                                offset.animateTo(0f, spring(stiffness = 400f, dampingRatio = .82f))
                            } else {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                offset.animateTo(direction * exitDistance, tween(210))
                                if (direction > 0) onKeep() else onReject()
                            }
                        }
                    },
                    onDragCancel = { scope.launch { offset.animateTo(0f, spring(stiffness = 400f)) } },
                ) { change, amount ->
                    change.consume()
                    scope.launch { offset.snapTo(offset.value + amount.x) }
                }
            },
        shape = RoundedCornerShape(topStart = 38.dp, topEnd = 24.dp, bottomEnd = 38.dp, bottomStart = 24.dp),
        color = tint,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 6.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .32f)),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(16.dp)) {
            val compact = maxHeight < 440.dp
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.Start) {
                ExpressiveArtwork(
                    state,
                    Modifier.fillMaxWidth().weight(1f),
                    RoundedCornerShape(topStart = 28.dp, topEnd = 16.dp, bottomEnd = 28.dp, bottomStart = 16.dp),
                )
                Spacer(Modifier.height(if (compact) 12.dp else 18.dp))
                Text(
                    state.metadata?.title ?: song.fileName.substringBeforeLast('.'),
                    style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    state.metadata?.artist ?: "Reading metadata…",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                state.metadata?.album?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            DecisionPill(
                text = "KEEP",
                icon = { Icon(Icons.Default.Check, null) },
                color = SwipeTuneTokens.Keep,
                modifier = Modifier.align(Alignment.TopStart).alpha((offset.value / threshold).coerceIn(0f, 1f)),
            )
            DecisionPill(
                text = "REJECT",
                icon = { Icon(Icons.Default.Close, null) },
                color = SwipeTuneTokens.Reject,
                modifier = Modifier.align(Alignment.TopEnd).alpha((-offset.value / threshold).coerceIn(0f, 1f)),
            )
        }
    }
}

@Composable
private fun DecisionPill(
    text: String,
    icon: @Composable () -> Unit,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = .88f), RoundedCornerShape(18.dp))
            .border(2.dp, color, RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        icon()
        Text(text, color = color, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun ExpressiveArtwork(state: AppUiState, modifier: Modifier, shape: RoundedCornerShape) {
    val art = state.metadata?.artwork
    Box(
        modifier.clip(shape).background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        if (art != null) {
            Image(
                art.asImageBitmap(),
                contentDescription = "Album artwork",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                Icons.Default.GraphicEq,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun ReviewTransport(state: AppUiState, viewModel: MusicViewModel) {
    Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        ExpressiveSeekBar(state, viewModel, showTime = true)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { viewModel.seekBy(-10_000) }, modifier = Modifier.size(52.dp)) {
                Icon(Icons.Default.FastRewind, "Back 10 seconds")
            }
            MorphingPlayButton(state.isPlaying, viewModel::togglePlayPause, Modifier.size(64.dp))
            IconButton(onClick = { viewModel.seekBy(10_000) }, modifier = Modifier.size(52.dp)) {
                Icon(Icons.Default.FastForward, "Forward 10 seconds")
            }
        }
    }
}

@Composable
private fun MorphingPlayButton(isPlaying: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val corner by animateDpAsState(if (isPlaying) 20.dp else 32.dp, spring(stiffness = 420f, dampingRatio = .78f), label = "play shape")
    FilledIconButton(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(corner)) {
        AnimatedContent(
            targetState = isPlaying,
            transitionSpec = { scaleIn(tween(150)) + fadeIn(tween(120)) togetherWith scaleOut(tween(100)) + fadeOut(tween(80)) },
            label = "play pause",
        ) { playing ->
            Icon(
                if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (playing) "Pause" else "Play",
                modifier = Modifier.size(30.dp),
            )
        }
    }
}

@Composable
private fun DecisionControls(state: AppUiState, viewModel: MusicViewModel) {
    Row(
        Modifier.fillMaxWidth().padding(top = 2.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DecisionButton(
            icon = { Icon(Icons.Default.Close, "Reject", modifier = Modifier.size(30.dp)) },
            color = SwipeTuneTokens.RejectContainer,
            contentColor = SwipeTuneTokens.Reject,
            onClick = { viewModel.decide(ReviewDecision.REJECTED) },
        )
        OutlinedButton(
            onClick = viewModel::undo,
            enabled = state.history.isNotEmpty(),
            modifier = Modifier.padding(horizontal = 14.dp).height(48.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Icon(Icons.AutoMirrored.Filled.Undo, null)
            Spacer(Modifier.width(7.dp))
            Text("Undo")
        }
        DecisionButton(
            icon = { Icon(Icons.Default.Check, "Keep", modifier = Modifier.size(30.dp)) },
            color = SwipeTuneTokens.KeepContainer,
            contentColor = SwipeTuneTokens.Keep,
            onClick = { viewModel.decide(ReviewDecision.KEPT) },
        )
    }
}

@Composable
private fun DecisionButton(
    icon: @Composable () -> Unit,
    color: Color,
    contentColor: Color,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.size(64.dp),
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 18.dp, bottomEnd = 26.dp, bottomStart = 18.dp),
        color = color,
        contentColor = contentColor,
        onClick = onClick,
    ) { Box(contentAlignment = Alignment.Center) { icon() } }
}

@Composable
private fun ExpressiveSeekBar(state: AppUiState, viewModel: MusicViewModel, showTime: Boolean) {
    var dragging by remember { mutableStateOf(false) }
    val max = state.durationMs.coerceAtLeast(1).toFloat()
    val currentPosition = state.positionMs.coerceAtMost(state.durationMs).toFloat()
    val sliderState = remember(max) { SliderState(currentPosition, 0, 0f..max) }

    LaunchedEffect(currentPosition, max, dragging) {
        if (!dragging) sliderState.value = currentPosition
    }

    Column {
        Slider(
            state = sliderState,
            onValueChange = { dragging = true },
            onValueChangeFinished = { viewModel.seekTo(sliderState.value.toLong()); dragging = false },
            colors = SliderDefaults.colors(
                activeTrackColor = MaterialTheme.colorScheme.primary,
                thumbColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            ),
        )
        if (showTime) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatExpressiveTime(sliderState.value.toLong()), style = MaterialTheme.typography.labelSmall)
                Text(formatExpressiveTime(state.durationMs), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun ExpressiveReviewComplete(state: AppUiState, viewModel: MusicViewModel) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            modifier = Modifier.size(88.dp),
            shape = RoundedCornerShape(30.dp),
            color = SwipeTuneTokens.KeepContainer,
            contentColor = SwipeTuneTokens.Keep,
        ) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Check, null, Modifier.size(48.dp)) } }
        Spacer(Modifier.height(24.dp))
        Text("You reached the end.", style = MaterialTheme.typography.headlineLarge)
        Text(
            "${state.reviewedCount} decisions saved · ${state.rejectedSongs.size} songs rejected",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = viewModel::undo, enabled = state.history.isNotEmpty(), shape = RoundedCornerShape(18.dp)) {
            Icon(Icons.AutoMirrored.Filled.Undo, null)
            Spacer(Modifier.width(8.dp))
            Text("Undo last decision")
        }
        TextButton(onClick = viewModel::restartReview) {
            Icon(Icons.Default.Refresh, null)
            Spacer(Modifier.width(8.dp))
            Text("Review undecided songs")
        }
    }
}

@Composable
private fun ExpressiveRejected(state: AppUiState, viewModel: MusicViewModel) {
    var confirmDelete by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 18.dp)) {
            Text("Rejected songs", style = MaterialTheme.typography.headlineLarge)
            Text(
                "These stay out of your library until you restore or permanently delete them.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = { confirmDelete = true },
                enabled = state.rejectedSongs.isNotEmpty() && !state.isDeleting,
                modifier = Modifier.padding(top = 14.dp).height(50.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
            ) {
                if (state.isDeleting) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Icon(Icons.Default.DeleteOutline, null)
                Spacer(Modifier.width(8.dp))
                Text(if (state.isDeleting) "Deleting…" else "Delete rejected files")
            }
        }

        if (state.rejectedSongs.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Default.Restore, null, Modifier.size(50.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(14.dp))
                Text("Nothing is rejected", style = MaterialTheme.typography.headlineSmall)
                Text("Songs you swipe left will wait here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(state.rejectedSongs, key = SongRef::uri) { song ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = .56f)).padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(Modifier.size(48.dp), RoundedCornerShape(15.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.MusicNote, null) }
                        }
                        Text(
                            song.fileName.substringBeforeLast('.'),
                            modifier = Modifier.weight(1f).padding(horizontal = 14.dp),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        IconButton(onClick = { viewModel.restore(song) }) { Icon(Icons.Default.Restore, "Restore to library") }
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            icon = { Icon(Icons.Default.Warning, null) },
            title = { Text("Delete these files permanently?") },
            text = { Text("This removes ${state.rejectedSongs.size} audio files from the selected folder. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = { confirmDelete = false; viewModel.deleteRejected() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) { Text("Delete permanently") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

private fun formatExpressiveTime(ms: Long): String {
    val totalSeconds = ms.coerceAtLeast(0) / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
