plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.mobileapp"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.mobileapp"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation(libs.play.services.maps)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    //библиотека для работы с geo
    implementation("org.locationtech.jts:jts-core:1.19.0")
    //работа с OSM
    implementation("org.osmdroid:osmdroid-android:6.1.20")
    implementation("org.osmdroid:osmdroid-wms:6.1.20")
    implementation("org.osmdroid:osmdroid-mapsforge:6.1.20")

    implementation("com.carto:carto-mobile-sdk:4.4.6@aar")
}