Arhcivo build.gradle necesario


import java.util.Properties
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.kotlin.plugin.serialization")
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}
android {
    namespace = "com.example.bookmark"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.bookmark"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val sbUrl = localProperties.getProperty("SUPABASE_URL") ?: ""
        val sbKey = localProperties.getProperty("SUPABASE_KEY") ?: ""

        buildConfigField("String", "SUPABASE_URL", localProperties.getProperty("SUPABASE_URL"))
        buildConfigField("String", "SUPABASE_KEY", localProperties.getProperty("SUPABASE_KEY"))
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {


    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    implementation(libs.androidx.compose.material.icons.extended)

// Retrofit (Librería principal)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")

    // Conversor Gson (Para convertir JSON a objetos de Java/Kotlin)
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

// Ya tienes el BOM, así que solo añade el módulo de Storage
    implementation("io.github.jan-tennert.supabase:storage-kt")
    // Librería base de Coil para Compose
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.animation.core)
   // implementation(libs.play.services.cast.framework)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation(platform("io.github.jan-tennert.supabase:bom:3.0.0")) // Versión recomendada
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.ktor:ktor-client-android:3.0.0") // Necesario para la conexión
}
