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

dependencies {
    testImplementation(libs.junit)
}
