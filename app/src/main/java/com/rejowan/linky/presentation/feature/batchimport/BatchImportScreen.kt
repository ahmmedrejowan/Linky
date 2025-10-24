package com.rejowan.linky.presentation.feature.batchimport

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rejowan.linky.data.local.preferences.ThemePreferences
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Batch Import Screen - Step 1: Paste Text
 *
 * Features:
 * - Large multiline text field for pasting URLs
 * - Character and line count indicators
 * - Import/Check button (enabled when text is not empty)
 * - Cancel button with confirmation dialog
 * - Auto-expanding text field
 * - Dismissable "How it works" card with preference persistence
 * - Info icon in top bar to show "How it works" dialog
 *
 * @param onNavigateBack Navigate back to Settings
 * @param onStartScan Start scanning for URLs (proceeds to next step)
 * @param modifier Modifier for styling
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchImportScreen(
    onNavigateBack: () -> Unit,
    onStartScan: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val themePreferences = remember { ThemePreferences(context) }
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    var pastedText by remember { mutableStateOf("") }
    var showConfirmationDialog by remember { mutableStateOf(false) }
    var showClearConfirmationDialog by remember { mutableStateOf(false) }
    var showHowItWorksDialog by remember { mutableStateOf(false) }

    // Observe the preference for showing "How it works" card
    val shouldShowHowItWorks by themePreferences.shouldShowBatchImportHowItWorks()
        .collectAsState(initial = true)

    // Calculate statistics
    val characterCount = pastedText.length
    val lineCount = if (pastedText.isEmpty()) 0 else pastedText.lines().size

    // Handle back press with confirmation
    val handleBackPress = {
        if (pastedText.isNotEmpty()) {
            showConfirmationDialog = true
        } else {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Batch Import Links") },
                navigationIcon = {
                    IconButton(onClick = handleBackPress) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showHowItWorksDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "How it works",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Test Section (for development/testing)
            TestDataCard(
                onLoadNormalText = {
                    pastedText = getRandomNormalTestData()
                },
                onLoadLargeText = {
                    pastedText = getLargeTestData()
                }
            )

            // Instructions Card (dismissable)
            if (shouldShowHowItWorks) {
                HowItWorksCard(
                    onDismiss = {
                        coroutineScope.launch {
                            themePreferences.setShowBatchImportHowItWorks(false)
                        }
                    }
                )
            }

            // Paste and Clear buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                // Clear button
                OutlinedButton(
                    onClick = {
                        if (pastedText.isNotEmpty()) {
                            showClearConfirmationDialog = true
                        }
                    },
                    enabled = pastedText.isNotEmpty(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text("Clear")
                }

                // Paste button
                OutlinedButton(
                    onClick = {
                        val clipboardText = clipboardManager.getText()?.text
                        if (!clipboardText.isNullOrEmpty()) {
                            // Append to existing text instead of replacing
                            pastedText = if (pastedText.isEmpty()) {
                                clipboardText
                            } else {
                                pastedText + "\n" + clipboardText
                            }
                        }
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = "Paste",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text("Paste")
                }
            }

            // Large multiline text field
            OutlinedTextField(
                value = pastedText,
                onValueChange = { pastedText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                placeholder = {
                    Text(
                        text = "Paste your URLs here...\n\nExample:\nhttps://example.com\nhttps://github.com/user/repo\nCheck out this link: https://blog.com/post",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                textStyle = MaterialTheme.typography.bodyMedium,
                shape = RoundedCornerShape(12.dp),
                maxLines = Int.MAX_VALUE,
                singleLine = false
            )

            // Statistics Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Character count
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$characterCount",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "characters",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Line count
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$lineCount",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "lines",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = handleBackPress,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = { onStartScan(pastedText) },
                    modifier = Modifier.weight(1f),
                    enabled = pastedText.isNotEmpty(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Check URLs")
                }
            }
        }
    }

    // Discard content confirmation dialog
    if (showConfirmationDialog) {
        ConfirmationDialog(
            title = "Discard Pasted Content?",
            message = "You have unsaved text. Do you want to discard it?",
            confirmText = "Discard",
            onConfirm = {
                showConfirmationDialog = false
                onNavigateBack()
            },
            onDismiss = {
                showConfirmationDialog = false
            }
        )
    }

    // Clear content confirmation dialog
    if (showClearConfirmationDialog) {
        ConfirmationDialog(
            title = "Clear All Text?",
            message = "This will remove all text from the field. This action cannot be undone.",
            confirmText = "Clear",
            onConfirm = {
                showClearConfirmationDialog = false
                pastedText = ""
            },
            onDismiss = {
                showClearConfirmationDialog = false
            }
        )
    }

    // How It Works Dialog
    if (showHowItWorksDialog) {
        HowItWorksDialog(
            onDismiss = { showHowItWorksDialog = false }
        )
    }
}

/**
 * Test Data Card - Load sample data for testing
 */
@Composable
private fun TestDataCard(
    onLoadNormalText: () -> Unit,
    onLoadLargeText: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Testing Tools",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Normal Text Button
                OutlinedButton(
                    onClick = onLoadNormalText,
                    modifier = Modifier
                        .weight(1f)
                        .height(70.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Normal",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "5-10 links",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }

                // Large Text Button
                OutlinedButton(
                    onClick = onLoadLargeText,
                    modifier = Modifier
                        .weight(1f)
                        .height(70.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Large",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "150+ links",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }
        }
    }
}

/**
 * How It Works Card - Dismissable with close button
 */
@Composable
private fun HowItWorksCard(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(end = 24.dp), // Extra padding for close button
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "How it works",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Paste text containing multiple URLs in any format. We'll automatically extract and import them for you.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Close button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * How It Works Dialog - Shown when info icon is clicked
 */
@Composable
private fun HowItWorksDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "How it works",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Paste text containing multiple URLs in any format. We'll automatically extract and import them for you.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "Examples:",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "• https://example.com\n" +
                          "• Check out https://github.com/user/repo\n" +
                          "• Multiple links on one line: site1.com, site2.com\n" +
                          "• Mixed with regular text",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

/**
 * Reusable confirmation dialog component
 */
@Composable
private fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Start
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    confirmText,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

/**
 * FOR TESTING ONLY: Get random normal test data (5-10 links)
 */
private fun getRandomNormalTestData(): String {
    val testDataList = listOf(
        // Test 1: Simple list of URLs
        """
        https://github.com/JetBrains/kotlin
        https://developer.android.com/jetpack/compose
        https://kotlinlang.org/docs/home.html
        https://medium.com/androiddevelopers/effective-state-management-for-jetpack-compose-d7e1bd9c1c5a
        https://stackoverflow.com/questions/tagged/android
        """.trimIndent(),

        // Test 2: Mixed text with URLs
        """
        Check out these amazing resources:
        - Kotlin documentation: https://kotlinlang.org/
        - Android guide at developer.android.com/guide
        - Great blog post: https://proandroiddev.com/jetpack-compose-best-practices
        Also visit https://github.com/android for official samples
        Don't forget reddit.com/r/androiddev for community discussions
        """.trimIndent(),

        // Test 3: URLs in sentences
        """
        I found this great article on https://medium.com/@dev/android-tips yesterday.
        You should also read https://blog.jetbrains.com/kotlin/ for the latest updates.
        The official guide is at developer.android.com/guide/components but there's
        also a good tutorial at https://www.raywenderlich.com/android-tutorials.
        For design inspiration, check out material.io/design.
        """.trimIndent(),

        // Test 4: Multiple URLs per line
        """
        Resources: https://github.com/topics/android, https://stackoverflow.com, developer.android.com
        Blogs: medium.com/tag/android, dev.to/t/android, https://androidweekly.net
        Libraries: github.com/square/retrofit, github.com/coil-kt/coil
        """.trimIndent(),

        // Test 5: URLs with different formats
        """
        https://www.youtube.com/watch?v=abc123
        github.com/user/repo/issues/42
        http://old-site.com/page.html
        www.example.com/path/to/resource
        subdomain.example.com/api/v1/endpoint
        https://docs.google.com/document/d/abc123/edit
        """.trimIndent(),

        // Test 6: News articles and blogs
        """
        Breaking: New Android version announced! https://android-developers.googleblog.com/2024/01/android-15-preview

        Tech news from https://techcrunch.com/android and https://theverge.com/tech

        Tutorial I'm following: https://www.udacity.com/course/android-basics

        Community: reddit.com/r/android, xda-developers.com
        """.trimIndent(),

        // Test 7: Development resources
        """
        Official docs: developer.android.com/docs

        Libraries to check:
        • Networking: github.com/square/retrofit
        • Images: github.com/coil-kt/coil
        • DI: insert-koin.io
        • Database: developer.android.com/training/data-storage/room

        Tutorials: https://www.vogella.com/tutorials/android.html
        """.trimIndent(),

        // Test 8: Mixed content with duplicates (for testing)
        """
        Important links for the project:

        Backend API: https://api.example.com/v2/docs
        Frontend repo: github.com/company/frontend
        Design system: https://www.figma.com/file/abc123

        Also see:
        https://api.example.com/v2/docs (same as above)
        Backup link: github.com/company/frontend

        Slack: company.slack.com/archives/C123
        """.trimIndent(),

        // Test 9: Social media and video links
        """
        Follow these channels:
        YouTube tutorial: https://www.youtube.com/watch?v=dQw4w9WgXcQ
        Twitter updates: twitter.com/androiddev
        LinkedIn post: https://www.linkedin.com/posts/user_android-mobile-dev

        Conference talk: https://www.youtube.com/watch?v=example
        Slides: speakerdeck.com/user/presentation
        """.trimIndent(),

        // Test 10: E-commerce and mixed domains
        """
        Shopping list:
        Phone case: https://www.amazon.com/dp/B08XYZ123
        Screen protector: amazon.com/gp/product/B09ABC456

        Reviews:
        https://www.cnet.com/reviews/android-phones/
        gsmarena.com/samsung_galaxy_s24-review-123.php

        Price comparison: camelcamelcamel.com
        Deals: slickdeals.net/android
        """.trimIndent()
    )

    return testDataList[Random.nextInt(testDataList.size)]
}

/**
 * FOR TESTING ONLY: Large test data with 50+ links for load testing
 */
private fun getLargeTestData(): String {
    return """
    📚 COMPREHENSIVE WEB DEVELOPMENT RESOURCES COLLECTION

    This is a curated list of essential web development resources gathered from various sources.
    Perfect for testing link import functionality with a large dataset.

    === OFFICIAL DOCUMENTATION ===

    Frontend Frameworks:
    React official docs: https://react.dev/learn
    Vue.js guide: https://vuejs.org/guide/introduction.html
    Angular documentation: https://angular.io/docs
    Svelte tutorial: https://svelte.dev/tutorial
    Next.js docs: https://nextjs.org/docs
    Nuxt.js guide: https://nuxt.com/docs
    Remix docs: https://remix.run/docs
    Astro documentation: https://docs.astro.build

    Backend & APIs:
    Node.js documentation: https://nodejs.org/docs/latest/api/
    Express.js guide: https://expressjs.com/en/guide/routing.html
    Django documentation: https://docs.djangoproject.com/
    Flask quickstart: https://flask.palletsprojects.com/quickstart/
    FastAPI docs: https://fastapi.tiangolo.com/
    Ruby on Rails guides: https://guides.rubyonrails.org/
    Laravel documentation: https://laravel.com/docs
    Spring Boot reference: https://docs.spring.io/spring-boot/docs/current/reference/html/

    === LEARNING PLATFORMS ===

    Interactive Tutorials:
    https://www.freecodecamp.org/learn
    https://www.codecademy.com/catalog
    https://www.udemy.com/courses/development/
    https://www.coursera.org/browse/computer-science
    https://www.edx.org/learn/computer-programming
    https://www.pluralsight.com/browse/software-development
    https://egghead.io/
    https://frontendmasters.com/
    https://www.udacity.com/courses/programming
    https://www.khanacademy.org/computing/computer-programming

    === DEVELOPER COMMUNITIES ===

    Forums & Discussion:
    Stack Overflow: https://stackoverflow.com/
    Reddit webdev: https://www.reddit.com/r/webdev/
    Dev.to community: https://dev.to/
    Hashnode blogs: https://hashnode.com/
    HackerNews: https://news.ycombinator.com/
    Lobsters: https://lobste.rs/

    === CODE REPOSITORIES & EXAMPLES ===

    GitHub Collections:
    https://github.com/topics/web-development
    https://github.com/topics/javascript
    https://github.com/topics/typescript
    https://github.com/topics/python
    https://github.com/trending
    https://github.com/collections/web-accessibility
    https://github.com/topics/progressive-web-apps
    https://github.com/topics/serverless

    === CSS & DESIGN ===

    CSS Resources:
    https://css-tricks.com/
    https://www.smashingmagazine.com/category/css/
    https://developer.mozilla.org/en-US/docs/Web/CSS
    https://web.dev/learn/css/
    https://cssreference.io/
    https://tailwindcss.com/docs
    https://getbootstrap.com/docs/
    https://bulma.io/documentation/

    Design Systems:
    Material Design: https://material.io/design
    Apple HIG: https://developer.apple.com/design/human-interface-guidelines/
    Ant Design: https://ant.design/
    Chakra UI: https://chakra-ui.com/
    Radix UI: https://www.radix-ui.com/

    === JAVASCRIPT ECOSYSTEM ===

    Libraries & Tools:
    Lodash utilities: https://lodash.com/docs/
    Axios HTTP client: https://axios-http.com/docs/intro
    Moment.js dates: https://momentjs.com/docs/
    Day.js alternative: https://day.js.org/
    Chart.js graphs: https://www.chartjs.org/docs/
    D3.js visualization: https://d3js.org/
    Three.js 3D: https://threejs.org/docs/
    Gsap animation: https://greensock.com/docs/

    === TESTING & QUALITY ===

    Testing Frameworks:
    Jest testing: https://jestjs.io/docs/getting-started
    Vitest modern: https://vitest.dev/guide/
    Cypress e2e: https://docs.cypress.io/
    Playwright testing: https://playwright.dev/docs/intro
    Testing Library: https://testing-library.com/docs/
    Mocha framework: https://mochajs.org/

    === DEPLOYMENT & HOSTING ===

    Cloud Platforms:
    Vercel hosting: https://vercel.com/docs
    Netlify deploy: https://docs.netlify.com/
    AWS documentation: https://docs.aws.amazon.com/
    Google Cloud: https://cloud.google.com/docs
    Azure docs: https://docs.microsoft.com/azure/
    Heroku platform: https://devcenter.heroku.com/
    DigitalOcean tutorials: https://www.digitalocean.com/community/tutorials
    Railway hosting: https://docs.railway.app/
    Render services: https://render.com/docs
    Fly.io platform: https://fly.io/docs/

    === DATABASE & BACKEND ===

    Databases:
    MongoDB docs: https://docs.mongodb.com/
    PostgreSQL manual: https://www.postgresql.org/docs/
    MySQL reference: https://dev.mysql.com/doc/
    Redis documentation: https://redis.io/documentation
    Firebase guides: https://firebase.google.com/docs
    Supabase docs: https://supabase.com/docs
    PlanetScale MySQL: https://planetscale.com/docs

    === PERFORMANCE & OPTIMIZATION ===

    Web Performance:
    https://web.dev/learn-core-web-vitals/
    https://developers.google.com/speed/docs/insights/
    https://www.webpagetest.org/
    https://gtmetrix.com/
    https://tools.pingdom.com/

    === SECURITY ===

    Security Resources:
    OWASP Top 10: https://owasp.org/www-project-top-ten/
    Security headers: https://securityheaders.com/
    Content Security Policy: https://content-security-policy.com/

    === TOOLS & UTILITIES ===

    Developer Tools:
    Can I Use: https://caniuse.com/
    Regexr patterns: https://regexr.com/
    JSON formatter: https://jsonformatter.org/
    Code beautifier: https://codebeautify.org/
    Epoch converter: https://www.epochconverter.com/
    Base64 encode: https://www.base64encode.org/
    Color picker: https://htmlcolorcodes.com/color-picker/

    === NEWS & BLOGS ===

    Tech News:
    https://www.theverge.com/tech
    https://techcrunch.com/
    https://www.wired.com/category/gear/
    https://arstechnica.com/

    Developer Blogs:
    https://overreacted.io/
    https://kentcdodds.com/blog
    https://css-tricks.com/
    https://www.joshwcomeau.com/
    https://leerob.io/blog

    === PODCASTS & VIDEOS ===

    YouTube Channels:
    Fireship: https://www.youtube.com/@Fireship
    Traversy Media: https://www.youtube.com/@TraversyMedia
    Web Dev Simplified: https://www.youtube.com/@WebDevSimplified
    The Net Ninja: https://www.youtube.com/@NetNinja
    Kevin Powell CSS: https://www.youtube.com/@KevinPowell

    === API RESOURCES ===

    Public APIs:
    https://github.com/public-apis/public-apis
    https://rapidapi.com/hub
    https://any-api.com/
    https://apilist.fun/

    === MISCELLANEOUS ===

    Code Challenges:
    LeetCode: https://leetcode.com/
    HackerRank: https://www.hackerrank.com/
    Codewars: https://www.codewars.com/
    Project Euler: https://projecteuler.net/
    Advent of Code: https://adventofcode.com/

    Icons & Assets:
    Font Awesome: https://fontawesome.com/
    Heroicons: https://heroicons.com/
    Unsplash photos: https://unsplash.com/
    Pexels videos: https://www.pexels.com/

    This collection contains over 150+ unique URLs across various categories for comprehensive testing.
    Use this to test batch import performance, duplicate detection, and URL extraction logic.
    """.trimIndent()
}
