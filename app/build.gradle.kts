plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.signtrack"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.signtrack"
        minSdk = 29
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)



//    //Core CameraX Library
////    implementation("androidx.camera:camera-core:1.3.1")
////    implementation("androidx.camera:camera-camera2:1.3.1")
////    implementation("androidx.camera:camera-lifecycle:1.3.1")
////    implementation("androidx.camera:camera-view:1.3.1")
////
////    // CameraX PreviewView
////    implementation("androidx.camera:camera-view:1.3.1")

    //Tensorflow-Lite Library
    implementation("org.tensorflow:tensorflow-lite:2.12.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.3")
    implementation("org.tensorflow:tensorflow-lite-select-tf-ops:2.12.0")
    implementation("com.google.guava:guava:31.1-android")

    implementation(project(":opencv"))
    implementation("org.tensorflow:tensorflow-lite:2.8.0")


}