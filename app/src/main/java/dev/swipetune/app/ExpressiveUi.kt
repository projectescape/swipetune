package dev.swipetune.app

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode as AnimationRepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class ExpressiveScreen { LIBRARY, REVIEW, REVIEW_DECISIONS, SETTINGS }
private enum class PlayerPane { NOW_PLAYING, LYRICS, QUEUE }

private val ScreenEdge = 16.dp
private val ListItemInset = 8.dp

@Composable
internal fun ExpressiveSwipeTuneApp(viewModel: MusicViewModel, onChooseFolder: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var screen by rememberSaveable { mutableStateOf(ExpressiveScreen.LIBRARY) }
    var showPlayer by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.statusMessage, state.errorMessage) {
        (state.errorMessage ?: state.statusMessage)?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(screen) {
        if (screen == ExpressiveScreen.REVIEW) {
            viewModel.enterReviewMode()
        } else {
            viewModel.exitReviewMode()
        }
    }

    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
        Box(Modifier.fillMaxSize()) {
            LivingBackdrop(state)
            Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                Column(Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))) {
                    AnimatedVisibility(state.nowPlayingUri != null && screen != ExpressiveScreen.REVIEW) {
                        CompactGlassPlayer(state, viewModel, onOpenPlayer = { showPlayer = true })
                    }
                }
            },
        ) { padding ->
            if (state.isScanning && state.songs.isEmpty()) {
                ExpressiveLoading(Modifier.padding(padding))
            } else {
                AnimatedContent(
                    targetState = screen,
                    modifier = Modifier
                        .padding(padding)
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
                    transitionSpec = {
                        if (state.expressiveMotion) {
                            (fadeIn(tween(260)) + scaleIn(tween(320), initialScale = .985f)) togetherWith
                                (fadeOut(tween(170)) + scaleOut(tween(170), targetScale = 1.015f))
                        } else {
                            fadeIn(tween(100)) togetherWith fadeOut(tween(100))
                        }
                    },
                    label = "screen",
                ) { destination ->
                    when (destination) {
                        ExpressiveScreen.LIBRARY -> ExpressiveLibrary(
                            state,
                            viewModel,
                            onOpenPlayer = { showPlayer = true },
                            onOpenSettings = { screen = ExpressiveScreen.SETTINGS },
                        )
                        ExpressiveScreen.REVIEW -> ExpressiveReview(
                            state,
                            viewModel,
                            onClose = { screen = ExpressiveScreen.SETTINGS },
                            onOpenDecisions = { screen = ExpressiveScreen.REVIEW_DECISIONS },
                        )
                        ExpressiveScreen.REVIEW_DECISIONS -> ExpressiveReviewDecisions(state, viewModel, onBack = { screen = ExpressiveScreen.SETTINGS })
                        ExpressiveScreen.SETTINGS -> ExpressiveSettings(
                            state,
                            viewModel,
                            onChooseFolder,
                            onBack = { screen = ExpressiveScreen.LIBRARY },
                            onStartReview = { scope -> viewModel.prepareReview(scope); screen = ExpressiveScreen.REVIEW },
                            onOpenReviewDecisions = { screen = ExpressiveScreen.REVIEW_DECISIONS },
                        )
                    }
                }
            }
            }
            AnimatedVisibility(
                visible = showPlayer && state.nowPlayingSong != null,
                enter = fadeIn(tween(220)) +
                    slideInVertically(tween(360)) { height -> height / 5 } +
                    scaleIn(tween(360), initialScale = .96f),
                exit = fadeOut(tween(170)) +
                    slideOutVertically(tween(240)) { height -> height / 7 } +
                    scaleOut(tween(220), targetScale = .98f),
            ) {
                FullPlayerOverlay(state, viewModel, onDismiss = { showPlayer = false })
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
        state.metadata?.artwork?.takeIf { state.artworkAtmosphere }?.let { bitmap ->
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
private fun ContextPageHeader(
    title: String,
    supportingText: String,
    backDescription: String,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, backDescription)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineLarge)
            Text(
                supportingText,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun ExpressiveOnboarding(onChooseFolder: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
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

private enum class LibraryView(val label: String) {
    SONGS("Songs"), ALBUMS("Albums"), ARTISTS("Artists"), GENRES("Genres"), FOLDERS("Folders"), FAVORITES("Favorites"), HISTORY("History"), PLAYLISTS("Playlists")
}

private enum class LibrarySort(val label: String) { TITLE("Title"), ARTIST("Artist"), ALBUM("Album") }

private fun librarySongsForView(
    visibleSongs: List<SongRef>,
    favorites: Set<String>,
    recentHistory: List<HistorySummary>,
    query: String,
    sort: LibrarySort,
    view: LibraryView,
): List<SongRef> {
    val source = when (view) {
        LibraryView.FAVORITES -> visibleSongs.filter { it.uri in favorites }
        LibraryView.HISTORY -> {
            val byUri = visibleSongs.associateBy(SongRef::uri)
            recentHistory.mapNotNull { event -> byUri[event.trackUri] }
        }
        else -> visibleSongs
    }
    return source.filter { song ->
        query.isBlank() || listOf(song.title, song.artist, song.album, song.folderName)
            .any { it.contains(query, ignoreCase = true) }
    }.let { songs ->
        when (sort) {
            LibrarySort.TITLE -> songs.sortedBy { it.title.lowercase() }
            LibrarySort.ARTIST -> songs.sortedWith(compareBy({ it.artist.lowercase() }, { it.title.lowercase() }))
            LibrarySort.ALBUM -> songs.sortedWith(compareBy({ it.album.lowercase() }, { it.trackNumber }, { it.title.lowercase() }))
        }
    }
}

@Composable
private fun ExpressiveLibrary(
    state: AppUiState,
    viewModel: MusicViewModel,
    onOpenPlayer: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var libraryView by rememberSaveable { mutableStateOf(LibraryView.SONGS) }
    var query by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf(LibrarySort.TITLE) }
    var selectedPlaylistId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedCollection by rememberSaveable { mutableStateOf<String?>(null) }
    var showCreatePlaylist by remember { mutableStateOf(false) }
    val visibleSongs = remember(state.songs, state.decisions) { state.visibleSongs }
    val pagerState = rememberPagerState(initialPage = libraryView.ordinal) { LibraryView.entries.size }
    val sectionListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    BackHandler(enabled = selectedCollection != null || selectedPlaylistId != null) {
        if (selectedCollection != null) selectedCollection = null else selectedPlaylistId = null
    }
    val cycleSort = {
        sort = when (sort) {
            LibrarySort.TITLE -> LibrarySort.ARTIST
            LibrarySort.ARTIST -> LibrarySort.ALBUM
            LibrarySort.ALBUM -> LibrarySort.TITLE
        }
    }
    LaunchedEffect(pagerState.currentPage) {
        libraryView = LibraryView.entries[pagerState.currentPage]
        selectedCollection = null
        selectedPlaylistId = null
    }
    LaunchedEffect(libraryView) {
        sectionListState.animateScrollToItem(libraryView.ordinal)
    }

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth < 360.dp) {
                Column(Modifier.fillMaxWidth().padding(horizontal = ScreenEdge, vertical = 12.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Your library", style = MaterialTheme.typography.headlineLarge, maxLines = 1)
                            Text(
                                "${visibleSongs.size} songs · ${state.favorites.size} favorites",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        IconButton(onClick = onOpenSettings, modifier = Modifier.size(52.dp)) {
                            Icon(Icons.Default.Settings, "Settings")
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = cycleSort, modifier = Modifier.size(52.dp)) {
                            Icon(Icons.Default.SortByAlpha, "Sort by ${sort.label}")
                        }
                        FilledIconButton(
                            onClick = viewModel::shuffleLibrary,
                            modifier = Modifier.size(56.dp),
                            shape = RoundedCornerShape(20.dp),
                        ) { Icon(Icons.Default.Shuffle, "Play a random song") }
                    }
                }
            } else {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = ScreenEdge, vertical = 16.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Your library", style = MaterialTheme.typography.headlineLarge)
                        Text(
                            "${visibleSongs.size} songs · ${state.favorites.size} favorites",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    IconButton(onClick = cycleSort, modifier = Modifier.size(52.dp)) {
                        Icon(Icons.Default.SortByAlpha, "Sort by ${sort.label}")
                    }
                    FilledIconButton(
                        onClick = viewModel::shuffleLibrary,
                        modifier = Modifier.size(56.dp),
                        shape = RoundedCornerShape(20.dp),
                    ) { Icon(Icons.Default.Shuffle, "Play a random song") }
                    IconButton(onClick = onOpenSettings, modifier = Modifier.size(52.dp)) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                }
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenEdge),
            singleLine = true,
            shape = RoundedCornerShape(22.dp),
            leadingIcon = { Icon(Icons.Default.Search, null) },
            placeholder = { Text("Search songs, artists, albums") },
        )

        LazyRow(
            state = sectionListState,
            contentPadding = PaddingValues(horizontal = ScreenEdge, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(LibraryView.entries) { item ->
                val selected = libraryView == item
                Surface(
                    onClick = {
                        libraryView = item
                        selectedCollection = null
                        selectedPlaylistId = null
                        scope.launch { pagerState.animateScrollToPage(item.ordinal) }
                    },
                    shape = RoundedCornerShape(18.dp),
                    color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = .62f),
                    contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                ) {
                    Text(item.label, Modifier.padding(horizontal = 16.dp, vertical = 9.dp), style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        AnimatedVisibility(visible = state.selectedSongUris.isNotEmpty()) {
            Column(Modifier.fillMaxWidth()) {
                Text("${state.selectedSongUris.size} selected", modifier = Modifier.padding(horizontal = ScreenEdge), style = MaterialTheme.typography.labelLarge)
                LazyRow(
                    contentPadding = PaddingValues(horizontal = ScreenEdge),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    item { TextButton(onClick = viewModel::playSelectedNext) { Text("Play next") } }
                    item { TextButton(onClick = viewModel::appendSelected) { Text("Add to queue") } }
                    item { TextButton(onClick = viewModel::favoriteSelected) { Text("Favorite") } }
                    item { TextButton(onClick = viewModel::rejectSelected) { Text("Reject") } }
                    item { TextButton(onClick = viewModel::clearSongSelection) { Text("Clear") } }
                }
            }
        }

        val currentSongs = librarySongsForView(visibleSongs, state.favorites, state.recentHistory, query, sort, libraryView)
        val collectionGroups = when (libraryView) {
            LibraryView.ALBUMS -> currentSongs.groupBy(SongRef::album)
            LibraryView.ARTISTS -> currentSongs.groupBy(SongRef::artist)
            LibraryView.GENRES -> currentSongs.groupBy(SongRef::genre)
            LibraryView.FOLDERS -> currentSongs.groupBy(SongRef::folderName)
            else -> emptyMap()
        }
        val activeCollection = selectedCollection?.takeIf(collectionGroups::containsKey)
        AnimatedContent(
            targetState = activeCollection,
            modifier = Modifier.weight(1f),
            transitionSpec = {
                if (targetState != null) {
                    (fadeIn(tween(180)) + slideInHorizontally(tween(280)) { it / 5 }) togetherWith
                        (fadeOut(tween(130)) + slideOutHorizontally(tween(220)) { -it / 8 })
                } else {
                    (fadeIn(tween(180)) + slideInHorizontally(tween(280)) { -it / 5 }) togetherWith
                        (fadeOut(tween(130)) + slideOutHorizontally(tween(220)) { it / 8 })
                }
            },
            label = "library detail",
        ) { collectionName ->
            val selectedSongs = collectionName?.let(collectionGroups::get)
            if (collectionName != null && selectedSongs != null) {
                CollectionDetail(
                    name = collectionName,
                    songs = selectedSongs,
                    state = state,
                    viewModel = viewModel,
                    onBack = { selectedCollection = null },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = selectedPlaylistId == null,
                    beyondViewportPageCount = 1,
                    key = { LibraryView.entries[it].name },
                ) { page ->
                    val pageView = LibraryView.entries[page]
                    val pageSongs = remember(visibleSongs, state.favorites, state.recentHistory, query, sort, pageView) {
                        librarySongsForView(visibleSongs, state.favorites, state.recentHistory, query, sort, pageView)
                    }
                    LibraryPage(
                        view = pageView,
                        songs = pageSongs,
                        query = query,
                        state = state,
                        viewModel = viewModel,
                        selectedPlaylistId = selectedPlaylistId,
                        onSelectedPlaylist = { selectedPlaylistId = it },
                        onCreatePlaylist = { showCreatePlaylist = true },
                        onOpenCollection = { selectedCollection = it },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

    }

    if (showCreatePlaylist) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreatePlaylist = false },
            icon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null) },
            title = { Text("New playlist") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("Playlist name") },
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.createPlaylist(name); showCreatePlaylist = false },
                    enabled = name.isNotBlank(),
                ) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showCreatePlaylist = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun LibraryPage(
    view: LibraryView,
    songs: List<SongRef>,
    query: String,
    state: AppUiState,
    viewModel: MusicViewModel,
    selectedPlaylistId: String?,
    onSelectedPlaylist: (String?) -> Unit,
    onCreatePlaylist: () -> Unit,
    onOpenCollection: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (view == LibraryView.PLAYLISTS) {
        AnimatedContent(
            targetState = selectedPlaylistId,
            modifier = modifier,
            transitionSpec = {
                if (targetState != null) {
                    (fadeIn(tween(180)) + slideInHorizontally(tween(280)) { it / 5 }) togetherWith
                        (fadeOut(tween(130)) + slideOutHorizontally(tween(220)) { -it / 8 })
                } else {
                    (fadeIn(tween(180)) + slideInHorizontally(tween(280)) { -it / 5 }) togetherWith
                        (fadeOut(tween(130)) + slideOutHorizontally(tween(220)) { it / 8 })
                }
            },
            label = "playlist detail",
        ) { playlistId ->
            ExpressivePlaylists(
                state = state,
                viewModel = viewModel,
                selectedPlaylistId = playlistId,
                onSelectedPlaylist = onSelectedPlaylist,
                onCreate = onCreatePlaylist,
                modifier = Modifier.fillMaxSize(),
            )
        }
        return
    }
    if (state.visibleSongs.isEmpty()) {
        EmptyLibrary(modifier)
        return
    }
    if (songs.isEmpty()) {
        EmptyLibraryResult(query, view, modifier)
        return
    }

    when (view) {
        LibraryView.SONGS, LibraryView.FAVORITES, LibraryView.HISTORY -> {
            val listState = rememberLazyListState()
            Box(modifier) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(end = 8.dp),
                    state = listState,
                    contentPadding = PaddingValues(start = ScreenEdge, end = 8.dp, top = 4.dp, bottom = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(songs, key = SongRef::uri) { song ->
                        Box(Modifier.animateItem(fadeInSpec = tween(150), fadeOutSpec = tween(120), placementSpec = tween(190))) {
                            ExpressiveSongRow(song, state, viewModel) { viewModel.playCollection(songs, song) }
                        }
                    }
                }
                ExpressiveVerticalScrollbar(listState, Modifier.align(Alignment.CenterEnd))
            }
        }
        LibraryView.ALBUMS -> LibraryCollections(
            groups = songs.groupBy(SongRef::album),
            state = state,
            viewModel = viewModel,
            modifier = modifier,
            icon = Icons.Default.Album,
            onOpen = onOpenCollection,
        )
        LibraryView.ARTISTS -> LibraryCollections(
            groups = songs.groupBy(SongRef::artist),
            state = state,
            viewModel = viewModel,
            modifier = modifier,
            icon = Icons.Default.Person,
            onOpen = onOpenCollection,
        )
        LibraryView.GENRES -> LibraryCollections(
            groups = songs.groupBy(SongRef::genre),
            state = state,
            viewModel = viewModel,
            modifier = modifier,
            icon = Icons.Default.GraphicEq,
            onOpen = onOpenCollection,
        )
        LibraryView.FOLDERS -> LibraryCollections(
            groups = songs.groupBy(SongRef::folderName),
            state = state,
            viewModel = viewModel,
            modifier = modifier,
            icon = Icons.Default.Folder,
            onOpen = onOpenCollection,
        )
        LibraryView.PLAYLISTS -> Unit
    }
}

@Composable
private fun ExpressivePlaylists(
    state: AppUiState,
    viewModel: MusicViewModel,
    selectedPlaylistId: String?,
    onSelectedPlaylist: (String?) -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val importM3u = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(viewModel::importPlaylist) }
    val exportM3u = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/x-mpegurl")) { uri ->
        val id = selectedPlaylistId
        if (uri != null && id != null) viewModel.exportPlaylist(id, uri)
    }
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val id = selectedPlaylistId
        if (uri != null && id != null) viewModel.setPlaylistCover(id, uri)
    }
    val smartSelection = when (selectedPlaylistId) {
        ACCEPTED_PLAYLIST_ID -> "Accepted" to state.acceptedSongs
        REJECTED_PLAYLIST_ID -> "Rejected" to state.rejectedSongs
        else -> null
    }
    val selected = state.playlists.firstOrNull { it.id == selectedPlaylistId }
    if (selected == null && smartSelection == null) {
        Column(modifier) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Playlists", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Button(onClick = onCreate, shape = RoundedCornerShape(18.dp)) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(6.dp))
                    Text("New")
                }
                IconButton(onClick = { importM3u.launch(arrayOf("audio/x-mpegurl", "audio/mpegurl", "text/plain")) }) {
                    Icon(Icons.Default.Add, "Import M3U playlist")
                }
            }
            LazyColumn(
                contentPadding = PaddingValues(horizontal = ScreenEdge, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                item {
                    SmartPlaylistRow(
                        name = "Accepted",
                        supportingText = "Kept in Review · always available",
                        songs = state.acceptedSongs,
                        state = state,
                        viewModel = viewModel,
                        onOpen = { onSelectedPlaylist(ACCEPTED_PLAYLIST_ID) },
                    )
                }
                item {
                    SmartPlaylistRow(
                        name = "Rejected",
                        supportingText = "Hidden from the rest of your library",
                        songs = state.rejectedSongs,
                        state = state,
                        viewModel = viewModel,
                        onOpen = { onSelectedPlaylist(REJECTED_PLAYLIST_ID) },
                    )
                }
                if (state.playlists.isNotEmpty()) {
                    item {
                        Text(
                            "Your playlists",
                            modifier = Modifier.padding(start = 8.dp, top = 14.dp, bottom = 4.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    items(state.playlists, key = UserPlaylist::id) { playlist ->
                        val songs = playlist.songUris.mapNotNull { uri ->
                            state.visibleSongs.firstOrNull { it.uri == uri }
                        }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(22.dp))
                                .clickable { onSelectedPlaylist(playlist.id) }
                                .padding(ListItemInset),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val first = songs.firstOrNull()
                            if (playlist.coverUri != null) {
                                PlaylistCover(playlist.coverUri, Modifier.size(62.dp))
                            } else if (first != null) {
                                LibraryArtwork(first, state, viewModel, Modifier.size(62.dp), RoundedCornerShape(18.dp), Icons.AutoMirrored.Filled.QueueMusic)
                            } else {
                                Surface(Modifier.size(62.dp), RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                                    Box(contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.QueueMusic, null) }
                                }
                            }
                            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                                Text(playlist.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${songs.size} ${if (songs.size == 1) "song" else "songs"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { if (songs.isNotEmpty()) viewModel.playCollection(songs) }, enabled = songs.isNotEmpty()) {
                                Icon(Icons.Default.PlayArrow, "Play ${playlist.name}")
                            }
                        }
                    }
                }
            }
        }
        return
    }

    if (smartSelection != null) {
        SmartPlaylistDetail(
            name = smartSelection.first,
            songs = smartSelection.second,
            rejected = selectedPlaylistId == REJECTED_PLAYLIST_ID,
            state = state,
            viewModel = viewModel,
            onBack = { onSelectedPlaylist(null) },
            modifier = modifier,
        )
        return
    }

    checkNotNull(selected)
    var confirmDelete by remember(selected.id) { mutableStateOf(false) }
    val songs = selected.songUris.mapNotNull { uri -> state.visibleSongs.firstOrNull { it.uri == uri } }
    Column(modifier) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = ScreenEdge, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { onSelectedPlaylist(null) }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to playlists") }
            Column(Modifier.weight(1f)) {
                Text(selected.name, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${songs.size} songs", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.DeleteOutline, "Delete playlist") }
            FilledIconButton(onClick = { if (songs.isNotEmpty()) viewModel.playCollection(songs) }, enabled = songs.isNotEmpty()) {
                Icon(Icons.Default.PlayArrow, "Play playlist")
            }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = ScreenEdge, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { OutlinedButton(onClick = { viewModel.duplicatePlaylist(selected.id) }) { Text("Duplicate") } }
            item { OutlinedButton(onClick = { viewModel.quickFillPlaylist(selected.id) }) { Text("Quick Fill") } }
            item { OutlinedButton(onClick = { coverPicker.launch(arrayOf("image/*")) }) { Text("Cover") } }
            item { OutlinedButton(onClick = { exportM3u.launch("${selected.name}.m3u8") }) { Text("Export M3U") } }
            val mergeSource = state.playlists.firstOrNull { it.id != selected.id }
            if (mergeSource != null) {
                item { OutlinedButton(onClick = { viewModel.mergePlaylist(selected.id, mergeSource.id) }) { Text("Merge ${mergeSource.name}") } }
            }
        }
        if (songs.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center) {
                Text("This playlist is empty", style = MaterialTheme.typography.headlineSmall)
                Text("Open a song menu and choose this playlist.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = ScreenEdge, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                itemsIndexed(songs, key = { _, song -> song.uri }) { index, song ->
                    Row(
                        Modifier
                            .animateItem(fadeInSpec = tween(150), fadeOutSpec = tween(120), placementSpec = tween(190))
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { viewModel.playCollection(songs, song) }
                            .padding(ListItemInset),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LibraryArtwork(song, state, viewModel, Modifier.size(52.dp), RoundedCornerShape(15.dp))
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(song.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { viewModel.movePlaylistSong(selected.id, index, index - 1) }, enabled = index > 0) { Text("↑") }
                        TextButton(onClick = { viewModel.movePlaylistSong(selected.id, index, index + 1) }, enabled = index < songs.lastIndex) { Text("↓") }
                        IconButton(onClick = { viewModel.removeFromPlaylist(selected.id, song) }) {
                            Icon(Icons.Default.Close, "Remove ${song.title} from playlist")
                        }
                    }
                }
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${selected.name}?") },
            text = { Text("The songs stay on your device. Only this playlist will be removed.") },
            confirmButton = {
                Button(onClick = { viewModel.deletePlaylist(selected.id); onSelectedPlaylist(null); confirmDelete = false }) {
                    Text("Delete playlist")
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SmartPlaylistRow(
    name: String,
    supportingText: String,
    songs: List<SongRef>,
    state: AppUiState,
    viewModel: MusicViewModel,
    onOpen: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).clickable(onClick = onOpen).padding(ListItemInset),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val first = songs.firstOrNull()
        if (first != null) {
            LibraryArtwork(first, state, viewModel, Modifier.size(62.dp), RoundedCornerShape(18.dp), Icons.AutoMirrored.Filled.QueueMusic)
        } else {
            Surface(Modifier.size(62.dp), RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.QueueMusic, null) }
            }
        }
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(name, style = MaterialTheme.typography.titleMedium)
            Text("${songs.size} songs · $supportingText", color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
        }
        IconButton(onClick = { if (songs.isNotEmpty()) viewModel.playCollection(songs) }, enabled = songs.isNotEmpty()) {
            Icon(Icons.Default.PlayArrow, "Play $name")
        }
    }
}

