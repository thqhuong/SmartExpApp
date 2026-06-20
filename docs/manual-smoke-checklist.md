# Manual Smoke Coverage Checklist

Manual critical-flow coverage for issue **#56** (final school submission). Run once on a
physical device or emulator, record date/device, and tick each row. Pair this with the
automated gates below and the accessibility pass in
[accessibility-smoke-checklist.md](accessibility-smoke-checklist.md).

## Automated gates (run first)

```powershell
.\gradlew.bat testDebugUnitTest            # unit + Robolectric (contracts, reminders, viewmodels)
.\gradlew.bat :app:assembleDebugAndroidTest  # instrumentation tests compile
.\gradlew.bat connectedDebugAndroidTest    # run instrumented smoke on attached device/emulator
```

`connectedDebugAndroidTest` runs the guest-mode launch smoke test (`ExampleInstrumentedTest`)
and requires an attached device/emulator. The unit suite includes the storage/quantity
contracts (`StorageContractTest`, `ProductQuantityValidatorTest`, `ExampleUnitTest`) and the
reminder scheduling check (`ReminderSchedulerTest`, `ExpiryReminderContentTest`).

## Manual critical flows

### Inventory lifecycle
- [ ] **Add** a product manually → appears in inventory with correct name/qty/storage/expiry.
- [ ] **Edit** the product (name, storage, expiry, photo) → changes persist after reopening.
- [ ] **Delete** a product → removed; **Undo** snackbar restores it.
- [ ] **Mark** consumed / wasted / donated → status updates; **Undo** reverts it.
- [ ] Restored/reverted items reappear in the active inventory.

### Smart input
- [ ] **OCR review**: scan a label (camera or gallery) → reviewable draft with candidate dates;
      nothing is saved until confirmed; correcting the date before save works.
- [ ] **Smart Add** (voice and typed): produces one or more reviewable drafts that fill the
      Add Product form for confirmation (does not silently mutate inventory).

### Browse
- [ ] **Filters** (expiry: all/still-good/expired; storage: all/room/fridge/freezer) narrow the list.
- [ ] **Sort** (oldest/name/newest) reorders correctly.
- [ ] Search narrows by name; filtered-empty state shows the right message.

### Insights & reminders
- [ ] **Stats**: totals, urgent, expired, and waste-prevented reflect current data.
- [ ] **Reminders**: enable notifications + set lead time → reminder fires for expiring items;
      tapping it opens Inventory filtered to expiring-soon.

### Account & data
- [ ] **Export** local data → JSON file is produced/shareable.
- [ ] **Delete** local data (with confirmation) → inventory/settings reset; reminders cancelled.

### AI configuration
- [ ] **Cloudflare configured** (worker URLs set): recipes/Smart Add use the Worker.
- [ ] **Offline / local fallback** (no network or no worker URL): app stays usable; local
      fallback recipes and local parsing work; no crash.

## Result

| Field | Value |
|---|---|
| Date | |
| Device / Android version | |
| Cloudflare configured | ☐ yes ☐ local-fallback only |
| `testDebugUnitTest` | ☐ pass |
| `:app:assembleDebugAndroidTest` | ☐ pass |
| `connectedDebugAndroidTest` | ☐ pass (device: __________ ) |
| Manual flows above | ☐ all pass ☐ issues (note below) |

Notes:
