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

// Authenticates the app to the VoxBox proxy. This is not the provider key: the provider key stays
// in the server environment and never reaches the APK. A token compiled into an APK is extractable,
// so the server pairs it with a rate limit and a daily request budget.
val voxBoxClientToken = providers.gradleProperty("VOXBOX_CLIENT_TOKEN")
    .orElse(providers.environmentVariable("VOXBOX_CLIENT_TOKEN"))
val configuredClientToken = voxBoxClientToken.orNull?.trim().orEmpty()

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
            // Optional locally: a mock proxy with no configured token accepts unauthenticated calls,
            // which keeps the loopback device-test workflow working without extra setup.
            buildConfigField(
                "String",
                "VOXBOX_CLIENT_TOKEN",
                buildConfigString(configuredClientToken),
            )
        }
        release {
            // Signed with the local debug keystore so the release build is installable for
            // coursework demos and device testing. This is deliberately NOT a distribution
            // signing setup: the debug keystore is shared, unprotected and well known, so this
            // APK must never be published. A real release needs its own keystore, with the
            // password supplied outside version control.
            signingConfig = signingConfigs.getByName("debug")
            buildConfigField(
                "String",
                "VOXBOX_API_BASE_URL",
                buildConfigString(releaseApiBaseUrlForBuildConfig),
            )
            buildConfigField(
                "String",
                "VOXBOX_CLIENT_TOKEN",
                buildConfigString(configuredClientToken),
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

val validateVoxBoxReleaseClientToken by tasks.registering {
    group = "verification"
    description = "Validates the proxy client token required by release builds."
    inputs.property("voxBoxClientToken", voxBoxClientToken.map(String::trim).orElse(""))
    doLast {
        val token = inputs.properties["voxBoxClientToken"]?.toString()?.trim().orEmpty()
        if (token.isBlank()) {
            throw GradleException(
                "A release client token is required so the deployed proxy can reject unauthenticated " +
                    "callers. Set -PVOXBOX_CLIENT_TOKEN=<token> or the VOXBOX_CLIENT_TOKEN environment " +
                    "variable to the same value configured on the server.",
            )
        }
        if (token.length < 24) {
            throw GradleException(
                "VOXBOX_CLIENT_TOKEN must be at least 24 characters. Generate one with " +
                    "`openssl rand -base64 32`.",
            )
        }
        if (token.any { it.isWhitespace() }) {
            throw GradleException("VOXBOX_CLIENT_TOKEN must not contain whitespace.")
        }
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
        dependsOn(validateVoxBoxReleaseApiBaseUrl, validateVoxBoxReleaseClientToken)
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
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