@Composable
private fun SmartPlaylistDetail(
    name: String,
    songs: List<SongRef>,
    rejected: Boolean,
    state: AppUiState,
    viewModel: MusicViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmDeleteAll by remember { mutableStateOf(false) }
    var deleteSong by remember { mutableStateOf<SongRef?>(null) }
    Column(modifier) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = ScreenEdge, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to playlists") }
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleLarge)
                Text(
                    if (rejected) {
                        "${songs.size} ${if (songs.size == 1) "song" else "songs"} · hidden elsewhere"
                    } else {
                        "${songs.size} ${if (songs.size == 1) "song" else "songs"} · kept in Review"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (rejected) {
                Button(
                    onClick = { confirmDeleteAll = true },
                    enabled = songs.isNotEmpty() && !state.isDeleting,
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                ) {
                    Icon(Icons.Default.DeleteOutline, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Delete all")
                }
            }
            FilledIconButton(onClick = { if (songs.isNotEmpty()) viewModel.playCollection(songs) }, enabled = songs.isNotEmpty()) {
                Icon(Icons.Default.PlayArrow, "Play $name")
            }
        }
        Text(
            "Built in · updates automatically · playlist cannot be deleted",
            modifier = Modifier.padding(horizontal = ScreenEdge, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (songs.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center) {
                Text("Nothing here yet", style = MaterialTheme.typography.headlineSmall)
                Text("Songs appear here as you make Review decisions.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = ScreenEdge, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(songs, key = SongRef::uri) { song ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable { viewModel.playCollection(songs, song) }.padding(ListItemInset),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LibraryArtwork(song, state, viewModel, Modifier.size(52.dp), RoundedCornerShape(15.dp))
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(song.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { viewModel.clearReviewDecision(song) }) {
                            Icon(
                                Icons.Default.Restore,
                                if (rejected) "Restore ${song.title} to library" else "Clear accepted decision for ${song.title}",
                            )
                        }
                        if (rejected) {
                            IconButton(onClick = { deleteSong = song }, enabled = !state.isDeleting) {
                                Icon(Icons.Default.DeleteOutline, "Delete ${song.title} from device")
                            }
                        }
                    }
                }
            }
        }
    }
    if (confirmDeleteAll) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAll = false },
            icon = { Icon(Icons.Default.Warning, null) },
            title = { Text("Delete all rejected files?") },
            text = { Text("This permanently removes ${songs.size} ${if (songs.size == 1) "song" else "songs"} from this device.") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDeleteAll = false
                        viewModel.requestDeleteRejected()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) { Text("Delete all") }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteAll = false }) { Text("Cancel") } },
        )
    }
    deleteSong?.let { song ->
        AlertDialog(
            onDismissRequest = { deleteSong = null },
            icon = { Icon(Icons.Default.DeleteOutline, null) },
            title = { Text("Delete this song?") },
            text = { Text("${song.title} will be permanently removed from this device.") },
            confirmButton = {
                Button(
                    onClick = {
                        deleteSong = null
                        viewModel.requestDeleteSong(song)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleteSong = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun PlaylistCover(uri: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(uri) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(android.net.Uri.parse(uri))?.use(android.graphics.BitmapFactory::decodeStream)
            }.getOrNull()
        }
    }
    Surface(modifier, RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        bitmap?.let { Image(it.asImageBitmap(), "Custom playlist cover", Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            ?: Box(contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.QueueMusic, null) }
    }
}

@Composable
private fun EmptyLibraryResult(query: String, view: LibraryView, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(32.dp), verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.Search, null, Modifier.size(44.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Text(if (query.isBlank()) "No ${view.label.lowercase()} yet" else "No matches for “$query”", style = MaterialTheme.typography.headlineSmall)
        Text("Try another search or library view.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LibraryCollections(
    groups: Map<String, List<SongRef>>,
    state: AppUiState,
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onOpen: (String) -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = ScreenEdge, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(groups.entries.sortedBy { it.key.lowercase() }, key = { it.key }) { (name, songs) ->
            val first = songs.minWithOrNull(compareBy<SongRef>({ it.trackNumber.takeIf { number -> number > 0 } ?: Int.MAX_VALUE }, { it.title })) ?: return@items
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).clickable { onOpen(name) }.padding(ListItemInset),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LibraryArtwork(first, state, viewModel, Modifier.size(62.dp), RoundedCornerShape(18.dp), icon)
                Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                    Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                    Text("${songs.size} ${if (songs.size == 1) "song" else "songs"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FilledIconButton(onClick = { viewModel.playCollection(songs, first) }) {
                    Icon(Icons.Default.PlayArrow, "Play $name")
                }
            }
        }
    }
}

@Composable
private fun CollectionDetail(
    name: String,
    songs: List<SongRef>,
    state: AppUiState,
    viewModel: MusicViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = ScreenEdge, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to collections") }
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${songs.size} songs · ${formatExpressiveTime(songs.sumOf(SongRef::durationMs))}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { viewModel.playNext(songs) }) { Icon(Icons.AutoMirrored.Filled.QueueMusic, "Play next") }
            FilledIconButton(onClick = { viewModel.playCollection(songs) }) { Icon(Icons.Default.PlayArrow, "Play $name") }
        }
        LazyColumn(
            contentPadding = PaddingValues(horizontal = ScreenEdge, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(songs, key = SongRef::uri) { song ->
                Box(Modifier.animateItem(fadeInSpec = tween(150), fadeOutSpec = tween(120), placementSpec = tween(190))) {
                    ExpressiveSongRow(song, state, viewModel) { viewModel.playCollection(songs, song) }
                }
            }
        }
    }
}

