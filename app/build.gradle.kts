plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.smartnotificationmanager"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.smartnotificationmanager"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    packaging {
        resources {
            excludes.add("META-INF/DEPENDENCIES")
        }
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
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Google API client libraries
    implementation("com.google.api-client:google-api-client-android:1.32.2")
    implementation("com.google.api-client:google-api-client-gson:1.31.2")
    // Google Docs API client library
    implementation("com.google.apis:google-api-services-docs:v1-rev61-1.25.0")
    // Google OAuth2 library
    implementation("com.google.android.gms:play-services-auth:21.0.0")

    implementation("com.google.http-client:google-http-client-android:1.38.1")
    implementation("com.google.auth:google-auth-library-oauth2-http:1.20.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4") // Check for the latest version
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4") // Check for the latest version

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}