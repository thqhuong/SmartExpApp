# SmartExpApp Project Completion Roadmap

Date inspected: 2026-06-12  
Branch inspected: `dev`  
Primary delivery target: native Android app in `app/`

## 1. Inspection Summary

The Android app is a Java/XML native application with AppCompat, Material Components, ConstraintLayout, Room, Robolectric tests, and ML Kit text recognition. The project also contains `googleaistudio/` and `stitch_smart_expiry_tracker/` design/prototype artifacts. Those should stay as reference material unless the team explicitly chooses to build and maintain a separate web app.

Current native app state:

- Inventory CRUD is partially implemented with Room-backed products.
- Dashboard, inventory, add/edit product, settings, notification settings, account details, help, and recipe/agent screens exist.
- OCR product entry is implemented with ML Kit text recognition: scanned text can fill a reviewable local product draft and expiry-date candidates remain confirm-before-save.
- Room schema already includes future concepts: storage locations, inventory actions, expiry scans, recipe cache, agent messages, and user settings.
- Support, advanced stats, and some release-readiness tasks are still incomplete or placeholder behavior.
- There is no production backend service in this branch. The native app is local-first Room data plus optional direct Gemini calls when configured. The React prototype calls `/api/generate-recipes`, but no matching checked-in Express/API route exists.

Verification performed:

- `.\gradlew.bat test` passes.
- `.\gradlew.bat :app:assembleDebug` passes.
- `.\gradlew.bat :app:lintDebug` now passes after fixing the blocking AppCompat tint usage.
- `C:\Users\ADMIN\AppData\Local\Android\Sdk\platform-tools\adb.exe devices` runs, but no emulator or device is currently attached for end-to-end runtime verification.

Completed local-first stabilization in this branch:

- Removed the fake sign-in entry point from the active app.
- Added Gemini-key-aware recipe suggestions with local inventory fallback and local recipe/message cache writes.
- Added explicit voice and typed Smart Add controls that use provider-neutral AI parsing when configured, fall back to local parsing, support one or more detected product drafts, and fill the Add Product form for user confirmation before each save.
- Persisted notification preferences and reminder lead time in Room-backed `UserSettingsEntity`, with local expiry reminders scheduled through WorkManager.
- Expiry reminder notifications now open Inventory with an expiring-soon filter applied.
- Added unit coverage for reminder lead-time windows, reminder notification text, and active-product filtering.
- Persisted the theme preference in Room-backed `UserSettingsEntity`; SharedPreferences is now only a cold-start theme cache.
- Added Room-backed default storage preferences and applied them to new product forms.
- Added Room-backed dietary preferences and passed them into Gemini and local fallback recipe suggestions.
- Reworked Account Details as an honest local profile screen with a Room-backed display name and no fake email/password/two-factor behavior.
- Added a real local JSON export action for products, settings, inventory actions, expiry scans, recipe cache, and agent messages.
- Added a confirmed in-app local data delete action that clears user-owned local data, resets settings defaults, and cancels reminders.
- Added cleanup for copied local product images when products are deleted, edited to replace/remove photos, discarded before save, or local data is reset.
- Improved OCR product entry by showing a reviewable product draft, showing candidate date snippets, and storing scan confidence/context when the product is saved.
- Upgraded OCR input to support both gallery images and full-resolution `TakePicture` camera capture through a scoped cache `FileProvider` URI.
- Removed remote profile/sample imagery and automatic sample-product seeding from the native app defaults.
- Replaced the hardcoded "Waste Prevented" dashboard value with a Room-backed count of consumed/donated inventory actions.
- Added a tested Room-backed dashboard snapshot for total tracked, urgent, expired, and waste-prevented metrics, and refresh it when returning to the dashboard.
- Moved Activity-facing product database access to async repository calls and removed Room `allowMainThreadQueries()`.
- Removed automatic demo product seeding so fresh local inventories stay empty until the user adds products.

## 2. Modern Android Direction

Use the current implementation as a native local-first app, then improve it incrementally rather than rewriting everything at once.

Recommended direction:

