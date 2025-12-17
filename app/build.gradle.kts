plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.rejowan.linky"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.rejowan.linky"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)


    // google fonts
    implementation(libs.androidx.ui.text.google.fonts)


    // koin
    implementation(libs.koin.androidx.compose)

    // lifecycle
    implementation(libs.androidx.runtime.livedata)
    implementation(libs.androidx.lifecycle.livedata.ktx)


    // splash screen api
    implementation(libs.androidx.core.splashscreen)

    // icon
    implementation(libs.androidx.material.icons.extended.android)

    // accompanist
    implementation(libs.accompanist.insets)
    implementation(libs.accompanist.permissions)
    implementation(libs.accompanist.systemuicontroller)


    // data store
    implementation(libs.androidx.datastore.preferences)


    // timber
    implementation(libs.timber)


    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Navigation Compose
    implementation(libs.androidx.navigation.compose)

    // Kotlin Serialization
    implementation(libs.kotlinx.serialization.json)

    // Coil - Image Loading
    implementation(libs.coil.compose)

    // Jsoup - HTML Parsing
    implementation(libs.jsoup)

    // Readability4J - Reader mode parsing
    implementation(libs.readability4j)

    // JetBrains Markdown - Markdown parsing and rendering
    implementation(libs.jetbrains.markdown)

    // gson
    implementation(libs.gson)

    // Security Crypto - Encrypted SharedPreferences
    implementation(libs.androidx.security.crypto)

    // Glance - App Widgets
    implementation(libs.androidx.glance)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

}
