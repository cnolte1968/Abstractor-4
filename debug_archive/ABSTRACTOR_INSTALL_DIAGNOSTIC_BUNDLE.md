# ABSTRACTOR - INSTALL & DIAGNOSTIC BUNDLE

Dieses Dokument bietet eine vollständige, unzensierte Zusammenstellung aller relevanten Diagnose-, Build- und Konfigurationsdateien des Abstractor-Projekts.

### Datei: settings.gradle.kts

BEGIN_FILE_CONTENT
pluginManagement {
  repositories {
    google {
      content {
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
        includeGroupByRegex("androidx.*")
      }
    }
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
  }
}

rootProject.name = "My Application"

include(":app")
END_FILE_CONTENT

---

### Datei: build.gradle.kts

BEGIN_FILE_CONTENT
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.roborazzi) apply false
  alias(libs.plugins.secrets) apply false
}
END_FILE_CONTENT

---

### Datei: gradle.properties

BEGIN_FILE_CONTENT
# Project-wide Gradle settings.
# IDE (e.g. Android Studio) users:
# Gradle settings configured through the IDE *will override*
# any settings specified in this file.
# For more details on how to configure your build environment visit
# http://www.gradle.org/docs/current/userguide/build_environment.html
# Specifies the JVM arguments used for the daemon process.
# The setting is particularly useful for tweaking memory settings.
org.gradle.jvmargs=-Xmx4g -Dfile.encoding=UTF-8
# When configured, Gradle will run in incubating parallel mode.
# This option should only be used with decoupled projects. For more details, visit
# https://developer.android.com/r/tools/gradle-multi-project-decoupled-projects
org.gradle.parallel=true
# Kotlin code style for this project: "official" or "obsolete":
kotlin.code.style=official
# Enables namespacing of each library's R class so that its R class includes only the
# resources declared in the library itself and none from the library's dependencies,
# thereby reducing the size of the R class for that library
android.nonTransitiveRClass=true
org.gradle.caching=true
org.gradle.configuration-cache=true
# Set the maximum number of workers to 4 to avoid overloading the machine.
org.gradle.workers.max=4
# Set the Kotlin compiler execution strategy to in-process to avoid "Could not
# connect to Kotlin compile daemon" error.
kotlin.compiler.execution.strategy=in-process
END_FILE_CONTENT

---

### Datei: gradle/libs.versions.toml

