import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.20"
}

val localProperties = Properties().apply {
    val propsFile = rootProject.file("local.properties")
    if (propsFile.exists()) {
        propsFile.inputStream().use { load(it) }
    }
}

val pwd = System.getenv("FCL_KEYSTORE_PASSWORD")
    ?: localProperties.getProperty("pwd")
    ?: "android"

val curseApiKey = System.getenv("CURSE_API_KEY")
    ?: localProperties.getProperty("curse.api.key")
    ?: ""

val oauthApiKey = System.getenv("OAUTH_API_KEY")
    ?: localProperties.getProperty("oauth.api.key")
    ?: ""

android {
    namespace = "com.duckmc.launcher"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.duckmc.launcher"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        resValue("string", "app_version", versionName)
        resValue("string", "curse_api_key", curseApiKey)
        resValue("string", "oauth_api_key", oauthApiKey)
    }

    signingConfigs {
        create("FCLKey") {
            storeFile = file("../key-store.jks")
            storePassword = pwd
            keyAlias = "FCL-Key"
            keyPassword = pwd
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
        }

        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("FCLKey")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
            pickFirsts += listOf("**/libbytehook.so")
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    splits {
        val arch = System.getProperty("arch", "all")
        if (arch != "all") {
            abi {
                isEnable = true
                reset()
                when (arch) {
                    "arm" -> include("armeabi-v7a")
                    "arm64" -> include("arm64-v8a")
                    "x86" -> include("x86")
                    "x86_64" -> include("x86_64")
                }
            }
        }
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))
    implementation(project(":FCLCore"))
    implementation(project(":FCLLibrary"))
    implementation(project(":FCLauncher"))
    implementation(project(":Terracotta"))

    implementation(libs.taptargetview)
    implementation(libs.nanohttpd)
    implementation(libs.commons.compress)
    implementation(libs.xz)
    implementation(libs.opennbt)
    implementation(libs.gson)
    implementation(libs.appcompat)
    implementation(libs.core.splashscreen)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.glide)
    implementation(libs.touchcontroller)
    implementation(libs.palette.ktx)
    implementation(libs.gamepad.remapper)
    implementation(libs.segmented.button)
    implementation(libs.datastore)
    implementation(libs.kotlinx.serialization.json)
}

tasks.register("updateMap") {
    doLast {
        val list = mutableListOf<String>()
        val mapFile = file("${rootDir}/version_map.json")
        if (!mapFile.exists()) return@doLast

        mapFile.forEachLine {
            list.add(
                when {
                    it.contains("versionCode") -> it.replace(
                        Regex("[0-9]+"),
                        android.defaultConfig.versionCode.toString()
                    )

                    it.contains("versionName") -> it.replace(
                        Regex("\\d+(\\.\\d+)+"),
                        android.defaultConfig.versionName.toString()
                    )

                    it.contains("date") -> it.replace(
                        Regex("\\d{4}\\.\\d{2}\\.\\d{2}"),
                        SimpleDateFormat("yyyy.MM.dd").format(Date())
                    )

                    it.contains("url") -> it.replace(
                        Regex("\\d+(\\.\\d+)+"),
                        android.defaultConfig.versionName.toString()
                    )

                    else -> it
                }
            )
        }

        mapFile.writeText(list.joinToString("\n"), Charsets.UTF_8)
    }
}
