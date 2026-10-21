import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.grimsqueaker.comicpulse"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.grimsqueaker.comicpulse"
        minSdk = 24
        targetSdk = 34
        versionCode = 4
        versionName = "1.2.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            val keystoreProps = Properties()
            if (keystorePropertiesFile.exists()) {
                keystorePropertiesFile.inputStream().use { keystoreProps.load(it) }
            }

            val rawPath = keystoreProps.getProperty("storeFile")
                ?: project.findProperty("RELEASE_KEYSTORE_PATH")?.toString()
                ?: System.getenv("RELEASE_KEYSTORE_PATH")
                ?: "keystore/comicpulse.keystore"

            val candidate1 = rootProject.file(rawPath)
            val candidate2 = project.file(rawPath)
            val keystoreFile = if (candidate1.exists()) candidate1 else candidate2

            println("Resolved release keystore: ${keystoreFile.absolutePath} (exists: ${keystoreFile.exists()})")

            storeFile = keystoreFile
            storePassword = keystoreProps.getProperty("storePassword")
                ?: (project.findProperty("RELEASE_KEYSTORE_PASSWORD") ?: System.getenv("RELEASE_KEYSTORE_PASSWORD") ?: "comicpulse_key_2026").toString()
            keyAlias = keystoreProps.getProperty("keyAlias")
                ?: (project.findProperty("RELEASE_KEY_ALIAS") ?: System.getenv("RELEASE_KEY_ALIAS") ?: "comicpulse").toString()
            keyPassword = keystoreProps.getProperty("keyPassword")
                ?: (project.findProperty("RELEASE_KEY_PASSWORD") ?: System.getenv("RELEASE_KEY_PASSWORD") ?: "comicpulse_key_2026").toString()
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
        }
    }

    buildTypes {
        release {
            isDebuggable = false
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
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

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.7"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// ----------------------------------------------------------------------------
// Resilient Dependency Declarations
// Using string delegation `"configurationName"` guarantees that Kotlin compiler
// does not trigger "unresolved reference" errors even if the script is analyzed
// out of compile context.
// ----------------------------------------------------------------------------
dependencies {
    // Core Android/Compose
    "implementation"("androidx.core:core-ktx:1.10.1")
    "implementation"("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    "implementation"("androidx.activity:activity-compose:1.7.2")

    // Jetpack Compose UI
    "implementation"("androidx.compose.ui:ui:1.5.0")
    "implementation"("androidx.compose.ui:ui-graphics:1.5.0")
    "implementation"("androidx.compose.ui:ui-tooling-preview:1.5.0")
    "implementation"("androidx.compose.material3:material3:1.1.1")
    "implementation"("androidx.compose.foundation:foundation:1.5.0")
    
    // ViewModel integration
    "implementation"("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")
    
    // Jetpack Compose Navigation
    "implementation"("androidx.navigation:navigation-compose:2.7.7")

    // HTML Scraping/Parsing
    "implementation"("org.jsoup:jsoup:1.16.1")

    // Asynchronous Image Loader
    "implementation"("io.coil-kt:coil-compose:2.4.0")

    // DocumentFile for SAF directory traversal
    "implementation"("androidx.documentfile:documentfile:1.0.1")

    // DataStore for storing the Library Folder URI persistently
    "implementation"("androidx.datastore:datastore-preferences:1.0.0")

    // Junrar for parsing native .cbr archives
    "implementation"("com.github.junrar:junrar:7.5.5")

    // 7-Zip-JBinding for supporting RAR5 and high-performance CBR formats on Android
    "implementation"("com.github.omicronapps:7-Zip-JBinding-4Android:Release-16.02-2.03")

    // Unit Testing
    "testImplementation"("junit:junit:4.13.2")
    "androidTestImplementation"("androidx.test.ext:junit:1.1.5")
    "androidTestImplementation"("androidx.test.espresso:espresso-core:3.5.1")
    "androidTestImplementation"("androidx.compose.ui:ui-test-junit4:1.5.0")
    "debugImplementation"("androidx.compose.ui:ui-tooling:1.5.0")
    "debugImplementation"("androidx.compose.ui:ui-test-manifest:1.5.0")
}