- Keep Room as the source of truth for inventory and settings.
- Add a ViewModel/state layer between Activities and repositories.
- Keep repositories as the only entry point to local database and remote services.
- Remove main-thread database queries.
- Add dependency injection. Manual DI is acceptable for a small app; Hilt becomes appropriate once ViewModels, WorkManager, and API services are added.
- Keep XML Views for the immediate release, or migrate to Jetpack Compose gradually only after the app is stable.
- Treat AI, and future cloud sync as optional data sources behind repositories, not direct Activity dependencies.

References used:

- Android architecture recommendations: https://developer.android.com/topic/architecture/recommendations
- Android Views architecture recommendations: https://developer.android.com/topic/architecture/views/recommendations-views
- Android data layer guide: https://developer.android.com/topic/architecture/data-layer
- Offline-first app guide: https://developer.android.com/topic/architecture/data-layer/offline-first
- Compose migration strategy: https://developer.android.com/develop/ui/compose/migrate/strategy

## 3. Main Gaps

### Frontend and UX

- The app uses custom bottom navigation rather than Material `NavigationBarView`, so selected state, accessibility labels, badges, and behavior must be maintained manually.
- Many screens hardcode user-visible text in layouts and Java code instead of `strings.xml`.
- `strings.xml` contains mojibake in strings such as `search_hint` and `product_meta_format`; this will display broken text.
- Many clickable ImageViews/FrameLayouts lack content descriptions.
- The previous fake sign-in path has been removed from the active native app.
- Settings now presents the app as local-only, with Room-backed local profile, notification, reminder, theme, and default storage controls.
- Notification settings now persist the master switch and selectable reminder window through Room-backed user settings, request Android 13+ notification permission, schedule local reminders, and open Inventory focused on expiring items from reminder taps.
- Account details can save the local display name, export local app data as JSON, and delete local data with confirmation.
- Help/support actions are mostly static.
- Top bar menu and search now navigate to Settings and Inventory.
- Recipe/Agent screen now uses local inventory, optional Gemini text generation, voice input, text-to-speech, local cache writes, and local fallback recipes.
- Recipe/Agent screen now has explicit voice and typed assistant entry points.
- Recipe/Agent screen now shows prompt chips generated from expiring local inventory items.
- Recipe/Agent screen now shows loading, empty-inventory, Gemini-source, and local-fallback status text instead of relying only on toasts.
- Dashboard totals, urgent count, expired count, storage summaries, grouped products, and "Waste Prevented" are now derived from Room-backed products/actions.
- Inventory uses a `LinearLayout` list instead of `RecyclerView`, which will not scale well.
- Seeded data and profile imagery now use local icons/drawables by default.
- Several labels use storage names inconsistently: "Freeze", "Freezer", "Frozen", and "Cool".

### Backend, Data, and Business Logic

- There is no app backend API or sync service in the native app.
- `ProductRepository` is still static and Context-based, but Activity-facing reads/writes now use async repository APIs.
- `AppDatabase` no longer allows main-thread Room queries.
- Room schema export is enabled and version 4 schema JSON is committed for migration review.
- `MIGRATION_1_2` is a no-op historical migration, `MIGRATION_2_3` adds the local profile display name, and `MIGRATION_3_4` adds the Room-backed theme setting. The migration path is covered by a Room migration test.
- Production reads initialize only storage/settings defaults and do not insert sample products.
- `UserSettingsEntity` now backs notification settings, reminder lead time, local display name, theme state, default storage, and dietary preferences. SharedPreferences remains only as a cold-start theme cache.
- Inventory action APIs exist for consumed/wasted/donated/expired, and the inventory UI can now trigger consumed/wasted/donated actions.
- Recipe cache and agent message tables are now written by the native recipe/agent flow.
- Product sync fields exist (`cloudId`, `ownerUserId`, `syncStatus`, `lastSyncedAt`) but no sync engine exists.
- Barcode fields exist but scanning and lookup are out of scope.
- OCR scan history is stored after product save with raw OCR text, selected date when detected or edited, candidate confidence, and no-date fallback metadata.
- Product category management still lives in Activity code, though database reads/writes now run through async repository calls.
- Business rules such as expiry grouping, progress, category defaults, and storage mapping still need a cleaner domain layer, but dashboard metric counts now have repository-level test coverage.

