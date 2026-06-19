# SmartExpApp: Setup and Release Checklist

This document details the complete path to configure, build, and release the SmartExpApp native Android product to the Google Play Store.

## 1. Local Setup & Build

For a new developer joining the project:

1. **Clone & Open:**
   - Clone the repository and checkout the latest `dev` branch.
   - Open the project root in **Android Studio**.

2. **Configure Local Properties (Dev Key Setup):**
   - In the root directory, create or open `local.properties`.
   - Add your Cloudflare AI Worker URL for local development:
     ```properties
     RECIPE_IMAGE_WORKER_URL="your-cloudflare-worker-url-here"
     ```
   - *Note: This URL is injected via `BuildConfig` during the build process. Do not commit `local.properties`.*

3. **Verify Build & Test:**
   - Run unit tests:
     ```powershell
     .\gradlew.bat test
     ```
   - Build the debug variant:
     ```powershell
     .\gradlew.bat :app:assembleDebug
     ```

## 2. Backend & Cloud Services

Although the current app operates local-first, the following cloud integrations must be managed:

- **Firebase Setup:**
  - If integrating Analytics, Crashlytics, or Auth in the future, create a Firebase project.
  - Download the `google-services.json` file and place it in the `app/` directory.
  - *Current Status:* Firebase is not yet required for local-first operations.

- **Cloudflare AI Backend & Firestore Sync:**
  - **Development:** AI calls target the Cloudflare Worker URL from `local.properties`.
  - **Production AI:** Ensure the Worker endpoint has appropriate rate limits and CORS policies configured for production domains before release.
  - **Firestore Sync:** Run `npm run test:firestore-rules` in the functions directory before release to ensure security rules are intact for synced inventory. Test sign-in and sync flows to ensure local-to-cloud sync behaves as expected for authenticated users.

## 3. Security, Permissions & Privacy

- **Declared Permissions:**
  - `INTERNET`: Required for communicating with Cloudflare AI endpoints and Firestore.
  - `POST_NOTIFICATIONS`: Required for Android 13+ to send local expiry reminders.
  - *Note: The camera intent is used via `MediaStore.ACTION_IMAGE_CAPTURE`, so no explicit `CAMERA` permission is required in the manifest.*

- **Privacy Policy:**
  - You **must** host a Privacy Policy URL.
  - The policy must explicitly disclose the use of on-device images (OCR) and text generation via AI endpoints (Cloudflare).
  - Clarify that inventory data is stored locally on the device.

- **Backup Rules:**
  - `android:allowBackup="true"` is enabled.
  - Data extraction and backup rules (`xml/data_extraction_rules.xml` and `xml/backup_rules.xml`) dictate what user data is synced with Google Drive. Review these files to ensure sensitive AI cache or temporary images are excluded if necessary.

## 4. Release Configuration

Before building the final AAB (Android App Bundle), apply these configurations in `app/build.gradle.kts` and `AndroidManifest.xml`:

- **App ID (Critical):**
  - **You MUST change the `applicationId`** from `com.example.smartexpapp` to a production-ready package name (e.g., `com.smartexp.app`). 
  - *Google Play rejects any app using the `com.example.*` namespace.*

- **App Name & Icon:**
  - Verify the final user-facing app name in `res/values/strings.xml` (`app_name`).
  - Verify the launcher icons in `res/mipmap/` (`ic_launcher` and `ic_launcher_round`).

- **Signing:**
  - Generate a secure Upload Keystore via Android Studio (Build > Generate Signed Bundle / APK).
  - Configure the `signingConfigs` block in `build.gradle.kts` for the `release` build type using environment variables or a local secure properties file.

- **Minify / R8:**
  - Enable R8 code shrinking and obfuscation in `build.gradle.kts`:
    ```kotlin
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    ```
  - Test the obfuscated release build thoroughly to ensure Room and ML Kit dependencies are not broken by R8.

## 5. Google Play Submission Checklist

Before submitting to the Play Console, ensure you have:

- [ ] **App Assets:**
  - App Icon (512x512 PNG).
  - Feature Graphic (1024x500 PNG).
  - Phone and Tablet Screenshots highlighting key features (Inventory, Add Product via OCR, Recipes).
- [ ] **Store Listing Details:**
  - Short and Long descriptions localized appropriately.
- [ ] **Privacy & Data Safety Policy:**
  - Link to the hosted Privacy Policy.
  - Fill out the Data Safety form correctly, clarifying that user inventory data remains local and explaining the API calls made for OCR and AI recipes.
- [ ] **Target API Level:**
  - Ensure `targetSdk` meets Google Play's latest requirements (currently set to 36, which is excellent).
- [ ] **Internal Testing:**
  - Upload the signed `.aab` to the Internal Testing track.
  - Have QA / team members test the installed build on real devices prior to Production rollout.