@Composable
private fun ExpressiveVerticalScrollbar(
    listState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier,
) {
    val metrics by remember(listState) {
        derivedStateOf {
            val layout = listState.layoutInfo
            val total = layout.totalItemsCount
            val visible = layout.visibleItemsInfo.size
            ScrollbarMetrics(
                totalItems = total,
                visibleItems = visible,
                firstVisibleItem = listState.firstVisibleItemIndex,
                canScroll = listState.canScrollBackward || listState.canScrollForward,
            )
        }
    }
    if (!metrics.canScroll || metrics.totalItems <= 0) return

    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var dragging by remember { mutableStateOf(false) }
    var dragY by remember { mutableFloatStateOf(0f) }
    val indicatorWidth by animateDpAsState(
        targetValue = if (dragging) 10.dp else 5.dp,
        animationSpec = tween(140),
        label = "library scrollbar width",
    )
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = .72f)
    val indicatorColor = MaterialTheme.colorScheme.primary

    BoxWithConstraints(
        modifier
            .fillMaxHeight()
            .width(28.dp)
            .pointerInput(metrics.totalItems) {
                fun scrollToPosition(y: Float) {
                    val progress = (y / size.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                    val target = (progress * (metrics.totalItems - 1)).roundToInt()
                    scope.launch { listState.scrollToItem(target) }
                }
                detectDragGestures(
                    onDragStart = { start ->
                        dragging = true
                        dragY = start.y
                        scrollToPosition(dragY)
                    },
                    onDragCancel = { dragging = false },
                    onDragEnd = { dragging = false },
                    onDrag = { change, amount ->
                        change.consume()
                        dragY = (dragY + amount.y).coerceIn(0f, size.height.toFloat())
                        scrollToPosition(dragY)
                    },
                )
            },
    ) {
        val heightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
        val minThumbPx = with(density) { 48.dp.toPx() }
        val thumbHeight = (heightPx * metrics.visibleItems / metrics.totalItems.toFloat())
            .coerceIn(minThumbPx, heightPx)
        val maxFirst = (metrics.totalItems - metrics.visibleItems).coerceAtLeast(1)
        val progress = (metrics.firstVisibleItem / maxFirst.toFloat()).coerceIn(0f, 1f)
        val thumbTop = progress * (heightPx - thumbHeight)
        val indicatorWidthPx = with(density) { indicatorWidth.toPx() }
        Canvas(Modifier.fillMaxSize()) {
            val x = size.width - with(density) { 7.dp.toPx() }
            drawLine(trackColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = with(density) { 2.dp.toPx() }, cap = StrokeCap.Round)
            drawLine(
                indicatorColor,
                Offset(x, thumbTop),
                Offset(x, thumbTop + thumbHeight),
                strokeWidth = indicatorWidthPx,
                cap = StrokeCap.Round,
            )
        }
    }
}

