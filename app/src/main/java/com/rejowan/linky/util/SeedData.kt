package com.rejowan.linky.util

import com.rejowan.linky.domain.model.Collection
import com.rejowan.linky.domain.model.Link
import java.util.UUID

/**
 * Seed data generator for development and testing
 * Contains real working URLs and realistic collection names
 */
object SeedData {

    /**
     * Generate seed collections
     */
    fun generateCollections(): List<Collection> {
        val now = System.currentTimeMillis()
        val day = 86400000L // 1 day in milliseconds
        val colors = listOf(
            "#FF6B6B", "#E74C3C", "#4ECDC4", "#45B7D1", "#3498DB",
            "#FFA07A", "#E67E22", "#98D8C8", "#2ECC71", "#F7B731",
            "#F39C12", "#5F27CD", "#9B59B6", "#EE5A6F", "#1ABC9C",
            "#34495E", "#7F8C8D", "#2C3E50", "#8E44AD", "#16A085",
            "#D35400", "#C0392B", "#27AE60", "#2980B9"
        )

        return listOf(
            // Development
            Collection(id = "col_android", name = "Android Development", color = colors[0], sortOrder = 0, createdAt = now - day * 45, updatedAt = now - day * 2),
            Collection(id = "col_kotlin", name = "Kotlin", color = colors[1], sortOrder = 1, createdAt = now - day * 40, updatedAt = now - day * 5),
            Collection(id = "col_compose", name = "Jetpack Compose", color = colors[2], sortOrder = 2, createdAt = now - day * 35, updatedAt = now - day * 3),
            Collection(id = "col_web", name = "Web Development", color = colors[3], sortOrder = 3, createdAt = now - day * 60, updatedAt = now - day * 10),
            Collection(id = "col_backend", name = "Backend & APIs", color = colors[4], sortOrder = 4, createdAt = now - day * 55, updatedAt = now - day * 12),

            // Design
            Collection(id = "col_design", name = "UI/UX Design", color = colors[5], sortOrder = 5, createdAt = now - day * 50, updatedAt = now - day * 7),
            Collection(id = "col_icons", name = "Icons & Assets", color = colors[6], sortOrder = 6, createdAt = now - day * 30, updatedAt = now - day * 15),
            Collection(id = "col_colors", name = "Color Palettes", color = colors[7], sortOrder = 7, createdAt = now - day * 25, updatedAt = now - day * 20),

            // Learning
            Collection(id = "col_tutorials", name = "Tutorials", color = colors[8], sortOrder = 8, createdAt = now - day * 90, updatedAt = now - day * 8),
            Collection(id = "col_courses", name = "Online Courses", color = colors[9], sortOrder = 9, createdAt = now - day * 85, updatedAt = now - day * 22),
            Collection(id = "col_docs", name = "Documentation", color = colors[10], sortOrder = 10, createdAt = now - day * 80, updatedAt = now - day * 4),

            // Tools
            Collection(id = "col_tools", name = "Developer Tools", color = colors[11], sortOrder = 11, createdAt = now - day * 100, updatedAt = now - day * 1),
            Collection(id = "col_ai", name = "AI & ML", color = colors[12], sortOrder = 12, createdAt = now - day * 20, updatedAt = now - day * 6),
            Collection(id = "col_productivity", name = "Productivity", color = colors[13], sortOrder = 13, createdAt = now - day * 75, updatedAt = now - day * 9),

            // News & Blogs
            Collection(id = "col_news", name = "Tech News", color = colors[14], sortOrder = 14, createdAt = now - day * 70, updatedAt = now - day * 1),
            Collection(id = "col_blogs", name = "Dev Blogs", color = colors[15], sortOrder = 15, createdAt = now - day * 65, updatedAt = now - day * 11),

            // Entertainment
            Collection(id = "col_videos", name = "Videos & Podcasts", color = colors[16], sortOrder = 16, createdAt = now - day * 95, updatedAt = now - day * 14),
            Collection(id = "col_music", name = "Music", color = colors[17], sortOrder = 17, createdAt = now - day * 120, updatedAt = now - day * 30),

            // Reference
            Collection(id = "col_github", name = "GitHub Repos", color = colors[18], sortOrder = 18, createdAt = now - day * 110, updatedAt = now - day * 2),
            Collection(id = "col_libraries", name = "Libraries", color = colors[19], sortOrder = 19, createdAt = now - day * 105, updatedAt = now - day * 18),
            Collection(id = "col_apis", name = "Public APIs", color = colors[20], sortOrder = 20, createdAt = now - day * 88, updatedAt = now - day * 25),

            // Personal
            Collection(id = "col_readlater", name = "Read Later", color = colors[21], sortOrder = 21, createdAt = now - day * 15, updatedAt = now - day * 1),
            Collection(id = "col_inspiration", name = "Inspiration", color = colors[22], sortOrder = 22, createdAt = now - day * 10, updatedAt = now - day * 3),
            Collection(id = "col_shopping", name = "Shopping", color = colors[23], sortOrder = 23, createdAt = now - day * 5, updatedAt = now - day * 2)
        )
    }

