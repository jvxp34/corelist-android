plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// URL da API. No debug, 10.0.2.2 é o "localhost" do PC visto pelo emulador.
// Para testar num celular com o Django do PC, rode com
// -Pcomprafacil.apiUrl=http://<ip-do-pc>:8000/api
val apiUrlDebug = (findProperty("comprafacil.apiUrl") as String?) ?: "http://10.0.2.2:8000/api"
val apiUrlRelease = "https://mvp-comprafacil.dql3968zgbfv8.amplifyapp.com/api"

android {
    namespace = "br.com.comprafacil"
    compileSdk = 37

    defaultConfig {
        applicationId = "br.com.comprafacil"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        // APK menor para um aparelho só: -Pcomprafacil.abi=x86_64 (emulador) ou arm64-v8a.
        (findProperty("comprafacil.abi") as String?)?.let { ndk { abiFilters += it } }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            buildConfigField("String", "API_URL", "\"$apiUrlDebug\"")
            resValue("string", "app_name", "CompraFácil (dev)")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("String", "API_URL", "\"$apiUrlRelease\"")
            resValue("string", "app_name", "CompraFácil")
            // Para a apresentação o APK de release é assinado com a chave de
            // debug, que permite instalar no celular. Antes de publicar na
            // Play Store, troque por uma chave própria (veja o README).
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.datastore.preferences)

    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.barcode.scanning)

    testImplementation(libs.junit)
    testImplementation(libs.okhttp.mockwebserver)
}
