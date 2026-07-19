plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.osscabinet.ossWallet.core.data"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        buildConfig = true
    }

    kotlin {
        jvmToolchain(11)
    }
}

dependencies {
    // Domain module — 'api' so :app gets domain types transitively via :core:data
    api(project(":core:domain"))

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.oss.hilt.work)
    ksp(libs.oss.hilt.work.compiler)

    // Room + SQLCipher
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.sqlcipher)
    implementation(libs.androidx.sqlite.ktx)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // CSV (for CsvProcessor / CsvImportServiceImpl)
    implementation(libs.commons.csv)
    implementation(libs.univocity.parsers)

    // AndroidX Core (for BackupRepositoryImpl)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.documentfile)

    // Work (for BackupSchedulerImpl)
    implementation(libs.androidx.work.runtime.ktx)
}
