plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.ipcbanking"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.ipcbanking"
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
    implementation("com.cloudinary:cloudinary-android:2.4.0") {
        exclude(group = "com.facebook.fresco")
    }
    implementation("com.facebook.fresco:fresco:3.2.0")
    implementation("com.facebook.fresco:imagepipeline:3.2.0")
    implementation("com.facebook.fresco:imagepipeline-okhttp3:3.2.0")
    implementation("com.facebook.fresco:animated-gif:3.2.0")
    implementation("com.facebook.fresco:webpsupport:3.2.0")
    implementation("com.facebook.fresco:animated-webp:3.2.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // [GOOGLE MAPS] Thêm dòng này
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.recyclerview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}