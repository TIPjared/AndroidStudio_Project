plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.finalprojectmobilecomputing"
    compileSdk = 35

    signingConfigs {
        create("release") {
            storeFile = file("C:/Users/johnj/keystores/Sikad-release.jks")
            storePassword = "Sikadpassword!"
            keyAlias = "Sikad-key-alias"
            keyPassword = "Sikadpassword!"
            storeType = "JKS"
        }
    }

    defaultConfig {
        applicationId = "com.example.finalprojectmobilecomputing"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
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
configurations.all {
    resolutionStrategy {
        force ("com.google.firebase:firebase-common:21.0.0")
    }
}
dependencies {

    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    // For REST API calls
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("com.squareup.okhttp3:logging-interceptor:5.0.0-alpha.2")
    // Image loading library
    implementation ("com.squareup.picasso:picasso:2.8")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.auth)
    implementation ("com.google.firebase:firebase-firestore:25.0.0")
    implementation ("com.google.firebase:firebase-auth:24.0.1")
    implementation ("com.google.firebase:firebase-common:21.0.0")
    implementation ("com.google.firebase:firebase-analytics:21.1.0")
    implementation ("com.google.android.material:material:1.11.0") // or latest
    implementation ("androidx.browser:browser:1.3.0")
    implementation("com.google.android.gms:play-services-auth:20.0.1")
    implementation ("com.google.android.gms:play-services-maps:18.0.2")
    implementation ("com.google.android.gms:play-services-location:17.0.0")
    implementation ("com.google.guava:guava:31.1-android")

    //Cameras
    implementation ("androidx.camera:camera-core:1.3.0")
    implementation ("androidx.camera:camera-camera2:1.3.0")
    implementation ("androidx.camera:camera-lifecycle:1.3.0")
    implementation ("androidx.camera:camera-view:1.3.0")
    // ML Kit Barcode Scanning
    implementation ("com.google.mlkit:barcode-scanning:17.1.0")
    implementation(libs.firebase.inappmessaging)
    implementation(libs.firebase.database)
    implementation(libs.firebase.storage)

    implementation ("com.google.maps.android:android-maps-utils:2.3.0")
    // Firebase Auth
    implementation ("com.google.firebase:firebase-auth:22.1.1")

    // Firestore
    implementation ("com.google.firebase:firebase-firestore:24.9.0")

    // OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.11.0")

    // Gson (if using for JSON serialization)
    implementation("com.google.code.gson:gson:2.10.1")

    // AI Dependency

    implementation(libs.generativeai)


    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}