private data class ScrollbarMetrics(
    val totalItems: Int,
    val visibleItems: Int,
    val firstVisibleItem: Int,
    val canScroll: Boolean,
)

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
        Text("Allow Android music access, then rescan—or add another storage folder from Settings.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExpressiveSongRow(song: SongRef, state: AppUiState, viewModel: MusicViewModel, onClick: () -> Unit) {
    var showMenu by remember(song.uri) { mutableStateOf(false) }
    var showPlaylistPicker by remember(song.uri) { mutableStateOf(false) }
    var confirmDelete by remember(song.uri) { mutableStateOf(false) }
    val playing = song.uri == state.nowPlayingUri
    val selected = song.uri in state.selectedSongUris
    val corner by animateDpAsState(
        targetValue = if (playing || selected) 22.dp else 14.dp,
        animationSpec = tween(180),
        label = "song row shape",
    )
    val container by animateColorAsState(
        targetValue = when {
        selected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = .92f)
        playing -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .82f)
        else -> Color.Transparent
        },
        animationSpec = tween(180),
        label = "song row state",
    )
    val shape = RoundedCornerShape(corner)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(container)
            .combinedClickable(
                onClick = { if (state.selectedSongUris.isEmpty()) onClick() else viewModel.toggleSongSelection(song) },
                onLongClick = { viewModel.toggleSongSelection(song) },
            )
            .padding(horizontal = ListItemInset, vertical = if (state.compactLibraryRows) 5.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        LibraryArtwork(song, state, viewModel, Modifier.size(52.dp), RoundedCornerShape(if (playing || selected) 18.dp else 14.dp))
        Column(Modifier.weight(1f)) {
            Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge, fontWeight = if (playing || selected) FontWeight.Bold else FontWeight.Normal)
            Text("${song.artist} · ${song.album}", maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
        if (playing) {
            PlayingBars(isPlaying = state.isPlaying)
        }
        IconButton(onClick = { viewModel.toggleFavorite(song) }) {
            AnimatedContent(
                targetState = state.isFavorite(song),
                transitionSpec = { scaleIn(spring(stiffness = 560f), initialScale = .55f) + fadeIn() togetherWith scaleOut(tween(100)) + fadeOut() },
                label = "favorite song",
            ) { favorite ->
                Icon(
                    if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    if (favorite) "Remove from favorites" else "Add to favorites",
                    tint = if (favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.MoreVert, "More options for ${song.title}")
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Play next") },
                    onClick = { viewModel.playNext(listOf(song)); showMenu = false },
                    leadingIcon = { Icon(Icons.Default.SkipNext, null) },
                )
                DropdownMenuItem(
                    text = { Text("Add to queue") },
                    onClick = { viewModel.appendToQueue(listOf(song)); showMenu = false },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, null) },
                )
                DropdownMenuItem(
                    text = { Text("Add to playlist") },
                    onClick = {
                        showMenu = false
                        showPlaylistPicker = true
                    },
                    enabled = state.playlists.isNotEmpty(),
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null) },
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Delete from device") },
                    onClick = {
                        showMenu = false
                        confirmDelete = true
                    },
                    leadingIcon = { Icon(Icons.Default.DeleteOutline, null) },
                )
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            icon = { Icon(Icons.Default.DeleteOutline, null) },
            title = { Text("Delete this song?") },
            text = { Text("${song.title} will be permanently removed from this device.") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDelete = false
                        viewModel.requestDeleteSong(song)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
    if (showPlaylistPicker) {
        AlertDialog(
            onDismissRequest = { showPlaylistPicker = false },
            icon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null) },
            title = { Text("Add to playlist") },
            text = {
                LazyColumn(
                    Modifier.fillMaxWidth().heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(state.playlists, key = UserPlaylist::id) { playlist ->
                        val alreadyAdded = song.uri in playlist.songUris
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable(enabled = !alreadyAdded) {
                                    viewModel.addToPlaylist(playlist.id, song)
                                    showPlaylistPicker = false
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(if (alreadyAdded) Icons.Default.Check else Icons.AutoMirrored.Filled.PlaylistAdd, null)
                            Text(
                                if (alreadyAdded) "${playlist.name} · added" else playlist.name,
                                Modifier.weight(1f).padding(start = 12.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showPlaylistPicker = false }) { Text("Done") } },
        )
    }
}

@Composable
private fun LibraryArtwork(
    song: SongRef,
    state: AppUiState,
    viewModel: MusicViewModel,
    modifier: Modifier,
    shape: RoundedCornerShape,
    fallback: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.MusicNote,
) {
    LaunchedEffect(song.uri) { viewModel.requestArtwork(song) }
    Surface(modifier = modifier, shape = shape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        val artwork = state.artworkByUri[song.uri]
        if (artwork != null) {
            Image(artwork.asImageBitmap(), "Artwork for ${song.title}", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Box(contentAlignment = Alignment.Center) { Icon(fallback, null, tint = MaterialTheme.colorScheme.primary) }
        }
    }
}

@Composable
private fun CompactGlassPlayer(state: AppUiState, viewModel: MusicViewModel, onOpenPlayer: () -> Unit) {
    FrostedSurface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenEdge, vertical = 8.dp),
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp, bottomEnd = 22.dp, bottomStart = 22.dp),
    ) {
        Column(Modifier.clickable(onClick = onOpenPlayer).padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ExpressiveArtwork(state, Modifier.size(58.dp), RoundedCornerShape(18.dp))
                AnimatedContent(
                    targetState = Triple(
                        state.nowPlayingUri,
                        state.metadata?.title ?: "Loading…",
                        state.metadata?.artist.orEmpty(),
                    ),
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                    transitionSpec = {
                        (fadeIn(tween(220)) + slideInVertically(tween(300)) { it / 3 }) togetherWith
                            (fadeOut(tween(130)) + slideOutVertically(tween(220)) { -it / 4 })
                    },
                    label = "mini player track",
                ) { (_, title, artist) ->
                    Column {
                        Text(
                            title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            artist,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = viewModel::previousVisibleSong) { Icon(Icons.Default.SkipPrevious, "Previous song") }
                MorphingPlayButton(state.isPlaying, viewModel::togglePlayPause, Modifier.size(54.dp))
                IconButton(onClick = viewModel::nextVisibleSong) { Icon(Icons.Default.SkipNext, "Next song") }
            }
            ExpressiveSeekBar(state, viewModel, showTime = false, compact = true)
        }
    }
}

