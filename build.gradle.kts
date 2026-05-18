// ============================================================================
// Gradle Configuration for Comic Aggregator POC
//
// NOTE on 'Unresolved reference: implementation':
// In Gradle Kotlin DSL, the type-safe accessor `implementation(...)` is generated
// dynamically *only* when the Java or Android plugin is applied in the same script.
// If you are putting dependencies in a root build script or if Gradle hasn't
// synced the plugins yet, you must use string delegation: `"implementation"(...)`
// ============================================================================

plugins {
    // Standard plugins matched with working environment
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.21" apply false
}

// ----------------------------------------------------------------------------
// HOW TO ADD DEPENDENCIES RESILIENTLY (Copy this into your app/build.gradle.kts)
// ----------------------------------------------------------------------------
// Using the String operator ("implementation"(...)) is highly robust because it
// resolves dynamically at runtime and NEVER throws compile-time unresolved reference errors!
// ----------------------------------------------------------------------------

/*
dependencies {
    // HTML Scraping / Parsing
    "implementation"("org.jsoup:jsoup:1.16.1")

    // Image Loading for Compose (Coil)
    "implementation"("io.coil-kt:coil-compose:2.4.0")

    // Jetpack & Compose standard dependencies
    "implementation"("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")
    "implementation"("androidx.compose.material3:material3:1.1.1")
    "implementation"("androidx.compose.foundation:foundation:1.5.0")

    // Testing
    "testImplementation"("junit:junit:4.13.2")
}
*/
