plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.0"
}

android {
    namespace = "com.theextramile.admin"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.theextramile.admin"
        minSdk = 24
        targetSdk = 34
        versionCode = 4
        versionName = "1.3.0"

        // ╔═══════════════════════════════════════════════════════════╗
        // ║   URL del HUB CENTRAL                                     ║
        // ║                                                           ║
        // ║   Se usa SOLO para "¿Olvidaste tu contraseña?": el HUB     ║
        // ║   manda una contraseña temporal al correo. Los colores ya  ║
        // ║   NO vienen de aquí (ver ui/theme/Color.kt).               ║
        // ╚═══════════════════════════════════════════════════════════╝
        buildConfigField("String", "HUB_URL", "\"https://gpanelcol.online/hub.php\"")

        // URL de fallback (en caso de no haber sesión todavía)
        buildConfigField("String", "API_BASE_URL", "\"https://theextramille.online/api/\"")
        buildConfigField("String", "SITE_BASE_URL", "\"https://theextramille.online/\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
        debug {
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17

        /*
         * La app usa java.time (LocalDate, Instant, DateTimeFormatter) en
         * Reservas, Calendario, Blog y Extracto, pero minSdk es 24 y esas
         * clases solo existen de forma nativa desde API 26. En Android 7
         * esas pantallas cerraban la app con NoClassDefFoundError.
         *
         * El "desugaring" mete una copia de esas clases dentro del APK, así
         * que funcionan también en Android 7 sin tocar el código.
         */
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // java.time en Android 7 (ver isCoreLibraryDesugaringEnabled arriba)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("androidx.compose.material:material-icons-extended:1.5.4")

    // Compose
    val composeBom = platform("androidx.compose:compose-bom:2024.02.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")

    // Retrofit + OkHttp (red)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // DataStore (preferencias)
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Coil (carga de imágenes)
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Splash screen
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Firebase Cloud Messaging (notificaciones push)
    implementation(platform("com.google.firebase:firebase-bom:32.7.1"))
    implementation("com.google.firebase:firebase-messaging-ktx")

    // Debug tools
    debugImplementation("androidx.compose.ui:ui-tooling")
}
