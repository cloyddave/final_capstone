plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.google.services)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.group5.safehomenotifier"
    compileSdk = 35

    defaultConfig {
        applicationId = "group5.safehomenotifier.com"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
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
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

        implementation ("androidx.navigation:navigation-compose:2.8.4")
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("androidx.compose.material:material-icons-extended:1.5.1")
    // implementation("androidx.credentials:credentials:1.5.0-beta01")
       // implementation("androidx.credentials:credentials-play-services-auth:1.5.0-beta01")
        implementation("androidx.datastore:datastore-preferences:1.0.0")
        implementation ("com.google.android.gms:play-services-auth:21.2.0")
        implementation ("com.google.firebase:firebase-auth:21.3.0")
        implementation ("androidx.credentials:credentials:<latest version>")
        implementation ("androidx.credentials:credentials-play-services-auth:<latest version>")
      //implementation ("com.google.firebase:firebase-auth:22.0.0")
        implementation("com.google.firebase:firebase-auth:23.1.0")
        implementation (platform("com.google.firebase:firebase-bom:32.0.0"))
        implementation("com.google.firebase:firebase-functions-ktx:20.1.1")
        implementation("io.coil-kt:coil-compose:2.4.0")
        implementation("androidx.compose.ui:ui:1.4.3")
        implementation("androidx.compose.material:material:1.5.0")
        implementation("androidx.compose.ui:ui-tooling-preview:1.5.0")
        implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
        implementation("androidx.activity:activity-compose:1.7.2")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
        implementation("com.squareup.retrofit2:retrofit:2.9.0")
        implementation("com.squareup.retrofit2:converter-gson:2.9.0")
        implementation(platform("com.google.firebase:firebase-bom:32.0.0"))
        implementation("com.google.firebase:firebase-firestore-ktx")
        implementation("com.google.firebase:firebase-messaging-ktx")






    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.androidx.storage)
    implementation(libs.androidx.espresso.core)
    implementation(libs.androidx.animation.core.lint)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
}