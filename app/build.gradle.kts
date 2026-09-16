import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.firemind.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.firemind.app"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Unit tests read the shipped catalog from src/main/assets so tests
    // assert against the real data rather than a fixture copy.
    sourceSets {
        getByName("test") {
            resources.srcDir("src/main/assets")
        }
    }

    buildTypes.forEach { buildType ->
        // Backend base URL. Cleartext HTTP is allowed by the network security
        // config ONLY for loopback/10.0.2.2 dev hosts; production must be HTTPS.
        buildType.buildConfigField(
            "String",
            "BACKEND_URL",
            "\"${System.getenv("FIREMIND_BACKEND_URL") ?: "http://10.0.2.2:8080"}\""
        )
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    // BOM pinned to the compileSdk-36 era; newer androidx (1.12+ / 2.10+)
    // requires compileSdk 37 + AGP 9.1.
    val composeBom = platform("androidx.compose:compose-bom:2026.06.00")
    implementation(composeBom)

    // TV-first Compose material (focus-aware components)
    implementation("androidx.tv:tv-material:1.1.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.12.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.navigation:navigation-compose:2.9.8")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("androidx.core:core-ktx:1.17.0")

    // Data + networking
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Local (JVM) unit tests - no Android device or emulator required.
    testImplementation("junit:junit:4.13.2")
}
