package com.rejowan.linky.presentation.feature.search

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TipsAndUpdates
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rejowan.linky.domain.model.CollectionWithLinkCount
import com.rejowan.linky.domain.model.Link
import com.rejowan.linky.presentation.components.LinkCard
import androidx.compose.material.icons.outlined.Folder
import androidx.core.graphics.toColorInt
import com.rejowan.linky.presentation.components.LoadingIndicator
import com.rejowan.linky.ui.theme.SoftAccents
import org.koin.androidx.compose.koinViewModel

// Recent searches preferences
private const val RECENT_SEARCHES_PREFS = "linky_recent_searches"
private const val RECENT_SEARCHES_KEY = "searches"
private const val MAX_RECENT_SEARCHES = 8
private const val COLLAPSED_RECENT_COUNT = 3

@Composable
fun SearchScreen(
    onLinkClick: (String) -> Unit,
    onCollectionClick: (String) -> Unit,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    val state by viewModel.state.collectAsStateWithLifecycle()

    // Recent searches state
    val recentSearches = remember { mutableStateListOf<String>() }

    // Handle UI events
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is SearchUiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Long
                    )
                }
                is SearchUiEvent.ShowFavoriteToggled -> {
                    val message = if (event.isFavorite) "Added to favorites" else "Removed from favorites"
                    val result = snackbarHostState.showSnackbar(
                        message = message,
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.onEvent(SearchEvent.OnToggleFavorite(event.linkId, !event.isFavorite, silent = true))
                    }
                }
            }
        }
    }

    // Load recent searches and request focus
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences(RECENT_SEARCHES_PREFS, Context.MODE_PRIVATE)
        val saved = prefs.getStringSet(RECENT_SEARCHES_KEY, emptySet()) ?: emptySet()
        recentSearches.clear()
        recentSearches.addAll(saved.take(MAX_RECENT_SEARCHES))
        focusRequester.requestFocus()
    }

    // Save search to recent when query has results
    LaunchedEffect(state.totalResults, state.searchQuery) {
        if (state.searchQuery.isNotBlank() && state.totalResults > 0) {
            val prefs = context.getSharedPreferences(RECENT_SEARCHES_PREFS, Context.MODE_PRIVATE)
            val updated = (listOf(state.searchQuery) + recentSearches.filter { it != state.searchQuery })
                .take(MAX_RECENT_SEARCHES)
            prefs.edit().putStringSet(RECENT_SEARCHES_KEY, updated.toSet()).apply()
            recentSearches.clear()
            recentSearches.addAll(updated)
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surface)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                }
        ) {
            // Search Header
            SearchHeader(
                query = state.searchQuery,
                onQueryChange = { viewModel.onEvent(SearchEvent.OnSearchQueryChange(it)) },
                onClear = { viewModel.onEvent(SearchEvent.OnClearSearch) },
                onBack = onBackClick,
                onSearch = {
                    viewModel.onEvent(SearchEvent.OnSearch)
                    focusManager.clearFocus()
                },
                focusRequester = focusRequester
            )

            // Content
            when {
                state.isSearching -> {
                    LoadingIndicator(message = "Searching...")
                }
                state.searchQuery.isBlank() -> {
                    SearchIdleContent(
                        recentSearches = recentSearches,
                        onRecentClick = { query ->
                            viewModel.onEvent(SearchEvent.OnSearchQueryChange(query))
                        },
                        onClearRecent = { query ->
                            recentSearches.remove(query)
                            val prefs = context.getSharedPreferences(RECENT_SEARCHES_PREFS, Context.MODE_PRIVATE)
                            prefs.edit().putStringSet(RECENT_SEARCHES_KEY, recentSearches.toSet()).apply()
                        },
                        onClearAllRecent = {
                            recentSearches.clear()
                            val prefs = context.getSharedPreferences(RECENT_SEARCHES_PREFS, Context.MODE_PRIVATE)
                            prefs.edit().remove(RECENT_SEARCHES_KEY).apply()
                        }
                    )
                }
                state.totalResults == 0 && state.hasSearched -> {
                    EmptySearchState(query = state.searchQuery)
                }
                else -> {
                    SearchResultsContent(
                        query = state.searchQuery,
                        linkResults = state.linkResults,
                        collectionResults = state.collectionResults,
                        onLinkClick = onLinkClick,
                        onCollectionClick = onCollectionClick,
                        onFavoriteClick = { linkId, isFavorite ->
                            viewModel.onEvent(SearchEvent.OnToggleFavorite(linkId, isFavorite))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Box(modifier = Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(
                                text = "Search links and collections...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = onQueryChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { onSearch() })
                        )
                    }

                    AnimatedVisibility(
                        visible = query.isNotEmpty(),
                        enter = fadeIn(tween(150)) + expandVertically(),
                        exit = fadeOut(tween(100)) + shrinkVertically()
                    ) {
                        Row {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .clickable(onClick = onClear),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Clear,
                                        contentDescription = "Clear",
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchIdleContent(
    recentSearches: List<String>,
    onRecentClick: (String) -> Unit,
    onClearRecent: (String) -> Unit,
    onClearAllRecent: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isRecentExpanded by remember { mutableStateOf(false) }
    val displayedSearches = if (isRecentExpanded) recentSearches else recentSearches.take(COLLAPSED_RECENT_COUNT)
    val hasMore = recentSearches.size > COLLAPSED_RECENT_COUNT

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        if (recentSearches.isNotEmpty()) {
            item(key = "recent_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = SoftAccents.Purple
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Recent Searches",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Clear all",
                        style = MaterialTheme.typography.labelMedium,
                        color = SoftAccents.Purple,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onClearAllRecent)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            itemsIndexed(
                items = displayedSearches,
                key = { _, query -> "recent_$query" }
            ) { index, query ->
                RecentSearchItem(
                    query = query,
                    onClick = { onRecentClick(query) },
                    onRemove = { onClearRecent(query) },
                    animationDelay = index * 30
                )
            }

            if (hasMore) {
                item(key = "recent_toggle") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { isRecentExpanded = !isRecentExpanded }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isRecentExpanded) "Show less" else "Show all (${recentSearches.size})",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = SoftAccents.Purple
                        )
                    }
                }
            }

            item(key = "spacer") { Spacer(modifier = Modifier.height(24.dp)) }
        }

        item(key = "tips") {
            SearchTipsSection()
        }
    }
}

@Composable
private fun RecentSearchItem(
    query: String,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    animationDelay: Int = 0,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(animationDelay == 0) }

    LaunchedEffect(Unit) {
        if (animationDelay > 0) {
            kotlinx.coroutines.delay(animationDelay.toLong())
            isVisible = true
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.95f,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "recent item scale"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Icon(
                imageVector = Icons.Outlined.AccessTime,
                contentDescription = null,
                modifier = Modifier
                    .padding(8.dp)
                    .size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = query,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Remove",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun SearchTipsSection(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.TipsAndUpdates,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = SoftAccents.Amber
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Search Tips",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        SearchTipCard(
            icon = Icons.Outlined.Link,
            title = "Search links",
            description = "Find links by title, URL, or description",
            accentColor = SoftAccents.Blue
        )

        Spacer(modifier = Modifier.height(10.dp))

        SearchTipCard(
            icon = Icons.Outlined.Folder,
            title = "Search collections",
            description = "Find collections by name",
            accentColor = SoftAccents.Purple
        )

        Spacer(modifier = Modifier.height(10.dp))

        SearchTipCard(
            icon = Icons.Outlined.Lightbulb,
            title = "Instant results",
            description = "Results appear as you type",
            accentColor = SoftAccents.Teal
        )
    }
}

@Composable
private fun SearchTipCard(
    icon: ImageVector,
    title: String,
    description: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = accentColor.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = accentColor.copy(alpha = 0.12f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(20.dp),
                    tint = accentColor
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun EmptySearchState(
    query: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 100.dp, start = 32.dp, end = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icon with subtle background
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                modifier = Modifier
                    .padding(20.dp)
                    .size(32.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "No results found",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "We couldn't find anything matching",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Query in a styled chip
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = SoftAccents.Blue.copy(alpha = 0.1f)
        ) {
            Text(
                text = "\"$query\"",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = SoftAccents.Blue,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Try a different search term",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun SearchResultsContent(
    query: String,
    linkResults: List<Link>,
    collectionResults: List<CollectionWithLinkCount>,
    onLinkClick: (String) -> Unit,
    onCollectionClick: (String) -> Unit,
    onFavoriteClick: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onPress = { focusManager.clearFocus() })
            },
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item(key = "header", contentType = "header") {
            SearchResultsHeader(
                query = query,
                linkCount = linkResults.size,
                collectionCount = collectionResults.size
            )
        }

        // Collections section
        if (collectionResults.isNotEmpty()) {
            item(key = "collections_header", contentType = "section_header") {
                SectionLabel(
                    title = "Collections",
                    count = collectionResults.size,
                    accentColor = SoftAccents.Purple
                )
            }

            itemsIndexed(
                items = collectionResults,
                key = { _, collection -> "collection_${collection.collection.id}" },
                contentType = { _, _ -> "collection" }
            ) { _, collectionWithCount ->
                CollectionSearchItem(
                    collection = collectionWithCount,
                    onClick = { onCollectionClick(collectionWithCount.collection.id) },
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .animateItem()
                )
            }
        }

        // Links section
        if (linkResults.isNotEmpty()) {
            item(key = "links_header", contentType = "section_header") {
                SectionLabel(
                    title = "Links",
                    count = linkResults.size,
                    accentColor = SoftAccents.Blue
                )
            }

            itemsIndexed(
                items = linkResults,
                key = { _, link -> "link_${link.id}" },
                contentType = { _, _ -> "link" }
            ) { _, link ->
                LinkCard(
                    link = link,
                    onClick = { onLinkClick(link.id) },
                    onFavoriteClick = { onFavoriteClick(link.id, !link.isFavorite) },
                    onMoreClick = { onLinkClick(link.id) },
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .animateItem()
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(
    title: String,
    count: Int,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = accentColor.copy(alpha = 0.12f)
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = accentColor,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun CollectionSearchItem(
    collection: CollectionWithLinkCount,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val collectionColor = collection.collection.color?.let {
        try { Color(it.toColorInt()) } catch (e: Exception) { null }
    } ?: MaterialTheme.colorScheme.primary

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = collectionColor.copy(alpha = 0.15f)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Folder,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(22.dp),
                    tint = collectionColor
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = collection.collection.name,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (collection.linkCount == 1) "1 link" else "${collection.linkCount} links",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun SearchResultsHeader(
    query: String,
    linkCount: Int,
    collectionCount: Int,
    modifier: Modifier = Modifier
) {
    val totalCount = linkCount + collectionCount

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Results for \"$query\"",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = buildString {
                    if (collectionCount > 0) {
                        append(if (collectionCount == 1) "1 collection" else "$collectionCount collections")
                    }
                    if (collectionCount > 0 && linkCount > 0) append(", ")
                    if (linkCount > 0) {
                        append(if (linkCount == 1) "1 link" else "$linkCount links")
                    }
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = SoftAccents.Blue.copy(alpha = 0.12f)
        ) {
            Text(
                text = totalCount.toString(),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = SoftAccents.Blue,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}
