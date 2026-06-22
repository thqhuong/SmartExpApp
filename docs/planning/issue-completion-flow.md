# Issue Completion Flow

This document defines the recommended order for completing GitHub issues #27-#61. The goal is to keep all three developers busy while avoiding branches that repeatedly conflict or build on unstable code.

All implementation branches should be created from `dev`. If an issue lists a dependency, do not start final integration until the dependency is merged into `dev`; exploratory reading and planning can still happen earlier.

## Team Lanes

| Lane | Owner | Main responsibility |
| --- | --- | --- |
| Critical path | `thqhuong` | Architecture, auth, release security, CI |
| Core product | `dhuyy-06` | Dashboard/Add Product refactors, inventory, OCR, Smart Add, agent UI |
| Parallel polish | `QusyPlus` | Docs, stats, UI cleanup, accessibility, tests, release docs |

## Critical Merge Gates

These issues should be treated as gates. Several later branches should wait until they are merged to `dev`.

| Gate | Must merge before | Reason |
| --- | --- | --- |
| #27 Add app dependency container | #28, #30, #31, #46, parts of #57 | Establishes dependency access pattern. |
| #28 Convert ProductRepository to injectable instance | #29, #30, #37, #38, #40, #48 | Product data access changes affect inventory/dashboard/stats work. |
| #29 Add InventoryViewModel | #37, #38, parts of #56 | Inventory UI tests and RecyclerView should target the new state layer. |
| #31 Add AddProductViewModel | #39, #40, #42, #43, parts of #56 | Add/edit/OCR/Smart Add behavior should stabilize before feature hardening. |
| #32 Firebase Auth setup | #33, #34, #35 | Auth features need dependencies and config in place. |
| #33 Email/password auth and #34 Google sign-in | #35, parts of #56 | Account state UI depends on real auth flows. |
| #45 Gemini proxy/security path | #60 | Release hardening must verify no Gemini secret is embedded. |
| #58 GitHub Actions CI | Later PRs should use it once merged | Speeds review and catches broken branches early. |

## Optimized Work Waves

### Wave 0: Start Immediately

These issues can begin at the same time from current `dev`.

| Issue | Owner | Dependency | Notes |
| --- | --- | --- | --- |
| #27 Add app dependency container | `thqhuong` | None | Highest priority. Keep scope minimal so other work can rebase quickly. |
| #32 Set up Firebase Authentication dependencies and project config | `thqhuong` | None | Can run in parallel with #27 if Gradle changes are coordinated. |
| #37 Replace Inventory LinearLayout product list with RecyclerView | `dhuyy-06` | None for prototype; final merge should wait for #29 if it lands soon | Start by preparing adapter/item binding. Expect rebase after #29. |
| #39 Harden product quantity and unit validation | `dhuyy-06` | None for validation research; final merge ideally after #31 | Can identify validation rules now. |
| #41 Standardize storage naming and values | `dhuyy-06` | None | Low conflict if kept to constants/mapping/strings. Useful before #39/#42. |
| #51 Move remaining hardcoded UI text into string resources | `QusyPlus` | None | Work screen-by-screen; avoid touching files under active refactors if possible. |
| #52 Replace top bar placeholder actions with real navigation | `QusyPlus` | None | Small, high-value UI cleanup. |
| #59 Create release checklist and setup documentation | `QusyPlus` | None | Can start as docs-only and update as auth/proxy decisions land. |
| #61 Remove barcode from final-product scope while preserving harmless schema fields | `QusyPlus` | Completed | Docs/UI cleanup completed; DB fields preserved. |

### Wave 1: Architecture Foundation

Start after #27 is merged to `dev`.

| Issue | Owner | Dependency | Notes |
| --- | --- | --- | --- |
| #28 Convert ProductRepository from static API to injectable instance | `thqhuong` | #27 | Keep compatibility wrappers temporarily if that reduces conflict. |
| #46 Extract Gemini client behind an interface | `dhuyy-06` | #27 | Can proceed before #45; should make #45 easier. |
| #57 Add WorkManager reminder scheduling tests | `QusyPlus` | #27 preferred | If scheduler abstraction is introduced, tests should target that shape. |

