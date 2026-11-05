plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.assetcapturerig"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.assetcapturerig"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0-ai"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Strips any accidental newlines, quotes, or spaces from the GitHub Secret
        val apiKey = (System.getenv("DASHSCOPE_API_KEY") ?: "")
            .replace("\n", "")
            .replace("\r", "")
            .replace("\"", "")
            .trim()
        buildConfigField("String", "DASHSCOPE_API_KEY", "\"$apiKey\"")

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
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
        debug {
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    // Critical for MediaPipe / TFLite: prevents APK compression so the model can be memory-mapped
    androidResources {
        noCompress += listOf("tflite", "lite", "task")
    }

    packaging {
        resources {
            excludes += listOf(
                "META-INF/INDEX.LIST",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt"
            )
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

// Automatically downloads magic_touch.tflite into src/main/assets/ during build if missing
tasks.register("downloadMagicTouchModel") {
    doLast {
        val assetsDir = file("src/main/assets")
        if (!assetsDir.exists()) {
            assetsDir.mkdirs()
        }
        val modelFile = file("src/main/assets/magic_touch.tflite")
        if (!modelFile.exists() || modelFile.length() < 100_000L) {
            println("Downloading magic_touch.tflite from Google MediaPipe storage...")
            val modelUrl = "https://storage.googleapis.com/mediapipe-models/interactive_segmenter/magic_touch/float32/1/magic_touch.tflite"
            java.net.URI(modelUrl).toURL().openStream().use { input ->
                modelFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            println("✓ magic_touch.tflite downloaded (${modelFile.length() / 1024} KB)")
        } else {
            println("✓ magic_touch.tflite already present (${modelFile.length() / 1024} KB)")
        }
    }
}

tasks.named("preBuild") {
    dependsOn("downloadMagicTouchModel")
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Exact Sceneview & ARCore engine matching MainActivity.kt
    implementation("com.google.ar:core:1.41.0")
    implementation("io.github.sceneview:arsceneview:2.2.1")

    // MediaPipe Tasks Vision for InteractiveSegmenter (magic_touch.tflite)
    implementation("com.google.mediapipe:tasks-vision:0.10.14")

    // Networking for Modal Batch Upload
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