@Composable
private fun ExpressiveReview(
    state: AppUiState,
    viewModel: MusicViewModel,
    onClose: () -> Unit,
    onOpenDecisions: () -> Unit,
) {
    BackHandler(onBack = onClose)
    LaunchedEffect(state.currentSwipeSong?.uri) {
        val current = state.currentSwipeSong
        if (current != null && current.uri != state.nowPlayingUri) viewModel.playReviewSong(current)
    }

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Close Review")
            }
            Column(Modifier.weight(1f)) {
                Text("Decide by listening", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "${state.swipeCursor} of ${state.swipeOrder.size} · ${state.reviewFolderName ?: "All music"}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                IconButton(onClick = viewModel::undo, enabled = state.history.isNotEmpty(), modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.Undo, "Undo last decision")
                }
                TextButton(onClick = onOpenDecisions, enabled = state.decisions.isNotEmpty()) {
                    Text("Results")
                }
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
private fun SwipeablePlayerArtwork(
    state: AppUiState,
    viewModel: MusicViewModel,
    modifier: Modifier,
    shape: RoundedCornerShape,
) {
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    val displayedOffset by animateFloatAsState(
        targetValue = dragOffset,
        animationSpec = if (dragging) tween(16) else spring(stiffness = 520f),
        label = "player artwork swipe",
    )
    BoxWithConstraints(modifier) {
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }.coerceAtLeast(1f)
        val threshold = widthPx * .18f
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(state.nowPlayingUri) {
                    detectDragGestures(
                        onDragStart = { dragging = true },
                        onDragCancel = {
                            dragging = false
                            dragOffset = 0f
                        },
                        onDragEnd = {
                            val releaseOffset = dragOffset
                            dragging = false
                            if (abs(releaseOffset) >= threshold) {
                                if (releaseOffset < 0) viewModel.nextVisibleSong() else viewModel.previousVisibleSong()
                            }
                            dragOffset = 0f
                        },
                        onDrag = { change, amount ->
                            if (abs(amount.x) >= abs(amount.y)) {
                                change.consume()
                                dragOffset = (dragOffset + amount.x).coerceIn(-widthPx, widthPx)
                            }
                        },
                    )
                },
        ) {
            ExpressiveArtwork(
                state,
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = displayedOffset
                        rotationZ = (displayedOffset / widthPx) * 2.5f
                        alpha = 1f - (abs(displayedOffset) / widthPx) * .28f
                    },
                shape,
            )
            val cueAlpha = (abs(displayedOffset) / threshold).coerceIn(0f, 1f)
            if (cueAlpha > 0f) {
                Surface(
                    modifier = Modifier
                        .align(if (displayedOffset < 0) Alignment.CenterEnd else Alignment.CenterStart)
                        .padding(16.dp)
                        .alpha(cueAlpha),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = .86f),
                ) {
                    Icon(
                        if (displayedOffset < 0) Icons.Default.SkipNext else Icons.Default.SkipPrevious,
                        if (displayedOffset < 0) "Release for next song" else "Release for previous song",
                        Modifier.padding(12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpressiveArtwork(state: AppUiState, modifier: Modifier, shape: RoundedCornerShape) {
    val art = state.metadata?.artwork
    Box(
        modifier.clip(shape).background(MaterialTheme.colorScheme.surfaceContainerLow),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = state.nowPlayingUri to art,
            transitionSpec = {
                (fadeIn(tween(300)) + scaleIn(tween(380), initialScale = .96f)) togetherWith
                    (fadeOut(tween(170)) + scaleOut(tween(240), targetScale = 1.025f))
            },
            label = "album artwork change",
        ) { (_, artwork) ->
            if (artwork != null) {
                Image(
                    artwork.asImageBitmap(),
                    contentDescription = "Album artwork",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.GraphicEq,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = .68f),
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayingBars(isPlaying: Boolean) {
    val color = MaterialTheme.colorScheme.primary
    if (!isPlaying) {
        Icon(Icons.Default.GraphicEq, "Current song paused", Modifier.size(24.dp), tint = color)
        return
    }
    val motion = rememberInfiniteTransition(label = "playing bars")
    val first by motion.animateFloat(
        initialValue = .35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(520, easing = LinearEasing), AnimationRepeatMode.Reverse),
        label = "playing bar one",
    )
    val second by motion.animateFloat(
        initialValue = 1f,
        targetValue = .25f,
        animationSpec = infiniteRepeatable(tween(430, easing = LinearEasing), AnimationRepeatMode.Reverse),
        label = "playing bar two",
    )
    val third by motion.animateFloat(
        initialValue = .5f,
        targetValue = .9f,
        animationSpec = infiniteRepeatable(tween(610, easing = LinearEasing), AnimationRepeatMode.Reverse),
        label = "playing bar three",
    )
    Row(
        Modifier.size(width = 24.dp, height = 26.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        listOf(first, second, third).forEach { level ->
            Box(
                Modifier
                    .width(4.dp)
                    .height(22.dp)
                    .graphicsLayer {
                        scaleY = .32f + (.68f * level)
                        transformOrigin = TransformOrigin(.5f, 1f)
                    }
                    .background(color, RoundedCornerShape(4.dp)),
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ExpressiveSeekBar(
    state: AppUiState,
    viewModel: MusicViewModel,
    showTime: Boolean,
    compact: Boolean = false,
) {
    var dragging by remember { mutableStateOf(false) }
    var scrubPosition by remember { mutableStateOf(0f) }
    var pendingSeekPosition by remember(state.nowPlayingUri) { mutableStateOf<Float?>(null) }
    var hapticStep by remember { mutableStateOf(-1) }
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val duration = state.durationMs.coerceAtLeast(1).toFloat()
    val currentPosition = state.positionMs.coerceIn(0, state.durationMs.coerceAtLeast(0)).toFloat()
    val smoothPosition = remember(state.nowPlayingUri) { Animatable(currentPosition) }
    val displayedPosition = if (dragging) scrubPosition else pendingSeekPosition ?: smoothPosition.value
    val amplitude by animateFloatAsState(
        targetValue = if (state.isPlaying && !dragging) 1f else 0f,
        animationSpec = tween(260),
        label = "seek wave life",
    )
    val thumbWidth by animateDpAsState(
        targetValue = if (dragging) 6.dp else if (compact) 10.dp else 14.dp,
        animationSpec = spring(stiffness = 520f, dampingRatio = .72f),
        label = "seek thumb width",
    )
    val thumbHeight by animateDpAsState(
        targetValue = if (dragging) 26.dp else if (compact) 10.dp else 14.dp,
        animationSpec = spring(stiffness = 520f, dampingRatio = .72f),
        label = "seek thumb height",
    )
    val primary = MaterialTheme.colorScheme.primary
    val inactive = MaterialTheme.colorScheme.surfaceContainerHighest

    LaunchedEffect(state.nowPlayingUri, currentPosition, dragging, state.isPlaying) {
        if (!dragging) {
            scrubPosition = currentPosition
            val pending = pendingSeekPosition
            val seekTolerance = maxOf(750f, duration * .005f)
            if (pending != null && abs(currentPosition - pending) <= seekTolerance) {
                smoothPosition.snapTo(currentPosition)
                pendingSeekPosition = null
            } else if (pending == null && abs(smoothPosition.value - currentPosition) > maxOf(1_500f, duration * .02f)) {
                smoothPosition.snapTo(currentPosition)
            } else if (pending == null) {
                smoothPosition.animateTo(
                    targetValue = currentPosition,
                    animationSpec = tween(if (state.isPlaying) 520 else 160, easing = LinearEasing),
                )
            }
        }
    }

    val seekModifier = Modifier
        .fillMaxWidth()
        .height(if (compact) 34.dp else 48.dp)
        .semantics {
            progressBarRangeInfo = ProgressBarRangeInfo(displayedPosition, 0f..duration)
            setProgress { target ->
                val value = target.coerceIn(0f, duration)
                scrubPosition = value
                pendingSeekPosition = value
                viewModel.seekTo(value.toLong())
                true
            }
        }
        .pointerInput(duration) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                pendingSeekPosition = null
                dragging = true
                scrubPosition = (down.position.x / size.width.toFloat()).coerceIn(0f, 1f) * duration
                hapticStep = ((scrubPosition / duration) * 20).toInt()
                down.consume()
                drag(down.id) { change ->
                    scrubPosition = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f) * duration
                    val nextStep = ((scrubPosition / duration) * 20).toInt()
                    if (nextStep != hapticStep) {
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                        hapticStep = nextStep
                    }
                    change.consume()
                }
                val targetPosition = scrubPosition
                pendingSeekPosition = targetPosition
                viewModel.seekTo(targetPosition.toLong())
                dragging = false
            }
        }

    Column {
        BoxWithConstraints(seekModifier, contentAlignment = Alignment.CenterStart) {
            val thumbFraction = (displayedPosition / duration).coerceIn(0f, 1f)
            val thumbTravelPx = with(density) { (maxWidth - 14.dp).toPx() }
            LinearWavyProgressIndicator(
                progress = { thumbFraction },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 7.dp),
                color = primary,
                trackColor = inactive,
                stroke = Stroke(width = with(density) { (if (compact) 3.dp else 4.dp).toPx() }, cap = StrokeCap.Round),
                trackStroke = Stroke(width = with(density) { (if (compact) 3.dp else 4.dp).toPx() }, cap = StrokeCap.Round),
                gapSize = 7.dp,
                wavelength = if (compact) 24.dp else 28.dp,
                amplitude = { amplitude },
                waveSpeed = if (state.isPlaying && !dragging) 14.dp else 0.dp,
            )
            Box(
                Modifier
                    .padding(start = 7.dp)
                    .graphicsLayer {
                        translationX = thumbTravelPx * thumbFraction - thumbWidth.toPx() / 2f
                    }
                    .size(width = thumbWidth, height = thumbHeight)
                    .background(primary, RoundedCornerShape(if (dragging) 4.dp else 50.dp)),
            )
        }
        if (showTime) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatExpressiveTime(displayedPosition.toLong()), style = MaterialTheme.typography.labelSmall)
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
        TextButton(onClick = { viewModel.prepareReview(state.reviewFolderName) }) {
            Icon(Icons.Default.Refresh, null)
            Spacer(Modifier.width(8.dp))
            Text("Review undecided songs")
        }
    }
}

@Composable
private fun ExpressiveReviewDecisions(state: AppUiState, viewModel: MusicViewModel, onBack: () -> Unit) {
    var confirmDelete by remember { mutableStateOf(false) }
    var showAccepted by rememberSaveable { mutableStateOf(false) }
    BackHandler(onBack = onBack)
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = ScreenEdge, vertical = 18.dp)) {
            ContextPageHeader(
                title = "Review decisions",
                supportingText = "Accepted songs stay in your library. Rejected songs remain hidden until you clear their decision.",
                backDescription = "Back to Settings",
                onBack = onBack,
            )
            Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !showAccepted,
                    onClick = { showAccepted = false },
                    label = { Text("Rejected ${state.rejectedSongs.size}") },
                )
                FilterChip(
                    selected = showAccepted,
                    onClick = { showAccepted = true },
                    label = { Text("Accepted ${state.acceptedSongs.size}") },
                )
            }
            if (!showAccepted) {
                Button(
                    onClick = { confirmDelete = true },
                    enabled = state.rejectedSongs.isNotEmpty() && !state.isDeleting,
                    modifier = Modifier.padding(top = 10.dp).height(50.dp),
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
        }

        AnimatedContent(
            targetState = showAccepted,
            modifier = Modifier.weight(1f),
            transitionSpec = {
                (fadeIn(tween(180)) + slideInHorizontally(tween(260)) { if (targetState) it / 6 else -it / 6 }) togetherWith
                    (fadeOut(tween(120)) + slideOutHorizontally(tween(200)) { if (targetState) -it / 8 else it / 8 })
            },
            label = "review decision filter",
        ) { accepted ->
            val songs = if (accepted) state.acceptedSongs else state.rejectedSongs
        if (songs.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(if (accepted) Icons.Default.Check else Icons.Default.Restore, null, Modifier.size(50.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(14.dp))
                Text(if (accepted) "Nothing accepted yet" else "Nothing rejected", style = MaterialTheme.typography.headlineSmall)
                Text(if (accepted) "Songs kept in Review will appear here." else "Songs rejected in Review will appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = ScreenEdge, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(songs, key = SongRef::uri) { song ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = .56f)).padding(ListItemInset),
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
                        IconButton(onClick = { viewModel.clearReviewDecision(song) }) {
                            Icon(Icons.Default.Restore, if (accepted) "Clear accepted decision" else "Restore to library")
                        }
                    }
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
                    onClick = { confirmDelete = false; viewModel.requestDeleteRejected() },
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

@Composable
private fun FullPlayerOverlay(
    state: AppUiState,
    viewModel: MusicViewModel,
    onDismiss: () -> Unit,
) {
    var showQueue by remember { mutableStateOf(false) }
    var showSaveQueue by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var showPlayerMenu by remember { mutableStateOf(false) }
    var showPlaylistPicker by remember { mutableStateOf(false) }
    var confirmDeleteSong by remember { mutableStateOf(false) }
    val lyricsPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::importLyrics)
    }
    BackHandler {
        when {
            showQueue -> showQueue = false
            showLyrics -> showLyrics = false
            else -> onDismiss()
        }
    }
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Box(Modifier.fillMaxSize()) {
            LivingBackdrop(state)
            Column(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = if (showQueue) ({ showQueue = false }) else onDismiss) {
                        Icon(Icons.Default.ExpandMore, if (showQueue) "Back to player" else "Close player")
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            when {
                                showQueue -> "Up next"
                                showLyrics -> "Lyrics"
                                else -> "Now playing"
                            },
                            style = MaterialTheme.typography.titleMedium,
                        )
                        if (!showQueue) {
                            Text(
                                state.metadata?.album.orEmpty(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    TextButton(onClick = { showLyrics = !showLyrics; showQueue = false }) { Text("Lyrics") }
                    IconButton(onClick = { showQueue = !showQueue }) {
                        Icon(Icons.AutoMirrored.Filled.QueueMusic, if (showQueue) "Hide queue" else "Show queue")
                    }
                    state.nowPlayingSong?.let { song ->
                        Box {
                            IconButton(onClick = { showPlayerMenu = true }) {
                                Icon(Icons.Default.MoreVert, "More playback options")
                            }
                            DropdownMenu(expanded = showPlayerMenu, onDismissRequest = { showPlayerMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Add to playlist") },
                                    onClick = {
                                        showPlayerMenu = false
                                        showPlaylistPicker = true
                                    },
                                    enabled = state.playlists.isNotEmpty(),
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null) },
                                )
                                val reviewDecision = state.decisions[song.uri]
                                if (reviewDecision != ReviewDecision.KEPT) {
                                    DropdownMenuItem(
                                        text = { Text("Mark accepted") },
                                        onClick = {
                                            showPlayerMenu = false
                                            viewModel.setReviewDecision(song, ReviewDecision.KEPT)
                                        },
                                        leadingIcon = { Icon(Icons.Default.Check, null) },
                                    )
                                }
                                if (reviewDecision != ReviewDecision.REJECTED) {
                                    DropdownMenuItem(
                                        text = { Text("Mark rejected") },
                                        onClick = {
                                            showPlayerMenu = false
                                            viewModel.setReviewDecision(song, ReviewDecision.REJECTED)
                                        },
                                        leadingIcon = { Icon(Icons.Default.Close, null) },
                                    )
                                }
                                if (reviewDecision != null) {
                                    DropdownMenuItem(
                                        text = { Text("Clear Review decision") },
                                        onClick = {
                                            showPlayerMenu = false
                                            viewModel.setReviewDecision(song, null)
                                        },
                                        leadingIcon = { Icon(Icons.Default.Restore, null) },
                                    )
                                }
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Delete from device") },
                                    onClick = {
                                        showPlayerMenu = false
                                        confirmDeleteSong = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.DeleteOutline, null) },
                                )
                            }
                        }
                    }
                }

                AnimatedContent(
                    targetState = when {
                        showQueue -> PlayerPane.QUEUE
                        showLyrics -> PlayerPane.LYRICS
                        else -> PlayerPane.NOW_PLAYING
                    },
                    modifier = Modifier.weight(1f),
                    transitionSpec = {
                        (fadeIn(tween(180)) + slideInHorizontally(tween(280)) { it / 7 }) togetherWith
                            (fadeOut(tween(130)) + slideOutHorizontally(tween(220)) { -it / 9 })
                    },
                    label = "player pane",
                ) { pane ->
                    Column(Modifier.fillMaxSize()) {
                if (pane == PlayerPane.QUEUE) {
                    val queue = remember(state.playbackQueue, state.songs) {
                        val songsByUri = state.songs.associateBy(SongRef::uri)
                        state.playbackQueue.mapNotNull(songsByUri::get)
                    }
                    if (queue.isEmpty()) {
                        Column(
                            Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Icon(Icons.AutoMirrored.Filled.QueueMusic, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(16.dp))
                            Text("The queue is empty", style = MaterialTheme.typography.headlineSmall)
                        }
                    } else {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(onClick = viewModel::undoQueueEdit, modifier = Modifier.weight(1f)) { Text("Undo") }
                            OutlinedButton(onClick = viewModel::clearUpcomingQueue, modifier = Modifier.weight(1f)) { Text("Clear") }
                            Button(onClick = { showSaveQueue = true }, modifier = Modifier.weight(1f)) { Text("Save") }
                        }
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            itemsIndexed(queue, key = { _, song -> song.uri }) { index, song ->
                                val active = song.uri == state.nowPlayingUri
                                Row(
                                    Modifier
                                        .animateItem(fadeInSpec = tween(140), fadeOutSpec = tween(120), placementSpec = tween(180))
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(if (active) 24.dp else 16.dp))
                                        .background(
                                            if (active) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .86f)
                                            else Color.Transparent,
                                        )
                                        .clickable { viewModel.playQueueItem(song.uri) }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    LibraryArtwork(song, state, viewModel, Modifier.size(52.dp), RoundedCornerShape(15.dp))
                                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                        Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                                        Text(song.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Column {
                                        TextButton(
                                            onClick = { viewModel.moveQueueItem(index, index - 1) },
                                            enabled = index > 0,
                                        ) { Text("↑") }
                                        TextButton(
                                            onClick = { viewModel.moveQueueItem(index, index + 1) },
                                            enabled = index < queue.lastIndex,
                                        ) { Text("↓") }
                                    }
                                    IconButton(onClick = { viewModel.removeQueueItem(song.uri) }) {
                                        Icon(Icons.Default.Close, "Remove ${song.title} from queue")
                                    }
                                }
                            }
                        }
                    }
                } else if (pane == PlayerPane.LYRICS) {
                    if (state.lyrics.isEmpty()) {
                        Column(
                            Modifier.weight(1f).fillMaxWidth().padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Icon(Icons.Default.MusicNote, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(16.dp))
                            Text("No local lyrics yet", style = MaterialTheme.typography.headlineSmall)
                            Text("Import a plain text or synchronized .lrc file for this song.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Button(onClick = { lyricsPicker.launch(arrayOf("text/plain", "application/octet-stream")) }, modifier = Modifier.padding(top = 18.dp)) {
                                Text("Import lyrics")
                            }
                        }
                    } else {
                        val activeIndex = state.lyrics.indexOfLast { line -> line.timestampMs?.let { it <= state.positionMs } == true }
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 32.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                        ) {
                            itemsIndexed(state.lyrics) { index, line ->
                                Text(
                                    line.text,
                                    style = if (index == activeIndex) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
                                    color = if (index == activeIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = line.timestampMs != null) { line.timestampMs?.let(viewModel::seekTo) }
                                        .padding(vertical = 4.dp),
                                )
                            }
                        }
                    }
                    ExpressiveSeekBar(state, viewModel, showTime = true)
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = viewModel::previousVisibleSong) { Icon(Icons.Default.SkipPrevious, "Previous song") }
                        MorphingPlayButton(state.isPlaying, viewModel::togglePlayPause, Modifier.size(64.dp))
                        IconButton(onClick = viewModel::nextVisibleSong) { Icon(Icons.Default.SkipNext, "Next song") }
                    }
                } else {
                    BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                        val artworkSize = minOf(maxWidth, maxHeight * .68f)
                        Column(
                            Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            SwipeablePlayerArtwork(
                                state,
                                viewModel,
                                Modifier.size(artworkSize),
                                RoundedCornerShape(topStart = 42.dp, topEnd = 26.dp, bottomEnd = 42.dp, bottomStart = 26.dp),
                            )
                            Spacer(Modifier.height(22.dp))
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        state.metadata?.title ?: state.nowPlayingSong?.title.orEmpty(),
                                        style = MaterialTheme.typography.headlineSmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        state.metadata?.artist ?: state.nowPlayingSong?.artist.orEmpty(),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                state.nowPlayingSong?.let { song ->
                                    IconButton(onClick = { viewModel.toggleFavorite(song) }) {
                                        Icon(
                                            if (state.isFavorite(song)) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            if (state.isFavorite(song)) "Remove from favorites" else "Add to favorites",
                                            tint = if (state.isFavorite(song)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    ExpressiveSeekBar(state, viewModel, showTime = true)
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = viewModel::toggleShuffle) {
                            Icon(
                                Icons.Default.Shuffle,
                                if (state.shuffleEnabled) "Disable shuffle" else "Enable shuffle",
                                tint = if (state.shuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = viewModel::previousVisibleSong, modifier = Modifier.size(58.dp)) {
                            Icon(Icons.Default.SkipPrevious, "Previous song", Modifier.size(34.dp))
                        }
                        MorphingPlayButton(state.isPlaying, viewModel::togglePlayPause, Modifier.size(76.dp))
                        IconButton(onClick = viewModel::nextVisibleSong, modifier = Modifier.size(58.dp)) {
                            Icon(Icons.Default.SkipNext, "Next song", Modifier.size(34.dp))
                        }
                        IconButton(onClick = viewModel::cycleRepeatMode) {
                            Icon(
                                if (state.repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                                "Repeat ${state.repeatMode.name.lowercase()}",
                                tint = if (state.repeatMode == RepeatMode.OFF) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                    }
                }
            }
        }
    }
    if (showSaveQueue) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSaveQueue = false },
            title = { Text("Save queue") },
            text = {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Playlist name") }, singleLine = true)
            },
            confirmButton = {
                Button(onClick = { viewModel.saveQueueAsPlaylist(name); showSaveQueue = false }, enabled = name.isNotBlank()) {
                    Text("Save playlist")
                }
            },
            dismissButton = { TextButton(onClick = { showSaveQueue = false }) { Text("Cancel") } },
        )
    }
    val currentSong = state.nowPlayingSong
    if (showPlaylistPicker && currentSong != null) {
        AlertDialog(
            onDismissRequest = { showPlaylistPicker = false },
            icon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null) },
            title = { Text("Add to playlist") },
            text = {
                LazyColumn(
                    Modifier.fillMaxWidth().heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(state.playlists, key = UserPlaylist::id) { playlist ->
                        val alreadyAdded = currentSong.uri in playlist.songUris
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable(enabled = !alreadyAdded) {
                                    viewModel.addToPlaylist(playlist.id, currentSong)
                                    showPlaylistPicker = false
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(if (alreadyAdded) Icons.Default.Check else Icons.AutoMirrored.Filled.PlaylistAdd, null)
                            Text(
                                if (alreadyAdded) "${playlist.name} · added" else playlist.name,
                                Modifier.weight(1f).padding(start = 12.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showPlaylistPicker = false }) { Text("Done") } },
        )
    }
    if (confirmDeleteSong && currentSong != null) {
        AlertDialog(
            onDismissRequest = { confirmDeleteSong = false },
            icon = { Icon(Icons.Default.DeleteOutline, null) },
            title = { Text("Delete this song?") },
            text = { Text("${currentSong.title} will be permanently removed from this device.") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDeleteSong = false
                        viewModel.requestDeleteSong(currentSong)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteSong = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ExpressiveSettings(
    state: AppUiState,
    viewModel: MusicViewModel,
    onChooseFolder: () -> Unit,
    onBack: () -> Unit,
    onStartReview: (String?) -> Unit,
    onOpenReviewDecisions: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    var reviewScope by rememberSaveable { mutableStateOf<String?>(state.reviewFolderName) }
    val reviewFolders = remember(state.songs) { state.songs.map(SongRef::folderName).distinct().sorted() }
    val reviewCandidates = state.songs.count { song ->
        song.uri !in state.decisions && (reviewScope == null || song.folderName == reviewScope)
    }
    val exportBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let(viewModel::exportBackup)
    }
    val importBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::importBackup)
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            ContextPageHeader(
                title = "Settings",
                supportingText = "Make SwipeTune feel at home on this phone.",
                backDescription = "Back to library",
                onBack = onBack,
            )
        }
        item {
            SettingsPanel("Appearance") {
                Text("Theme", style = MaterialTheme.typography.titleMedium)
                Text("Follow Android or choose a fixed appearance.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(ThemeMode.entries) { mode ->
                        val selected = state.themeMode == mode
                        Surface(
                            onClick = { viewModel.setThemeMode(mode) },
                            shape = RoundedCornerShape(18.dp),
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        ) {
                            Row(
                                Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                if (selected) Icon(Icons.Default.Check, null, Modifier.size(18.dp))
                                Text(mode.name.lowercase().replaceFirstChar(Char::uppercase), style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Use phone colors", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Build the palette from your Android wallpaper.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Switch(checked = state.dynamicColor, onCheckedChange = viewModel::setDynamicColor)
                }
                AppearanceSwitch("Artwork atmosphere", "Use the current cover as a soft player backdrop.", state.artworkAtmosphere, viewModel::setArtworkAtmosphere)
                AppearanceSwitch("Expressive motion", "Use container, shape, and shared-axis transitions.", state.expressiveMotion, viewModel::setExpressiveMotion)
                AppearanceSwitch("Compact library rows", "Fit more songs on screen without shrinking touch targets.", state.compactLibraryRows, viewModel::setCompactLibraryRows)
            }
        }
        item {
            SettingsPanel("Music library") {
                Text(
                    "${state.songs.size} indexed songs · ${state.rejectedSongs.size} hidden by Review",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(onClick = viewModel::rescanLibrary, modifier = Modifier.weight(1f), shape = RoundedCornerShape(18.dp)) {
                        Icon(Icons.Default.Refresh, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Rescan")
                    }
                    OutlinedButton(onClick = onChooseFolder, modifier = Modifier.weight(1f), shape = RoundedCornerShape(18.dp)) {
                        Icon(Icons.Default.FolderOpen, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add folder")
                    }
                }
                OutlinedButton(
                    onClick = viewModel::findDuplicates,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    shape = RoundedCornerShape(18.dp),
                ) { Text("Find duplicates") }
                state.duplicateGroups.take(3).forEach { group ->
                    val keep = group.songs.firstOrNull { it.uri == group.recommendedKeepUri }
                    Text(
                        "${group.songs.size} copies · keep ${keep?.fileName ?: "highest quality"}",
                        modifier = Modifier.padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        item {
            SettingsPanel("Review your music") {
                Text(
                    "Optional listening cleanup. Rejected songs stay hidden throughout SwipeTune; accepted songs remain in your library.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text("Choose what to review", modifier = Modifier.padding(top = 14.dp), style = MaterialTheme.typography.titleMedium)
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        FilterChip(
                            selected = reviewScope == null,
                            onClick = { reviewScope = null },
                            label = { Text("All music") },
                        )
                    }
                    items(reviewFolders) { folder ->
                        FilterChip(
                            selected = reviewScope == folder,
                            onClick = { reviewScope = folder },
                            label = { Text(folder, maxLines = 1) },
                        )
                    }
                }
                Button(
                    onClick = { onStartReview(reviewScope) },
                    enabled = reviewCandidates > 0 || state.reviewFolderName == reviewScope,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Icon(Icons.Default.Style, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (state.reviewFolderName == reviewScope && state.currentSwipeSong != null) "Continue Review" else "Start Review · $reviewCandidates")
                }
                OutlinedButton(
                    onClick = onOpenReviewDecisions,
                    enabled = state.decisions.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Text("Accepted ${state.acceptedSongs.size} · Rejected ${state.rejectedSongs.size}")
                }
                Text(
                    "Your position and decisions are saved when you leave Review.",
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        item {
            SettingsPanel("Playback") {
                Text("Android media controls", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Playback is shared with the lock screen, notification shade, Bluetooth, and headset controls.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(
                    Modifier.fillMaxWidth().padding(top = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Equalizer", style = MaterialTheme.typography.titleMedium)
                        Text("Attach audio effects to this player’s active audio session.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = state.playbackPreferences.equalizerEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.updatePlaybackPreferences { it.copy(equalizerEnabled = enabled) }
                        },
                    )
                }
                if (state.playbackPreferences.equalizerEnabled) {
                    var bassDragging by remember { mutableStateOf(false) }
                    var loudnessDragging by remember { mutableStateOf(false) }
                    val bassState = remember { SliderState(state.playbackPreferences.bassBoost.toFloat(), 0, 0f..1000f) }
                    val loudnessState = remember { SliderState(state.playbackPreferences.loudnessGainMb.toFloat(), 0, 0f..2000f) }
                    LaunchedEffect(state.playbackPreferences.bassBoost, bassDragging) {
                        if (!bassDragging) bassState.value = state.playbackPreferences.bassBoost.toFloat()
                    }
                    LaunchedEffect(state.playbackPreferences.loudnessGainMb, loudnessDragging) {
                        if (!loudnessDragging) loudnessState.value = state.playbackPreferences.loudnessGainMb.toFloat()
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Preset ${state.playbackPreferences.equalizerPreset + 1}", style = MaterialTheme.typography.labelLarge)
                            Text("Uses the presets exposed by this phone.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OutlinedButton(
                            onClick = {
                                viewModel.updatePlaybackPreferences {
                                    it.copy(equalizerPreset = ((it.equalizerPreset + 1) % 10).toShort())
                                }
                            },
                        ) { Text("Next preset") }
                    }
                    Text("Bass boost", modifier = Modifier.padding(top = 14.dp), style = MaterialTheme.typography.labelLarge)
                    Slider(
                        state = bassState,
                        onValueChange = { bassDragging = true },
                        onValueChangeFinished = {
                            bassDragging = false
                            viewModel.updatePlaybackPreferences { it.copy(bassBoost = bassState.value.toInt().toShort()) }
                        },
                    )
                    Text("Loudness", style = MaterialTheme.typography.labelLarge)
                    Slider(
                        state = loudnessState,
                        onValueChange = { loudnessDragging = true },
                        onValueChangeFinished = {
                            loudnessDragging = false
                            viewModel.updatePlaybackPreferences { it.copy(loudnessGainMb = loudnessState.value.toInt()) }
                        },
                    )
                }
                HorizontalDivider(Modifier.padding(vertical = 16.dp))
                Text("Playback quality", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Tune decoding and transitions without changing your files.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                AppearanceSwitch(
                    "ReplayGain",
                    "Use embedded gain tags to reduce unexpectedly loud tracks.",
                    state.playbackPreferences.replayGainEnabled,
                ) { enabled -> viewModel.updatePlaybackPreferences { it.copy(replayGainEnabled = enabled) } }
                AppearanceSwitch(
                    "Gapless playback",
                    "Ask compatible decoders to remove silence between consecutive tracks.",
                    state.playbackPreferences.gaplessEnabled,
                ) { enabled -> viewModel.updatePlaybackPreferences { it.copy(gaplessEnabled = enabled) } }
                AppearanceSwitch(
                    "Battery-friendly offload",
                    "Let supported phones play audio on dedicated low-power hardware.",
                    state.playbackPreferences.offloadEnabled,
                ) { enabled -> viewModel.updatePlaybackPreferences { it.copy(offloadEnabled = enabled) } }
                Text(
                    if (state.playbackPreferences.crossfadeSeconds == 0) "Track transition · direct" else "Track transition · ${state.playbackPreferences.crossfadeSeconds}s fade",
                    modifier = Modifier.padding(top = 16.dp),
                    style = MaterialTheme.typography.labelLarge,
                )
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(listOf(0, 2, 4, 6, 8)) { seconds ->
                        FilterChip(
                            selected = state.playbackPreferences.crossfadeSeconds == seconds,
                            onClick = { viewModel.updatePlaybackPreferences { it.copy(crossfadeSeconds = seconds) } },
                            label = { Text(if (seconds == 0) "Direct" else "${seconds}s") },
                        )
                    }
                }
                Text(
                    "Fade transitions lower the ending track before advancing; true overlapping crossfade remains disabled to preserve one authoritative media session.",
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        item {
            SettingsPanel("Sleep timer") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timer, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        state.playbackPreferences.sleepDeadlineMs.takeIf { it > System.currentTimeMillis() }
                            ?.let { "Stops in ${((it - System.currentTimeMillis()) / 60_000L).coerceAtLeast(1)} min" }
                            ?: "Off",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(listOf(15, 30, 45, 60, 0)) { minutes ->
                        OutlinedButton(onClick = { viewModel.setSleepTimer(minutes) }) {
                            Text(if (minutes == 0) "Turn off" else "$minutes min")
                        }
                    }
                }
            }
        }
        item {
            SettingsPanel("Backup & restore") {
                Text(
                    "Android can back up app data through your configured cloud transport. You can also keep a portable SwipeTune backup yourself.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    Modifier.fillMaxWidth().padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = { exportBackup.launch("SwipeTune-backup.zip") },
                        modifier = Modifier.weight(1f),
                    ) { Text("Export") }
                    OutlinedButton(
                        onClick = { importBackup.launch(arrayOf("application/zip", "application/octet-stream")) },
                        modifier = Modifier.weight(1f),
                    ) { Text("Restore") }
                }
                Text(
                    "Backups contain app metadata, playlists, Review decisions, history, and settings—not your audio files.",
                    modifier = Modifier.padding(top = 10.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        item {
            SettingsPanel("About") {
                Text("SwipeTune", style = MaterialTheme.typography.titleMedium)
                Text(
                    "An open-source, local-first music player with an optional Review mode.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text("Version ${BuildConfig.VERSION_NAME}", modifier = Modifier.padding(top = 8.dp), style = MaterialTheme.typography.labelLarge)
                Text(
                    "GNU GPLv3 · Source: github.com/projectescape/swipetune · Provided without warranty.",
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedButton(
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://github.com/projectescape/swipetune/blob/main/PRIVACY.md"),
                                ),
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Text("Privacy policy")
                }
                TextButton(
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://github.com/projectescape/swipetune"),
                                ),
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("View source code")
                }
            }
        }
    }
}

@Composable
private fun AppearanceSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(top = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsPanel(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 20.dp, bottomEnd = 28.dp, bottomStart = 20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = .82f),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

private fun formatExpressiveTime(ms: Long): String {
    val totalSeconds = ms.coerceAtLeast(0) / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