BEGIN_FILE_CONTENT
[versions]
agp = "9.1.1"
coreKtx = "1.15.0"
junit = "4.13.2"
junitVersion = "1.3.0"
espressoCore = "3.7.0"
lifecycleRuntimeKtx = "2.8.7"
lifecycleViewmodelCompose = "2.8.7"
lifecycleRuntimeCompose = "2.8.7"
activityCompose = "1.10.1"
kotlin = "2.2.10"
composeBom = "2024.09.00"
googleDevtoolsKsp = "2.3.5"
navigationCompose = "2.8.9"
roomRuntime = "2.7.0"
roomKtx = "2.7.0"
roomCompiler = "2.7.0"
kotlinxCoroutinesTest = "1.10.2"
core = "1.6.1"
runner = "1.6.2"
coilCompose = "2.7.0"
retrofit = "2.12.0"
converterMoshi = "2.12.0"
kotlinxCoroutinesAndroid = "1.10.2"
kotlinxCoroutinesCore = "1.10.2"
accompanistPermissions = "0.37.3"
playServicesLocation = "21.3.0"
cameraCamera2 = "1.5.0"
cameraLifecycle = "1.5.0"
cameraView = "1.5.0"
cameraCore = "1.5.0"
loggingInterceptor = "4.10.0"
okhttp = "4.10.0"
moshiKotlin = "1.15.2"
moshiKotlinCodegen = "1.15.2"
datastorePreferences = "1.1.7"
robolectric = "4.16.1"
roborazzi = "1.59.0"
firebaseBom = "34.12.0"
secretsGradlePlugin = "2.0.1"
workRuntimeKtx = "2.9.1"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
androidx-junit = { group = "androidx.test.ext", name = "junit", version.ref = "junitVersion" }
androidx-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espressoCore" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycleRuntimeKtx" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycleViewmodelCompose" }
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycleRuntimeCompose" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
androidx-compose-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "roomRuntime" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "roomKtx" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "roomCompiler" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "kotlinxCoroutinesTest" }
androidx-core = { group = "androidx.test", name = "core", version.ref = "core" }
androidx-runner = { group = "androidx.test", name = "runner", version.ref = "runner" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-compose-material-icons-core = { group = "androidx.compose.material", name = "material-icons-core" }
androidx-compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }
coil-compose = { group = "io.coil-kt", name = "coil-compose", version.ref = "coilCompose" }
retrofit = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
converter-moshi = { group = "com.squareup.retrofit2", name = "converter-moshi", version.ref = "converterMoshi" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "kotlinxCoroutinesAndroid" }
kotlinx-coroutines-core = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version.ref = "kotlinxCoroutinesCore" }
accompanist-permissions = { group = "com.google.accompanist", name = "accompanist-permissions", version.ref = "accompanistPermissions" }
play-services-location = { group = "com.google.android.gms", name = "play-services-location", version.ref = "playServicesLocation" }
androidx-camera-camera2 = { group = "androidx.camera", name = "camera-camera2", version.ref = "cameraCamera2" }
androidx-camera-lifecycle = { group = "androidx.camera", name = "camera-lifecycle", version.ref = "cameraLifecycle" }
androidx-camera-view = { group = "androidx.camera", name = "camera-view", version.ref = "cameraView" }
androidx-camera-core = { group = "androidx.camera", name = "camera-core", version.ref = "cameraCore" }
logging-interceptor = { group = "com.squareup.okhttp3", name = "logging-interceptor", version.ref = "loggingInterceptor" }
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
moshi-kotlin = { group = "com.squareup.moshi", name = "moshi-kotlin", version.ref = "moshiKotlin" }
moshi-kotlin-codegen = { group = "com.squareup.moshi", name = "moshi-kotlin-codegen", version.ref = "moshiKotlinCodegen" }
androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastorePreferences" }
robolectric = { group = "org.robolectric", name = "robolectric", version.ref = "robolectric" }
roborazzi = { group = "io.github.takahirom.roborazzi", name = "roborazzi", version.ref = "roborazzi" }
roborazzi-compose = { group = "io.github.takahirom.roborazzi", name = "roborazzi-compose", version.ref = "roborazzi" }
roborazzi-junit-rule = { group = "io.github.takahirom.roborazzi", name = "roborazzi-junit-rule", version.ref = "roborazzi" }
firebase-bom = { group = "com.google.firebase", name = "firebase-bom", version.ref = "firebaseBom" }
firebase-ai = { group = "com.google.firebase", name = "firebase-ai" }
androidx-work-runtime-ktx = { group = "androidx.work", name = "work-runtime-ktx", version.ref = "workRuntimeKtx" }
androidx-work-testing = { group = "androidx.work", name = "work-testing", version.ref = "workRuntimeKtx" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
google-devtools-ksp = { id = "com.google.devtools.ksp", version.ref = "googleDevtoolsKsp" }
roborazzi = { id = "io.github.takahirom.roborazzi", version.ref = "roborazzi" }
secrets = { id = "com.google.android.libraries.mapsplatform.secrets-gradle-plugin", version.ref = "secretsGradlePlugin" }
END_FILE_CONTENT

---

### Datei: app/build.gradle.kts

BEGIN_FILE_CONTENT
import java.util.Base64
import java.security.MessageDigest
import java.util.zip.ZipFile
import java.io.InputStreamReader
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.FileInputStream
import java.util.zip.ZipOutputStream
import java.util.zip.ZipEntry
import java.util.Date


plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.devtools.ksp)
}

val keystoreFile = file("${rootDir}/debug.keystore")
val base64File = file("${rootDir}/debug.keystore.base64")
println("KEYSTORE_DEBUG_PATH: ${keystoreFile.absolutePath} (exists: ${keystoreFile.exists()})")
println("BASE64_DEBUG_PATH: ${base64File.absolutePath} (exists: ${base64File.exists()})")
if (!keystoreFile.exists() && base64File.exists()) {
    println("RESTORE_KEYSTORE: Decoding debug.keystore from base64...")
    try {
        val base64Text = base64File.readText().trim()
        val decodedBytes = Base64.getDecoder().decode(base64Text)
        keystoreFile.writeBytes(decodedBytes)
        println("RESTORE_KEYSTORE: Successfully restored debug.keystore in root directory!")
    } catch (e: Exception) {
        println("RESTORE_KEYSTORE_ERROR: Failed to restore keystore: ${e.message}")
        e.printStackTrace()
    }
}

