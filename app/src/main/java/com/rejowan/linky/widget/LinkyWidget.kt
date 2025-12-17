package com.rejowan.linky.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.rejowan.linky.MainActivity
import com.rejowan.linky.R
import com.rejowan.linky.data.local.database.LinkyDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Linky Home Screen Widget
 * Shows recent links with quick access to open them
 */
class LinkyWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Fetch recent links from database
        val recentLinks = withContext(Dispatchers.IO) {
            try {
                val database = LinkyDatabase.getDatabase(context)
                database.linkDao().getRecentLinksForWidget(limit = 5)
            } catch (e: Exception) {
                emptyList()
            }
        }

        provideContent {
            GlanceTheme {
                LinkyWidgetContent(
                    links = recentLinks.map { entity ->
                        WidgetLink(
                            id = entity.id,
                            title = entity.title,
                            url = entity.url,
                            previewUrl = entity.previewUrl
                        )
                    }
                )
            }
        }
    }
}

/**
 * Simple data class for widget links
 */
data class WidgetLink(
    val id: String,
    val title: String,
    val url: String,
    val previewUrl: String?
)

@Composable
private fun LinkyWidgetContent(links: List<WidgetLink>) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .padding(12.dp)
    ) {
        // Header
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_launcher_foreground),
                contentDescription = "Linky",
                modifier = GlanceModifier.size(24.dp)
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            Text(
                text = "Linky",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            // Add button
            Box(
                modifier = GlanceModifier
                    .size(32.dp)
                    .clickable(actionStartActivity<MainActivity>(
                        actionParametersOf(
                            ActionParameters.Key<Boolean>("add_link") to true
                        )
                    )),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    style = TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        if (links.isEmpty()) {
            // Empty state
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .clickable(actionStartActivity<MainActivity>()),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No links yet\nTap to add one",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                )
            }
        } else {
            // Links list
            LazyColumn(
                modifier = GlanceModifier.fillMaxSize()
            ) {
                items(links) { link ->
                    LinkItem(link = link)
                }
            }
        }
    }
}

@Composable
private fun LinkItem(link: WidgetLink) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable(
                actionRunCallback<OpenLinkAction>(
                    actionParametersOf(
                        OpenLinkAction.urlKey to link.url
                    )
                )
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Link icon placeholder
        Box(
            modifier = GlanceModifier
                .size(36.dp)
                .background(GlanceTheme.colors.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = link.title.firstOrNull()?.uppercase() ?: "L",
                style = TextStyle(
                    color = GlanceTheme.colors.onPrimaryContainer,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }

        Spacer(modifier = GlanceModifier.width(10.dp))

        Column(
            modifier = GlanceModifier.defaultWeight()
        ) {
            Text(
                text = link.title,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1
            )
            Text(
                text = extractDomain(link.url),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 12.sp
                ),
                maxLines = 1
            )
        }
    }
}

/**
 * Extract domain from URL for display
 */
private fun extractDomain(url: String): String {
    return try {
        val uri = Uri.parse(url)
        uri.host?.removePrefix("www.") ?: url
    } catch (e: Exception) {
        url
    }
}

/**
 * Action to open a link in browser
 */
class OpenLinkAction : ActionCallback {
    companion object {
        val urlKey = ActionParameters.Key<String>("url")
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val url = parameters[urlKey] ?: return
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}

/**
 * Widget receiver to handle widget events
 */
class LinkyWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LinkyWidget()
}
