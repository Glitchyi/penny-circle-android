import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_21) }
}

dependencies {
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    // Android supplies iroh through computer.iroh:iroh-android (native .so per ABI).
    // JVM tests use the desktop artifact, which bundles linux-x86-64 natives.
    compileOnly("computer.iroh:iroh:1.2.0")
    testImplementation("computer.iroh:iroh:1.2.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}

tasks.test {
    // Real-network iroh tests are opt-in: ./gradlew :core:test -PirohOnline=true
    systemProperty("irohOnline", providers.gradleProperty("irohOnline").getOrElse("false"))
    testLogging { events("passed", "failed", "skipped"); showStandardStreams = true }
}
