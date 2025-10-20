package com.rejowan.linky.presentation.feature.collectiondetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rejowan.linky.domain.model.Link
import com.rejowan.linky.presentation.components.EmptyStates
import com.rejowan.linky.presentation.components.ErrorStates
import com.rejowan.linky.presentation.components.LinkCard
import com.rejowan.linky.presentation.components.LoadingIndicator
import org.koin.androidx.compose.koinViewModel

/**
 * Collection Detail Screen
 * Shows all links within a specific collection
 *
 * Features:
 * - Collection info header (name, color, favorite status)
 * - List of links in the collection using LinkCard
 * - Empty state when no links
 * - Error handling
 * - Back navigation
 *
 * @param onNavigateBack Callback to navigate back
 * @param onLinkClick Callback when a link is clicked
 * @param onFavoriteClick Callback when favorite icon is clicked on a link
 * @param modifier Modifier for styling
 * @param viewModel CollectionDetailViewModel injected via Koin
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionDetailScreen(
    onNavigateBack: () -> Unit,
    onLinkClick: (String) -> Unit,
    onFavoriteClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CollectionDetailViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show error in Snackbar
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(error)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Collection color indicator
                        state.collection?.color?.let { colorHex ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(colorHex)))
                            )
                        }

                        // Collection name
                        Text(
                            text = state.collection?.name ?: "Collection",
                            style = MaterialTheme.typography.titleLarge
                        )

                        // Favorite indicator
                        if (state.collection?.isFavorite == true) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Favorite collection",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                },
                actions = {
                    // Show link count
                    Text(
                        text = "${state.links.size} ${if (state.links.size == 1) "link" else "links"}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                // Loading state
                state.isLoading && state.links.isEmpty() -> {
                    LoadingIndicator(message = "Loading links...")
                }

                // Error state
                state.error != null && state.links.isEmpty() && state.collection == null -> {
                    ErrorStates.GenericError(
                        errorMessage = state.error ?: "Unknown error",
                        onRetryClick = { viewModel.onEvent(CollectionDetailEvent.OnRefresh) }
                    )
                }

                // Empty state
                state.links.isEmpty() && state.collection != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No links in this collection yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Links list
                else -> {
                    LinksList(
                        links = state.links,
                        onLinkClick = onLinkClick,
                        onFavoriteClick = onFavoriteClick
                    )
                }
            }
        }
    }
}

/**
 * Links list display
 */
@Composable
private fun LinksList(
    links: List<Link>,
    onLinkClick: (String) -> Unit,
    onFavoriteClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = links,
            key = { it.id }
        ) { link ->
            LinkCard(
                link = link,
                onClick = { onLinkClick(link.id) },
                onFavoriteClick = { onFavoriteClick(link.id) }
            )
        }
    }
}
