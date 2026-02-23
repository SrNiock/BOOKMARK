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


    // Necesaria para usar ViewModels en Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    // Necesaria para que Retrofit entienda el formato JSON de Google Books usando Gson
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    //Corrutinas
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    //Tutorial
    implementation("com.squareup.picasso:picasso:2.71828")

    // Retrofit (El motor de las llamadas)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")

    // Converter para Kotlin Serialization (Para que entienda tus DTOs)
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")

    // OkHttp (Para ver logs de lo que pasa en la red - Opcional pero recomendado)
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")


    implementation("com.android.volley:volley:1.2.1")
    implementation("com.github.bumptech.glide:glide:4.16.0")

    implementation("io.coil-kt:coil-compose:2.6.0")


    implementation("androidx.compose.foundation:foundation:1.10.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    implementation(libs.androidx.compose.material.icons.extended)



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