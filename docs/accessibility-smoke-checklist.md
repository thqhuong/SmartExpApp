# Accessibility Smoke Checklist (TalkBack + Touch Targets)

Evidence checklist for issue **#53** (final UI/localization cleanup) and the manual
accessibility portion of **#56** (smoke/critical-flow tests).

Run this once on a physical device with **TalkBack ON**. Record date, device, and a
pass/fail per row. Capture a short screen recording for the submission evidence pack (#86).

## Setup

1. Device: Settings → Accessibility → TalkBack → **On**.
2. Optional: enable Settings → Accessibility → Color and motion → display layout bounds is
   not needed; instead use TalkBack's spoken feedback as the source of truth.
3. Install the debug build: `.\gradlew.bat :app:installDebug` (or run from Android Studio
   on the connected phone).

## What "pass" means

- **Interactive controls** are reachable by swiping right/left and announce a **meaningful
  label** (not "unlabelled", not the literal drawable/app name).
- **Decorative icons** (leading icons next to a text label, chevrons, illustrations) are
  **skipped** by TalkBack — they should not steal a separate focus stop.
- **Touch targets**: each control is comfortably tappable; the controls below were sized to
  ≥48dp in this change.

## Core-flow checklist

### Top bar (appears on every screen)
- [ ] Menu / Back button announces **"Menu"** on top-level screens and **"Back"** on
      sub-screens (e.g. Product History, Help). *(Back label fixed in BaseActivity.)*
- [ ] Theme toggle announces **"Toggle Light/Dark Theme"** as a **single** focus stop
      (the inner sun/moon icons should NOT each grab focus).
- [ ] Search button announces **"Search products..."**.

### Bottom navigation
- [ ] Inventory / Stats / Agent / Settings each announce their label.
- [ ] Center **Add** button announces **"Add Product"**.

### Dashboard (Stats / Main)
- [ ] Storage summary rows: the row announces the storage name + count; the leading storage
      icon is skipped.
- [ ] Expiring item rows: row announces product name + status; the product image is not a
      separate "unlabelled" stop.

### Inventory
- [ ] Each product card is focusable and announces name + expiry meta.
- [ ] Delete (X) button announces a delete label and is easy to tap (now 48dp).
- [ ] Opening the **product actions** bottom sheet: Edit / Mark Consumed / Mark Wasted /
      Mark Donated / Delete each announce their text label; the circular action icons are
      skipped (decorative).

### Add Product
- [ ] Name, quantity, unit, category, storage controls are reachable and labelled.
- [ ] Storage options (Room Temp / Refrigerator / Freezer) announce their text label; the
      storage icon is skipped.
- [ ] Photo picker frame announces **"Add product photo"** (single stop; inner gallery icon
      and overlay are skipped).
- [ ] Date field announces the expiry-date prompt/value.

### Edit Product dialog (from Inventory → card → Edit)
- [ ] Same fields as Add Product are reachable and labelled.
- [ ] Photo preview frame announces **"Add product photo"** (single stop).

### Mark status / Delete confirm dialogs
- [ ] Header icon is skipped; the action label, product name, note field, and Confirm/Cancel
      buttons are reachable and labelled.
- [ ] Warning row (when shown) reads the warning text; the warning icon is skipped.

### Settings / Account / Notifications / Help
- [ ] Setting rows announce title (+ subtitle); the leading icon and the trailing chevron are
      skipped. Rows with a switch announce the switch state.
- [ ] Help/Support: the top illustration is skipped (decorative).

## Touch-target spot check (no TalkBack needed)
Controls resized to ≥48dp in this change — confirm they tap reliably:
- [ ] Top bar: Menu, Search, History/Archive.
- [ ] Inventory card: Delete (X).

## Known minor items (acceptable for school scope)
- Theme toggle pill is 72×36dp overall (one control, wide but 36dp tall). Tappable; left as a
  deliberate compact design element.
- Broad Help/Support body text is still partly hardcoded (tracked separately under the
  hardcoded-text cleanup, not this issue).

## Result

| Field | Value |
|---|---|
| Date | |
| Device / Android version | |
| TalkBack pass | ☐ pass ☐ issues (note below) |
| Screen recording attached | ☐ |

Notes:
