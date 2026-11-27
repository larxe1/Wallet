import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

fun readDotEnvFile(file: File): Map<String, String> {
    if (!file.exists() || !file.isFile) return emptyMap()
    val result = mutableMapOf<String, String>()
    file.readLines().forEach { rawLine ->
        val line = rawLine.trim()
        if (line.isEmpty() || line.startsWith("#")) return@forEach
        val eqIdx = line.indexOf('=')
        if (eqIdx <= 0) return@forEach
        val key = line.substring(0, eqIdx).trim()
        var value = line.substring(eqIdx + 1).trim()
        if ((value.startsWith("\"") && value.endsWith("\"") && value.length >= 2) ||
            (value.startsWith("'") && value.endsWith("'") && value.length >= 2)
        ) {
            value = value.substring(1, value.length - 1)
        }
        if (key.isNotEmpty()) {
            result[key] = value
        }
    }
    return result
}

fun resolveSupabaseConfig(): Pair<String, String> {
    val localPropsFile = rootProject.file("local.properties")
    val localProps = Properties().apply {
        if (localPropsFile.exists()) {
            localPropsFile.inputStream().use { load(it) }
        }
    }
    val envProd = readDotEnvFile(rootProject.file("../.env.production"))
    val envDefault = readDotEnvFile(rootProject.file("../.env"))

    val url = listOf(
        localProps.getProperty("SUPABASE_URL"),
        envProd["VITE_SUPABASE_URL"],
        envDefault["VITE_SUPABASE_URL"],
        System.getenv("SUPABASE_URL")
    ).firstOrNull { !it.isNullOrBlank() }?.trim() ?: "https://placeholder.supabase.co"

    val anonKey = listOf(
        localProps.getProperty("SUPABASE_ANON_KEY"),
        envProd["VITE_SUPABASE_ANON_KEY"],
        envDefault["VITE_SUPABASE_ANON_KEY"],
        System.getenv("SUPABASE_ANON_KEY")
    ).firstOrNull { !it.isNullOrBlank() }?.trim() ?: "placeholder-key"

    return url to anonKey
}

fun escapeForBuildConfig(value: String): String =
    value.replace("\\", "\\\\").replace("\"", "\\\"")

val (supabaseUrl, supabaseAnonKey) = resolveSupabaseConfig()

android {
    namespace = "com.moneytracker.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.moneytracker.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "1.2.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "SUPABASE_URL", "\"${escapeForBuildConfig(supabaseUrl)}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${escapeForBuildConfig(supabaseAnonKey)}\"")
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.ui.text.google.fonts)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.foundation)

    implementation(libs.google.material)

    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.work.runtime.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
