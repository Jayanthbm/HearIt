import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
}

// Pure Kotlin/JVM module: the first-launch setup flow — the nine steps, their skip rules,
// the prerequisites each step and Start require, and the persisted flow state. Contains no
// Android types so every gating rule and the state codec are unit-testable on the JVM; the
// step UI, permission probes and service start live in `:app` (ADR-023).
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    testImplementation(libs.junit)
}
