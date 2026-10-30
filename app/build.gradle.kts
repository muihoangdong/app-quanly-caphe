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
// Bật MySQL + API PHP trong Docker (bỏ qua nếu Docker Desktop chưa mở).
// Máy ảo gọi API qua http://10.0.2.2:8080 nên không cần adb reverse.
val batServer = tasks.register<Exec>("batServer") {
    group = "quan ly cafe"
    description = "docker compose up -d trong thư mục server"
    workingDir = rootProject.file("server")
    commandLine("docker", "compose", "up", "-d")
    isIgnoreExitValue = true
}
tasks.matching { it.name == "assembleDebug" }.configureEach {
    finalizedBy(batServer)
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.gridlayout)
    implementation(libs.material)
    // Gọi API PHP (server/api) bằng Retrofit, dữ liệu JSON
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}