### Wave 2: ViewModels and Auth Features

Start after the relevant Wave 1 dependencies merge.

| Issue | Owner | Dependency | Notes |
| --- | --- | --- | --- |
| #29 Add InventoryViewModel and move inventory filtering/sorting out of Activity | `thqhuong` | #28 | Blocks the cleanest version of #37 and #38. |
| #30 Add DashboardViewModel and move dashboard grouping out of MainActivity | `dhuyy-06` | #28 | Helps #48 stats reuse calculations. |
| #31 Add AddProductViewModel for manual, edit, OCR, and Smart Add flows | `dhuyy-06` | #27; ideally #28 | Large branch. Coordinate with #39/#40/#42/#43. |
| #33 Build email and password account creation and sign-in | `thqhuong` | #32 | Keep UI minimal until #35. |
| #34 Add Google sign-in using Credential Manager and Firebase Auth | `thqhuong` | #32 | Can run parallel with #33 if auth service boundaries are clear. |
| #50 Rename dashboard and stats language consistently | `QusyPlus` | None; ideally after #30 if touching dashboard files | Good parallel work if scoped to strings/docs first. |
| #55 Replace ExampleUnitTest and ExampleInstrumentedTest with real smoke tests | `QusyPlus` | None | Can start with non-auth smoke coverage; expand later. |

### Wave 3: Core Product Integration

Start after ViewModel and auth foundations merge.

| Issue | Owner | Dependency | Notes |
| --- | --- | --- | --- |
| #35 Rework Settings and Account screens around real auth state | `thqhuong` | #33 and #34 | Merges auth into visible product UI. |
| #37 Replace Inventory LinearLayout product list with RecyclerView | `dhuyy-06` | #29 for final merge | If started earlier, rebase and wire to InventoryViewModel. |
| #38 Add undo for delete and status actions | `dhuyy-06` | #29 and #37 preferred | Easier once inventory state/list handling is stable. |
| #39 Harden product quantity and unit validation | `dhuyy-06` | #31 preferred | Apply validation through AddProductViewModel. |
| #40 Move category management out of AddProductActivity | `dhuyy-06` | #31 preferred | Avoid doing this before AddProductActivity responsibilities are reduced. |
| #42 Polish OCR review and correction UX | `dhuyy-06` | #31 preferred | Keep confirm-before-save behavior intact. |
| #43 Harden Smart Add draft parsing and batch review | `dhuyy-06` | #31 and #46 preferred | Uses AddProductViewModel and fakeable Gemini client. |
| #36 Define signed-in local data ownership | `QusyPlus` | #33, #34, #35 | Requires real auth state and account decisions. |
| #48 Build dedicated stats screen from Room-backed actions | `QusyPlus` | #30 preferred | Can reuse DashboardViewModel or extracted stats calculations. |
| #53 Accessibility pass for touch targets and content descriptions | `QusyPlus` | Best after #37 and #35 for touched screens | Do final TalkBack pass after large UI churn. |

### Wave 4: Agent, Stats, and Release Security

Start once core product branches are stable enough to avoid churn.

| Issue | Owner | Dependency | Notes |
| --- | --- | --- | --- |
| #45 Move Gemini calls behind release-safe backend or proxy | `thqhuong` | #46 preferred | Security-critical. Must land before release hardening. |
| #47 Improve Agent and Recipe UI states | `dhuyy-06` | #46 and ideally #45 | Should understand Gemini vs local fallback vs proxy errors. |
| #48 Build dedicated stats screen from Room-backed actions | `QusyPlus` | #30; can run parallel with #45/#47 | Coordinate navigation naming with #50. |
| #49 Add date ranges and trend summaries to stats | `QusyPlus` | #48 | Do after base stats screen exists. |
| #44 Add privacy copy for OCR, voice, AI parsing, and local data | `QusyPlus` | #45 preferred for final copy | Can draft early, finalize after proxy/security decision. |
| #54 Remove stale auth and prototype visual resources | `QusyPlus` | #35 and #51 preferred | Avoid deleting resources before auth UI and string cleanup settle. |

### Wave 5: CI, Tests, and Release Closeout

