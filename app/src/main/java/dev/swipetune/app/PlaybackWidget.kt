package dev.swipetune.app

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

class PlaybackWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = PlaybackStore(context).load()
        val current = snapshot.items.getOrNull(snapshot.currentIndex)
        provideContent {
            WidgetContent(
                title = current?.mediaMetadata?.title?.toString() ?: "SwipeTune",
                artist = current?.mediaMetadata?.artist?.toString() ?: "Choose something to play",
            )
        }
    }
}

@Composable
private fun WidgetContent(title: String, artist: String) {
    Column(
        modifier = GlanceModifier.fillMaxSize().background(ColorProvider(Color(0xFF1E1B24))).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = TextStyle(color = ColorProvider(Color(0xFFF1ECF4)), fontWeight = FontWeight.Bold))
        Text(artist, style = TextStyle(color = ColorProvider(Color(0xFFCFC5D5))))
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(top = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            WidgetButton("‹‹", PlaybackService.ACTION_PREVIOUS)
            WidgetButton("Play / pause", PlaybackService.ACTION_PLAY_PAUSE)
            WidgetButton("››", PlaybackService.ACTION_NEXT)
        }
    }
}

@Composable
private fun WidgetButton(label: String, command: String) {
    val icon = when (command) {
        PlaybackService.ACTION_PREVIOUS -> android.R.drawable.ic_media_previous
        PlaybackService.ACTION_NEXT -> android.R.drawable.ic_media_next
        else -> android.R.drawable.ic_media_play
    }
    androidx.glance.appwidget.components.CircleIconButton(
        imageProvider = androidx.glance.ImageProvider(icon),
        contentDescription = label,
        onClick = actionRunCallback<PlaybackWidgetAction>(actionParametersOf(PlaybackWidgetAction.command to command)),
    )
}

class PlaybackWidgetAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val action = parameters[command] ?: return
        context.startService(Intent(context, PlaybackService::class.java).setAction(action))
    }

    companion object {
        val command = ActionParameters.Key<String>("command")
    }
}

class PlaybackWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PlaybackWidget()
}
