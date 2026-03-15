package com.rejowan.linky.presentation.feature.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rejowan.linky.ui.theme.OnboardingColors
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.sin

data class ShapeConfig(
    val offsetX: Float,
    val offsetY: Float,
    val size: Float,
    val rotation: Float = 0f,
    val alpha: Float = 0.1f,
    val topStartCorner: Int = 50,
    val topEndCorner: Int = 50,
    val bottomStartCorner: Int = 50,
    val bottomEndCorner: Int = 50,
    val isCircle: Boolean = false
)

data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val accentColor: Color,
    val topStartCorner: Int,
    val topEndCorner: Int,
    val bottomStartCorner: Int,
    val bottomEndCorner: Int,
    val iconContainerScale: Float = 1f,
    val iconOffsetX: Float = 0f,
    val iconOffsetY: Float = 0f,
    val iconRotation: Float = 0f,
    val bgShape1: ShapeConfig,
    val bgShape2: ShapeConfig,
    val bgShape3: ShapeConfig,
    val bgShape4: ShapeConfig? = null,
    val bgShape5: ShapeConfig? = null
)

@Composable
private fun getOnboardingPages(): List<OnboardingPage> = listOf(
    // Page 1: Save Links - Diamond/Angular theme with diagonal shapes
    OnboardingPage(
        icon = Icons.Rounded.Bookmark,
        title = "Save Any Link",
        description = "Bookmark links from any app with a single tap. Share from browsers, social media, or any other app.",
        accentColor = OnboardingColors.Page1,
        topStartCorner = 30,
        topEndCorner = 45,
        bottomStartCorner = 45,
        bottomEndCorner = 30,
        iconContainerScale = 1.02f,
        iconRotation = -5f,
        bgShape1 = ShapeConfig(
            offsetX = -40f, offsetY = 100f, size = 140f,
            rotation = 45f, alpha = 0.06f,
            topStartCorner = 10, topEndCorner = 40,
            bottomStartCorner = 40, bottomEndCorner = 10
        ),
        bgShape2 = ShapeConfig(
            offsetX = 260f, offsetY = 380f, size = 180f,
            rotation = -30f, alpha = 0.045f,
            topStartCorner = 45, topEndCorner = 15,
            bottomStartCorner = 15, bottomEndCorner = 45
        ),
        bgShape3 = ShapeConfig(
            offsetX = 300f, offsetY = 80f, size = 70f,
            rotation = 60f, alpha = 0.04f,
            topStartCorner = 20, topEndCorner = 50,
            bottomStartCorner = 20, bottomEndCorner = 50
        ),
        bgShape4 = ShapeConfig(
            offsetX = -20f, offsetY = 550f, size = 95f,
            rotation = -45f, alpha = 0.035f,
            topStartCorner = 35, topEndCorner = 10,
            bottomStartCorner = 35, bottomEndCorner = 10
        ),
        bgShape5 = ShapeConfig(
            offsetX = 180f, offsetY = 200f, size = 55f,
            rotation = 30f, alpha = 0.025f,
            isCircle = true
        )
    ),
    // Page 2: Snapshots - Circular/Organic with soft rounded shapes
    OnboardingPage(
        icon = Icons.Rounded.CameraAlt,
        title = "Capture Snapshots",
        description = "Save offline copies of web pages. Never lose access to important content even when the original is gone.",
        accentColor = OnboardingColors.Page2,
        topStartCorner = 50,
        topEndCorner = 50,
        bottomStartCorner = 50,
        bottomEndCorner = 50,
        iconContainerScale = 0.98f,
        iconOffsetY = -5f,
        iconRotation = 3f,
        bgShape1 = ShapeConfig(
            offsetX = 280f, offsetY = 120f, size = 200f,
            rotation = 15f, alpha = 0.055f,
            isCircle = true
        ),
        bgShape2 = ShapeConfig(
            offsetX = -80f, offsetY = 320f, size = 170f,
            rotation = -20f, alpha = 0.05f,
            topStartCorner = 50, topEndCorner = 45,
            bottomStartCorner = 45, bottomEndCorner = 50
        ),
        bgShape3 = ShapeConfig(
            offsetX = 200f, offsetY = 500f, size = 110f,
            rotation = 0f, alpha = 0.04f,
            isCircle = true
        ),
        bgShape4 = ShapeConfig(
            offsetX = -30f, offsetY = 60f, size = 80f,
            rotation = 25f, alpha = 0.03f,
            topStartCorner = 45, topEndCorner = 50,
            bottomStartCorner = 50, bottomEndCorner = 45
        ),
        bgShape5 = ShapeConfig(
            offsetX = 320f, offsetY = 420f, size = 50f,
            rotation = 0f, alpha = 0.025f,
            isCircle = true
        )
    ),
    // Page 3: Secure Vault - Asymmetric shield-like shapes
    OnboardingPage(
        icon = Icons.Rounded.Shield,
        title = "Secure Vault",
        description = "Keep sensitive links private with PIN-protected vault. Your private bookmarks, always secure.",
        accentColor = OnboardingColors.Page3,
        topStartCorner = 40,
        topEndCorner = 40,
        bottomStartCorner = 25,
        bottomEndCorner = 25,
        iconContainerScale = 1.0f,
        iconOffsetX = 0f,
        iconRotation = 0f,
        bgShape1 = ShapeConfig(
            offsetX = -60f, offsetY = 400f, size = 220f,
            rotation = 35f, alpha = 0.055f,
            topStartCorner = 50, topEndCorner = 30,
            bottomStartCorner = 15, bottomEndCorner = 15
        ),
        bgShape2 = ShapeConfig(
            offsetX = 270f, offsetY = 60f, size = 150f,
            rotation = -25f, alpha = 0.05f,
            topStartCorner = 40, topEndCorner = 40,
            bottomStartCorner = 20, bottomEndCorner = 20
        ),
        bgShape3 = ShapeConfig(
            offsetX = 310f, offsetY = 480f, size = 120f,
            rotation = 50f, alpha = 0.04f,
            topStartCorner = 35, topEndCorner = 35,
            bottomStartCorner = 10, bottomEndCorner = 10
        ),
        bgShape4 = ShapeConfig(
            offsetX = -40f, offsetY = 180f, size = 65f,
            rotation = -40f, alpha = 0.035f,
            topStartCorner = 45, topEndCorner = 45,
            bottomStartCorner = 25, bottomEndCorner = 25
        ),
        bgShape5 = ShapeConfig(
            offsetX = 150f, offsetY = 280f, size = 45f,
            rotation = 20f, alpha = 0.02f,
            topStartCorner = 50, topEndCorner = 30,
            bottomStartCorner = 20, bottomEndCorner = 20
        )
    )
)

