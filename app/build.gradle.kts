plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "ir.jarvis.fa"
    compileSdk = 35
    defaultConfig {
        applicationId = "ir.jarvis.fa"
        minSdk = 23
        targetSdk = 28
        versionCode = 1
        versionName = "1.0"
    }
}
