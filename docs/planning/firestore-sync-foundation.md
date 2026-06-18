# Firestore Sync Foundation

This document is the contract for issues #78, #80, and #81. It sets up Firestore as an authenticated, owner-scoped sync target while keeping SmartExpApp local-first through Room.

## Status

- Firestore Android SDK is available through the existing Firebase BOM.
- Firestore is not used by product, settings, or history repositories yet.
- Local-only and guest mode remain local-only.
- The app must continue to compile with the checked-in dummy Firebase config.

## Android Setup

Gradle uses the existing Firebase BOM and adds:

```kotlin
implementation(libs.firebase.firestore)
```

The app exposes Firestore path constants in `FirestoreContract` and a guarded `FirestoreProvider`. `FirestoreProvider.getInstance(context)` returns `null` when the project is using the generated placeholder `google-services.json`, so future sync code can skip cloud sync without breaking local builds.

## Firestore Paths

All syncable user data lives under the authenticated Firebase UID:

```text
users/{uid}
users/{uid}/products/{productId}
users/{uid}/settings/default
users/{uid}/inventoryActions/{actionId}
users/{uid}/categories/{categoryId}
users/{uid}/storageLocations/{storageLocationId}
```

No user-owned data should be stored in top-level collections. Rules deny every path not listed above.

## Product Document

`users/{uid}/products/{productId}` mirrors the Room product model used by issue #80.

Required fields:

```text
ownerUserId: string, must equal {uid}
name: string
category: string
quantity: string
unit: string
storageLocationId: string
expiryDateMillis: number
status: ACTIVE | EXPIRED | CONSUMED | WASTED | DONATED
createdAt: number
updatedAt: number
```

Optional fields:

```text
localId: string
barcode: string
imageUri: string
deletedAt: number
```

Room mapping:

```text
ProductEntity.id            -> localId
ProductEntity.cloudId       -> Firestore document id
ProductEntity.ownerUserId   -> ownerUserId
ProductEntity.syncStatus    -> local sync state only
ProductEntity.lastSyncedAt  -> local sync bookkeeping only
created_at/updated_at       -> createdAt/updatedAt
```

`syncStatus` and `lastSyncedAt` are not remote fields. They describe the local device's relationship to Firestore and can differ per install.

## Settings Document

`users/{uid}/settings/default` is reserved for issue #81.

Fields:

```text
ownerUserId: string, must equal {uid}
displayName: string
reminderDaysBefore: number
dietaryPreferences: string
darkMode: boolean
languageTag: string
defaultStorageLocationId: string
notificationEnabled: boolean
createdAt: number
updatedAt: number
```

The Firebase Auth profile remains the source of truth for signed-in account identity. The settings display name is the app preference used by local UI and must not be treated as an authentication name.

## Inventory Action Document

`users/{uid}/inventoryActions/{actionId}` is reserved for issue #81.

Fields:

```text
ownerUserId: string, must equal {uid}
localId: string
productLocalId: string
productCloudId: string
actionType: EXPIRED | CONSUMED | WASTED | DONATED
quantityChanged: number
actionAt: number
note: string
createdAt: number
updatedAt: number
```

Stats should be derived from synced action history after #81 lands. Product status and action history must be reconciled together when possible, because status-only sync can corrupt waste-prevention stats.

## Categories And Storage

`users/{uid}/categories/{categoryId}` is reserved for custom categories once category persistence is repository-backed.

Fields:

```text
ownerUserId: string, must equal {uid}
name: string
sortOrder: number
createdAt: number
updatedAt: number
deletedAt: number
```

Default storage locations are seeded locally and should not be user-writable in Firestore v1. The `storageLocations` path is read-only for future server-managed or migration data, and current rules deny client writes.

## V1 Sync Scope

Issue #80:

- Products for signed-in users.
- Product creates, edits, status changes, and deletes.
- Offline retry through local `syncStatus` and `lastSyncedAt`.

Issue #81:

- User settings.
- Custom categories after category persistence moves behind a repository.
- Inventory action history for consumed, wasted, donated, and expired actions.

Deferred:

- Guest/local-only data upload without explicit migration.
- OCR scan metadata.
- Recipe cache.
- Agent chat/message history.
- Local product images or image file upload.
- Household sharing and multi-user write access.

## Guest And Local Migration

Guest and local-only records do not sync. When a user signs in, new records receive `ownerUserId` from Firebase Auth and become eligible for sync.

Existing local records with no `ownerUserId` stay local until a dedicated migration flow is implemented. That flow must be explicit to the user because it changes where their data is stored.

## Conflict Policy

Use last-write-wins based on `updatedAt` for the first product sync implementation:

- If remote `updatedAt` is newer than local `updatedAt`, apply remote to Room.
- If local `updatedAt` is newer than remote `updatedAt`, keep local and upload it.
- If timestamps are equal, prefer the local row and mark it synced after confirming the remote document matches.
- If both local and remote changed while offline and neither side clearly wins, set local `syncStatus` to a conflict state before overwriting.

Deletes should use a `deletedAt` tombstone remotely so a second device can remove the local row. Local hard delete can happen after the tombstone is uploaded and observed, or after a retention window is added.

The Android implementation uses the local product id as the Firestore document id until `cloudId` is known. On successful upload, Room stores the document id in `cloudId`, sets `syncStatus` to `SYNCED`, and records `lastSyncedAt`.

## Security Rules

Rules live in `firestore.rules`.

They enforce:

- A signed-in Firebase user is required.
- The path UID must equal `request.auth.uid`.
- User-owned documents with `ownerUserId` must match the path UID.
- Unknown top-level paths and unknown nested user paths are denied.
- OCR, recipe cache, and agent history are not syncable until explicitly added.

## Emulator Workflow

Install Firebase CLI if needed:

```powershell
npm install -g firebase-tools
```

Start the local emulator:

```powershell
firebase emulators:start --only firestore
```

Run a rules-only check in CI or locally:

```powershell
firebase emulators:exec --only firestore "Write-Host Firestore emulator started"
```

Manual rule verification checklist:

- Signed-in UID `user-a` can read and write `users/user-a/products/{id}` when `ownerUserId` is `user-a`.
- Signed-in UID `user-a` cannot read or write `users/user-b/products/{id}`.
- Signed-in UID `user-a` cannot create `users/user-a/products/{id}` with `ownerUserId` set to `user-b`.
- Unauthenticated requests cannot read or write any user path.
- Writes to deferred paths such as `users/user-a/agentMessages/{id}` are denied.

## Build Verification

Run:

```powershell
.\gradlew.bat :app:assembleDebug
```

The app should build with the generated dummy `app/google-services.json`. Real Firestore sync requires a real Firebase project config and authenticated Firebase user.