/**
 * Onboarding screen that introduces users to the app's features
 * Features unique morphing animations with varied patterns
 */
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val onboardingPages = getOnboardingPages()

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { onboardingPages.size }
    )

    val currentPage by remember { derivedStateOf { pagerState.currentPage } }
    val isLastPage = currentPage == onboardingPages.size - 1

    val currentAccentColor by animateColorAsState(
        targetValue = onboardingPages[currentPage].accentColor,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "accent"
    )

    // Icon container corner animations
    val topStartCorner by animateFloatAsState(
        targetValue = onboardingPages[currentPage].topStartCorner.toFloat(),
        animationSpec = tween(500, easing = EaseInOutCubic),
        label = "topStart"
    )
    val topEndCorner by animateFloatAsState(
        targetValue = onboardingPages[currentPage].topEndCorner.toFloat(),
        animationSpec = tween(500, easing = EaseInOutCubic),
        label = "topEnd"
    )
    val bottomStartCorner by animateFloatAsState(
        targetValue = onboardingPages[currentPage].bottomStartCorner.toFloat(),
        animationSpec = tween(500, easing = EaseInOutCubic),
        label = "bottomStart"
    )
    val bottomEndCorner by animateFloatAsState(
        targetValue = onboardingPages[currentPage].bottomEndCorner.toFloat(),
        animationSpec = tween(500, easing = EaseInOutCubic),
        label = "bottomEnd"
    )

    val iconContainerScale by animateFloatAsState(
        targetValue = onboardingPages[currentPage].iconContainerScale,
        animationSpec = tween(450),
        label = "iconScale"
    )
    val iconOffsetX by animateFloatAsState(
        targetValue = onboardingPages[currentPage].iconOffsetX,
        animationSpec = tween(450),
        label = "iconOffsetX"
    )
    val iconOffsetY by animateFloatAsState(
        targetValue = onboardingPages[currentPage].iconOffsetY,
        animationSpec = tween(450),
        label = "iconOffsetY"
    )
    val iconRotation by animateFloatAsState(
        targetValue = onboardingPages[currentPage].iconRotation,
        animationSpec = tween(600),
        label = "iconRotation"
    )

    // Background shape animations - Shape 1
    val bg1X by animateFloatAsState(targetValue = onboardingPages[currentPage].bgShape1.offsetX, animationSpec = tween(650), label = "bg1X")
    val bg1Y by animateFloatAsState(targetValue = onboardingPages[currentPage].bgShape1.offsetY, animationSpec = tween(700), label = "bg1Y")
    val bg1Size by animateFloatAsState(targetValue = onboardingPages[currentPage].bgShape1.size, animationSpec = tween(550), label = "bg1Size")
    val bg1Rotation by animateFloatAsState(targetValue = onboardingPages[currentPage].bgShape1.rotation, animationSpec = tween(750), label = "bg1Rot")
    val bg1Alpha by animateFloatAsState(targetValue = onboardingPages[currentPage].bgShape1.alpha, animationSpec = tween(450), label = "bg1Alpha")
    val bg1TS by animateFloatAsState(targetValue = onboardingPages[currentPage].bgShape1.topStartCorner.toFloat(), animationSpec = tween(600), label = "bg1TS")
    val bg1TE by animateFloatAsState(targetValue = onboardingPages[currentPage].bgShape1.topEndCorner.toFloat(), animationSpec = tween(600), label = "bg1TE")
    val bg1BS by animateFloatAsState(targetValue = onboardingPages[currentPage].bgShape1.bottomStartCorner.toFloat(), animationSpec = tween(600), label = "bg1BS")
    val bg1BE by animateFloatAsState(targetValue = onboardingPages[currentPage].bgShape1.bottomEndCorner.toFloat(), animationSpec = tween(600), label = "bg1BE")
    val bg1Circle = onboardingPages[currentPage].bgShape1.isCircle

    // Shape 2
    val bg2X by animateFloatAsState(targetValue = onboardingPages[currentPage].bgShape2.offsetX, animationSpec = tween(750), label = "bg2X")
    val bg2Y by animateFloatAsState(targetValue = onboardingPages[currentPage].bgShape2.offsetY, animationSpec = tween(800), label = "bg2Y")
    val bg2Size by animateFloatAsState(targetValue = onboardingPages[currentPage].bgShape2.size, animationSpec = tween(650), label = "bg2Size")
    val bg2Rotation by animateFloatAsState(targetValue = onboardingPages[currentPage].bgShape2.rotation, animationSpec = tween(850), label = "bg2Rot")
    val bg2Alpha by animateFloatAsState(targetValue = onboardingPages[currentPage].bgShape2.alpha, animationSpec = tween(500), label = "bg2Alpha")
    val bg2TS by animateFloatAsState(targetValue = onboardingPages[currentPage].bgShape2.topStartCorner.toFloat(), animationSpec = tween(650), label = "bg2TS")
    val bg2TE by animateFloatAsState(targetValue = onboardingPages[currentPage].bgShape2.topEndCorner.toFloat(), animationSpec = tween(650), label = "bg2TE")
    val bg2BS by animateFloatAsState(targetValue = onboardingPages[currentPage].bgShape2.bottomStartCorner.toFloat(), animationSpec = tween(650), label = "bg2BS")
    val bg2BE by animateFloatAsState(targetValue = onboardingPages[currentPage].bgShape2.bottomEndCorner.toFloat(), animationSpec = tween(650), label = "bg2BE")
    val bg2Circle = onboardingPages[currentPage].bgShape2.isCircle

    // Shape 3
    val bg3X by animateFloatAsState(targetValue = onboardingPages[currentPage].bgShape3.offsetX, animationSpec = tween(600), label = "bg3X")
    val bg3Y by animateFloatAsState(targetValue = onboardingPages[currentPage].bgShape3.offsetY, animationSpec = tween(650), label = "bg3Y")
    val bg3Size by animateFloatAsState(targetValue = onboardingPages[currentPage].bgShape3.size, animationSpec = tween(500), label = "bg3Size")
    val bg3Rotation by animateFloatAsState(targetValue = onboardingPages[currentPage].bgShape3.rotation, animationSpec = tween(700), label = "bg3Rot")
    val bg3Alpha by animateFloatAsState(targetValue = onboardingPages[currentPage].bgShape3.alpha, animationSpec = tween(400), label = "bg3Alpha")
    val bg3TS by animateFloatAsState(targetValue = onboardingPages[currentPage].bgShape3.topStartCorner.toFloat(), animationSpec = tween(550), label = "bg3TS")
    val bg3TE by animateFloatAsState(targetValue = onboardingPages[currentPage].bgShape3.topEndCorner.toFloat(), animationSpec = tween(550), label = "bg3TE")
    val bg3BS by animateFloatAsState(targetValue = onboardingPages[currentPage].bgShape3.bottomStartCorner.toFloat(), animationSpec = tween(550), label = "bg3BS")
    val bg3BE by animateFloatAsState(targetValue = onboardingPages[currentPage].bgShape3.bottomEndCorner.toFloat(), animationSpec = tween(550), label = "bg3BE")
    val bg3Circle = onboardingPages[currentPage].bgShape3.isCircle

    // Shape 4
    val bg4Config = onboardingPages[currentPage].bgShape4
    val bg4X by animateFloatAsState(targetValue = bg4Config?.offsetX ?: -200f, animationSpec = tween(680), label = "bg4X")
    val bg4Y by animateFloatAsState(targetValue = bg4Config?.offsetY ?: 300f, animationSpec = tween(720), label = "bg4Y")
    val bg4Size by animateFloatAsState(targetValue = bg4Config?.size ?: 0f, animationSpec = tween(580), label = "bg4Size")
    val bg4Rotation by animateFloatAsState(targetValue = bg4Config?.rotation ?: 0f, animationSpec = tween(800), label = "bg4Rot")
    val bg4Alpha by animateFloatAsState(targetValue = bg4Config?.alpha ?: 0f, animationSpec = tween(450), label = "bg4Alpha")
    val bg4TS by animateFloatAsState(targetValue = (bg4Config?.topStartCorner ?: 50).toFloat(), animationSpec = tween(620), label = "bg4TS")
    val bg4TE by animateFloatAsState(targetValue = (bg4Config?.topEndCorner ?: 50).toFloat(), animationSpec = tween(620), label = "bg4TE")
    val bg4BS by animateFloatAsState(targetValue = (bg4Config?.bottomStartCorner ?: 50).toFloat(), animationSpec = tween(620), label = "bg4BS")
    val bg4BE by animateFloatAsState(targetValue = (bg4Config?.bottomEndCorner ?: 50).toFloat(), animationSpec = tween(620), label = "bg4BE")
    val bg4Circle = bg4Config?.isCircle ?: false

    // Shape 5
    val bg5Config = onboardingPages[currentPage].bgShape5
    val bg5X by animateFloatAsState(targetValue = bg5Config?.offsetX ?: -200f, animationSpec = tween(720), label = "bg5X")
    val bg5Y by animateFloatAsState(targetValue = bg5Config?.offsetY ?: 300f, animationSpec = tween(760), label = "bg5Y")
    val bg5Size by animateFloatAsState(targetValue = bg5Config?.size ?: 0f, animationSpec = tween(620), label = "bg5Size")
    val bg5Alpha by animateFloatAsState(targetValue = bg5Config?.alpha ?: 0f, animationSpec = tween(480), label = "bg5Alpha")
    val bg5Circle = bg5Config?.isCircle ?: false

    val infiniteTransition = rememberInfiniteTransition(label = "ambient")

    // Unique pulse - faster, more subtle
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Outer glow - slower, wider range
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    // Slow rotation for background shapes
    val bgRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(25000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bgRotation"
    )

    // Diagonal wave movement
    val waveX by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waveX"
    )

    val waveY by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waveY"
    )

    // Breathing scale effect
    val breathe by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    // Secondary wave for variety
    val wave2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f, // 2*PI for sine wave
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave2"
    )

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background morphing shapes
            BackgroundMorphingShapes(
                accentColor = currentAccentColor,
                rotation = bgRotation,
                waveX = waveX,
                waveY = waveY,
                breathe = breathe,
                wave2 = wave2,
                shape1X = bg1X, shape1Y = bg1Y, shape1Size = bg1Size,
                shape1Rotation = bg1Rotation, shape1Alpha = bg1Alpha,
                shape1TS = bg1TS, shape1TE = bg1TE, shape1BS = bg1BS, shape1BE = bg1BE,
                shape1Circle = bg1Circle,
                shape2X = bg2X, shape2Y = bg2Y, shape2Size = bg2Size,
                shape2Rotation = bg2Rotation, shape2Alpha = bg2Alpha,
                shape2TS = bg2TS, shape2TE = bg2TE, shape2BS = bg2BS, shape2BE = bg2BE,
                shape2Circle = bg2Circle,
                shape3X = bg3X, shape3Y = bg3Y, shape3Size = bg3Size,
                shape3Rotation = bg3Rotation, shape3Alpha = bg3Alpha,
                shape3TS = bg3TS, shape3TE = bg3TE, shape3BS = bg3BS, shape3BE = bg3BE,
                shape3Circle = bg3Circle,
                shape4X = bg4X, shape4Y = bg4Y, shape4Size = bg4Size,
                shape4Rotation = bg4Rotation, shape4Alpha = bg4Alpha,
                shape4TS = bg4TS, shape4TE = bg4TE, shape4BS = bg4BS, shape4BE = bg4BE,
                shape4Circle = bg4Circle,
                shape5X = bg5X, shape5Y = bg5Y, shape5Size = bg5Size,
                shape5Alpha = bg5Alpha, shape5Circle = bg5Circle
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Skip button row
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    if (!isLastPage) {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(onboardingPages.size - 1)
                                }
                            },
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Text(
                                "Skip",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Pager
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { page ->
                    val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction

                    OnboardingPageContent(
                        page = onboardingPages[page],
                        pageOffset = pageOffset,
                        topStartCorner = topStartCorner,
                        topEndCorner = topEndCorner,
                        bottomStartCorner = bottomStartCorner,
                        bottomEndCorner = bottomEndCorner,
                        iconContainerScale = iconContainerScale,
                        iconOffsetX = iconOffsetX,
                        iconOffsetY = iconOffsetY,
                        iconRotation = iconRotation,
                        pulseScale = pulseScale,
                        pulseAlpha = pulseAlpha,
                        glowScale = glowScale,
                        glowAlpha = glowAlpha,
                        accentColor = currentAccentColor
                    )
                }

                // Bottom section with indicators and button
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Page indicators
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(bottom = 32.dp)
                    ) {
                        repeat(onboardingPages.size) { index ->
                            PageIndicator(
                                isSelected = index == currentPage,
                                accentColor = currentAccentColor,
                                breathe = if (index == currentPage) breathe else 1f
                            )
                        }
                    }

                    // Action button
                    Button(
                        onClick = {
                            if (isLastPage) {
                                onComplete()
                            } else {
                                scope.launch {
                                    pagerState.animateScrollToPage(currentPage + 1)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(
                            topStart = topStartCorner.toInt().coerceIn(12, 24).dp,
                            topEnd = topEndCorner.toInt().coerceIn(12, 24).dp,
                            bottomStart = bottomStartCorner.toInt().coerceIn(12, 24).dp,
                            bottomEnd = bottomEndCorner.toInt().coerceIn(12, 24).dp
                        ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = currentAccentColor
                        )
                    ) {
                        Text(
                            text = if (isLastPage) "Get Started" else "Continue",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (!isLastPage) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BackgroundMorphingShapes(
    accentColor: Color,
    rotation: Float,
    waveX: Float,
    waveY: Float,
    breathe: Float,
    wave2: Float,
    shape1X: Float, shape1Y: Float, shape1Size: Float,
    shape1Rotation: Float, shape1Alpha: Float,
    shape1TS: Float, shape1TE: Float, shape1BS: Float, shape1BE: Float,
    shape1Circle: Boolean,
    shape2X: Float, shape2Y: Float, shape2Size: Float,
    shape2Rotation: Float, shape2Alpha: Float,
    shape2TS: Float, shape2TE: Float, shape2BS: Float, shape2BE: Float,
    shape2Circle: Boolean,
    shape3X: Float, shape3Y: Float, shape3Size: Float,
    shape3Rotation: Float, shape3Alpha: Float,
    shape3TS: Float, shape3TE: Float, shape3BS: Float, shape3BE: Float,
    shape3Circle: Boolean,
    shape4X: Float, shape4Y: Float, shape4Size: Float,
    shape4Rotation: Float, shape4Alpha: Float,
    shape4TS: Float, shape4TE: Float, shape4BS: Float, shape4BE: Float,
    shape4Circle: Boolean,
    shape5X: Float, shape5Y: Float, shape5Size: Float,
    shape5Alpha: Float, shape5Circle: Boolean
) {
    val sineOffset = sin(wave2.toDouble()).toFloat() * 8f

    // Shape 1 - Diagonal drift
    Box(
        modifier = Modifier
            .size(shape1Size.dp)
            .offset(
                x = (shape1X + waveX * 0.7f).dp,
                y = (shape1Y + waveY * 0.5f + sineOffset).dp
            )
            .scale(breathe)
            .rotate(shape1Rotation + rotation * 0.018f)
            .clip(
                if (shape1Circle) CircleShape
                else RoundedCornerShape(
                    topStartPercent = shape1TS.toInt(),
                    topEndPercent = shape1TE.toInt(),
                    bottomStartPercent = shape1BS.toInt(),
                    bottomEndPercent = shape1BE.toInt()
                )
            )
            .background(accentColor.copy(alpha = shape1Alpha))
    )

    // Shape 2 - Counter movement
    Box(
        modifier = Modifier
            .size(shape2Size.dp)
            .offset(
                x = (shape2X - waveX * 0.5f).dp,
                y = (shape2Y + waveY * 0.8f).dp
            )
            .scale(1.02f + (breathe - 1f) * 0.6f)
            .rotate(shape2Rotation - rotation * 0.012f)
            .clip(
                if (shape2Circle) CircleShape
                else RoundedCornerShape(
                    topStartPercent = shape2TS.toInt(),
                    topEndPercent = shape2TE.toInt(),
                    bottomStartPercent = shape2BS.toInt(),
                    bottomEndPercent = shape2BE.toInt()
                )
            )
            .background(accentColor.copy(alpha = shape2Alpha))
    )

    // Shape 3 - Vertical bob
    Box(
        modifier = Modifier
            .size(shape3Size.dp)
            .offset(
                x = (shape3X + sineOffset * 0.5f).dp,
                y = (shape3Y - waveY * 1.2f).dp
            )
            .scale(breathe * 0.92f)
            .rotate(shape3Rotation + rotation * 0.022f)
            .clip(
                if (shape3Circle) CircleShape
                else RoundedCornerShape(
                    topStartPercent = shape3TS.toInt(),
                    topEndPercent = shape3TE.toInt(),
                    bottomStartPercent = shape3BS.toInt(),
                    bottomEndPercent = shape3BE.toInt()
                )
            )
            .background(accentColor.copy(alpha = shape3Alpha))
    )

    // Shape 4 - Horizontal slide
    if (shape4Alpha > 0.01f) {
        Box(
            modifier = Modifier
                .size(shape4Size.dp)
                .offset(
                    x = (shape4X + waveX * 1.1f).dp,
                    y = (shape4Y - sineOffset * 0.4f).dp
                )
                .scale(1f + (breathe - 1f) * 0.8f)
                .rotate(shape4Rotation - rotation * 0.028f)
                .clip(
                    if (shape4Circle) CircleShape
                    else RoundedCornerShape(
                        topStartPercent = shape4TS.toInt(),
                        topEndPercent = shape4TE.toInt(),
                        bottomStartPercent = shape4BS.toInt(),
                        bottomEndPercent = shape4BE.toInt()
                    )
                )
                .background(accentColor.copy(alpha = shape4Alpha))
        )
    }

    // Shape 5 - Small accent (circles only, pulsing)
    if (shape5Alpha > 0.01f) {
        Box(
            modifier = Modifier
                .size(shape5Size.dp)
                .offset(
                    x = (shape5X - waveX * 0.3f + sineOffset).dp,
                    y = (shape5Y + waveY * 0.6f).dp
                )
                .scale(breathe * 1.1f)
                .alpha(shape5Alpha * (0.8f + sin(wave2.toDouble() * 2).toFloat() * 0.2f))
                .clip(if (shape5Circle) CircleShape else RoundedCornerShape(50))
                .background(accentColor.copy(alpha = shape5Alpha))
        )
    }
}

@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    pageOffset: Float,
    topStartCorner: Float,
    topEndCorner: Float,
    bottomStartCorner: Float,
    bottomEndCorner: Float,
    iconContainerScale: Float,
    iconOffsetX: Float,
    iconOffsetY: Float,
    iconRotation: Float,
    pulseScale: Float,
    pulseAlpha: Float,
    glowScale: Float,
    glowAlpha: Float,
    accentColor: Color
) {
    val contentAlpha by animateFloatAsState(
        targetValue = 1f - (pageOffset.absoluteValue * 0.6f).coerceIn(0f, 0.6f),
        animationSpec = spring(),
        label = "alpha"
    )

    val contentScale by animateFloatAsState(
        targetValue = 1f - (pageOffset.absoluteValue * 0.08f).coerceIn(0f, 0.08f),
        animationSpec = spring(),
        label = "scale"
    )

    val contentTranslateX by animateFloatAsState(
        targetValue = pageOffset * 30f,
        animationSpec = spring(),
        label = "translateX"
    )

    val iconShape = RoundedCornerShape(
        topStartPercent = topStartCorner.toInt(),
        topEndPercent = topEndCorner.toInt(),
        bottomStartPercent = bottomStartCorner.toInt(),
        bottomEndPercent = bottomEndCorner.toInt()
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .graphicsLayer {
                alpha = contentAlpha
                scaleX = contentScale
                scaleY = contentScale
                translationX = contentTranslateX
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon container with pulse effects
        Box(
            modifier = Modifier
                .size(240.dp)
                .offset(x = iconOffsetX.dp, y = iconOffsetY.dp)
                .scale(iconContainerScale)
                .rotate(iconRotation),
            contentAlignment = Alignment.Center
        ) {
            // Outer glow
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .scale(glowScale)
                    .clip(iconShape)
                    .background(accentColor.copy(alpha = glowAlpha))
            )

            // Inner pulse
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .scale(pulseScale)
                    .clip(iconShape)
                    .background(accentColor.copy(alpha = pulseAlpha))
            )

            // Icon background
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(iconShape)
                    .background(accentColor.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = page.icon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(52.dp)
                        .scale(1f + (pulseScale - 1f) * 0.2f),
                    tint = accentColor
                )
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        // Title
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                lineHeight = 38.sp
            ),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Description
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge.copy(
                lineHeight = 24.sp
            ),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}

@Composable
private fun PageIndicator(
    isSelected: Boolean,
    accentColor: Color,
    breathe: Float
) {
    val width by animateDpAsState(
        targetValue = if (isSelected) 24.dp else 8.dp,
        animationSpec = spring(
            dampingRatio = 0.65f,
            stiffness = 350f
        ),
        label = "width"
    )

    val height by animateDpAsState(
        targetValue = if (isSelected) 8.dp else 8.dp,
        animationSpec = spring(),
        label = "height"
    )

    val color by animateColorAsState(
        targetValue = if (isSelected) accentColor else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(250),
        label = "color"
    )

    Box(
        modifier = Modifier
            .padding(horizontal = 5.dp)
            .height(height)
            .width(width)
            .scale(if (isSelected) 1f + (breathe - 1f) * 0.12f else 1f)
            .clip(RoundedCornerShape(4.dp))
            .background(color)
    )
}
