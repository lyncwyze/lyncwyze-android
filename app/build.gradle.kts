import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
}

val app_home_by_provider = gradleLocalProperties(rootDir, providers).getProperty("app_home_by_provider", "false")
val profile_setup_2_isbackgroundverification = gradleLocalProperties(rootDir, providers).getProperty("profile_setup_2_isbackgroundverification", "false")
val app_key_a = gradleLocalProperties(rootDir, providers).getProperty("app_key_a", "")
val defaultCountryCode = gradleLocalProperties(rootDir, providers).getProperty("defaultCountryCode", "")
val isValidOtp = gradleLocalProperties(rootDir, providers).getProperty("isValidOtp", "false")


android {
    namespace = "com.intelly.lyncwyze"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.intelly.lyncwyze"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        resValue("string", "app_home_by_provider", app_home_by_provider)
        resValue("string", "profile_setup_2_isbackgroundverification", profile_setup_2_isbackgroundverification)
        resValue("string", "app_key_a", app_key_a)
        resValue("string", "defaultCountryCode", defaultCountryCode)
        resValue("string", "isValidOtp", isValidOtp)
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
    buildFeatures {
        compose = true
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.appcompat)
    // Library For checking update from play store
    implementation(libs.play.core)

//    implementation(libs.com.google.android.material)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.firebase.messaging)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)


    implementation("com.auth0.android:jwtdecode:2.0.2")

    // Retrofit for API calling
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // OkHttp for request/response interception
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.10.0")

    // JSON <--> Object
    implementation("com.google.code.gson:gson:2.11.0")

    // Alerts
    implementation("com.github.f0ris.sweetalert:library:1.6.2")

    // country code and phone number
    implementation ("com.hbb20:ccp:2.6.1")

    // List
    implementation ("com.github.bumptech.glide:glide:4.13.0")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.13.0")

    // Coroutine dependencies
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")

    // Lifecycle dependencies (for lifecycleScope)
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1") // Latest version available

    // Logger
    implementation("io.github.microutils:kotlin-logging-jvm:2.0.11") // Kotlin-Logging
    implementation("org.slf4j:slf4j-android:1.7.32") // SLF4J for Android

    // Location
    implementation("com.google.android.gms:play-services-location:21.1.0")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1") // Or latest version

}