plugins {
    alias(libs.plugins.android.application)
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.civicreportapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.civicreportapp"
        minSdk = 31
        targetSdk = 36
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.play.services.location)
    implementation(libs.firebase.database)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.google.android.libraries.places:places:3.3.0")
    implementation("com.google.firebase:firebase-firestore:24.10.3")
    implementation(platform("com.google.firebase:firebase-bom:34.0.0"))
    implementation("com.google.firebase:firebase-analytics")
    // Firebase Storage
    implementation("com.google.firebase:firebase-storage:20.3.0")

    // Firebase Core (optional but recommended)
    implementation("com.google.firebase:firebase-analytics:21.6.1")


}