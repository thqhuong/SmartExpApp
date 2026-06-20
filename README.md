# SmartExpApp

SmartExpApp is a local-first Android app for tracking food inventory, expiry dates, reminders, OCR-assisted entry, Smart Add parsing, recipe suggestions, stats, and account data controls.

## Current Architecture

- Android native app in `app/`
- Room-backed local inventory, history, categories, reminders, recipe cache, and settings
- Optional Firebase Auth/Firestore sync for signed-in users
- Optional Cloudflare Workers AI endpoints for recipe/chat generation, product parsing, and recipe images
- Local fallback behavior when Cloudflare URLs are blank or unavailable
- Firestore rules tests under `scripts/` and `firestore.rules`

## Setup

Open the project root in Android Studio. A real `app/google-services.json` is optional for local/demo work and is ignored by Git. If it is missing, the Gradle build creates a placeholder file so local builds can still run.

Optional Worker URLs can be set in `local.properties` or environment variables:

```properties
AI_WORKER_URL=https://your-worker.example.workers.dev
RECIPE_IMAGE_WORKER_URL=https://your-worker.example.workers.dev
PRODUCT_PARSER_WORKER_URL=https://your-worker.example.workers.dev
```

Do not put Cloudflare API tokens, Firebase service credentials, or AI provider secrets in the Android app. Keep secrets in local-only files or Worker environment configuration.

## Verification

```powershell
.\gradlew.bat test
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:lintDebug
npm run test:firestore-rules
```

With an emulator/device attached:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

## Release APK

For a signed release APK, create an ignored `release-signing.properties` file in the project root:

```properties
STORE_FILE=C:\\path\\to\\smartexp-release.jks
STORE_PASSWORD=...
KEY_ALIAS=...
KEY_PASSWORD=...
```

Or set equivalent environment variables: `SMARTEXP_RELEASE_STORE_FILE`, `SMARTEXP_RELEASE_STORE_PASSWORD`, `SMARTEXP_RELEASE_KEY_ALIAS`, and `SMARTEXP_RELEASE_KEY_PASSWORD`.

Then run:

```powershell
.\gradlew.bat :app:assembleRelease
```

If signing values are absent, Gradle still produces `app-release-unsigned.apk` for inspection, but that file is not installable as a release submission artifact.

## Demo Flow

1. Launch as guest or local-only user.
2. Add a product manually and verify quantity validation.
3. Edit the product storage between `Room Temp`, `Refrigerator`, and `Freezer`.
4. Use OCR review to detect an expiry date from an image.
5. Use Smart Add by text or voice for one or more products.
6. Search, filter, sort, mark consumed/wasted/donated, undo, and restore from history.
7. Open stats and switch date ranges.
8. Review reminder settings.
9. Export data and review the delete-local-data confirmation.
10. Show Cloudflare configured behavior if URLs are available, then mention the local fallback path.

## Known Limits For School Demo

- Cloudflare Workers AI is optional; the demo remains valid with local fallback.
- Firestore sync requires Firebase configuration and sign-in.
- `connectedDebugAndroidTest` needs an attached emulator or physical device.
- This is a school APK/demo target, not a Play Store-ready release package.
