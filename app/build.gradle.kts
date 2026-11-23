plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.zeljkobilbija001.motorwayinfo"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.zeljkobilbija001.motorwayinfo"
        minSdk = 24
        targetSdk = 37
        versionCode = 3
        versionName = "1.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // R8 (skraćivanje i maskiranje) – Google traži bar 25% „offuscamento“.
            // Pravila u proguard-rules.pro čuvaju WorkDatabase_Impl (WorkManager iz AdMob-a; Room ga traži po imenu,
            // bez pravila je release verzija padala pri pokretanju, InterstateInfo 16.11.2026).
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.work.runtime)
    implementation(libs.appcompat)
    implementation(libs.core.splashscreen)
    implementation(libs.recyclerview)
    implementation(libs.play.services.ads)
    implementation(libs.ump)
    implementation(libs.play.services.location)
    implementation(libs.billing)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}