### Build, Quality, and Security

- Lint currently passes for the debug variant.
- Lint still reports maintainability issues worth tracking over time, especially hardcoded text and unused/prototype resources if stricter checks are enabled.
- Generated/prototype folders increase repository noise and should be clearly marked as reference or moved out of the shipping source path.
- The web prototype README now labels `googleaistudio/` as reference-only and says `npm run dev` starts Vite only; no checked-in backend route exists.
- `INTERNET` permission is declared for optional Gemini calls; document the local/dev API-key setup and future production proxy strategy before release.
- Android 13+ notification permission handling is present for expiry reminders.
- No release build hardening is enabled: `minifyEnabled` is false, no signing/release checklist exists, and no privacy policy has been finalized.

## 4. Redundant or Candidate Cleanup

Review and either remove or deliberately wire these:

- `EditProductDialog.java` and `dialog_edit_product.xml`: editing now routes through `AddProductActivity`.
- `customCategoryInput` in `activity_add_product.xml` and `AddProductActivity`: hidden and unused by the active category flow.
- `SampleData` product/recipe/storage helpers were removed; it now only provides the settings row list.
- `bottom_nav_menu.xml` and several unused nav icons: custom bottom nav is used instead.
- Unused drawables reported by lint, especially duplicate dark/glass/nav resources.
- `ExampleUnitTest` and `ExampleInstrumentedTest`: replace with real smoke/navigation tests or remove.
- `googleaistudio` README text and Express-only dependencies were cleaned up because the web backend is not being built.

## 5. Completion Plan

### Phase 0: Stabilize the Branch

Goal: make the current app buildable, testable, and easier to change.

Steps:

1. Fix lint blocking errors by replacing `android:tint` with `app:tint` in affected XML files.
2. Fix mojibake in `strings.xml` and prototype files that may be reused.
3. Move obvious hardcoded strings to resources, prioritizing top bar, bottom nav, inventory, add product, settings, and dialogs.
4. Add content descriptions to bottom navigation, top search, theme toggle, add button, photo actions, delete actions, and avatar edit actions.
5. Remove or document dead code listed in section 4.
6. Decide whether `googleaistudio/` is reference-only. If yes, update its README and avoid treating it as a backend.
7. Add a short `docs/planning/current-audit.md` or keep this file updated as the source of truth.

Acceptance criteria:

- `.\gradlew.bat test` passes.
- `.\gradlew.bat :app:assembleDebug` passes.
- `.\gradlew.bat :app:lintDebug` has no errors. Warnings are triaged into fix-now or baseline-later.

### Phase 1: Lock Product Scope and Information Architecture

Goal: prevent the app from feeling like separate prototypes stitched together.

Steps:

1. Confirm first release scope: local-first inventory, expiry dashboard, reminders, OCR, basic agent/recipe help, settings, and stats.
2. Rename "Stats" destination to either "Dashboard" or make it a real stats screen. Avoid using `MainActivity` as an ambiguous name.
3. Decide whether bottom nav destination 4 is "Agent", "Recipes", or "Assistant". Use one name consistently in code, strings, docs, and icons.
4. Define the user journey:
   - Sign in or continue as guest.
   - Add product manually, or by OCR.
   - See expiring/expired items.
   - Act on items: consume, waste, donate, delete, edit.
   - Get reminders.
   - Ask for recipe/help.
5. Completed for the main chrome: menu/search actions now navigate to Settings and Inventory instead of placeholder toasts.

Acceptance criteria:

- Navigation labels, Activity names, screen titles, and docs use consistent product language.
- Every bottom-nav destination has working behavior, not a placeholder.

### Phase 2: Modernize App Architecture

Goal: separate UI, state, data, and domain rules.

Steps:

