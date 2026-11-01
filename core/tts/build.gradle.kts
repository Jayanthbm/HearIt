import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
}

// Pure Kotlin/JVM module: the announcement-template builder and the TextToSpeechEngine
// contract. Contains no Android types so template output and the contract's invariants are
// unit-testable on the JVM; the Android TextToSpeech wrapper satisfies this contract later
// (ADR-002, ADR-018).
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(project(":core:common"))
    testImplementation(libs.junit)
}
