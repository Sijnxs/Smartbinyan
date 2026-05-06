plugins {
    id("com.android.application")
    kotlin("android")
    // ✅ Required for Firebase initialization
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.smartbinyan"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.smartbinyan"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "11"
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

    buildFeatures {
        viewBinding = true
    }

    // 🧩 Prevent duplicate META-INF files (for mail libs)
    packaging {
        resources {
            excludes += setOf(
                "META-INF/NOTICE.md",
                "META-INF/LICENSE.md",
                "META-INF/LICENSE",
                "META-INF/NOTICE"
            )
        }
    }
}

dependencies {
    // ✅ 🔥 Firebase (core + auth + realtime db + analytics + messaging)
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // ✅ Firestore + Storage (enabled & updated)
    implementation("com.google.firebase:firebase-firestore:25.0.0")
    implementation("com.google.firebase:firebase-storage:20.3.0")

    // ✅ Glide (for loading images from Firebase Storage)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // ✅ MQTT Client
    implementation("com.github.hannesa2:paho.mqtt.android:3.6.4")
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")

    // ✅ AndroidX + Material
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")

    // ✅ Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")

    // ✅ Chart library
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // ✅ JavaMail
    implementation("com.sun.mail:android-mail:1.6.7")
    implementation("com.sun.mail:android-activation:1.6.7")

    // ✅ Kotlin + Firebase extensions
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("com.google.firebase:firebase-common-ktx:21.0.0")

    // ✅ Desugaring for Java 8 APIs
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // ✅ Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
