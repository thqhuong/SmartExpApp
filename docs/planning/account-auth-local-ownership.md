# Account Auth and Local Ownership

Date: 2026-06-18

## Scope

This note covers issue #35: account-state UI and signed-in local ownership. It intentionally does not add Firestore or cloud sync behavior.

## Account States

SmartExpApp supports three account states before sync is implemented:

1. Signed in: Firebase Auth has a non-anonymous user. Settings and Account Details show the Firebase display name or email. New local products are tagged with that user's Firebase UID in `ownerUserId`.
2. Guest: the user entered through guest mode or Firebase anonymous auth. Data remains local to the device. New products do not receive an `ownerUserId`.
3. Local-only: Firebase is not configured or the user has not signed in. Data remains local to the device. New products do not receive an `ownerUserId`.

## Guest To Signed In

Signing in after guest usage does not delete, hide, or upload existing guest/local data. Existing local rows remain visible on the device. New signed-in product records are associated with the signed-in Firebase UID so later sync work can distinguish user-owned records from pre-sign-in local records.

## Deferred Sync

Firestore upload/download, sync status UI, and account-level cloud recovery remain deferred to the Firestore follow-up issues. The account screen should not claim that sync, backup, or cloud security is active until that work lands.
