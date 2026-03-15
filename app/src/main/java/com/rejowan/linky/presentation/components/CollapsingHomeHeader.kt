package com.rejowan.linky.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Calendar

/**
 * Collapsing home header with greeting and search bar
 * The search bar collapses when scrolling down
 *
 * @param scrollProgress 0f = fully expanded, 1f = fully collapsed
 * @param onSearchClick Callback when search bar is clicked
 * @param onAccountClick Callback when account icon is clicked
 * @param modifier Modifier for the component
 */
@Composable
fun CollapsingHomeHeader(
    scrollProgress: Float,
    onSearchClick: () -> Unit,
    onAccountClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animated values based on scroll progress
    val searchBarAlpha by animateFloatAsState(
        targetValue = 1f - (scrollProgress * 1.5f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 150),
        label = "searchBarAlpha"
    )

    val searchBarScale by animateFloatAsState(
        targetValue = 1f - (scrollProgress * 0.1f).coerceIn(0f, 0.1f),
        animationSpec = tween(durationMillis = 150),
        label = "searchBarScale"
    )

    val headerHeight by animateFloatAsState(
        targetValue = if (scrollProgress > 0.5f) 56f else 120f,
        animationSpec = tween(durationMillis = 200),
        label = "headerHeight"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp)
    ) {
        // Top row: Greeting and account icon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Greeting text
            Column {
                Text(
                    text = getGreeting(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Account icon
            IconButton(onClick = onAccountClick) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Account",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Collapsible search bar
        AnimatedVisibility(
            visible = scrollProgress < 0.7f,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
        ) {
            SearchBarPlaceholder(
                onClick = onSearchClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .graphicsLayer {
                        alpha = searchBarAlpha
                        scaleX = searchBarScale
                        scaleY = searchBarScale
                    }
            )
        }
    }
}

/**
 * Search bar placeholder that navigates to search screen when clicked
 */
@Composable
private fun SearchBarPlaceholder(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(28.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Search links...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Get greeting based on time of day
 */
private fun getGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Good Morning"
        hour < 17 -> "Good Afternoon"
        else -> "Good Evening"
    }
}

/**
 * Compact header shown when scrolled (just title)
 */
@Composable
fun CompactHeader(
    title: String,
    onAccountClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        IconButton(onClick = onAccountClick) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Account",
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