    /**
     * Generate seed links with real working URLs
     */
    fun generateLinks(): List<Link> {
        val now = System.currentTimeMillis()

        return listOf(
            // Android Development
            Link(
                id = UUID.randomUUID().toString(),
                title = "Android Developers",
                description = "Official Android developer documentation and guides",
                url = "https://developer.android.com",
                collectionId = "col_android",
                createdAt = now - 86400000 * 1,
                updatedAt = now
            ),
            Link(
                id = UUID.randomUUID().toString(),
                title = "Android Weekly",
                description = "Free weekly Android development newsletter",
                url = "https://androidweekly.net",
                collectionId = "col_android",
                createdAt = now - 86400000 * 2,
                updatedAt = now
            ),
            Link(
                id = UUID.randomUUID().toString(),
                title = "ProAndroidDev",
                description = "Professional Android Development publication on Medium",
                url = "https://proandroiddev.com",
                collectionId = "col_android",
                createdAt = now - 86400000 * 3,
                updatedAt = now
            ),

            // Kotlin
            Link(
                id = UUID.randomUUID().toString(),
                title = "Kotlin Language",
                description = "Official Kotlin programming language website",
                url = "https://kotlinlang.org",
                collectionId = "col_kotlin",
                createdAt = now - 86400000 * 4,
                updatedAt = now
            ),
            Link(
                id = UUID.randomUUID().toString(),
                title = "Kotlin Playground",
                description = "Online Kotlin code editor and runner",
                url = "https://play.kotlinlang.org",
                collectionId = "col_kotlin",
                createdAt = now - 86400000 * 5,
                updatedAt = now
            ),

            // Jetpack Compose
            Link(
                id = UUID.randomUUID().toString(),
                title = "Compose Documentation",
                description = "Official Jetpack Compose documentation",
                url = "https://developer.android.com/jetpack/compose",
                collectionId = "col_compose",
                createdAt = now - 86400000 * 6,
                updatedAt = now
            ),
            Link(
                id = UUID.randomUUID().toString(),
                title = "Composables.com",
                description = "Jetpack Compose UI components and examples",
                url = "https://www.composables.com",
                collectionId = "col_compose",
                createdAt = now - 86400000 * 7,
                updatedAt = now
            ),

            // Web Development
            Link(
                id = UUID.randomUUID().toString(),
                title = "MDN Web Docs",
                description = "Resources for developers, by developers",
                url = "https://developer.mozilla.org",
                collectionId = "col_web",
                createdAt = now - 86400000 * 8,
                updatedAt = now
            ),
            Link(
                id = UUID.randomUUID().toString(),
                title = "CSS-Tricks",
                description = "Tips, tricks, and techniques on using CSS",
                url = "https://css-tricks.com",
                collectionId = "col_web",
                createdAt = now - 86400000 * 9,
                updatedAt = now
            ),
            Link(
                id = UUID.randomUUID().toString(),
                title = "Smashing Magazine",
                description = "For web designers and developers",
                url = "https://www.smashingmagazine.com",
                collectionId = "col_web",
                createdAt = now - 86400000 * 10,
                updatedAt = now
            ),

            // Backend & APIs
            Link(
                id = UUID.randomUUID().toString(),
                title = "Postman",
                description = "API platform for building and using APIs",
                url = "https://www.postman.com",
                collectionId = "col_backend",
                createdAt = now - 86400000 * 11,
                updatedAt = now
            ),
            Link(
                id = UUID.randomUUID().toString(),
                title = "JSON Placeholder",
                description = "Free fake API for testing and prototyping",
                url = "https://jsonplaceholder.typicode.com",
                collectionId = "col_backend",
                createdAt = now - 86400000 * 12,
                updatedAt = now
            ),

            // UI/UX Design
            Link(
                id = UUID.randomUUID().toString(),
                title = "Dribbble",
                description = "Discover the world's top designers & creatives",
                url = "https://dribbble.com",
                collectionId = "col_design",
                createdAt = now - 86400000 * 13,
                updatedAt = now
            ),
            Link(
                id = UUID.randomUUID().toString(),
                title = "Behance",
                description = "Showcase and discover creative work",
                url = "https://www.behance.net",
                collectionId = "col_design",
                createdAt = now - 86400000 * 14,
                updatedAt = now
            ),
            Link(
                id = UUID.randomUUID().toString(),
                title = "Figma",
                description = "Collaborative interface design tool",
                url = "https://www.figma.com",
                collectionId = "col_design",
                createdAt = now - 86400000 * 15,
                updatedAt = now,
                isFavorite = true
            ),

            // Icons & Assets
            Link(
                id = UUID.randomUUID().toString(),
                title = "Material Icons",
                description = "Google Material Design icons",
                url = "https://fonts.google.com/icons",
                collectionId = "col_icons",
                createdAt = now - 86400000 * 16,
                updatedAt = now
            ),
            Link(
                id = UUID.randomUUID().toString(),
                title = "Heroicons",
                description = "Beautiful hand-crafted SVG icons",
                url = "https://heroicons.com",
                collectionId = "col_icons",
                createdAt = now - 86400000 * 17,
                updatedAt = now
            ),
            Link(
                id = UUID.randomUUID().toString(),
                title = "Unsplash",
                description = "Beautiful free images & pictures",
                url = "https://unsplash.com",
                collectionId = "col_icons",
                createdAt = now - 86400000 * 18,
                updatedAt = now,
                isFavorite = true
            ),

            // Color Palettes
            Link(
                id = UUID.randomUUID().toString(),
                title = "Coolors",
                description = "The super fast color palettes generator",
                url = "https://coolors.co",
                collectionId = "col_colors",
                createdAt = now - 86400000 * 19,
                updatedAt = now
            ),
            Link(
                id = UUID.randomUUID().toString(),
                title = "Color Hunt",
                description = "Curated collection of beautiful color palettes",
                url = "https://colorhunt.co",
                collectionId = "col_colors",
                createdAt = now - 86400000 * 20,
                updatedAt = now
            ),

            // Tutorials
            Link(
                id = UUID.randomUUID().toString(),
                title = "Ray Wenderlich",
                description = "High quality programming tutorials",
                url = "https://www.kodeco.com",
                collectionId = "col_tutorials",
                createdAt = now - 86400000 * 21,
                updatedAt = now
            ),
            Link(
                id = UUID.randomUUID().toString(),
                title = "freeCodeCamp",
                description = "Learn to code for free",
                url = "https://www.freecodecamp.org",
                collectionId = "col_tutorials",
                createdAt = now - 86400000 * 22,
                updatedAt = now,
                isFavorite = true
            ),

            // Online Courses
            Link(
                id = UUID.randomUUID().toString(),
                title = "Udemy",
                description = "Online learning and teaching marketplace",
                url = "https://www.udemy.com",
                collectionId = "col_courses",
                createdAt = now - 86400000 * 23,
                updatedAt = now
            ),
            Link(
                id = UUID.randomUUID().toString(),
                title = "Coursera",
                description = "Build skills with courses from top universities",
                url = "https://www.coursera.org",
                collectionId = "col_courses",
                createdAt = now - 86400000 * 24,
                updatedAt = now
            ),

            // Documentation
            Link(
                id = UUID.randomUUID().toString(),
                title = "DevDocs",
                description = "API documentation browser",
                url = "https://devdocs.io",
                collectionId = "col_docs",
                createdAt = now - 86400000 * 25,
                updatedAt = now,
                isFavorite = true
            ),

            // Developer Tools
            Link(
                id = UUID.randomUUID().toString(),
                title = "GitHub",
                description = "Where the world builds software",
                url = "https://github.com",
                collectionId = "col_tools",
                createdAt = now - 86400000 * 26,
                updatedAt = now,
                isFavorite = true
            ),
            Link(
                id = UUID.randomUUID().toString(),
                title = "Stack Overflow",
                description = "Where developers learn, share, & build careers",
                url = "https://stackoverflow.com",
                collectionId = "col_tools",
                createdAt = now - 86400000 * 27,
                updatedAt = now,
                isFavorite = true
            ),
            Link(
                id = UUID.randomUUID().toString(),
                title = "Regex101",
                description = "Online regex tester and debugger",
                url = "https://regex101.com",
                collectionId = "col_tools",
                createdAt = now - 86400000 * 28,
                updatedAt = now
            ),

            // AI & ML
            Link(
                id = UUID.randomUUID().toString(),
                title = "OpenAI",
                description = "AI research and deployment company",
                url = "https://openai.com",
                collectionId = "col_ai",
                createdAt = now - 86400000 * 29,
                updatedAt = now
            ),
            Link(
                id = UUID.randomUUID().toString(),
                title = "Hugging Face",
                description = "The AI community building the future",
                url = "https://huggingface.co",
                collectionId = "col_ai",
                createdAt = now - 86400000 * 30,
                updatedAt = now
            ),
            Link(
                id = UUID.randomUUID().toString(),
                title = "Papers With Code",
                description = "Machine learning papers with code",
                url = "https://paperswithcode.com",
                collectionId = "col_ai",
                createdAt = now - 86400000 * 31,
                updatedAt = now
            ),

            // Productivity
            Link(
                id = UUID.randomUUID().toString(),
                title = "Notion",
                description = "All-in-one workspace for notes, tasks, wikis",
                url = "https://www.notion.so",
                collectionId = "col_productivity",
                createdAt = now - 86400000 * 32,
                updatedAt = now,
                isFavorite = true
            ),
            Link(
                id = UUID.randomUUID().toString(),
                title = "Linear",
                description = "Modern issue tracking and project management",
                url = "https://linear.app",
                collectionId = "col_productivity",
                createdAt = now - 86400000 * 33,
                updatedAt = now
            ),

            // Tech News
            Link(
                id = UUID.randomUUID().toString(),
                title = "Hacker News",
                description = "Social news website focusing on computer science",
                url = "https://news.ycombinator.com",
                collectionId = "col_news",
                createdAt = now - 86400000 * 34,
                updatedAt = now,
                isFavorite = true
            ),
            Link(
                id = UUID.randomUUID().toString(),
                title = "TechCrunch",
                description = "Startup and technology news",
                url = "https://techcrunch.com",
                collectionId = "col_news",
                createdAt = now - 86400000 * 35,
                updatedAt = now
            ),
            Link(
                id = UUID.randomUUID().toString(),
                title = "The Verge",
                description = "Technology, science, art, and culture",
                url = "https://www.theverge.com",
                collectionId = "col_news",
                createdAt = now - 86400000 * 36,
                updatedAt = now
            ),

            // Dev Blogs
            Link(
                id = UUID.randomUUID().toString(),
                title = "Dev.to",
                description = "Community of software developers",
                url = "https://dev.to",
                collectionId = "col_blogs",
                createdAt = now - 86400000 * 37,
                updatedAt = now
            ),
            Link(
                id = UUID.randomUUID().toString(),
                title = "Medium",
                description = "Where good ideas find you",
                url = "https://medium.com",
                collectionId = "col_blogs",
                createdAt = now - 86400000 * 38,
                updatedAt = now
            ),
            Link(
                id = UUID.randomUUID().toString(),
                title = "Hashnode",
                description = "Blogging platform for developers",
                url = "https://hashnode.com",
                collectionId = "col_blogs",
                createdAt = now - 86400000 * 39,
                updatedAt = now
            ),

            // Videos & Podcasts
            Link(
                id = UUID.randomUUID().toString(),
                title = "YouTube",
                description = "Share your videos with friends, family, and the world",
                url = "https://www.youtube.com",
                collectionId = "col_videos",
                createdAt = now - 86400000 * 40,
                updatedAt = now
            ),
            Link(
                id = UUID.randomUUID().toString(),
                title = "Syntax.fm",
                description = "A Tasty Treats Podcast for Web Developers",
                url = "https://syntax.fm",
                collectionId = "col_videos",
                createdAt = now - 86400000 * 41,
                updatedAt = now
            ),

            // Music
            Link(
                id = UUID.randomUUID().toString(),
                title = "Spotify",
                description = "Music for everyone",
                url = "https://www.spotify.com",
                collectionId = "col_music",
                createdAt = now - 86400000 * 42,
                updatedAt = now
            ),
            Link(
                id = UUID.randomUUID().toString(),
                title = "SoundCloud",
                description = "Listen to free music and podcasts",
                url = "https://soundcloud.com",
                collectionId = "col_music",
                createdAt = now - 86400000 * 43,
                updatedAt = now
            ),

            // GitHub Repos
            Link(
                id = UUID.randomUUID().toString(),
                title = "Awesome Android",
                description = "Curated list of awesome Android resources",
                url = "https://github.com/JStumpp/awesome-android",
                collectionId = "col_github",
                createdAt = now - 86400000 * 44,
                updatedAt = now
            ),
            Link(
                id = UUID.randomUUID().toString(),
                title = "Awesome Kotlin",
                description = "A curated list of awesome Kotlin resources",
                url = "https://github.com/KotlinBy/awesome-kotlin",
                collectionId = "col_github",
                createdAt = now - 86400000 * 45,
                updatedAt = now
            ),

            // Libraries
            Link(
                id = UUID.randomUUID().toString(),
                title = "Maven Central",
                description = "Search Maven packages",
                url = "https://search.maven.org",
                collectionId = "col_libraries",
                createdAt = now - 86400000 * 46,
                updatedAt = now
            ),
            Link(
                id = UUID.randomUUID().toString(),
                title = "npm",
                description = "Build amazing things with JavaScript",
                url = "https://www.npmjs.com",
                collectionId = "col_libraries",
                createdAt = now - 86400000 * 47,
                updatedAt = now
            ),

            // Public APIs
            Link(
                id = UUID.randomUUID().toString(),
                title = "Public APIs",
                description = "A collective list of free APIs",
                url = "https://github.com/public-apis/public-apis",
                collectionId = "col_apis",
                createdAt = now - 86400000 * 48,
                updatedAt = now
            ),
            Link(
                id = UUID.randomUUID().toString(),
                title = "RapidAPI",
                description = "Discover and connect to thousands of APIs",
                url = "https://rapidapi.com",
                collectionId = "col_apis",
                createdAt = now - 86400000 * 49,
                updatedAt = now
            ),

            // Read Later (no collection assigned - general links)
            Link(
                id = UUID.randomUUID().toString(),
                title = "Wikipedia",
                description = "The free encyclopedia",
                url = "https://www.wikipedia.org",
                collectionId = "col_readlater",
                createdAt = now - 86400000 * 50,
                updatedAt = now
            ),

            // Inspiration
            Link(
                id = UUID.randomUUID().toString(),
                title = "Awwwards",
                description = "Website awards for design, creativity and innovation",
                url = "https://www.awwwards.com",
                collectionId = "col_inspiration",
                createdAt = now - 86400000 * 51,
                updatedAt = now,
                isFavorite = true
            ),

            // Shopping
            Link(
                id = UUID.randomUUID().toString(),
                title = "Amazon",
                description = "Online shopping from the earth's biggest selection",
                url = "https://www.amazon.com",
                collectionId = "col_shopping",
                createdAt = now - 86400000 * 52,
                updatedAt = now
            )
        )
    }
}
