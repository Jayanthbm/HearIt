plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.jayanth.hearit.core.notifications"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 29
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(project(":core:common"))
    testImplementation(libs.junit)
}
