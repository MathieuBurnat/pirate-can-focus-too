plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

/**
 * Nombre de commits sur la branche courante : le dernier chiffre de la version (0.1.x).
 * Sous Windows, git vit dans WSL, d'où le second essai via `wsl`.
 */
fun gitCommitCount(): Int {
    val attempts = listOf(
        listOf("git", "rev-list", "--count", "HEAD"),
        listOf("wsl", "git", "rev-list", "--count", "HEAD"),
    )
    for (command in attempts) {
        val count = runCatching {
            providers.exec {
                commandLine(command)
                workingDir = rootDir
                isIgnoreExitValue = true
            }.standardOutput.asText.get().trim().toInt()
        }.getOrNull()
        if (count != null) return count
    }
    return 0
}

val commitCount = gitCommitCount()

android {
    namespace = "dev.mathieuburnat.piratefocus"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.mathieuburnat.piratefocus"
        minSdk = 26
        targetSdk = 35
        versionCode = commitCount.coerceAtLeast(1)
        versionName = "0.1.$commitCount"
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
}