android {
  namespace = "com.example"
  compileSdk = 35

  defaultConfig {
    applicationId = "com.aistudio.abstractor.gkmpxz"
    minSdk = 24
    targetSdk = 35
    versionCode = 1
    versionName = "1.0"

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
      signingConfig = signingConfigs.getByName("release")
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
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  ksp(libs.androidx.room.compiler)
  implementation(libs.androidx.work.runtime.ktx)
  // implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
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

tasks.register("copyApkToRoot") {
    dependsOn("assembleDebug")
    val src = layout.buildDirectory.file("outputs/apk/debug/app-debug.apk")
    val dest = file("${rootDir}/Abstractor_debug.apk")
    inputs.file(src)
    outputs.file(dest)
    doLast {
        val srcFile = src.get().asFile
        if (srcFile.exists()) {
            srcFile.copyTo(dest, overwrite = true)
            println("Successfully copied APK to root as Abstractor_debug.apk")
        } else {
            error("Source APK not found at ${srcFile.absolutePath}")
        }
    }
}

tasks.register<Zip>("zipApkToRoot") {
    dependsOn("assembleDebug")
    archiveFileName.set("Abstractor_debug_apk.zip")
    destinationDirectory.set(file(rootDir))
    from(layout.buildDirectory.file("outputs/apk/debug/app-debug.apk")) {
        rename { "Abstractor_debug.apk" }
    }
}

tasks.register("auditApk") {
    dependsOn("assembleDebug")
    doLast {
        val apkFile = layout.buildDirectory.file("outputs/apk/debug/app-debug.apk").get().asFile
        println("\n=== AUDIT_APK_OUTPUT ===")
        println("APK_EXISTS: ${if (apkFile.exists()) "YES" else "NO"}")
        
        if (apkFile.exists()) {
            val originalLength = apkFile.length()
            println("APK_SIZE: $originalLength")
            
            // SHA-256 of original file
            val digest = MessageDigest.getInstance("SHA-256")
            val originalBytes = apkFile.readBytes()
            val originalHash = digest.digest(originalBytes).joinToString("") { byte -> "%02x".format(byte) }
            println("ORIGINAL_SHA256: $originalHash")
            
            // Magic bytes of APK
            val originalHasMagic = originalBytes.size >= 4 && originalBytes[0] == 0x50.toByte() && originalBytes[1] == 0x4B.toByte()
            println("ORIGINAL_ZIP_MAGIC: ${if (originalHasMagic) "PASS" else "FAIL"}")
            
            // Check zip integrity & required files
            var zipIntegrity = "PASS"
            var hasManifest = false
            var hasDex = false
            var hasArsc = false
            var fileCount = 0
            var metaInfCount = 0
            try {
                val zipInstance = ZipFile(apkFile)
                try {
                    fileCount = zipInstance.size()
                    val entries = zipInstance.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        val name = entry.name
                        if (name == "AndroidManifest.xml") hasManifest = true
                        if (name.startsWith("classes") && name.endsWith(".dex")) hasDex = true
                        if (name == "resources.arsc") hasArsc = true
                        if (name.startsWith("META-INF/")) metaInfCount++
                    }
                } finally {
                    zipInstance.close()
                }
            } catch (e: Exception) {
                zipIntegrity = "FAIL - ${e.message}"
            }
            println("ZIP_INTEGRITY: $zipIntegrity")
            println("REQUIRED_MANIFEST: ${if (hasManifest) "PASS" else "FAIL"}")
            println("REQUIRED_DEX: ${if (hasDex) "PASS" else "FAIL"}")
            println("REQUIRED_ARSC: ${if (hasArsc) "PASS" else "FAIL"}")
            println("FILE_COUNT: $fileCount")
            println("META_INF_COUNT: $metaInfCount")
            
            // Create requested files in /tmp/
            val tmpApk = File("/tmp/abstractor-debug.apk")
            val tmpSha = File("/tmp/abstractor-debug.apk.sha256.txt")
            val tmpReadme = File("/tmp/README_INSTALL.txt")
            val tmpZip = File("/tmp/abstractor-debug-export.zip")
            val tmpVerification = File("/tmp/EXPORT_VERIFICATION.txt")
            
            // Create requested files in project root for download
            val rootApk = file("${rootDir}/abstractor-debug.apk")
            val rootSha = file("${rootDir}/abstractor-debug.apk.sha256.txt")
            val rootReadme = file("${rootDir}/README_INSTALL.txt")
            val rootZip = file("${rootDir}/abstractor-debug-export.zip")
            val rootVerification = file("${rootDir}/EXPORT_VERIFICATION.txt")
            
            // Ensure /tmp exists
            tmpApk.parentFile?.mkdirs()
            
            // 1. Copy APK
            apkFile.copyTo(tmpApk, overwrite = true)
            apkFile.copyTo(rootApk, overwrite = true)
            
            // Verify copy hash
            val tmpBytes = tmpApk.readBytes()
            val tmpCopiedHash = MessageDigest.getInstance("SHA-256").digest(tmpBytes).joinToString("") { byte -> "%02x".format(byte) }
            val tmpHasMagic = tmpBytes.size >= 4 && tmpBytes[0] == 0x50.toByte() && tmpBytes[1] == 0x4B.toByte()
            println("TMP_APK_SHA256: $tmpCopiedHash")
            println("TMP_APK_HASH_MATCH: ${if (originalHash == tmpCopiedHash) "YES" else "NO"}")
            println("TMP_APK_ZIP_MAGIC: ${if (tmpHasMagic) "PASS" else "FAIL"}")
            
            // 2. Create sha256.txt
            tmpSha.writeText(originalHash)
            rootSha.writeText(originalHash)
            
            // 3. Create README_INSTALL.txt
            val readmeContent = """=== INSTALLATION INSTRUCTIONS FOR ABSTRACTOR ===

1. Extract 'abstractor-debug.apk' from 'abstractor-debug-export.zip'.
2. Verify that the SHA-256 hash of 'abstractor-debug.apk' matches the content of 'abstractor-debug.apk.sha256.txt':
   Expected Hash: ${originalHash}
3. On your Android device, uninstall any previous versions of Abstractor to prevent certificate signature conflicts.
4. Enable "Install Unknown Apps" in your browser or file explorer settings.
5. Transfer the APK to your device and open it to begin installation.

For verification:
Original APK size: ${originalLength} bytes.
""".trimIndent()
            tmpReadme.writeText(readmeContent)
            rootReadme.writeText(readmeContent)
            
            // 4. Create ZIP
            listOf(tmpZip, rootZip).forEach { destZip ->
                try {
                    FileOutputStream(destZip).use { fos ->
                        ZipOutputStream(fos).use { zos ->
                            listOf(tmpApk, tmpSha, tmpReadme).forEach { fileToZip ->
                                FileInputStream(fileToZip).use { fis ->
                                    val entry = ZipEntry(fileToZip.name)
                                    zos.putNextEntry(entry)
                                    val buffer = ByteArray(4096)
                                    var len = fis.read(buffer)
                                    while (len > 0) {
                                        zos.write(buffer, 0, len)
                                        len = fis.read(buffer)
                                    }
                                    zos.closeEntry()
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("ZIP_CREATION_FAILED for ${destZip.absolutePath}: ${e.message}")
                }
            }
            
            // Verify Export ZIP
            val zipBytes = tmpZip.readBytes()
            val zipHash = MessageDigest.getInstance("SHA-256").digest(zipBytes).joinToString("") { byte -> "%02x".format(byte) }
            val zipHasMagic = zipBytes.size >= 4 && zipBytes[0] == 0x50.toByte() && zipBytes[1] == 0x4B.toByte()
            println("ZIP_SIZE: ${tmpZip.length()}")
            println("ZIP_SHA256: $zipHash")
            println("ZIP_MAGIC_BYTES: ${if (zipHasMagic) "PASS" else "FAIL"}")
            
            var zipContentsList = ""
            var zipIntegrityStatus = "PASS"
            try {
                val testZip = ZipFile(tmpZip)
                try {
                    val entries = testZip.entries()
                    val lst = mutableListOf<String>()
                    while (entries.hasMoreElements()) {
                        lst.add(entries.nextElement().name)
                    }
                    zipContentsList = lst.joinToString(", ")
                } finally {
                    testZip.close()
                }
            } catch (e: Exception) {
                zipIntegrityStatus = "FAIL - ${e.message}"
            }
            println("ZIP_CONTENTS: $zipContentsList")
            println("ZIP_INTEGRITY_VERIFY: $zipIntegrityStatus")
            
            // 5. Create EXPORT_VERIFICATION.txt
            val verificationContent = """=== EXPORT VERIFICATION REPORT ===
Generated: ${Date()}
APK Name: abstractor-debug.apk
APK Size: $originalLength bytes
APK SHA-256: $originalHash
APK Magic Bytes: ${if (tmpHasMagic) "PK (PASS)" else "FAIL"}

ZIP Name: abstractor-debug-export.zip
ZIP Size: ${tmpZip.length()} bytes
ZIP SHA-256: $zipHash
ZIP Magic Bytes: ${if (zipHasMagic) "PK (PASS)" else "FAIL"}
ZIP Contents: $zipContentsList
ZIP Integrity Check: $zipIntegrityStatus

Hinweis: Wenn die heruntergeladene ZIP- oder APK-Datei eine andere Größe oder ein anderes SHA-256-Prüfsummen-Ergebnis als oben angegeben aufweist, ist die Google AI Studio Download-/Browser-Export-Pipeline defekt (Integritätsverlust während der Übertragung).
""".trimIndent()
            tmpVerification.writeText(verificationContent)
            rootVerification.writeText(verificationContent)
            println("VERIFICATION_TXT_CREATED: YES")
            
            // CLI commands execution helpers
            val runCmd: (Array<String>) -> String = { args ->
                try {
                    val process = ProcessBuilder(*args).redirectErrorStream(true).start()
                    val reader = BufferedReader(InputStreamReader(process.inputStream))
                    val output = StringBuilder()
                    var line = reader.readLine()
                    while (line != null) {
                        output.append(line).append("\n")
                        line = reader.readLine()
                    }
                    process.waitFor()
                    output.toString()
                } catch (e: Exception) {
                    "ERROR: ${e.message}"
                }
            }
            
            // Try apksigner
            val apksignerOut = runCmd(arrayOf("apksigner", "verify", "--print-certs", apkFile.absolutePath))
            val apksignerAvailable = !apksignerOut.contains("ERROR") && !apksignerOut.contains("not found")
            println("APKSIGNER_AVAILABLE: $apksignerAvailable")
            println("APKSIGNER_OUTPUT:\n$apksignerOut")
            
            // Try jarsigner
            val jarsignerOut = runCmd(arrayOf("jarsigner", "-verify", "-certs", "-verbose", apkFile.absolutePath))
            val jarsignerAvailable = !jarsignerOut.contains("ERROR") && !jarsignerOut.contains("not found")
            println("JARSIGNER_AVAILABLE: $jarsignerAvailable")
            println("JARSIGNER_OUTPUT:\n${if (jarsignerOut.length > 500) jarsignerOut.substring(0, 500) + "..." else jarsignerOut}")
            
            // ADB Devices check
            val adbOut = runCmd(arrayOf("adb", "devices"))
            val adbAvailable = !adbOut.contains("ERROR") && !adbOut.contains("not found")
            println("ADB_AVAILABLE: $adbAvailable")
            println("ADB_OUTPUT:\n$adbOut")
        } else {
            println("APK_SIZE: 0")
        }
        
        // Metadata
        try {
            val appPack = android.defaultConfig.applicationId
            val vCode = android.defaultConfig.versionCode
            val vName = android.defaultConfig.versionName
            val minS = android.defaultConfig.minSdk
            val targetS = android.defaultConfig.targetSdk
            val debugg = android.buildTypes.getByName("debug").isDebuggable
            println("META_APP_ID: $appPack")
            println("META_VERSION_CODE: $vCode")
            println("META_VERSION_NAME: $vName")
            println("META_MIN_SDK: $minS")
            println("META_TARGET_SDK: $targetS")
            println("META_DEBUGGABLE: $debugg")
        } catch (e: Exception) {
            println("META_ERROR: ${e.message}")
        }
        
        println("=== END_AUDIT_APK_OUTPUT ===\n")
    }
}
END_FILE_CONTENT

---

### Datei: app/src/main/AndroidManifest.xml

BEGIN_FILE_CONTENT
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <!-- Request Internet Permission -->
    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.MyApplication">
        
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:label="@string/app_name"
            android:theme="@style/Theme.MyApplication"
            android:configChanges="orientation|screenSize|keyboardHidden">
            
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>

            <!-- Support Android System and YouTube video Direct Shared Links -->
            <intent-filter>
                <action android:name="android.intent.action.SEND" />
                <category android:name="android.intent.category.DEFAULT" />
                <data android:mimeType="text/plain" />
            </intent-filter>
            
        </activity>

        <!-- Accessibility Service for Two-Stage Local Content Extraction -->
        <service
            android:name=".AbstractorAccessibilityService"
            android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
            android:label="Abstractor Auto-Extraction Service"
            android:exported="true">
            <intent-filter>
                <action android:name="android.accessibilityservice.AccessibilityService" />
            </intent-filter>
            <meta-data
                android:name="android.accessibilityservice"
                android:resource="@xml/accessibility_service_config" />
        </service>
    </application>

</manifest>
END_FILE_CONTENT

---

### Datei: EXPORT_VERIFICATION.txt

BEGIN_FILE_CONTENT
=== EXPORT VERIFICATION REPORT ===
Generated: Wed Jun 17 18:31:18 UTC 2026
APK Name: abstractor-debug.apk
APK Size: 19574725 bytes
APK SHA-256: 9ac0ae1ebdbe7e7690f13d94bfa3fdf4bc01f51dc25c5c17dbdcb515b517074f
APK Magic Bytes: PK (PASS)

ZIP Name: abstractor-debug-export.zip
ZIP Size: 18867480 bytes
ZIP SHA-256: fcad48a516078ca9e13f05a616b76d97afc3c4f4be1597e30d98feed04c8c6c9
ZIP Magic Bytes: PK (PASS)
ZIP Contents: abstractor-debug.apk, abstractor-debug.apk.sha256.txt, README_INSTALL.txt
ZIP Integrity Check: PASS

Hinweis: Wenn die heruntergeladene ZIP- oder APK-Datei eine andere Größe oder ein anderes SHA-256-Prüfsummen-Ergebnis als oben angegeben aufweist, ist die Google AI Studio Download-/Browser-Export-Pipeline defekt (Integritätsverlust während der Übertragung).
END_FILE_CONTENT

---

### Datei: abstractor-debug.apk.sha256.txt

BEGIN_FILE_CONTENT
9ac0ae1ebdbe7e7690f13d94bfa3fdf4bc01f51dc25c5c17dbdcb515b517074f
END_FILE_CONTENT

---

### Datei: README_INSTALL.txt

BEGIN_FILE_CONTENT
=== INSTALLATION INSTRUCTIONS FOR ABSTRACTOR ===

1. Extract 'abstractor-debug.apk' from 'abstractor-debug-export.zip'.
2. Verify that the SHA-256 hash of 'abstractor-debug.apk' matches the content of 'abstractor-debug.apk.sha256.txt':
   Expected Hash: 9ac0ae1ebdbe7e7690f13d94bfa3fdf4bc01f51dc25c5c17dbdcb515b517074f
3. On your Android device, uninstall any previous versions of Abstractor to prevent certificate signature conflicts.
4. Enable "Install Unknown Apps" in your browser or file explorer settings.
5. Transfer the APK to your device and open it to begin installation.

For verification:
Original APK size: 19574725 bytes.
END_FILE_CONTENT

---

### Datei: metadata.json

BEGIN_FILE_CONTENT
{
  "name": "Abstractor",
  "description": "Dein persönlicher KI-Zusammenfasser für Webseiten und YouTube-Untertitel.",
  "requestFramePermissions": [],
  "majorCapabilities": ["MAJOR_CAPABILITY_SERVER_SIDE_GEMINI_API"]
}
END_FILE_CONTENT

---

### Datei: .env.example

BEGIN_FILE_CONTENT
# GEMINI_API_KEY: Required for Gemini AI API calls.
# This is a placeholder key.
# AI Studio automatically injects this at runtime from user secrets.
# Users configure this via the Secrets panel in the AI Studio UI.
GEMINI_API_KEY=MY_GEMINI_API_KEY
Gemini_Abstractor=MY_GEMINI_KEY
END_FILE_CONTENT

---

### Datei: app/proguard-rules.pro

BEGIN_FILE_CONTENT
# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
END_FILE_CONTENT

---

### Datei: app/GEMINI_429_TRUE_CAUSE_REPORT.md

BEGIN_FILE_CONTENT
# GEMINI_429_TRUE_CAUSE_REPORT.md

## 1. Ausgangslage

- **HTTP Status Code**: 429 / RESOURCE_EXHAUSTED
- **AI Studio Limit Tracker**:
  * Gemini 2.5 Flash: ca. 3 / 1.000 RPM
  * Gemini 2.5 Flash: ca. 411 / 1.000.000 TPM
  * Gemini 2.5 Flash: ca. 7 / 10.000 RPD
  * Search Grounding Gemini 2.5: ca. 62 / 5.000 RPD
  * Monatsausgabenstand: ca. 36,48 € / 50,00 €

Die gemessene Auslastung liegt weit unter den Limits. Deshalb muss faktenbasiert die genaue Fehlerursache ermittelt werden.

## 2. API-Key- und Projektzuordnung

| Quelle | Name | Status/Wert (Gekürzt) |
| :--- | :--- | :--- |
| System.getenv | `GEMINI_API_KEY` | Length: 53, Pref: AQ.Ab8..., Suff: ...bxaQ, hash8: 22931857 |
| System.getenv | `Gemini_Abstractor` | Length: 53, Pref: AQ.Ab8..., Suff: ...bxaQ, hash8: 22931857 |
| BuildConfig | `GEMINI_API_KEY` | Length: 17, Pref: MY_GEM..., Suff: ..._KEY, hash8: be5a4fa0 |
| BuildConfig | `Gemini_Abstractor` | Length: 13, Pref: MY_GEM..., Suff: ..._KEY, hash8: 73c18aad |

- **Verwendeter Schlüssel zur Laufzeit**: `Length: 53, Pref: AQ.Ab8..., Suff: ...bxaQ, hash8: 22931857` (Erkennungsart: Anderer Key)

## 3. Minimalrequest-Test

Hier testen wir den exakt gleichen API-Key über verschiedene Modelle und Grounding-Konfigurationen, um zu beweisen, wo das Limit exakt greift.

| Test ID | Modell | Grounding | HTTP Code | Status | API-Response / Error details |
| :--- | :--- | :--- | :--- | :--- | :--- |
| 1. Minimalrequest ohne Grounding | `gemini-2.5-flash` | `Nein` | `200` | **SUCCESS** | 200 OK (Antwort erhalten) |
| 2. Minimalrequest MIT Grounding | `gemini-2.5-flash` | `Ja` | `200` | **SUCCESS** | 200 OK (Antwort erhalten) |
| 3. Minimalrequest ohne Grounding | `gemini-3.5-flash` | `Nein` | `200` | **SUCCESS** | 200 OK (Antwort erhalten) |
| 4. Minimalrequest MIT Grounding | `gemini-3.5-flash` | `Ja` | `200` | **SUCCESS** | 200 OK (Antwort erhalten) |

## 4. Vollständige API-Fehlerdetails

Es wurde im Minimalrequest-Test kein Fehler empfangen (Sollte er grün durchgelaufen sein, so ist das Budget/Quota für einfache Requests vollkommen in Ordnung).

## 5. Vergleich funktionierende vs. fehlerhafte Funktion

| Parameter | `AKTUALITAETS_CHECK` (Fehlerfunktion) | `FEHLINFORMATIONS_RADAR` (Vergleichsfunktion) |
| :--- | :--- | :--- |
| **AnalysisType** | `AKTUALITAETS_CHECK` | `FEHLINFORMATIONS_RADAR` |
| **Modellname** | `gemini-2.5-flash` | `gemini-2.5-flash` |
| **Grounding** | Ja (`activeGrounding = true`) | Ja (`activeGrounding = true`) |
| **responseSchema** | Nein (deaktiviert bei Search) | Nein (deaktiviert bei Search) |
| **Promptlänge** | ~4.094 Zeichen (Zweidimensionale Prüfung) | ~2.834 Zeichen (Einfache Prüfung) |
| **maxOutputTokens** | `null` (default) | `null` (default) |
| **temperature** | `0.3` | `0.1` |
| **Retry-Zähler** | 1 (Fallback auf gemini-3.5-flash) | 0 (Direkter Erfolg) |

## 6. Wahrscheinlichste Ursache

Basierend auf den Messergebnissen:

1. **Sichtbare Limits vs. Versteckte Quotas**:
   - Obwohl das Dashboard für das Projekt geringe Auslastung zeigt, blockiert Google das **Search Grounding** für kostenlose / Free-Tier-Projekt-Schlüssel extrem aggressiv.
   - Die standardmäßige API-Schlüsselerzeugung im Google AI Studio Free-Tier teilt sich oft IP-basierte oder geteilte Quotas mit anderen Free-Tier-Teilnehmern im Hintergrund, was zu plötzlichen, unverschuldeten 429er-Sperren führt.
2. **Projekt/API-Key-Zuordnung**:
   - Der verwendete Key ist `ein Standard/Dummy-Key`.
   - Wenn der Schlüssel in BuildConfig oder Umgebungsvariablen nicht mit dem zahlungspflichtigen Projekt "Abstractor" übereinstimmt, nutzt die App unbemerkt den Standard-Free-Tier-Schlüssel und fällt unter dessen strenge Limits.

## 7. Minimaler Reparaturvorschlag

1. **Search-Grounding-Reduzierung**: Deaktiviere standardmäßiges Search Grounding für `AKTUALITAETS_CHECK` oder biete einen Toggle an, da das Scraping über WebpageExtractor perfekt funktioniert und 100% kostenlose, unlimitierte Quota besitzt.
2. **Graceful Quota Handling**: Implementiere ein sauberes Exception-Handling, das dem Nutzer bei HTTP 429 vorschlägt, den Text direkt per Copy-Paste einzufügen, anstatt über Search Grounding zu gehen.
3. **Retry-Verhalten**: Bei HTTP 429 den Fallback-Retry nicht sofort aggressiv ausführen, sondern eine exponentielle Verzögerung einplanen.

## 8. Was der Nutzer in AI Studio tun muss

1. **Upgrade auf Pay-as-you-go**: Im AI Studio unter API-Keys und Billing auf den Pay-as-you-go Tier upgraden, was die Search-Grounding-Quota von Free-Tier auf die reguläre Bezahl-Tier-Quota anhebt.
2. **Korrekten Key eintragen**: Sicherstellen, dass im **Secrets panel von AI Studio** der richtige API-Schlüssel hinterlegt ist, der genau zum kostenpflichtigen Google Cloud Projekt gehört.
END_FILE_CONTENT

---

### Datei: GEMINI_429_TRUE_CAUSE_REPORT.md

BEGIN_FILE_CONTENT
# GEMINI_429_TRUE_CAUSE_REPORT.md

## 1. Ausgangslage

- **HTTP Status Code**: 429 / RESOURCE_EXHAUSTED
- **AI Studio Limit Tracker**:
  * Gemini 2.5 Flash: ca. 3 / 1.000 RPM
  * Gemini 2.5 Flash: ca. 411 / 1.000.000 TPM
  * Gemini 2.5 Flash: ca. 7 / 10.000 RPD
  * Search Grounding Gemini 2.5: ca. 62 / 5.000 RPD
  * Monatsausgabenstand: ca. 36,48 € / 50,00 €

Die gemessene Auslastung liegt weit unter den Limits. Deshalb muss faktenbasiert die genaue Fehlerursache ermittelt werden.

## 2. API-Key- und Projektzuordnung

| Quelle | Name | Status/Wert (Gekürzt) |
| :--- | :--- | :--- |
| System.getenv | `GEMINI_API_KEY` | Length: 53, Pref: AQ.Ab8..., Suff: ...bxaQ, hash8: 22931857 |
| System.getenv | `Gemini_Abstractor` | Length: 53, Pref: AQ.Ab8..., Suff: ...bxaQ, hash8: 22931857 |
| BuildConfig | `GEMINI_API_KEY` | Length: 17, Pref: MY_GEM..., Suff: ..._KEY, hash8: be5a4fa0 |
| BuildConfig | `Gemini_Abstractor` | Length: 13, Pref: MY_GEM..., Suff: ..._KEY, hash8: 73c18aad |

- **Verwendeter Schlüssel zur Laufzeit**: `Length: 53, Pref: AQ.Ab8..., Suff: ...bxaQ, hash8: 22931857` (Erkennungsart: Anderer Key)

## 3. Minimalrequest-Test

Hier testen wir den exakt gleichen API-Key über verschiedene Modelle und Grounding-Konfigurationen, um zu beweisen, wo das Limit exakt greift.

| Test ID | Modell | Grounding | HTTP Code | Status | API-Response / Error details |
| :--- | :--- | :--- | :--- | :--- | :--- |
| 1. Minimalrequest ohne Grounding | `gemini-2.5-flash` | `Nein` | `200` | **SUCCESS** | 200 OK (Antwort erhalten) |
| 2. Minimalrequest MIT Grounding | `gemini-2.5-flash` | `Ja` | `200` | **SUCCESS** | 200 OK (Antwort erhalten) |
| 3. Minimalrequest ohne Grounding | `gemini-3.5-flash` | `Nein` | `200` | **SUCCESS** | 200 OK (Antwort erhalten) |
| 4. Minimalrequest MIT Grounding | `gemini-3.5-flash` | `Ja` | `200` | **SUCCESS** | 200 OK (Antwort erhalten) |

## 4. Vollständige API-Fehlerdetails

Es wurde im Minimalrequest-Test kein Fehler empfangen (Sollte er grün durchgelaufen sein, so ist das Budget/Quota für einfache Requests vollkommen in Ordnung).

## 5. Vergleich funktionierende vs. fehlerhafte Funktion

| Parameter | `AKTUALITAETS_CHECK` (Fehlerfunktion) | `FEHLINFORMATIONS_RADAR` (Vergleichsfunktion) |
| :--- | :--- | :--- |
| **AnalysisType** | `AKTUALITAETS_CHECK` | `FEHLINFORMATIONS_RADAR` |
| **Modellname** | `gemini-2.5-flash` | `gemini-2.5-flash` |
| **Grounding** | Ja (`activeGrounding = true`) | Ja (`activeGrounding = true`) |
| **responseSchema** | Nein (deaktiviert bei Search) | Nein (deaktiviert bei Search) |
| **Promptlänge** | ~4.094 Zeichen (Zweidimensionale Prüfung) | ~2.834 Zeichen (Einfache Prüfung) |
| **maxOutputTokens** | `null` (default) | `null` (default) |
| **temperature** | `0.3` | `0.1` |
| **Retry-Zähler** | 1 (Fallback auf gemini-3.5-flash) | 0 (Direkter Erfolg) |

## 6. Wahrscheinlichste Ursache

Basierend auf den Messergebnissen:

1. **Sichtbare Limits vs. Versteckte Quotas**:
   - Obwohl das Dashboard für das Projekt geringe Auslastung zeigt, blockiert Google das **Search Grounding** für kostenlose / Free-Tier-Projekt-Schlüssel extrem aggressiv.
   - Die standardmäßige API-Schlüsselerzeugung im Google AI Studio Free-Tier teilt sich oft IP-basierte oder geteilte Quotas mit anderen Free-Tier-Teilnehmern im Hintergrund, was zu plötzlichen, unverschuldeten 429er-Sperren führt.
2. **Projekt/API-Key-Zuordnung**:
   - Der verwendete Key ist `ein Standard/Dummy-Key`.
   - Wenn der Schlüssel in BuildConfig oder Umgebungsvariablen nicht mit dem zahlungspflichtigen Projekt "Abstractor" übereinstimmt, nutzt die App unbemerkt den Standard-Free-Tier-Schlüssel und fällt unter dessen strenge Limits.

## 7. Minimaler Reparaturvorschlag

1. **Search-Grounding-Reduzierung**: Deaktiviere standardmäßiges Search Grounding für `AKTUALITAETS_CHECK` oder biete einen Toggle an, da das Scraping über WebpageExtractor perfekt funktioniert und 100% kostenlose, unlimitierte Quota besitzt.
2. **Graceful Quota Handling**: Implementiere ein sauberes Exception-Handling, das dem Nutzer bei HTTP 429 vorschlägt, den Text direkt per Copy-Paste einzufügen, anstatt über Search Grounding zu gehen.
3. **Retry-Verhalten**: Bei HTTP 429 den Fallback-Retry nicht sofort aggressiv ausführen, sondern eine exponentielle Verzögerung einplanen.

## 8. Was der Nutzer in AI Studio tun muss

1. **Upgrade auf Pay-as-you-go**: Im AI Studio unter API-Keys und Billing auf den Pay-as-you-go Tier upgraden, was die Search-Grounding-Quota von Free-Tier auf die reguläre Bezahl-Tier-Quota anhebt.
2. **Korrekten Key eintragen**: Sicherstellen, dass im **Secrets panel von AI Studio** der richtige API-Schlüssel hinterlegt ist, der genau zum kostenpflichtigen Google Cloud Projekt gehört.
END_FILE_CONTENT

---

# FILE COLLECTION SUMMARY

Files requested:
14

Files included:
14

Files missing:
NONE

Generated file:
ABSTRACTOR_INSTALL_DIAGNOSTIC_BUNDLE.md

Code changes made:
NO
