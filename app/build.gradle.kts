plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.calculator"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.calculator"
        minSdk = 35
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // buildTypes {
    //     release {
    //         isMinifyEnabled = false
    //         proguardFiles(
    //             getDefaultProguardFile("proguard-android-optimize.txt"),
    //             "proguard-rules.pro"
    //         )
    //     }
    // }
    compileOptions {
         sourceCompatibility = JavaVersion.VERSION_11
         targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
         jvmTarget = "11"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("net.objecthunter:exp4j:0.4.8")  // 使用双引号和括号
    implementation("androidx.gridlayout:gridlayout:1.0.0")
    implementation("androidx.appcompat:appcompat:1.6.1") // 提供AppCompat控件
    // implementation("com.google.android.material:material:1.11.0") // 提供Material组件
}