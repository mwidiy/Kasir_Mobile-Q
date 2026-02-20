import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.kasir"
    compileSdk {
        version = release(36)
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.example.kasir"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Load API_BASE_URL from local.properties
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(FileInputStream(localPropertiesFile))
        }
        var apiBaseUrl = localProperties.getProperty("API_BASE_URL") ?: "http://192.168.1.4:3000/"
        apiBaseUrl = apiBaseUrl.replace("\"", "")
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")

        // Dynamic PWA_BASE_URL: Derive from API_BASE_URL if not set
        var pwaBaseUrl = localProperties.getProperty("PWA_BASE_URL")
        if (pwaBaseUrl == null) {
             // Default to API URL but switch port 3000 -> 3001
             pwaBaseUrl = apiBaseUrl.replace(":3000", ":3001")
        }
        pwaBaseUrl = pwaBaseUrl.replace("\"", "")
        buildConfigField("String", "PWA_BASE_URL", "\"$pwaBaseUrl\"")

        // Load WEB_CLIENT_ID for Google Login
        var webClientId = localProperties.getProperty("WEB_CLIENT_ID") ?: ""
        webClientId = webClientId.replace("\"", "")
        buildConfigField("String", "WEB_CLIENT_ID", "\"$webClientId\"")
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
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    
    // CameraX
    implementation("androidx.camera:camera-core:1.3.1")
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")
    implementation("com.google.accompanist:accompanist-permissions:0.34.0") // Useful for permissions

    // Retrofit & Gson
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
    implementation("androidx.lifecycle:lifecycle-process:2.8.0")
    implementation("io.socket:socket.io-client:2.1.0")
    implementation("io.coil-kt:coil-compose:2.5.0")

    // QR Code Generator
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.google.zxing:core:3.5.2")

    // Credential Manager (Google Sign In)
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.0")
    
    // Legacy Google Sign In (Debug Savior)
    implementation("com.google.android.gms:play-services-auth:21.0.0")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Security Crypto
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
}