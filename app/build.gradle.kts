plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

/**
 * Une commande git ; sous Windows, git vit dans WSL, d'où le second essai via `wsl`. Null si git ne répond pas.
 */
fun git(vararg args: String): String? {
    for (prefix in listOf(emptyList(), listOf("wsl"))) {
        val out = runCatching {
            providers.exec {
                commandLine(prefix + "git" + args.toList())
                workingDir = rootDir
                isIgnoreExitValue = true
            }.standardOutput.asText.get().trim()
        }.getOrNull()
        if (!out.isNullOrEmpty()) return out
    }
    return null
}

/** Nombre de commits sur la branche courante : le versionCode, qui ne fait que grimper. */
val commitCount = git("rev-list", "--count", "HEAD")?.toIntOrNull() ?: 0

/**
 * La version : « majeur.mineur » vient de app/version.txt, le dernier chiffre compte les commits depuis le
 * dernier changement de ce fichier (0.2.0 sur ce commit-là, puis 0.2.1...). Ça survit à un merge ou à un squash.
 */
val versionBase = file("version.txt").readText().trim()
val versionPatch = git("log", "-1", "--format=%H", "--", "app/version.txt")
    ?.let { git("rev-list", "--count", "$it..HEAD")?.toIntOrNull() }
    ?: 0

android {
    namespace = "dev.mathieuburnat.piratefocus"
    compileSdk = 35

    defaultConfig {
        // L'identifiant de l'app (Play Store, Google OAuth) ; le code garde son package dev.mathieuburnat.piratefocus.
        applicationId = "ch.piratecanfocustoo"
        minSdk = 26
        targetSdk = 35
        versionCode = commitCount.coerceAtLeast(1)
        versionName = "$versionBase.$versionPatch"
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
    // Connexion Google (Credential Manager) : le jeton part au Worker, qui le vérifie.
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)
    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
}
