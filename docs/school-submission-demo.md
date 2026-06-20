# Final School Submission Package And Demo Evidence

Use this document to collect the final evidence before submitting the APK or presenting the demo.

## Screenshots Or Screen Recordings

- Inventory list with product cards and filters
- Add Product manual form showing quantity validation
- OCR capture/review flow
- Smart Add text or voice review flow
- Smart recipes or assistant screen
- Stats dashboard with date range controls
- Settings/reminders screen
- Account screen with export/delete data controls

## Demo Script

1. Start from a clean local or guest session.
2. Add a product manually with a valid decimal quantity.
3. Attempt an invalid quantity such as `0` to show validation.
4. Edit the product and switch storage to `Freezer`.
5. Add another product through OCR review.
6. Use Smart Add for multiple products in one sentence.
7. Filter inventory by storage and expiry state.
8. Mark one product consumed or wasted, undo it, then restore from history.
9. Open stats and switch week/month/all-time ranges.
10. Open settings/account and show reminders, export, and delete-local-data controls.

## Known Limits

- Cloudflare Workers AI is optional. If URLs are not configured, local fallback remains functional.
- Firebase sync is optional and requires real Firebase configuration plus sign-in.
- Image OCR quality depends on source image clarity.
- The current package name is acceptable for school APK/demo submission unless the instructor requires a production package.

## Final Verification Evidence

Fill in the command result and date after the final run:

| Check | Result | Date |
| --- | --- | --- |
| `.\gradlew.bat test` | Passed | 2026-06-20 |
| `.\gradlew.bat :app:assembleDebug` | Passed | 2026-06-20 |
| `.\gradlew.bat :app:assembleRelease` | Passed; signed local release APK generated at `app/build/outputs/apk/release/app-release.apk` | 2026-06-20 |
| `.\gradlew.bat :app:lintDebug` | Passed | 2026-06-20 |
| `npm run test:firestore-rules` | Passed, 10 tests | 2026-06-20 |
| `.\gradlew.bat :app:assembleDebugAndroidTest` | Passed | 2026-06-20 |
| `.\scripts\build-placeholder-debug-apk.ps1` | Passed; local `app/google-services.json` restored | 2026-06-20 |
| `.\scripts\scan-apk-secrets.ps1` | Passed for debug APK; no stale Gemini app strings, Cloudflare token, Firebase key, or provider-key patterns | 2026-06-20 |
| `.\scripts\scan-apk-secrets.ps1 -ApkPath app\build\outputs\apk\release\app-release.apk` | Passed for signed release APK; no stale Gemini app strings, Cloudflare token, Firebase key, or provider-key patterns | 2026-06-20 |
| Android SDK `apksigner verify --print-certs app\build\outputs\apk\release\app-release.apk` | Passed; certificate SHA-256 `ef3b4ec9bb8010c83e78aea4a2c85b182a11c8609678dc4c50fec96cd4c7b21f` | 2026-06-20 |
| `.\gradlew.bat connectedDebugAndroidTest` | Passed on `Medium_Phone_API_36.1` AVD | 2026-06-20 |
| Signed release install/launch smoke | Passed on `Medium_Phone_API_36.1`; `app-release.apk` installed and launched `InventoryActivity` | 2026-06-20 |
| Full manual demo smoke test | Pending: run the demo script above before presenting | Pending |
