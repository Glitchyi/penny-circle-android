import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}
android {
    namespace = "com.spendingbuddy.app"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.spendingbuddy.app"
        minSdk = 33
        targetSdk = 36
        versionCode = 2
        versionName = "0.2.0"
        // Both target phones (Xiaomi 17, OnePlus Nord) are arm64. x86_64 is kept for emulator testing.
        ndk { abiFilters += listOf("arm64-v8a", "x86_64") }
    }
    buildTypes { release { isMinifyEnabled = false } }
    // iroh-ffi 1.2 bindings are compiled for Java 21.
    compileOptions { sourceCompatibility = JavaVersion.VERSION_21; targetCompatibility = JavaVersion.VERSION_21 }
    buildFeatures { compose = true; buildConfig = true }
    packaging {
        resources.excludes += setOf(
            "/META-INF/{AL2.0,LGPL2.1}",
            // The JVM iroh jar bundles desktop natives as resources; Android uses jni/ libs from iroh-android.
            "darwin-aarch64/**", "linux-aarch64/**", "linux-x86-64/**", "win32-x86-64/**"
        )
        jniLibs.useLegacyPackaging = false
    }
    testOptions { unitTests.isReturnDefaultValues = true; unitTests.isIncludeAndroidResources = true }
    lint { abortOnError = true; checkReleaseBuilds = true; disable += setOf("NewApi") }
}
kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_21) } }

configurations.all {
    // Android must use JNA's @aar (ships libjnidispatch.so per ABI), never the desktop jar.
    exclude(group = "net.java.dev.jna", module = "jna-platform")
}

dependencies {
    implementation(project(":core"))
    implementation("computer.iroh:iroh-android:1.2.0")
    implementation(platform("androidx.compose:compose-bom:2025.08.00"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.2")
    implementation("androidx.lifecycle:lifecycle-process:2.9.2")
    implementation("androidx.lifecycle:lifecycle-service:2.9.2")
    implementation("androidx.navigation:navigation-compose:2.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("androidx.work:work-runtime-ktx:2.10.3")
    // Pairing: QR generation (pure Java) and Google's code scanner (no CAMERA permission needed).
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.google.android.gms:play-services-code-scanner:16.1.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
