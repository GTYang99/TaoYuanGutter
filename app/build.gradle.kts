import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Properties
import kotlin.toString

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.compose)
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

fun getConfiguredVersionCode(): Int {
    val propertiesFile = project.file("version.properties")
    if (!propertiesFile.canRead()) {
        throw GradleException("Could not read ${propertiesFile.name}")
    }

    val properties = Properties()
    properties.load(FileInputStream(propertiesFile))
    return properties["P0_VERSION_CODE"].toString().toInt()
}

fun getVersionName(): String {
    val propertiesFile = project.file("version.properties")
    if (!propertiesFile.canRead()) {
        throw GradleException("Could not read ${propertiesFile.name}")
    }
    val properties = Properties()
    properties.load(FileInputStream(propertiesFile))
    return "${properties["P1_VERSION_NAME_MAJOR"]}.${properties["P2_VERSION_NAME_MINOR"]}.${properties["P3_VERSION_NAME_BUILD"]}"
}

val versionPropertiesFile = project.file("version.properties")
val currentVersionCode = getConfiguredVersionCode()
require(currentVersionCode < Int.MAX_VALUE) { "P0_VERSION_CODE has reached the Android versionCode limit" }
val releaseVersionCode = currentVersionCode + 1
val localSigningProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.isFile) file.inputStream().use(::load)
}
fun signingValue(property: String, environment: String): String? =
    System.getenv(environment)?.takeIf(String::isNotBlank)
        ?: localSigningProperties.getProperty(property)?.takeIf(String::isNotBlank)
val updateKeystorePath = signingValue("UPDATE_KEYSTORE_FILE", "UPDATE_KEYSTORE_FILE")
val updateKeyAlias = signingValue("UPDATE_KEY_ALIAS", "UPDATE_KEY_ALIAS")
val updateKeyPassword = signingValue("UPDATE_KEY_PASSWORD", "UPDATE_KEY_PASSWORD")
val updateStorePassword = signingValue("UPDATE_STORE_PASSWORD", "UPDATE_STORE_PASSWORD")
val updateSigningConfigured = listOf(updateKeystorePath, updateKeyAlias, updateKeyPassword, updateStorePassword).all { it != null }

android {
    namespace = "com.example.taoyuangutter"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.taoyuangutter"
        minSdk = 24
        targetSdk = 36
        versionCode = releaseVersionCode
        versionName = getVersionName()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("updateRelease") {
            if (updateSigningConfigured) {
                val configuredFile = File(updateKeystorePath!!).let { if (it.isAbsolute) it else rootProject.file(it) }
                storeFile = configuredFile
                keyAlias = updateKeyAlias
                keyPassword = updateKeyPassword
                storePassword = updateStorePassword
            }
        }
    }
    buildTypes {
        release {
            if (updateSigningConfigured) signingConfig = signingConfigs.getByName("updateRelease")
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
        viewBinding = true
        compose = true
        buildConfig = true
    }
}

secrets {
    // 真實金鑰存放於此（local.properties），已加入 .gitignore，不會上傳至 GitHub
    propertiesFileName = "local.properties"
    // 安全模板，可上傳至 GitHub，提示團隊需設定哪些金鑰
    defaultPropertiesFileName = "secrets.defaults.properties"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.play.services.maps)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.exifinterface)
    // CameraX：自訂橫向相機
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)
    // GPS 定位
    implementation(libs.play.services.location)
    // Coroutines（lifecycleScope.launch）
    implementation(libs.kotlinx.coroutines.android)
    // Retrofit + OkHttp（網路 API）
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)
    // Glide（從 URL 載入照片）
    implementation(libs.glide)
    implementation(libs.photoview)
    // Compose (for ui/theme files)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}


// The lifecycle task has no outputs, so this action runs after its dependencies on every invocation.
// A dependency failure prevents the action from running; a successful assembly consumes the code.
tasks.configureEach {
    if (name == "assembleRelease") {
        doFirst {
        if (!updateSigningConfigured) {
            throw GradleException("Release APK signing is required. Configure UPDATE_KEYSTORE_FILE, UPDATE_KEY_ALIAS, UPDATE_KEY_PASSWORD, and UPDATE_STORE_PASSWORD in ignored local.properties or protected environment variables.")
        }
        if (updateKeystorePath != null) {
            val signingFile = File(updateKeystorePath).let { if (it.isAbsolute) it else rootProject.file(it) }
            if (!signingFile.isFile) throw GradleException("Configured release keystore file is unavailable.")
        }
    }
        doLast {
            val oldText = versionPropertiesFile.readText()
            val codeLine = Regex("(?m)^P0_VERSION_CODE=[^\r\n]*$")
            check(codeLine.containsMatchIn(oldText)) { "P0_VERSION_CODE is missing from version.properties" }
            val updatedText = codeLine.replace(oldText, "P0_VERSION_CODE=$releaseVersionCode")
            val temporary = File(versionPropertiesFile.parentFile, versionPropertiesFile.name + ".tmp")
            temporary.writeText(updatedText)
            try {
                Files.move(temporary.toPath(), versionPropertiesFile.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } catch (_: Exception) {
                Files.move(temporary.toPath(), versionPropertiesFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }
}
