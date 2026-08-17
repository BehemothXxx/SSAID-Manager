plugins {
    alias(libs.plugins.android.application)
}

fun getGitCommitCount(): Int = try {
    val process = ProcessBuilder("git", "rev-list", "--count", "HEAD")
        .redirectErrorStream(true)
        .start()
    val count = process.inputStream.bufferedReader().readText().trim().toInt()
    process.waitFor()
    count
} catch (_: Exception) {
    1
}

android {
    namespace = "top.nkbe.ssaid"
    compileSdk = 37

    defaultConfig {
        applicationId = "top.nkbe.ssaid"
        minSdk = 29
        targetSdk = 37
        versionCode = getGitCommitCount()
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }


    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
