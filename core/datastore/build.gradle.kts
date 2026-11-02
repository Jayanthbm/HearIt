plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.jayanth.hearit.core.datastore"
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
    implementation(project(":core:setup"))
    implementation(project(":core:tts"))
    implementation(libs.androidx.datastore.preferences)
    testImplementation(libs.junit)
}
