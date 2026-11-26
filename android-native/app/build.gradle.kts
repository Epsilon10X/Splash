import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.splash.offline"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.splash.offline"
        minSdk = 26
        targetSdk = 36
        versionCode = 35
        versionName = "2.0.24"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            val ksPropsFile = rootProject.file("../android/keystore.properties")
            if (ksPropsFile.exists()) {
                val ksProps = Properties()
                FileInputStream(ksPropsFile).use { ksProps.load(it) }
                val relStorePath = ksProps.getProperty("storeFile")
                if (!relStorePath.isNullOrBlank()) {
                    storeFile = rootProject.file("../android/app/$relStorePath")
                    storePassword = ksProps.getProperty("storePassword")
                    keyAlias = ksProps.getProperty("keyAlias")
                    keyPassword = ksProps.getProperty("keyPassword")
                }
            }
        }
    }

    buildTypes {
        release {
            val ksPropsFile = rootProject.file("../android/keystore.properties")
            if (ksPropsFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
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

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi"
        )
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.11.01"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("dev.chrisbanes.haze:haze:1.7.3")

    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
}
