plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.signtrack"
    compileSdk = 33 // API level 33

    defaultConfig {
        applicationId = "com.example.signtrack"
        minSdk = 29
        targetSdk = 33 // API level 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8 // Gunakan Java 8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    // AndroidX Libraries
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // CameraX dependencies
    implementation("androidx.camera:camera-core:1.2.0") // CameraX core
    implementation("androidx.camera:camera-camera2:1.2.0") // Camera2 (untuk CameraX)
    implementation("androidx.camera:camera-lifecycle:1.2.0") // CameraX lifecycle support

    // Testing dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}