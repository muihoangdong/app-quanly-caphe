plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "hutech.cntt.app_quanlycafe"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "hutech.cntt.app_quanlycafe"
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

// ===== Tự động khi bấm Run / build bản debug =====
// 1. Bật MySQL trong Docker (bỏ qua nếu Docker Desktop chưa mở)
// 2. adb reverse: máy ảo / điện thoại cắm USB nối 127.0.0.1:3307 là tới MySQL trên máy tính
val batServer = tasks.register<Exec>("batServer") {
    group = "quan ly cafe"
    description = "docker compose up -d trong thư mục server"
    workingDir = rootProject.file("server")
    commandLine("docker", "compose", "up", "-d")
    isIgnoreExitValue = true
}
val adbReverse = tasks.register<Exec>("adbReverse") {
    group = "quan ly cafe"
    description = "adb reverse tcp:3307 tcp:3307 cho thiết bị đang kết nối"
    executable = androidComponents.sdkComponents.adb.get().asFile.absolutePath
    args("reverse", "tcp:3307", "tcp:3307")
    isIgnoreExitValue = true
    mustRunAfter(batServer)
}
tasks.matching { it.name == "assembleDebug" }.configureEach {
    finalizedBy(batServer, adbReverse)
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.gridlayout)
    implementation(libs.material)
    // Kết nối thẳng MySQL (JDBC). Bản 5.1.x là bản cuối còn chạy được trên Android
    implementation(libs.mysql.connector)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}