1. Add a simple dependency container, for example `SmartExpAppApplication`, `AppContainer`, and repository instances.
2. Convert `ProductRepository` from static methods into an injectable class.
3. Add repositories for settings, inventory actions, expiry scans, recipes, agent messages, and remote product lookup.
4. Introduce ViewModels per screen:
   - `InventoryViewModel`
   - `DashboardViewModel`
   - `AddProductViewModel`
   - `SettingsViewModel`
   - `NotificationSettingsViewModel`
   - `AgentViewModel`
   - `StatsViewModel`
5. Expose UI state through observable holders. In Java/XML, LiveData is the lower-friction path. If migrating to Kotlin, prefer StateFlow.
6. Move expiry grouping, status labels, storage summaries, category list building, and validation out of Activities.
7. Replace Activity-to-Activity state passing where possible with stable IDs and ViewModel loading.
8. Add a lightweight domain/use-case package only where logic is shared by multiple screens.

Acceptance criteria:

- Activities bind UI and dispatch user events, but do not contain database queries or business rules.
- Repositories own all data source access.
- Product CRUD, filters, and dashboard grouping are unit tested without launching Activities.

### Phase 3: Fix Persistence and Local-First Behavior

Goal: make Room the reliable source of truth.

Steps:

1. Completed: remove `allowMainThreadQueries()` from `AppDatabase`.
2. Completed for current Activity flows: run product database work on repository executors.
3. Completed: enable Room schema export and commit schema JSON files.
4. Completed for the current schema: add real migration history through version 4 and cover it with a Room migration test; keep schema history going for future changes.
5. Add indexes for frequent queries if needed: status plus expiry date, category, storage location.
6. Completed for local MVP: remove automatic demo seeding from production repository reads.
7. Completed for current MVP settings: notification settings, reminder lead time, local display name, theme state, default storage, and dietary preferences now use `UserSettingsEntity`; keep only boot-critical cache outside Room.
8. Add backup/data extraction rules that deliberately include or exclude local inventory, settings, images, and scan history.
9. Completed for product delete/edit/reset: clean up locally copied product images when products are deleted, edited to replace/remove photos, discarded before save, or all local data is reset.

Acceptance criteria:

- Empty production inventory remains empty after restart.
- Database access no longer runs on the main thread.
- Migrations are repeatable and covered by tests.
- Settings persist across process death and app restart.

### Phase 4: Complete Inventory and Expiry Workflows

Goal: make inventory management complete enough for real use.

Steps:

1. Replace the inventory `LinearLayout` list with `RecyclerView` and `ListAdapter`.
2. Add swipe or explicit actions for consume, wasted, donated, expired, edit, and delete.
3. Store inventory actions for all outcome changes.
4. Add undo for destructive actions where practical.
5. Add robust quantity/unit validation and support decimal quantities.
6. Add category CRUD through a repository or settings table instead of scanning products ad hoc.
7. Standardize storage values and labels.
8. Add empty, loading, error, and filtered-empty states for each relevant screen.
9. Add tests for search, filters, sorting, category rename/delete, storage mapping, and status transitions.

Acceptance criteria:

- User can fully manage a product lifecycle without losing data.
- Dashboard and inventory update automatically after changes.
- Product status history is reflected in the current dashboard's prevented-waste stat.

### Phase 5: Add Real Reminders and Notifications

Goal: turn notification settings into actual app behavior.

Steps:

1. Completed for expiry reminders: add a notification channel.
2. Completed: request `POST_NOTIFICATIONS` at runtime where required.
3. Completed: use WorkManager for periodic daily checks.
4. Completed for current flows: schedule or refresh reminders when products, notification preferences, or reminder lead time change.
5. Completed: persist notification settings in `UserSettingsEntity`.
6. Completed for local logic: unit-test reminder lead-time windows, active-product filtering, and notification title/text generation.
7. Completed for local MVP: notification taps open Inventory with the expiring-soon filter applied.
8. Add deeper tests around WorkManager scheduling policy and worker behavior.

Acceptance criteria:

