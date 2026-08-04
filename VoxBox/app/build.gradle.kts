import java.net.URI

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val voxBoxReleaseApiBaseUrl = providers.gradleProperty("VOXBOX_API_BASE_URL")
    .orElse(providers.environmentVariable("VOXBOX_API_BASE_URL"))
val configuredReleaseApiBaseUrl = voxBoxReleaseApiBaseUrl.orNull
    ?.trim()
    ?.trimEnd('/')
    .orEmpty()
val releaseApiBaseUrlForBuildConfig = configuredReleaseApiBaseUrl.ifBlank {
    "https://invalid.voxbox.local"
}

fun buildConfigString(value: String): String =
    "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

android {
    namespace = "me.thimmaiah.voxbox"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "me.thimmaiah.voxbox"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            // Android reaches the development machine through `adb reverse tcp:8787 tcp:8787`.
            buildConfigField(
                "String",
                "VOXBOX_API_BASE_URL",
                buildConfigString("http://127.0.0.1:8787"),
            )
        }
        release {
            buildConfigField(
                "String",
                "VOXBOX_API_BASE_URL",
                buildConfigString(releaseApiBaseUrlForBuildConfig),
            )
            optimization {
                enable = false
            }
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
}

val validateVoxBoxReleaseApiBaseUrl by tasks.registering {
    group = "verification"
    description = "Validates the HTTPS VoxBox API base URL required by release builds."
    inputs.property(
        "voxBoxApiBaseUrl",
        voxBoxReleaseApiBaseUrl.map(String::trim).orElse(""),
    )
    doLast {
        val configured = inputs.properties["voxBoxApiBaseUrl"]
            ?.toString()
            ?.trim()
            ?.trimEnd('/')
            .orEmpty()
        if (configured.isBlank()) {
            throw GradleException(
                "A release API URL is required. Set -PVOXBOX_API_BASE_URL=https://api.example.com " +
                    "or the VOXBOX_API_BASE_URL environment variable.",
            )
        }
        val uri = try {
            URI(configured)
        } catch (error: Exception) {
            throw GradleException("VOXBOX_API_BASE_URL is not a valid absolute URL: $configured", error)
        }
        if (
            !uri.isAbsolute || uri.scheme != "https" || uri.host.isNullOrBlank() ||
            uri.userInfo != null || uri.query != null || uri.fragment != null
        ) {
            throw GradleException(
                "VOXBOX_API_BASE_URL must be an absolute HTTPS base URL without credentials, " +
                    "a query, or a fragment (for example https://api.example.com).",
            )
        }
    }
}

tasks.configureEach {
    if (name == "preReleaseBuild") {
        dependsOn(validateVoxBoxReleaseApiBaseUrl)
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.google.mlkit.text.recognition)
    implementation(libs.androidx.exifinterface)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
