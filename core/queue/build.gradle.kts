import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
}

// Pure Kotlin/JVM module: the single shared announcement queue — grouping, priority
// ordering, scheduling, overflow, expiry, dedup and call-hold storage. Contains no Android
// types so every scheduling rule is unit-testable on the JVM; the foreground service
// drives it from a single-writer scope later (ADR-002, ADR-005, ADR-019).
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(project(":core:common"))
    testImplementation(libs.junit)
}
