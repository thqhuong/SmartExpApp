# SmartExpApp School Submission Checklist

This checklist is for the final school APK/demo package. It is not a Play Store release checklist.

## Local Setup

1. Open the repository in Android Studio from the project root.
2. Keep `local.properties` local only. It may contain optional Worker URLs:

   ```properties
   AI_WORKER_URL=https://your-worker.example.workers.dev
   RECIPE_IMAGE_WORKER_URL=https://your-worker.example.workers.dev
   PRODUCT_PARSER_WORKER_URL=https://your-worker.example.workers.dev
   ```

   *Note: These URLs are injected via `BuildConfig` during the build process. Do not commit `local.properties`.*
3. Do not commit real Firebase files or secrets. `app/google-services.json` is ignored, and the Gradle build creates a placeholder file when it is missing.

## Verification Gates

Run these before packaging the final APK:

```powershell
.\gradlew.bat test
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:lintDebug
npm run test:firestore-rules
```

With an emulator or device attached, also run:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

## Cloud And AI Behavior

- The Android app is local-first. Inventory, OCR drafts, recipe cache, settings, reminders, and export/delete flows work from local app storage.
- Cloudflare Workers AI is optional. If Worker URLs are absent, the app uses local fallback parsing and recipe suggestions.
- Worker URLs are public endpoint configuration, not private API keys. Any Cloudflare secrets must stay in Worker environment bindings or Cloudflare dashboard settings, not in the APK.
- Firestore sync is optional and only applies to signed-in user data when Firebase is configured. Guest/local-only data remains local.
- The checked-in Worker under `cloudflare-recipe-images/` supports recipe images, product parsing, and AI recipe/chat endpoints.
- **Declared Permissions:**
  - `INTERNET`: Required for communicating with Cloudflare AI endpoints and Firestore.
  - `POST_NOTIFICATIONS`: Required for Android 13+ to send local expiry reminders.
  - *Note: The camera intent is used via `MediaStore.ACTION_IMAGE_CAPTURE`, so no explicit `CAMERA` permission is required in the manifest.*

## Privacy And Security Notes

- OCR uses images selected or captured by the user to extract expiry text and product drafts. These are processed completely on-device (via ML Kit) and not sent to the cloud.
- Voice input uses Android speech recognition, then the parsed text may be handled locally or by the configured Worker.
- The Privacy Policy (if hosted) must explicitly disclose the use of on-device images (OCR) and text generation via AI endpoints (Cloudflare), and clarify that inventory data is stored locally on the device by default.
- Local export/delete controls are available in account/settings flows.
- Backup remains enabled. Review `app/src/main/res/xml/backup_rules.xml` and `app/src/main/res/xml/data_extraction_rules.xml` if the school requires a stricter privacy posture.
- Before the final APK demo, verify that no real secrets are embedded in `BuildConfig`, `local.properties`, `.env*`, `google-services.json`, or documentation screenshots.

For the cleanest school evidence build, use the placeholder Firebase helper so any ignored local Firebase config is restored after the build:

```powershell
.\scripts\build-placeholder-debug-apk.ps1
.\scripts\scan-apk-secrets.ps1
```

## APK Sanity

- Keep the package name `com.example.smartexpapp` for school submission unless the school explicitly requires a production package.
- Verify English and Vietnamese resources both compile with `lintDebug`.
- Check the visible storage labels: `Room Temp`, `Refrigerator`, and `Freezer` in English, with matching Vietnamese translations.
- Confirm quantity validation rejects blank, zero, negative, and non-numeric values, and accepts positive decimals.

## Manual Smoke Test

- First launch as guest/local user.
- Add, edit, delete, undo, and restore a product.
- Add a product through OCR review.
- Add one or more products with Smart Add text or voice.
- Search, filter, sort, and open product actions from inventory.
- Review stats ranges and storage overview.
- Toggle reminder settings.
- Open account/settings, export data, and verify delete-local-data confirmation.
- Test with Cloudflare Worker URLs configured and with them blank to confirm local fallback.

## Play Store Later

For a future Play Store release, revisit package rename, signing, R8/minification, hosted privacy policy, store assets, Play Data Safety, and production monitoring. Those are not required for the current school submission unless the school specifically asks for them.
