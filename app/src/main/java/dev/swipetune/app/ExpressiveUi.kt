package dev.swipetune.app

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

private enum class ExpressiveScreen { LIBRARY, REVIEW, REVIEW_DECISIONS, SETTINGS }

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
            if (state.isScanning) {
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
            if (showPlayer && state.nowPlayingSong != null) {
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
    val filtered = remember(state.visibleSongs, query, sort, libraryView, state.favorites) {
        val source = when (libraryView) {
            LibraryView.FAVORITES -> state.visibleSongs.filter { it.uri in state.favorites }
            LibraryView.HISTORY -> state.recentHistory.mapNotNull { event -> state.visibleSongs.firstOrNull { it.uri == event.trackUri } }
            else -> state.visibleSongs
        }
        source.filter { song ->
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

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 22.dp, end = 16.dp, top = 18.dp, bottom = 12.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Your library", style = MaterialTheme.typography.headlineLarge)
                Text(
                    "${state.visibleSongs.size} songs · ${state.favorites.size} favorites",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            IconButton(
                onClick = {
                    sort = when (sort) {
                        LibrarySort.TITLE -> LibrarySort.ARTIST
                        LibrarySort.ARTIST -> LibrarySort.ALBUM
                        LibrarySort.ALBUM -> LibrarySort.TITLE
                    }
                },
                modifier = Modifier.size(52.dp),
            ) { Icon(Icons.Default.SortByAlpha, "Sort by ${sort.label}") }
            FilledIconButton(
                onClick = viewModel::shuffleLibrary,
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(20.dp),
            ) { Icon(Icons.Default.Shuffle, "Play a random song") }
            IconButton(onClick = onOpenSettings, modifier = Modifier.size(52.dp)) {
                Icon(Icons.Default.Settings, "Settings")
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            singleLine = true,
            shape = RoundedCornerShape(22.dp),
            leadingIcon = { Icon(Icons.Default.Search, null) },
            placeholder = { Text("Search songs, artists, albums") },
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(LibraryView.entries) { item ->
                val selected = libraryView == item
                Surface(
                    onClick = { libraryView = item },
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
                Text("${state.selectedSongUris.size} selected", modifier = Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.labelLarge)
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 10.dp),
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

        val collectionGroups = when (libraryView) {
            LibraryView.ALBUMS -> filtered.groupBy(SongRef::album)
            LibraryView.ARTISTS -> filtered.groupBy(SongRef::artist)
            LibraryView.GENRES -> filtered.groupBy(SongRef::genre)
            LibraryView.FOLDERS -> filtered.groupBy(SongRef::folderName)
            else -> emptyMap()
        }
        val selectedSongs = selectedCollection?.let(collectionGroups::get)

        if (selectedSongs != null) {
            CollectionDetail(
                name = selectedCollection.orEmpty(),
                songs = selectedSongs,
                state = state,
                viewModel = viewModel,
                onBack = { selectedCollection = null },
                modifier = Modifier.weight(1f),
            )
        } else if (libraryView == LibraryView.PLAYLISTS) {
            ExpressivePlaylists(
                state = state,
                viewModel = viewModel,
                selectedPlaylistId = selectedPlaylistId,
                onSelectedPlaylist = { selectedPlaylistId = it },
                onCreate = { showCreatePlaylist = true },
                modifier = Modifier.weight(1f),
            )
        } else if (state.visibleSongs.isEmpty()) {
            EmptyLibrary(Modifier.weight(1f))
        } else if (filtered.isEmpty()) {
            EmptyLibraryResult(query, libraryView, Modifier.weight(1f))
        } else {
            when (libraryView) {
                LibraryView.SONGS, LibraryView.FAVORITES, LibraryView.HISTORY -> LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(filtered, key = SongRef::uri) { song ->
                        ExpressiveSongRow(song, state, viewModel) { viewModel.playCollection(filtered, song) }
                    }
                }
                LibraryView.ALBUMS -> LibraryCollections(
                    groups = filtered.groupBy(SongRef::album),
                    state = state,
                    viewModel = viewModel,
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Album,
                    onOpen = { selectedCollection = it },
                )
                LibraryView.ARTISTS -> LibraryCollections(
                    groups = filtered.groupBy(SongRef::artist),
                    state = state,
                    viewModel = viewModel,
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Person,
                    onOpen = { selectedCollection = it },
                )
                LibraryView.GENRES -> LibraryCollections(
                    groups = filtered.groupBy(SongRef::genre),
                    state = state,
                    viewModel = viewModel,
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.GraphicEq,
                    onOpen = { selectedCollection = it },
                )
                LibraryView.FOLDERS -> LibraryCollections(
                    groups = filtered.groupBy(SongRef::folderName),
                    state = state,
                    viewModel = viewModel,
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Folder,
                    onOpen = { selectedCollection = it },
                )
                LibraryView.PLAYLISTS -> Unit
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
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
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
                                .padding(10.dp),
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
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
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
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
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
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                itemsIndexed(songs, key = { _, song -> song.uri }) { index, song ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable { viewModel.playCollection(songs, song) }.padding(10.dp),
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
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).clickable(onClick = onOpen).padding(10.dp),
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
    Column(modifier) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to playlists") }
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleLarge)
                Text(
                    if (rejected) "${songs.size} songs · hidden elsewhere" else "${songs.size} songs · kept in Review",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FilledIconButton(onClick = { if (songs.isNotEmpty()) viewModel.playCollection(songs) }, enabled = songs.isNotEmpty()) {
                Icon(Icons.Default.PlayArrow, "Play $name")
            }
        }
        Text(
            "Built in · updates automatically · cannot be deleted",
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
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
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(songs, key = SongRef::uri) { song ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable { viewModel.playCollection(songs, song) }.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LibraryArtwork(song, state, viewModel, Modifier.size(52.dp), RoundedCornerShape(15.dp))
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(song.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { viewModel.clearReviewDecision(song) }) {
                            Text(if (rejected) "Restore" else "Unmark")
                        }
                    }
                }
            }
        }
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
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(groups.entries.sortedBy { it.key.lowercase() }, key = { it.key }) { (name, songs) ->
            val first = songs.minWithOrNull(compareBy<SongRef>({ it.trackNumber.takeIf { number -> number > 0 } ?: Int.MAX_VALUE }, { it.title })) ?: return@items
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).clickable { onOpen(name) }.padding(10.dp),
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
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
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
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(songs, key = SongRef::uri) { song ->
                ExpressiveSongRow(song, state, viewModel) { viewModel.playCollection(songs, song) }
            }
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
        Text("Allow Android music access, then rescan—or add another storage folder from Settings.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExpressiveSongRow(song: SongRef, state: AppUiState, viewModel: MusicViewModel, onClick: () -> Unit) {
    var showMenu by remember(song.uri) { mutableStateOf(false) }
    val playing = song.uri == state.nowPlayingUri
    val selected = song.uri in state.selectedSongUris
    val shape = RoundedCornerShape(if (playing || selected) 22.dp else 14.dp)
    val container = when {
        selected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = .92f)
        playing -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .82f)
        else -> Color.Transparent
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(container)
            .combinedClickable(
                onClick = { if (state.selectedSongUris.isEmpty()) onClick() else viewModel.toggleSongSelection(song) },
                onLongClick = { viewModel.toggleSongSelection(song) },
            )
            .padding(horizontal = 12.dp, vertical = if (state.compactLibraryRows) 5.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        LibraryArtwork(song, state, viewModel, Modifier.size(52.dp), RoundedCornerShape(if (playing || selected) 18.dp else 14.dp))
        Column(Modifier.weight(1f)) {
            Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge, fontWeight = if (playing || selected) FontWeight.Bold else FontWeight.Normal)
            Text("${song.artist} · ${song.album}", maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
        IconButton(onClick = { viewModel.toggleFavorite(song) }) {
            Icon(
                if (state.isFavorite(song)) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                if (state.isFavorite(song)) "Remove from favorites" else "Add to favorites",
                tint = if (state.isFavorite(song)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
                if (state.playlists.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("Create a playlist first") },
                        onClick = { showMenu = false },
                        enabled = false,
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null) },
                    )
                } else {
                    state.playlists.forEach { playlist ->
                        val alreadyAdded = song.uri in playlist.songUris
                        DropdownMenuItem(
                            text = { Text(if (alreadyAdded) "In ${playlist.name}" else "Add to ${playlist.name}") },
                            onClick = {
                                viewModel.addToPlaylist(playlist.id, song)
                                showMenu = false
                            },
                            enabled = !alreadyAdded,
                            leadingIcon = { Icon(if (alreadyAdded) Icons.Default.Check else Icons.AutoMirrored.Filled.PlaylistAdd, null) },
                        )
                    }
                }
            }
        }
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
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp, bottomEnd = 22.dp, bottomStart = 22.dp),
    ) {
        Column(Modifier.clickable(onClick = onOpenPlayer).padding(horizontal = 12.dp, vertical = 10.dp)) {
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
private fun ExpressiveArtwork(state: AppUiState, modifier: Modifier, shape: RoundedCornerShape) {
    val art = state.metadata?.artwork
    Box(
        modifier.clip(shape).background(MaterialTheme.colorScheme.surfaceContainerLow),
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
                tint = MaterialTheme.colorScheme.primary.copy(alpha = .68f),
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
    val songs = if (showAccepted) state.acceptedSongs else state.rejectedSongs
    BackHandler(onBack = onBack)
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to Settings") }
                Text("Review decisions", style = MaterialTheme.typography.headlineLarge)
            }
            Text(
                "Accepted songs stay in your library. Rejected songs remain hidden until you clear their decision.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
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

        if (songs.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(if (showAccepted) Icons.Default.Check else Icons.Default.Restore, null, Modifier.size(50.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(14.dp))
                Text(if (showAccepted) "Nothing accepted yet" else "Nothing rejected", style = MaterialTheme.typography.headlineSmall)
                Text(if (showAccepted) "Songs kept in Review will appear here." else "Songs rejected in Review will appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(songs, key = SongRef::uri) { song ->
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
                        IconButton(onClick = { viewModel.clearReviewDecision(song) }) {
                            Icon(Icons.Default.Restore, if (showAccepted) "Clear accepted decision" else "Restore to library")
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

@Composable
private fun FullPlayerOverlay(
    state: AppUiState,
    viewModel: MusicViewModel,
    onDismiss: () -> Unit,
) {
    var showQueue by remember { mutableStateOf(false) }
    var showSaveQueue by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
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
                }

                if (showQueue) {
                    val queue = state.playbackQueue.mapNotNull { uri -> state.songs.firstOrNull { it.uri == uri } }
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
                } else if (showLyrics) {
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
                            ExpressiveArtwork(
                                state,
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to library") }
                Text("Settings", style = MaterialTheme.typography.headlineLarge)
            }
            Text(
                "Make SwipeTune feel at home on this phone.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
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
