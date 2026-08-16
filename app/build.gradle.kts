import java.security.MessageDigest

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.secrets)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "com.example"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.aistudio.relevantor.gkmpxz"
        minSdk = 24
        targetSdk = 35
        versionCode = 3
        versionName = "2.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    
    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
            storeFile = file(keystorePath)
            storePassword = System.getenv("STORE_PASSWORD")
            keyAlias = "upload"
            keyPassword = System.getenv("KEY_PASSWORD")
        }
        create("debugConfig") {
            storeFile = file("${rootDir}/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
            enableV1Signing = true
            enableV2Signing = true
        }
    }

    buildTypes {
        release {
            isCrunchPngs = false
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            signingConfig = signingConfigs.getByName("debugConfig")
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    
    buildFeatures {
        compose = true
        buildConfig = true
    }
    
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

secrets {
    propertiesFileName = ".env"
    defaultPropertiesFileName = ".env.example"
}

dependencies {
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    implementation(platform(libs.androidx.compose.bom))
    implementation(platform(libs.firebase.bom))
    
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.converter.moshi)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.logging.interceptor)
    implementation(libs.moshi.kotlin)
    implementation(libs.okhttp)
    implementation(libs.retrofit)
    implementation("com.google.openlocationcode:openlocationcode:1.0.4")
    
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.core)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.androidx.work.testing)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
    
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.runner)
    
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

val buildDirectoryLayout = layout.buildDirectory
tasks.register("verifyApk") {
    val buildDir = buildDirectoryLayout.get().asFile
    val apkFile = File(buildDir, "outputs/apk/debug/app-debug.apk")
    val rootApkFile = File(project.rootDir, "app-debug.apk")
    inputs.file(apkFile)
    outputs.file(rootApkFile)

    doLast {
        if (apkFile.exists()) {
            val size = apkFile.length()
            val sha256 = MessageDigest.getInstance("SHA-256")
                .digest(apkFile.readBytes())
                .joinToString("") { b: Byte -> "%02x".format(b) }

            apkFile.copyTo(rootApkFile, overwrite = true)

            println("==========================================")
            println("       APK BUILD VERIFIED SUCCESSFULLY    ")
            println("==========================================")
            println("File Path:  ${apkFile.absolutePath}")
            println("Root Path:  ${rootApkFile.absolutePath}")
            println("File Size:  $size Bytes")
            println("SHA-256:    $sha256")
            println("Visibility: Copy to root folder successful!")
            println("==========================================")
        } else {
            throw GradleException("APK Verification Error: APK file not found at expected path: ${apkFile.absolutePath}")
        }
    }
}

tasks.matching { it.name == "assembleDebug" }.configureEach {
    finalizedBy("verifyApk")
}
dependencies {
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
}
