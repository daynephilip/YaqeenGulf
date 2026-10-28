import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.yaqeengulf.deepfake"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.yaqeengulf.deepfake"
        minSdk = 29
        targetSdk = 36
        versionCode = 2
        versionName = "2.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val localProps = Properties()
            val localPropsFile = project.rootProject.file("local.properties")
            if (localPropsFile.exists()) {
                FileInputStream(localPropsFile).use { stream ->
                    localProps.load(stream)
                }
            }

            val keystorePath = (project.findProperty("RELEASE_KEYSTORE_PATH") as? String)
                ?: localProps.getProperty("RELEASE_KEYSTORE_PATH")
            val keystorePassword = (project.findProperty("RELEASE_KEYSTORE_PASSWORD") as? String)
                ?: localProps.getProperty("RELEASE_KEYSTORE_PASSWORD")
            val keyAliasStr = (project.findProperty("RELEASE_KEY_ALIAS") as? String)
                ?: localProps.getProperty("RELEASE_KEY_ALIAS")
            val keyPasswordStr = (project.findProperty("RELEASE_KEY_PASSWORD") as? String)
                ?: localProps.getProperty("RELEASE_KEY_PASSWORD")

            if (!keystorePath.isNullOrBlank() && file(keystorePath).exists()) {
                storeFile = file(keystorePath)
                storePassword = keystorePassword ?: ""
                keyAlias = keyAliasStr ?: ""
                keyPassword = keyPasswordStr ?: ""
            } else {
                // Fallback to debug keystore if release keystore is not yet generated
                storeFile = file("${System.getProperty("user.home")}/.android/debug.keystore")
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }

    buildFeatures {
        compose = true
        aidl = false
        buildConfig = true
        shaders = false
    }

    androidResources {
        ignoreAssetsPattern = "!.svn:!.git:!.ds_store:!*.scc:.*:!CVS:!thumbs.db:!picasa.ini:!*~:voice_en.onnx:voice_en_fc_weights.json"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Core Android dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Architecture & Lifecycle
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Jetpack Compose & Material 3
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // DataStore (Settings & Preferences)
    implementation(libs.androidx.datastore.preferences)

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)

    // ONNX Runtime Mobile/Android
    implementation(libs.onnxruntime.android)


    // Tooling & Testing
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.onnxruntime)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
