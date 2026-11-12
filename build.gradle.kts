import java.util.Properties

// Baca kredensial Supabase dari local.properties (tidak ikut ke git)
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.autophoto.ai"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.autophoto.ai"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        vectorDrawables { useSupportLibrary = true }

        buildConfigField("String", "SUPABASE_URL", "\"${localProps.getProperty("supabase.url", "")}\"")
        buildConfigField("String", "SUPABASE_KEY", "\"${localProps.getProperty("supabase.key", "")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    // Compose compiler 1.5.14 kompatibel dengan Kotlin 1.9.24
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    // --- Core & Lifecycle ---
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-savedstate:2.7.0")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // --- Jetpack Compose (versi dikunci oleh BOM) ---
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // --- Navigation Compose ---
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // --- CameraX ---
    val cameraX = "1.3.4"
    implementation("androidx.camera:camera-core:$cameraX")
    implementation("androidx.camera:camera-camera2:$cameraX")
    implementation("androidx.camera:camera-lifecycle:$cameraX")
    implementation("androidx.camera:camera-view:$cameraX")

    // --- Room (KSP menggantikan kapt yang sudah deprecated) ---
    val room = "2.6.1"
    implementation("androidx.room:room-runtime:$room")
    implementation("androidx.room:room-ktx:$room")
    ksp("androidx.room:room-compiler:$room")

    // --- Coil ---
    implementation("io.coil-kt:coil-compose:2.6.0")

    // --- Google ML Kit ---
    // Subject Segmentation (via Google Play services, model diunduh otomatis)
    implementation("com.google.android.gms:play-services-mlkit-subject-segmentation:16.0.0-beta1")
    // Face Detection (model ikut ter-bundle di APK)
    implementation("com.google.mlkit:face-detection:16.1.6")

    // --- Coroutines (+ .await() untuk Task ML Kit) ---
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // --- Supabase (supabase-kt 2.x kompatibel dengan Kotlin 1.9) ---
    implementation(platform("io.github.jan-tennert.supabase:bom:2.5.4"))
    implementation("io.github.jan-tennert.supabase:gotrue-kt")     // Auth
    implementation("io.github.jan-tennert.supabase:postgrest-kt")  // Database
    implementation("io.github.jan-tennert.supabase:storage-kt")    // Storage
    implementation("io.ktor:ktor-client-android:2.3.12")           // HTTP engine
}
