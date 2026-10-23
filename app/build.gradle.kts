plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.example.tirepressure"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.example.tirepressure"
        minSdk = 29
        targetSdk = 29   // ต่ำไว้เพื่อให้เริ่ม service จาก widget ได้บนหัวเครื่อง
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
