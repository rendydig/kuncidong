plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
}

android {
    namespace = "com.rendy.customlockscreen"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.rendy.customlockscreen"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "1.0.1"

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
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation("androidx.cardview:cardview:1.0.0")
    
    // Room database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    
    // Gson for JSON conversion
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // UI Enhancement Libraries
    implementation("com.airbnb.android:lottie:6.2.0") // Beautiful animations
    implementation("androidx.viewpager2:viewpager2:1.0.0") // Smooth swiping
    implementation("androidx.constraintlayout:constraintlayout:2.1.4") // MotionLayout
    implementation("com.facebook.shimmer:shimmer:0.5.0") // Loading effects
    implementation("de.hdodenhof:circleimageview:3.1.0") // Circular images
    implementation("jp.wasabeef:recyclerview-animators:4.0.2") // List animations
    
    // HTTP Client for downloading questions
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // Kid-friendly fonts and icons
    implementation("com.mikepenz:iconics-core:5.4.0") // Icon library
    implementation("com.mikepenz:iconics-views:5.4.0") // Icon views
    implementation("com.mikepenz:material-design-iconic-typeface:2.2.0.8-kotlin@aar") // Material icons
    implementation("com.mikepenz:fontawesome-typeface:5.9.0.2-kotlin@aar") // FontAwesome icons
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}