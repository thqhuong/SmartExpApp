# Firebase Authentication Setup

This document provides step-by-step instructions for setting up and configuring Firebase Authentication (Email/Password and Google Sign-In) for the **SmartExpApp** project.

---

## 1. Firebase Project Registration

1. Go to the [Firebase Console](https://console.firebase.google.com/).
2. Click **Add Project** (or select an existing one).
3. Name your project (e.g., `SmartExpApp`) and complete the creation steps.
4. Once the project is created, click the **Android icon** on the project overview page to register a new Android app.
5. In the **Android package name** field, enter:
   ```
   com.example.smartexpapp
   ```
   *Note: If you change the app's package name for release (e.g., in `app/build.gradle.kts`), you must register the new package name in the Firebase project settings as well.*
6. (Optional) Provide an app nickname (e.g., `SmartExp App Dev`).
7. Enter the **Debug signing certificate SHA-1** fingerprint (see [Section 4](#4-generating-sha-fingerprints) on how to generate this). This is **required** for Google Sign-In to function.
8. Click **Register app**.

---

## 2. Configuration File (`google-services.json`)

1. After registering the app, download the `google-services.json` file from the Firebase console.
2. Move/copy the downloaded `google-services.json` into the `app/` directory of your local project structure:
   ```
   SmartExpApp/
   ├── app/
   │   ├── google-services.json  <-- Place it here (overwriting the auto-generated dummy file)
   │   ├── build.gradle.kts
   │   └── src/
   ```
3. **DO NOT commit your production `google-services.json` containing sensitive keys to the repository.** We have added `/google-services.json` to [app/.gitignore](file:///c:/Users/ADMIN/AndroidStudioProjects/SmartExpApp/app/.gitignore) so it is excluded from Git tracking. On clean clones or CI/CD pipelines, a dummy placeholder `google-services.json` is automatically generated on the first Gradle build (via logic in [app/build.gradle.kts](file:///c:/Users/ADMIN/AndroidStudioProjects/SmartExpApp/app/build.gradle.kts)) to prevent compilation failures. When you add your real file, it will overwrite the dummy and will not be tracked by Git.

---

## 3. Enable Authentication Sign-In Providers

1. In the Firebase Console, select **Authentication** from the left navigation menu.
2. Click **Get Started** if this is a new setup.
3. Go to the **Sign-in method** tab.
4. Enable the following providers:
   - **Email/Password**: Click edit, toggle **Enable**, and click **Save**.
   - **Google**: Click edit, toggle **Enable**, select the project support email, and click **Save**.
     - *Note: Enabling Google Sign-In will automatically generate a Web SDK configuration Client ID, which is used under-the-hood by the Android Credentials API.*

## Firestore Setup

Firestore setup for sync work is documented in `docs/planning/firestore-sync-foundation.md`.

For Firebase Console setup:

1. Open the Firebase project used by `com.example.smartexpapp`.
2. Select **Firestore Database**.
3. Create the database.
4. Start in production mode and deploy the repository `firestore.rules`.
5. Use the emulator workflow from the Firestore sync foundation document for local development.

---

## 4. Generating SHA Fingerprints

Google Sign-In requires your app's signing certificate fingerprint (SHA-1 and optionally SHA-256) to be registered in the Firebase console.

### Local Debug Key Fingerprints

To generate fingerprints for the default Android debug keystore:

#### Option A: Using Gradle (Recommended)
1. Open a terminal in the project root directory.
2. Run the signingReport task:
   ```powershell
   .\gradlew.bat signingReport
   ```
3. Look for the `variant: debug` block. It will output something like:
   ```text
   Variant: debug
   Config: debug
   Store: C:\Users\<Username>\.android\debug.keystore
   Alias: AndroidDebugKey
   MD5: XX:XX:XX...
   SHA1: AA:BB:CC:DD:EE:FF:11:22:33:44:55:66:77:88:99:00:AA:BB:CC:DD
   SHA-256: 11:22:33...
   ```
4. Copy the **SHA1** and **SHA-256** values.

#### Option B: Using Keytool
Run the following command in your terminal (adjust path to `debug.keystore` if needed):
```powershell
keytool -list -v -alias androiddebugkey -keystore "$HOME/.android/debug.keystore" -storepass android
```

### Adding Fingerprints to Firebase

1. Open the Firebase Console.
2. Click the gear icon next to **Project Overview** in the left sidebar, and select **Project Settings**.
3. Scroll down to the **Your apps** section.
4. Select the registered `com.example.smartexpapp` app.
5. Click **Add fingerprint**.
6. Paste the SHA-1 fingerprint and click **Save**.
7. Click **Add fingerprint** again, paste the SHA-256 fingerprint, and click **Save**.
8. **Crucial:** After adding or changing SHA fingerprints, you **must download a new `google-services.json`** file and replace the old one in your project.
