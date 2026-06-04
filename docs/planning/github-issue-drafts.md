# SmartExpApp GitHub Issue Drafts

These issue drafts mirror the planned development route. They were used to create GitHub issues #7 through #15 and remain here as a local planning reference.

## Issue 1: Planning: finalize scope, diagrams, and delivery route

Goal:
Create the planning baseline for SmartExpApp before feature implementation.

Acceptance criteria:
1. Product scope, MVP boundaries, local-first storage, and later cloud-sync direction are documented.
2. Use case, architecture, ERD, DFD, OCR sequence, Agent prompt sequence, and future sync sequence diagrams are available.
3. Issue backlog and numbered work route are documented.
4. Report requirements from the assignment are reflected in the planning docs.

## Issue 2: Foundation: lock app architecture and persistent data layer

Goal:
Make the native Android app the delivery target and replace sample/in-memory data with a persistent local data layer.

Acceptance criteria:
1. Native Android app is documented as the main implementation target.
2. Product, storage, scan, recipe, inventory action, and settings data models are finalized.
3. Local database/repository layer is implemented.
4. Existing screens load data through the repository instead of hard-coded sample state.
5. Basic unit tests cover expiry status and repository behavior.

## Issue 3: Inventory: implement CRUD, search, filter, and sort

Goal:
Allow users to fully manage inventory items.

Acceptance criteria:
1. Users can add, edit, and delete products.
2. Users can search by product name.
3. Users can filter by expiry status, category, and storage location.
4. Users can sort by expiry date, name, and recently added.
5. Empty, loading, and error states are handled.

## Issue 4: Expiry: dashboard status groups and local reminders

Goal:
Make the app proactively warn users before products expire.

Acceptance criteria:
1. Dashboard groups products into urgent, soon, safe, and expired.
2. Expiry calculations are consistent across dashboard, inventory, and product detail.
3. Users can configure reminder timing.
4. Local notifications are scheduled and cancelled when products are created, updated, consumed, or deleted.
5. Permission request and denied-permission states are handled.

## Issue 5: OCR: scan and confirm expiry dates from product labels

Goal:
Reduce manual typing by extracting expiry dates from product labels.

Acceptance criteria:
1. User can open camera or image picker from Add Product.
2. OCR extracts text from the selected image.
3. Date parser finds likely expiry-date candidates.
4. User confirms or corrects the detected date before saving.
5. Scan metadata is saved for debugging and report explanation.

## Issue 6: Barcode: lookup products and auto-fill metadata

Goal:
Speed up product entry by scanning barcodes and filling product details from an external provider.

Acceptance criteria:
1. User can scan a barcode from Add Product.
2. App calls a product lookup provider such as Open Food Facts.
3. Product name, brand/category, and image are auto-filled when available.
4. User can edit all auto-filled fields before saving.
5. Missing product and network-error fallbacks are handled.

## Issue 7: Agent: voice/chat assistant with recipe help

Goal:
Provide an AI Agent screen that can answer inventory questions through typed prompts, suggested questions, and voice input/output.

Acceptance criteria:
1. App identifies relevant and expiring products from local inventory.
2. Agent supports typed prompts, suggested question chips, and voice input/output.
3. Agent can answer recipe questions using expiring products and dietary preferences.
4. Recipe help shows used ingredients, missing ingredients, source/instructions, and next actions when available.
5. API failure, permission denial, and no-result states are handled.

## Issue 8: Insights: stats, waste impact, and settings

Goal:
Show users how much waste they reduce and let them configure app behavior.

Acceptance criteria:
1. Users can mark items as consumed, wasted, donated, or expired.
2. Stats screen shows saved items, wasted items, expired items, and category patterns.
3. Settings support reminder preferences, dietary preferences, and default storage.
4. Stats update from real inventory action history.

## Issue 9: QA and submission: tests, README, report, slides, and demo

Goal:
Prepare the final app and all required submission materials.

Acceptance criteria:
1. Unit tests cover model calculations, parsers, and repository behavior.
2. Manual QA checklist covers core user flows and permission states.
3. README explains setup, API keys, features, and demo usage.
4. Report includes scope, diagrams, implementation details, evaluation, and references.
5. Slides and demo video follow the assignment requirements.
