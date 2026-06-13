import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use(::load)
    }
}
val aiWorkerUrl = providers.environmentVariable("AI_WORKER_URL")
    .orElse(localProperties.getProperty("AI_WORKER_URL", ""))
    .get()
val recipeImageWorkerUrl = providers.environmentVariable("RECIPE_IMAGE_WORKER_URL")
    .orElse(localProperties.getProperty("RECIPE_IMAGE_WORKER_URL", aiWorkerUrl))
    .get()
val productParserWorkerUrl = providers.environmentVariable("PRODUCT_PARSER_WORKER_URL")
    .orElse(localProperties.getProperty("PRODUCT_PARSER_WORKER_URL", if (aiWorkerUrl.isNotBlank()) aiWorkerUrl else recipeImageWorkerUrl))
    .get()

android {
    namespace = "com.example.smartexpapp"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.smartexpapp"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "AI_WORKER_URL", "\"${aiWorkerUrl.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
        buildConfigField("String", "RECIPE_IMAGE_WORKER_URL", "\"${recipeImageWorkerUrl.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
        buildConfigField("String", "PRODUCT_PARSER_WORKER_URL", "\"${productParserWorkerUrl.replace("\\", "\\\\").replace("\"", "\\\"")}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf(
                    "room.schemaLocation" to "$projectDir/schemas",
                    "room.incremental" to "true"
                )
            }
        }
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.room.runtime)
    implementation(libs.work.runtime)
    implementation(libs.mlkit.text.recognition)
    annotationProcessor(libs.room.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.room.testing)
    testImplementation(libs.robolectric)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
