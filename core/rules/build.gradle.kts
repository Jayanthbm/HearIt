import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
}

// Pure Kotlin/JVM module: deterministic announcement-eligibility pipeline.
// Contains no Android types so every rule can be tested on the JVM.
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

// Keep Java and Kotlin targets aligned (see :core:common).
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    implementation(project(":core:common"))
    testImplementation(libs.junit)
}
