plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

// Room exporta el esquema de cada versión de la base de datos a JSON en
// app/schemas. Esos archivos se suben al repositorio y los usan los tests
// de migraciones (MigrationTestHelper) para verificar que no se pierden datos.
room {
    schemaDirectory("$projectDir/schemas")
}

android {
    namespace = "com.example.estudia"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.estudia"
        minSdk = 24
        targetSdk = 36
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        // Permite usar java.time (LocalDate, LocalTime) desde Android 7 (API 24).
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.runtime.compose)
    // Room: persistencia local en SQLite (entidades, DAOs y base de datos)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    // WorkManager: recordatorio diario en segundo plano
    implementation(libs.androidx.work.runtime.ktx)
    // Íconos de Material (Add, Delete, Edit, AttachFile, etc.)
    implementation(libs.androidx.compose.material.icons.extended)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    constraints {
        // room-testing (MigrationTestHelper) necesita kotlinx-serialization 1.8+,
        // pero navigation-compose trae la 1.7.3. Los tests instrumentados usan
        // las mismas versiones que la app, así que se sube acá.
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.8.1")
    }

    // Tests unitarios (JVM): ViewModels con un repositorio falso
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    // Tests instrumentados (emulador): DAOs y migraciones de Room
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}