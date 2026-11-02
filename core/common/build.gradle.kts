import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
}

// Pure Kotlin/JVM module: shared domain models with no Android dependency,
// so business rules stay unit-testable without an emulator.
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

// Match the Kotlin target for the (empty) Java compilation too: without this, compileJava
// inherits the Gradle daemon's JDK target and Gradle rejects the inconsistent pair.
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    testImplementation(libs.junit)
}
