plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.riegosostenible"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.riegosostenible"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    // HABILITAR VIEW BINDING (con sintaxis .kts)
    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    // --- LIBRERÍAS BASE ---
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")

    // --- LIBRERÍAS DE RED (API / Retrofit) ---
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // --- COROUTINES (Para llamadas asíncronas) ---
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // --- LIBRERÍA DEL GRÁFICO ---
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // --- LIBRERÍAS DE TEST ---
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // ViewModel y LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")

// Fragment KTX (si usas fragmentos)
    implementation("androidx.fragment:fragment-ktx:1.6.1")

// Logging de peticiones HTTP (útil en Retrofit)
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

// Para carga de imágenes (si mostrarás sensores o datos visuales)
    implementation("io.coil-kt:coil:2.4.0")

}