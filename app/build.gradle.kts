plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.mrpsp.assetapp"
    compileSdk = 35

    defaultConfig {
        implementation("androidx.activity:activity-ktx:1.10.1")
implementation("androidx.core:core-ktx:1.15.0")
        applicationId = "com.mrpsp.assetapp"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}
