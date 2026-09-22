import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// The Google Services plugin fails the build when google-services.json is
// missing, which would stop anyone building the app before Firebase is set
// up (and would stop CI without the secret). Applied only when the file is
// there; without it, push is simply off and everything else works.
val hasFirebaseConfig = file("google-services.json").exists()
if (hasFirebaseConfig) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
}

android {
    namespace = "com.rimagwinya.app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.rimagwinya.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Supabase credentials live in local.properties, which is gitignored.
        // Empty defaults so the project builds for anyone without them.
        buildConfigField("String", "SUPABASE_URL", "\"${localProperty("SUPABASE_URL")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${localProperty("SUPABASE_ANON_KEY")}\"")
        // Push only works once Firebase is set up; the app checks this
        // rather than crashing on a missing FirebaseApp.
        buildConfigField("boolean", "PUSH_ENABLED", hasFirebaseConfig.toString())
        // The Google **Web** client id (not the Android one). Empty until
        // Phase 12 is set up, and the button hides itself when it is empty.
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${localProperty("GOOGLE_WEB_CLIENT_ID")}\"")
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // java.time on API 24 and 25
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

ksp {
    // Room writes its schema here so migrations can be diffed in review
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // --- Compose ---
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.appcompat)

    // --- Lifecycle / navigation ---
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    // --- DI ---
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    // --- Network (Phase 2) ---
    implementation(libs.retrofit)
    implementation(libs.retrofit.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)

    // --- Auth + realtime (Phase 3) ---
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.auth)
    implementation(libs.supabase.realtime)
    // supabase-kt runs on Ktor and needs an engine on the classpath.
    implementation(libs.ktor.client.okhttp)

    // --- Google sign-in (Phase 12) ---
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)

    // --- Push notifications (Phase 11) ---
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    // --- Offline cache and sync (Phase 10) ---
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // --- Settings storage and biometrics (Phase 9) ---
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.biometric)

    // --- Charts (Phase 8) ---
    implementation(libs.vico.compose.m3)

    // --- Java 8+ APIs on API 24 ---
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // --- Test ---
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

/** Reads a key from local.properties, returning "" when it isn't set. */
fun localProperty(key: String): String {
    val f = rootProject.file("local.properties")
    if (!f.exists()) return ""
    val props = Properties()
    f.inputStream().use { props.load(it) }
    return props.getProperty(key).orEmpty()
}
