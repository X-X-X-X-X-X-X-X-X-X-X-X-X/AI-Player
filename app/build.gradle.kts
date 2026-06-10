plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.plugin.compose")
}

android {
  namespace = "cn.xuexc.ai_player"
  compileSdk { version = release(36) }

  defaultConfig {
    applicationId = "cn.xuexc.ai_player"
    minSdk = 30
    targetSdk = 36

    // 从环境变量读取版本名称（如 v1.2.1），本地开发默认为 v1.2
    val envVersionName = System.getenv("APP_VERSION_NAME") ?: "v999"
    val cleanVersion = envVersionName.replace("v", "")
    versionName = cleanVersion

    // 根据版本号规则（x.y.z）自动计算 versionCode（例：1.2.1 -> 10201）
    versionCode = run {
      val envCode = System.getenv("APP_VERSION_CODE")
      if (!envCode.isNullOrEmpty()) {
        envCode.toInt()
      } else {
        try {
          val parts = cleanVersion.split(".")
          val major = if (parts.isNotEmpty()) parts[0].toInt() else 1
          val minor = if (parts.size >= 2) parts[1].toInt() else 0
          val patch = if (parts.size >= 3) parts[2].toInt() else 0
          major * 10000 + minor * 100 + patch
        } catch (e: Exception) {
          3
        }
      }
    }

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_FILE_PATH")
      if (!keystorePath.isNullOrEmpty()) {
        storeFile = file(keystorePath)
        storePassword = System.getenv("KEYSTORE_PASSWORD")
        keyAlias = System.getenv("KEY_ALIAS")
        keyPassword = System.getenv("KEY_PASSWORD")
      } else {
        // 本地未配置环境变量时，回退到 debug 签名，确保本地调试运行正常
        val debugConfig = signingConfigs.getByName("debug")
        storeFile = debugConfig.storeFile
        storePassword = debugConfig.storePassword
        keyAlias = debugConfig.keyAlias
        keyPassword = debugConfig.keyPassword
      }
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    getByName("debug") { applicationIdSuffix = ".debug" }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures { compose = true }
}

dependencies {
  implementation("androidx.core:core-ktx:1.18.0")
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
  implementation("androidx.activity:activity-compose:1.13.0")
  implementation(platform("androidx.compose:compose-bom:2024.09.00"))
  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.ui:ui-graphics")
  implementation("androidx.compose.ui:ui-tooling-preview")
  implementation("androidx.compose.material3:material3")
  implementation("androidx.compose.material:material-icons-extended")
  implementation("androidx.media:media:1.7.0")
  testImplementation("junit:junit:4.13.2")
  androidTestImplementation("androidx.test.ext:junit:1.3.0")
  androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
  androidTestImplementation(platform("androidx.compose:compose-bom:2024.09.00"))
  androidTestImplementation("androidx.compose.ui:ui-test-junit4")
  debugImplementation("androidx.compose.ui:ui-tooling")
  debugImplementation("androidx.compose.ui:ui-test-manifest")
}
