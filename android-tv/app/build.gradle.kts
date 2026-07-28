plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val media3 = "1.4.1"
val ffmpegAar = layout.projectDirectory.file("libs/media3-decoder-ffmpeg.aar").asFile

if (!ffmpegAar.isFile) {
    throw GradleException(
        "缺少 Media3 FFmpeg 扩展：${ffmpegAar.path}。请按 app/libs/README.md 准备该文件。"
    )
}

android {
    namespace = "com.homektv.tv"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "com.homektv.tv"
        minSdk = 29          // Android 10（PRD§9：TV 兼容 Android 10+）
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    val releaseKeystore = providers.environmentVariable("KTV_TV_KEYSTORE").orNull
    val releaseStorePassword = providers.environmentVariable("KTV_TV_STORE_PASSWORD").orNull
    val releaseKeyAlias = providers.environmentVariable("KTV_TV_KEY_ALIAS").orNull
    val releaseKeyPassword = providers.environmentVariable("KTV_TV_KEY_PASSWORD").orNull
    val releaseSigning = if (
        releaseKeystore != null && releaseStorePassword != null &&
        releaseKeyAlias != null && releaseKeyPassword != null
    ) {
        signingConfigs.create("release") {
            storeFile = rootProject.file(releaseKeystore)
            storePassword = releaseStorePassword
            keyAlias = releaseKeyAlias
            keyPassword = releaseKeyPassword
        }
    } else null

    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = false
            if (releaseSigning != null) signingConfig = releaseSigning
        }
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // AndroidX 基础
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")

    // Leanback（TV 启动器兼容，详设§12.2）
    implementation("androidx.leanback:leanback:1.0.0")

    // Media3 / ExoPlayer（P1.28 播放引擎）
    implementation("androidx.media3:media3-exoplayer:$media3")
    implementation("androidx.media3:media3-ui:$media3")
    implementation("androidx.media3:media3-common:$media3")
    implementation("androidx.media3:media3-datasource-okhttp:$media3")
    implementation(files(ffmpegAar))

    // WebSocket + JSON（P1.27）
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // 协程
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    testImplementation("junit:junit:4.13.2")
}
