plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "io.github.halilozel1903.dictionary.sample"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.halilozel1903.dictionary.sample"
        minSdk = 30
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/INDEX.LIST",
                "/META-INF/DEPENDENCIES",
                "/META-INF/versions/9/OSGI-INF/MANIFEST.MF",
                "/META-INF/versions/9/module-info.class",
                "/META-INF/versions/9/previous-compilation-data.bin",
            )
        }
    }
}

dependencies {
    implementation(project(":dictionary"))
    // OkHttp is the usual HTTP stack on Android; without this line the client uses Ktor's CIO engine.
    implementation(libs.ktor.client.okhttp)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.wear.compose.foundation)
    implementation(libs.androidx.wear.compose.material3)
    // The system text input (keyboard, voice, handwriting) for typing a word on the watch.
    implementation(libs.androidx.wear.input)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
