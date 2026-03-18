package com.rejowan.linky.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Shimmer effect brush for loading animations
 */
@Composable
fun shimmerBrush(
    targetValue: Float = 1000f,
    showShimmer: Boolean = true
): Brush {
    return if (showShimmer) {
        val shimmerColors = listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )

        val transition = rememberInfiniteTransition(label = "shimmer")
        val translateAnimation by transition.animateFloat(
            initialValue = 0f,
            targetValue = targetValue,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 1000,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmer_translate"
        )

        Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(translateAnimation - 200f, translateAnimation - 200f),
            end = Offset(translateAnimation, translateAnimation)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color.Transparent, Color.Transparent)
        )
    }
}

/**
 * Shimmer placeholder box with rounded corners
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(4.dp)
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(shimmerBrush())
    )
}

/**
 * Shimmer version of LinkCard - matches the exact structure
 * Row layout: 72x72dp image, title/URL/date column, action icons column
 */
@Composable
fun ShimmerLinkCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Preview Image placeholder (72x72dp)
            ShimmerBox(
                modifier = Modifier.size(72.dp),
                shape = RoundedCornerShape(8.dp)
            )

            // Content (Title, URL, Timestamp)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Title placeholder (2 lines worth)
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(18.dp)
                )
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(18.dp)
                )

                // URL placeholder
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(14.dp)
                )

                // Timestamp placeholder
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.3f)
                        .height(12.dp)
                )
            }

            // Action Icons column
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Favorite icon placeholder
                ShimmerBox(
                    modifier = Modifier.size(24.dp),
                    shape = CircleShape
                )

                // More icon placeholder
                ShimmerBox(
                    modifier = Modifier.size(24.dp),
                    shape = CircleShape
                )
            }
        }
    }
}

/**
 * Shimmer version of LinkGridCard - matches the exact structure
 * Vertical card: 120dp image at top, title/URL/date below
 */
@Composable
fun ShimmerLinkGridCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column {
            // Preview Image placeholder (full width, 120dp height)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                ShimmerBox(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                )

                // Favorite icon overlay (top-left)
                ShimmerBox(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .size(28.dp),
                    shape = CircleShape
                )

                // More icon overlay (top-right)
                ShimmerBox(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(28.dp),
                    shape = CircleShape
                )
            }

            // Content
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Title placeholder (2 lines)
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(16.dp)
                )
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(16.dp)
                )

                Spacer(modifier = Modifier.height(2.dp))

                // URL placeholder
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(12.dp)
                )

                // Timestamp placeholder
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.3f)
                        .height(10.dp)
                )
            }
        }
    }
}

/**
 * Shimmer version of CollectionCard - matches the exact structure
 * Row layout: 64dp icon box, name/count/previews column, favorite icon
 */
@Composable
fun ShimmerCollectionCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Collection Icon placeholder (64dp)
            ShimmerBox(
                modifier = Modifier.size(64.dp),
                shape = RoundedCornerShape(12.dp)
            )

            // Collection Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Collection Name
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(18.dp)
                )

                // Link Count
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.3f)
                        .height(14.dp)
                )

                // Preview Thumbnails row
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(3) {
                        ShimmerBox(
                            modifier = Modifier.size(20.dp),
                            shape = CircleShape
                        )
                    }
                }
            }

            // Favorite Icon placeholder
            ShimmerBox(
                modifier = Modifier.size(24.dp),
                shape = CircleShape
            )
        }
    }
}

/**
 * Shimmer version of CollectionGridCard - matches the exact structure
 * Vertical card: 80dp color banner with icon, name/count below
 */
@Composable
fun ShimmerCollectionGridCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Color banner placeholder with icon
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            ) {
                ShimmerBox(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                )

                // Favorite button placeholder (top-right)
                ShimmerBox(
                    modifier = Modifier
                        .padding(8.dp)
                        .size(28.dp)
                        .align(Alignment.TopEnd),
                    shape = CircleShape
                )
            }

            // Content below banner
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Collection Name
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(16.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Link Count
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(12.dp)
                )
            }
        }
    }
}

/**
 * Shimmer loading list for links (list view)
 */
@Composable
fun ShimmerLinkList(
    itemCount: Int = 5,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Count header placeholder
        item {
            ShimmerBox(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .width(60.dp)
                    .height(14.dp)
            )
        }

        items(itemCount) {
            ShimmerLinkCard(
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

/**
 * Shimmer loading grid for links (grid view)
 */
@Composable
fun ShimmerLinkGrid(
    itemCount: Int = 6,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = 100.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(itemCount) {
            ShimmerLinkGridCard()
        }
    }
}

/**
 * Shimmer loading list for collections (list view)
 */
@Composable
fun ShimmerCollectionList(
    itemCount: Int = 4,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(itemCount) {
            ShimmerCollectionCard()
        }
    }
}

/**
 * Shimmer loading grid for collections (grid view)
 */
@Composable
fun ShimmerCollectionGrid(
    itemCount: Int = 4,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(itemCount) {
            ShimmerCollectionGridCard()
        }
    }
}