- Toggling notification settings changes scheduled work.
- Changing the reminder window persists to Room and refreshes scheduled work.
- Expiring products generate local notifications at the configured lead time.
- Notifications open Inventory focused on expiring items; later work can target product-specific filters.

### Phase 6: Finish Smart Input

Goal: reduce manual product entry without making unreliable automation decisions.

Steps:

1. Improve OCR flow:
   - Completed: support gallery images and full-resolution camera capture through `TakePicture` and a cache `FileProvider` URI.
   - Completed: parse OCR text into a reviewable local product draft for name, quantity, category, and storage.
   - Completed: show date candidates with raw OCR snippets.
   - Completed: let users correct the date before saving.
   - Completed: store scan metadata with candidate confidence or manual-edit fallback confidence.
2. Add manual fallback for every smart-input flow.
3. Add privacy copy for image/OCR handling.

Acceptance criteria:

- OCR flows never save unconfirmed product data.
- Manual add remains reliable when scanning fails.
- Scan history is persisted and visible/debuggable.

### Phase 7: Build the Agent and Recipe Feature

Goal: replace the current recipe sample screen with useful inventory-aware assistance.

Steps:

1. Completed for local MVP: use both optional Gemini calls and local rule-based fallback.
2. For release: move Gemini calls behind a backend proxy or another secret-safe mechanism; the current direct API key path is suitable only for local/dev builds.
3. Completed: create an `AgentRepository` that reads inventory context, calls Gemini when configured, caches messages, and returns structured UI state.
4. Completed: store agent messages in `AgentMessageEntity`.
5. Completed: store recipe results in `RecipeCacheEntity`.
6. Completed: surface local fallback and empty-inventory states in the recipe UI.
7. Completed for local MVP: pass Room-backed dietary preferences into Gemini prompts and local fallback recipe summaries/status.
8. Completed: add prompt chips based on expiring local inventory items.
9. Completed: explicit typed and voice input controls are available for the current recipe/assistant surface.
10. Completed for product adding: Smart Add turns voice/text into one or more reviewable product drafts and fills the Add Product form for confirmation rather than mutating inventory directly.

Acceptance criteria:

- Agent answers are based on current local inventory.
- Recipe suggestions prioritize products expiring soon.
- Prompt chips prioritize products expiring soon and are hidden when no cookable local items exist.
- Saved dietary preferences guide Gemini prompts and local fallback recipe copy.
- Recipe screen makes local fallback and empty-inventory states visible.
- No secrets are stored in the app APK.

### Phase 8: Make Settings, Profile, and Auth Honest

Goal: avoid fake account behavior in a local-first release.

Steps:

1. Completed for the local MVP: first-release auth is deferred and the app presents a local profile only.
   - Guest/local-only mode, or
   - real Google/Firebase sign-in, or
   - later cloud sync only.
2. Completed: fake sign-in/password behavior was removed from the active native app.
3. Persist profile fields only if they are used.
4. Completed for local MVP: notification settings, theme, default storage, and dietary preferences are persisted in Room-backed settings.
5. Connect category preferences to persisted settings if category customization stays in scope.
6. Remove billing/password language unless real account management exists.
7. Completed for export: add a JSON export action for local inventory, settings, scans, recipe cache, and agent messages.
8. Completed: add a confirmed in-app delete-local-data action.

Acceptance criteria:

- Account screens do not imply unsupported security or cloud features.
- Local profile data can be exported without requiring sign-in.
- Local user-owned data can be deleted in-app with confirmation.
- Settings survive restart and affect app behavior.

### Phase 9: Complete Stats and Waste Insights

Goal: make the dashboard and stats credible.

Steps:

1. Completed for visible dashboard MVP: derive total tracked, urgent, expired, and waste-prevented counts from Room products and `InventoryActionEntity`.
2. Derive broader wasted, consumed, donated, expired, and active counts for a dedicated stats screen.
3. Add date ranges: week, month, all time.
4. Add simple trend visuals using native Views or a chart library.
5. Completed for dashboard MVP: define "waste prevented" as consumed or donated inventory actions.
6. Add tests for expanded stats calculations.

Acceptance criteria:

