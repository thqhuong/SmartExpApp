import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

// Generate placeholder google-services.json if it doesn't exist to prevent compile failure
val googleServicesFile = file("google-services.json")
if (!googleServicesFile.exists()) {
    googleServicesFile.writeText(
        """
        {
          "project_info": {
            "project_number": "123456789012",
            "project_id": "placeholder-project-id",
            "storage_bucket": "placeholder-project-id.appspot.com"
          },
          "client": [
            {
              "client_info": {
                "mobilesdk_app_id": "1:123456789012:android:0123456789abcdef",
                "android_client_info": {
                  "package_name": "com.example.smartexpapp"
                }
              },
              "oauth_client": [
                {
                  "client_id": "1234567890-placeholder.apps.googleusercontent.com",
                  "client_type": 3
                }
              ],
              "api_key": [
                {
                  "current_key": "placeholder_api_key_value"
                }
              ],
              "services": {
                "appinvite_service": {
                  "other_platform_oauth_client": []
                }
              }
            }
          ],
          "configuration_version": "1"
        }
        """.trimIndent()
    )
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
    implementation(libs.recyclerview)
    annotationProcessor(libs.room.compiler)

    // Firebase & Credential Manager
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.androidx.credentials.core)
    implementation(libs.androidx.credentials.play)
    implementation(libs.googleid)
    testImplementation(libs.junit)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.room.testing)
    testImplementation(libs.robolectric)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