These should finish near the end, but some can start earlier.

| Issue | Owner | Dependency | Notes |
| --- | --- | --- | --- |
| #58 Add GitHub Actions CI for Android checks | `thqhuong` | None | Start early if possible; it benefits all later PRs. |
| #56 Add UI tests for critical user flows | `QusyPlus` | #33, #34, #35, #37, #39 | Test final user flows, not temporary UI. |
| #60 Harden release build and privacy backup behavior | `thqhuong` | #45, #58, #59; ideally all feature work | Final gate before release submission. |
| #59 Create release checklist and setup documentation | `QusyPlus` | Can start immediately; finalize after #45 and #60 | Keep updated throughout the project. |

## Async Work That Does Not Need to Wait

These can be worked on immediately and merged whenever clean, as long as the owner avoids files under active large refactors.

- #51 Move remaining hardcoded UI text into string resources.
- #52 Replace top bar placeholder actions with real navigation.
- #58 Add GitHub Actions CI for Android checks.
- #59 Create release checklist and setup documentation.
- #61 Remove barcode from final-product scope while preserving harmless schema fields.
- #41 Standardize storage naming and values, if kept to constants/strings/mapping and coordinated with Add Product.
- #55 Replace example tests with real smoke tests, if it avoids auth-dependent tests until auth is ready.

## Issues That Should Wait

Do not start final implementation for these until their blockers are merged.

| Issue | Wait for |
| --- | --- |
| #28 | #27 |
| #29 | #28 |
| #30 | #28 |
| #31 | #27, preferably #28 |
| #33 | #32 |
| #34 | #32 |
| #35 | #33 and #34 |
| #36 | #35 |
| #37 | #29 for final merge |
| #38 | #29, preferably #37 |
| #39 | #31 preferred |
| #40 | #31 preferred |
| #42 | #31 preferred |
| #43 | #31 and #46 preferred |
| #45 | #46 preferred |
| #47 | #46, preferably #45 |
| #48 | #30 preferred |
| #49 | #48 |
| #53 | #35 and #37 preferred |
| #54 | #35 and #51 preferred |
| #56 | #33, #34, #35, #37, #39 |
| #60 | #45, #58, #59 |

## Recommended Weekly Pull Request Strategy

1. Keep PRs small: one issue per branch unless two issues are explicitly coupled.
2. Merge #27 first, then immediately rebase #28, #30, #31, and #46 onto `dev`.
3. Merge #32 early so auth work can split into #33 and #34 in parallel.
4. Prefer this merge order for the critical path:
   - #27 -> #28 -> #29
   - #32 -> #33 and #34 -> #35 -> #36
   - #46 -> #45 -> #60
5. Let `QusyPlus` keep a low-conflict docs/tests/UI cleanup branch moving while the larger architecture branches are in review.
6. Once #58 lands, require every later PR to pass CI before review.

## Conflict Hotspots

Coordinate before editing these files because multiple issues are likely to touch them:

- `app/src/main/java/com/example/smartexpapp/AddProductActivity.java`: #31, #39, #40, #42, #43, #51, #53.
- `app/src/main/java/com/example/smartexpapp/InventoryActivity.java`: #29, #37, #38, #41, #51, #53.
- `app/src/main/java/com/example/smartexpapp/data/ProductRepository.java`: #28, #29, #30, #37, #38, #40, #48.
- `app/src/main/java/com/example/smartexpapp/data/AgentRepository.java`: #43, #45, #46, #47.
- `app/src/main/java/com/example/smartexpapp/SettingsActivity.java` and `AccountDetailsActivity.java`: #35, #36, #51, #53.
- `app/src/main/res/values/strings.xml`: #35, #41, #44, #50, #51, #52, #53.

## Definition of Ready for Each Issue

Before opening a PR:

- Branch was created from current `dev` or recently rebased onto `dev`.
- The issue body acceptance criteria are addressed.
- `.\gradlew.bat test` passes unless the issue is docs-only.
- `.\gradlew.bat :app:assembleDebug` passes unless the issue is docs-only.
- Any issue touching UI includes a short manual smoke-test note in the PR.