- Dashboard top metrics are computed through a tested Room-backed snapshot; continue replacing broader analytics placeholders as stats expand.
- Metrics have a documented calculation.
- Dashboard and stats match the same source of truth.

### Phase 10: UI Polish, Accessibility, and Localization

Goal: make the app feel like one mature Android product.

Steps:

1. Replace hardcoded strings with resources and fix all mojibake.
2. Add content descriptions and accessibility labels for all interactive controls.
3. Verify touch targets are at least 48dp.
4. Review color contrast in light and dark themes.
5. Standardize Material component usage:
   - TextInputLayout for forms where useful.
   - MaterialToolbar or a consistent custom top bar.
   - NavigationBarView unless the custom nav is intentionally kept.
6. Add landscape/tablet sanity layouts or responsive constraints for wide screens.
7. Optimize image loading with a real image library such as Coil or Glide, or add caching/downsampling if staying custom.
8. Remove excessive decorative resources and unused drawables after lint confirms they are safe to delete.
9. Add screenshots for key flows.

Acceptance criteria:

- Lint hardcoded-text warnings are fixed or intentionally baselined.
- Core flows work with TalkBack.
- Light/dark mode is consistent across every screen.

### Phase 11: Testing and Release Readiness

Goal: ship with confidence instead of only demo confidence.

Steps:

1. Keep current unit tests for product and repository behavior, then expand them.
2. Add ViewModel tests for inventory, add/edit, dashboard, settings, reminders, and agent.
3. Completed for version 2 to 3: add Room migration tests. Continue adding one for each future schema version.
4. Add UI tests for:
   - first launch,
   - add product,
   - edit product,
   - delete/undo,
   - search/filter/sort,
   - notification settings,
   - theme toggle.
5. Add lint and tests to CI.
6. Add release checklist:
   - app id finalized,
   - app name finalized,
   - launcher icon finalized,
   - min/target SDK reviewed,
   - release signing configured,
   - R8/minification decision made,
   - backup/privacy behavior documented,
   - permissions documented,
   - demo data disabled for release.

Acceptance criteria:

- `test`, `assembleDebug`, and `lintDebug` run cleanly in CI.
- Release build is reproducible.
- README describes setup, app architecture, and feature status accurately.

## 6. Suggested Implementation Order

1. Fix lint errors, mojibake, and obvious dead code.
2. Make scope honest: rename placeholders, remove fake auth promises, and document local-first behavior.
3. Add ViewModels and injectable repositories around existing Room behavior.
4. Remove main-thread Room queries and stabilize migrations.
5. Finish inventory lifecycle actions and settings persistence.
6. Add reminders.
7. Finish OCR.
8. Build agent/recipe behavior through a proper repository and optional backend proxy.
9. Replace hardcoded stats with computed insights.
10. Finish accessibility, localization, tests, CI, and release docs.

## 7. Practical Branching

Use existing planned work areas from `docs/planning/general-route.md`, but keep each PR narrowly scoped:

1. `02-foundation-persistence`: lint, cleanup, DI, repositories, ViewModels, Room migration setup.
2. `03-inventory-crud`: RecyclerView, status actions, category/storage cleanup, validation.
3. `04-expiry-reminders`: notifications, settings persistence, scheduling.
4. `05-ocr-expiry-scan`: OCR UX, scan metadata, image handling.
5. `07-agent-assistant`: typed agent, recipe cache, optional backend proxy.
6. `08-stats-settings`: computed stats, account/settings honesty, export/delete local data.
7. `09-testing-docs-submission`: UI tests, CI, screenshots, README, release checklist.

## 8. Definition of Done for the Project

The project should be considered complete when:

- A user can manage inventory without sample data reappearing unexpectedly.
- Expiry reminders work from persisted settings.
- OCR flows are confirm-before-save.
- Agent/recipe behavior is either genuinely implemented or clearly labeled as local suggestions.
- All visible account/auth behavior is real or honestly presented as local-only.
- Dashboard and stats are derived from stored data.
- App passes tests, debug build, and lint gates.
- Documentation matches the implemented product.
