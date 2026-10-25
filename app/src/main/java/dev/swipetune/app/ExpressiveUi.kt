package dev.swipetune.app

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlin.math.abs

private enum class ExpressiveScreen { LIBRARY, REVIEW, REJECTED, SETTINGS }

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
        if (screen == ExpressiveScreen.REVIEW || screen == ExpressiveScreen.REJECTED) {
            viewModel.enterReviewMode()
        } else {
            viewModel.exitReviewMode()
        }
    }

    if (state.folderUri == null && !state.isScanning) {
        ExpressiveOnboarding(onChooseFolder)
        return
    }

    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
        Box(Modifier.fillMaxSize()) {
            LivingBackdrop(state)
            Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = { ExpressiveHeader(onChooseFolder) },
            bottomBar = {
                Column {
                    AnimatedVisibility(state.nowPlayingUri != null && screen != ExpressiveScreen.REVIEW) {
                        CompactGlassPlayer(state, viewModel, onOpenPlayer = { showPlayer = true })
                    }
                    FloatingNavigation(
                        screen = screen,
                        onScreen = { screen = it },
                    )
                }
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
                        ExpressiveScreen.LIBRARY -> ExpressiveLibrary(state, viewModel, onOpenPlayer = { showPlayer = true })
                        ExpressiveScreen.REVIEW -> ExpressiveReview(state, viewModel, onOpenRejected = { screen = ExpressiveScreen.REJECTED })
                        ExpressiveScreen.REJECTED -> ExpressiveRejected(state, viewModel, onBack = { screen = ExpressiveScreen.REVIEW })
                        ExpressiveScreen.SETTINGS -> ExpressiveSettings(state, viewModel, onChooseFolder)
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
                    selected = screen == ExpressiveScreen.SETTINGS,
                    icon = { Icon(Icons.Default.Settings, null) },
                    label = "Settings",
                    onClick = { onScreen(ExpressiveScreen.SETTINGS) },
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
    Column(
        Modifier
            .width(70.dp)
            .height(60.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(background)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        icon()
        Text(
            label,
            modifier = Modifier.padding(top = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
        )
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
    SONGS("Songs"), ALBUMS("Albums"), ARTISTS("Artists"), FOLDERS("Folders"), FAVORITES("Favorites"), PLAYLISTS("Playlists")
}

private enum class LibrarySort(val label: String) { TITLE("Title"), ARTIST("Artist"), ALBUM("Album") }

@Composable
private fun ExpressiveLibrary(
    state: AppUiState,
    viewModel: MusicViewModel,
    onOpenPlayer: () -> Unit,
) {
    var libraryView by rememberSaveable { mutableStateOf(LibraryView.SONGS) }
    var query by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf(LibrarySort.TITLE) }
    var selectedPlaylistId by rememberSaveable { mutableStateOf<String?>(null) }
    var showCreatePlaylist by remember { mutableStateOf(false) }
    val filtered = remember(state.visibleSongs, query, sort, libraryView, state.favorites) {
        val source = if (libraryView == LibraryView.FAVORITES) {
            state.visibleSongs.filter { it.uri in state.favorites }
        } else state.visibleSongs
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

    Column(Modifier.fillMaxSize()) {
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

        if (libraryView == LibraryView.PLAYLISTS) {
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
                LibraryView.SONGS, LibraryView.FAVORITES -> LazyColumn(
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
                )
                LibraryView.ARTISTS -> LibraryCollections(
                    groups = filtered.groupBy(SongRef::artist),
                    state = state,
                    viewModel = viewModel,
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Person,
                )
                LibraryView.FOLDERS -> LibraryCollections(
                    groups = filtered.groupBy(SongRef::folderName),
                    state = state,
                    viewModel = viewModel,
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Folder,
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
    val selected = state.playlists.firstOrNull { it.id == selectedPlaylistId }
    if (selected == null) {
        Column(modifier) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Your playlists", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Button(onClick = onCreate, shape = RoundedCornerShape(18.dp)) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(6.dp))
                    Text("New")
                }
            }
            if (state.playlists.isEmpty()) {
                Column(
                    Modifier.fillMaxSize().padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.AutoMirrored.Filled.QueueMusic, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(14.dp))
                    Text("Build your first playlist", style = MaterialTheme.typography.headlineSmall)
                    Text("Create one here, then add songs from their menu.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = onCreate, modifier = Modifier.padding(top = 18.dp), shape = RoundedCornerShape(18.dp)) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Create playlist")
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
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
                            if (first != null) {
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
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(groups.entries.sortedBy { it.key.lowercase() }, key = { it.key }) { (name, songs) ->
            val first = songs.minWithOrNull(compareBy<SongRef>({ it.trackNumber.takeIf { number -> number > 0 } ?: Int.MAX_VALUE }, { it.title })) ?: return@items
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).clickable { viewModel.playCollection(songs, first) }.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LibraryArtwork(first, state, viewModel, Modifier.size(62.dp), RoundedCornerShape(18.dp), icon)
                Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                    Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                    Text("${songs.size} ${if (songs.size == 1) "song" else "songs"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Default.PlayArrow, null, tint = MaterialTheme.colorScheme.primary)
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
        Text("Choose a folder that contains audio files.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ExpressiveSongRow(song: SongRef, state: AppUiState, viewModel: MusicViewModel, onClick: () -> Unit) {
    var showMenu by remember(song.uri) { mutableStateOf(false) }
    val selected = song.uri == state.nowPlayingUri
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
        LibraryArtwork(song, state, viewModel, Modifier.size(52.dp), RoundedCornerShape(if (selected) 18.dp else 14.dp))
        Column(Modifier.weight(1f)) {
            Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
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
    onOpenRejected: () -> Unit,
) {
    LaunchedEffect(state.currentSwipeSong?.uri) {
        val current = state.currentSwipeSong
        if (current != null && current.uri != state.nowPlayingUri) viewModel.playReviewSong(current)
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
            Column(horizontalAlignment = Alignment.End) {
                IconButton(onClick = viewModel::undo, enabled = state.history.isNotEmpty(), modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.Undo, "Undo last decision")
                }
                TextButton(onClick = onOpenRejected, enabled = state.rejectedSongs.isNotEmpty()) {
                    Text("${state.rejectedSongs.size} rejected")
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
private fun ExpressiveRejected(state: AppUiState, viewModel: MusicViewModel, onBack: () -> Unit) {
    var confirmDelete by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to Review") }
                Text("Rejected songs", style = MaterialTheme.typography.headlineLarge)
            }
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

@Composable
private fun FullPlayerOverlay(
    state: AppUiState,
    viewModel: MusicViewModel,
    onDismiss: () -> Unit,
) {
    var showQueue by remember { mutableStateOf(false) }
    BackHandler {
        if (showQueue) showQueue = false else onDismiss()
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
                    .statusBarsPadding()
                    .navigationBarsPadding()
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
                            if (showQueue) "Up next" else "Now playing",
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
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            items(queue, key = SongRef::uri) { song ->
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
                                    IconButton(onClick = { viewModel.removeQueueItem(song.uri) }) {
                                        Icon(Icons.Default.Close, "Remove ${song.title} from queue")
                                    }
                                }
                            }
                        }
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
}

@Composable
private fun ExpressiveSettings(
    state: AppUiState,
    viewModel: MusicViewModel,
    onChooseFolder: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("Settings", style = MaterialTheme.typography.headlineLarge)
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
                        Text("Folder")
                    }
                }
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
            }
        }
        item {
            SettingsPanel("About") {
                Text("SwipeTune", style = MaterialTheme.typography.titleMedium)
                Text(
                    "A private, local-first music player with a swipe review mode.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text("Version ${BuildConfig.VERSION_NAME}", modifier = Modifier.padding(top = 8.dp), style = MaterialTheme.typography.labelLarge)
            }
        }
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
