package com.rejowan.linky.presentation.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Navigation items for the animated bottom nav
 */
enum class NavItem(
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val label: String
) {
    HOME(Icons.Filled.Home, Icons.Outlined.Home, "Home"),
    COLLECTIONS(Icons.Filled.Folder, Icons.Outlined.Folder, "Collections"),
    SETTINGS(Icons.Filled.Settings, Icons.Outlined.Settings, "Settings")
}

/**
 * Animated bottom navigation bar with floating circle indicator
 *
 * @param selectedItem Currently selected nav item
 * @param onItemSelected Callback when an item is selected
 * @param modifier Modifier for the component
 */
@Composable
fun AnimatedBottomNav(
    selectedItem: NavItem,
    onItemSelected: (NavItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = NavItem.entries
    val selectedIndex = items.indexOf(selectedItem)

    // Calculate positions
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val density = LocalDensity.current
    val itemWidth = screenWidth / items.size
    val circleSize = 56.dp
    val navHeight = 72.dp
    val circleOffsetY = -16.dp // How much the circle floats above the bar

    // Animate circle position
    val circleOffsetX by animateDpAsState(
        targetValue = itemWidth * selectedIndex + (itemWidth - circleSize) / 2,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "circleOffset"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(navHeight)
    ) {
        // Bar with cutout
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(navHeight)
                .clip(
                    BarShape(
                        cutoutCenterX = with(density) { (circleOffsetX + circleSize / 2).toPx() },
                        cutoutRadius = with(density) { (circleSize / 2 + 8.dp).toPx() },
                        cutoutDepth = with(density) { 20.dp.toPx() }
                    )
                ),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    NavItemView(
                        item = item,
                        isSelected = index == selectedIndex,
                        onClick = { onItemSelected(item) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Floating circle with selected icon
        Box(
            modifier = Modifier
                .offset(x = circleOffsetX, y = circleOffsetY)
                .size(circleSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = selectedItem.selectedIcon,
                contentDescription = selectedItem.label,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

/**
 * Individual nav item view
 */
@Composable
private fun NavItemView(
    item: NavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isSelected) 0f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "alpha"
    )

    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 8.dp)
            .scale(scale)
            .alpha(alpha),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = item.unselectedIcon,
            contentDescription = item.label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Custom shape for the navigation bar with a curved cutout
 */
private fun BarShape(
    cutoutCenterX: Float,
    cutoutRadius: Float,
    cutoutDepth: Float
) = GenericShape { size, _ ->
    val width = size.width
    val height = size.height

    // Start from top-left
    moveTo(0f, cutoutDepth)

    // Line to start of cutout curve
    val cutoutStart = cutoutCenterX - cutoutRadius
    val cutoutEnd = cutoutCenterX + cutoutRadius

    lineTo(maxOf(0f, cutoutStart), cutoutDepth)

    // Cutout curve using cubic bezier
    if (cutoutStart >= 0 && cutoutEnd <= width) {
        // Control points for the curve
        val controlPoint1Y = cutoutDepth
        val controlPoint2Y = 0f
        val bottomY = 0f

        // Left side of curve
        cubicTo(
            cutoutStart + cutoutRadius * 0.3f, controlPoint1Y,
            cutoutStart + cutoutRadius * 0.5f, controlPoint2Y,
            cutoutCenterX, bottomY
        )

        // Right side of curve
        cubicTo(
            cutoutCenterX + cutoutRadius * 0.5f, controlPoint2Y,
            cutoutEnd - cutoutRadius * 0.3f, controlPoint1Y,
            cutoutEnd, cutoutDepth
        )
    }

    // Continue to top-right
    lineTo(width, cutoutDepth)

    // Down to bottom-right
    lineTo(width, height)

    // Along bottom to bottom-left
    lineTo(0f, height)

    // Close the path
    close()
}
