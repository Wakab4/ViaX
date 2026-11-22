plugins {
    id("com.android.application")
}

android {
    namespace = "moe.via.browser"
    compileSdk {
        version = release(37) { minorApiLevel = 0 }
    }

    defaultConfig {
        applicationId = "moe.via.browser"
        minSdk = 28
        targetSdk = 36
        versionCode = 20261028
        versionName = "7.3.3"
    }

    androidResources {
        localeFilters += listOf("en", "ja", "zh-rCN", "zh-rTW")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.webkit:webkit:1.17.1")
    implementation("com.google.zxing:core:3.5.4")
    implementation("io.noties.markwon:core:4.6.2")
    implementation("com.atlassian.commonmark:commonmark-ext-gfm-tables:0.13.0")
    implementation("com.squareup.okhttp3:okhttp:5.5.1")
    implementation("androidx.customview:customview:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("androidx.dynamicanimation:dynamicanimation:1.0.0")
    implementation("androidx.fragment:fragment:1.7.0")
    implementation("androidx.viewpager2:viewpager2:1.0.0")
}


