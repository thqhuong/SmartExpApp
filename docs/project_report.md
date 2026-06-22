# SmartExpApp: Comprehensive Project Report

## 1. Project Overview
**SmartExpApp** is a robust, local-first Android application designed to help users manage their food inventory, track expiration dates, and minimize food waste. It combines traditional CRUD (Create, Read, Update, Delete) inventory management with modern AI and Machine Learning features like OCR-based data entry, natural language processing for fast logging, and intelligent recipe generation.

The app prioritizes privacy and offline usability (local-first) while offering optional cloud synchronization and AI enhancements for a premium experience.

---

## 2. Architecture & Technology Stack
The project is built natively for Android using a modern Java stack and standard Android Architecture Components. 

### Core Technologies:
*   **Language:** Java
*   **Build System:** Gradle (AGP 9.1.1, dependencies managed via `libs.versions.toml`)
*   **UI Toolkit:** Standard Android XML Layouts (`ConstraintLayout`, `RecyclerView`) and Google Material Design Components.
*   **Local Database:** **Room Persistence Library** (an abstraction layer over SQLite) for robust local data caching of inventory, history, categories, and settings.
*   **Background Processing:** **AndroidX WorkManager** for scheduling reliable, asynchronous background tasks (specifically for local push notifications).

### Machine Learning & AI:
*   **Google ML Kit (Text Recognition):** Powers the on-device Optical Character Recognition (OCR) feature, allowing users to extract text and dates directly from the camera without sending images to a server.
*   **Cloudflare Workers AI (Serverless):** Used for advanced natural language processing. The app connects to Cloudflare Worker endpoints to intelligently parse messy text/voice inputs into structured product data, generate dynamic recipes, and create recipe images.

### Cloud Integration (Optional):
*   **Firebase Authentication:** Supports Google Sign-In via `androidx.credentials` (Credential Manager).
*   **Firebase Firestore:** Provides real-time cloud synchronization of the user's inventory if they choose to create an account.
*   **Firebase BOM:** Manages Firebase dependency versions.

---

## 3. Detailed Feature Breakdown

### 3.1. Advanced Inventory Management
The core of the application revolves around tracking products. 
*   **Data Structure:** Each product tracks its Name, Category, Quantity, Unit (e.g., pcs, kg, ml), Storage Method, and Expiry Date.
*   **Storage Classification:** Items are categorized by storage locations (`Room Temp`, `Refrigerator`, `Freezer`) to help users physically locate their food.
*   **Lifecycle Tracking:** Instead of simply deleting items, users mark them with a `ProductStatus`: **Consumed**, **Wasted**, or **Donated**.
*   **Organization:** The inventory screen supports searching by keyword, advanced filtering by category/storage/status, and sorting (e.g., by expiry date).

### 3.2. Smart Add & Natural Language Parsing
To reduce the friction of manual data entry, the app implements a "Smart Add" feature.
*   **Input:** Users can type or use speech-to-text to enter natural phrases like *"2 gallons of milk in the fridge expiring next week"*.
*   **Hybrid Parsing Engine:** 
    *   **Cloudflare AI:** The input is sent to an AI worker to accurately extract product names, quantities, storage locations, and infer exact expiry dates.
    *   **Local Regex Fallback:** If the user is offline or the AI endpoint is unreachable, a highly optimized local Java Regex parser (`AgentRepository.java`) extracts quantities, units, and matches keywords for storage and expiry, ensuring the feature never completely breaks.

### 3.3. OCR Expiry Date Extraction
*   Users can take a photo of a product's label.
*   `OcrCaptureRepository` handles the secure image capture via Android's `FileProvider`.
*   Google ML Kit processes the image entirely on-device to extract text.
*   A custom `DateParser` scans the raw text for common expiry date formats (e.g., MM/DD/YY, "Best Before", etc.) and auto-fills the product entry form.

### 3.4. Intelligent Recipe Engine
A major feature to combat food waste is the recipe suggestion engine, which looks at what is expiring soonest.
*   **Local Recipes:** If offline, the app has a fallback logic that dynamically inserts the user's expiring ingredients into template recipes (e.g., "Use-First Skillet", "No-Waste Soup").
*   **AI Recipes:** When online, the app sends the inventory list to a Cloudflare Worker AI, which generates custom, culinary-sound recipes utilizing the exact ingredients the user needs to consume. It respects user-defined dietary preferences.
*   **AI Image Generation:** Cloudflare Workers are also utilized to generate thumbnail images for the AI-created recipes. 
*   **Recipe Caching:** Generated recipes are saved into a Room database table (`RecipeCacheEntity`) to save API calls and allow offline viewing later.

### 3.5. Statistics & History
*   **Product History:** A log of all actions taken (added, edited, consumed, wasted). Users can undo actions from this screen.
*   **Analytics Dashboard:** The `StatsActivity` aggregates data to show users their consumption versus waste ratios over custom date ranges (Week, Month, Year). This gamifies the experience, encouraging users to reduce their food waste.

### 3.6. Notifications and Reminders
*   `ReminderScheduler` and `ExpiryReminderWorker` utilize Android's `WorkManager` to run daily checks against the local Room database.
*   If products fall within the user's defined "Expiring Soon" threshold (e.g., 3 days), the app issues local push notifications, keeping the user informed without requiring a backend server to push alerts.

### 3.7. Data Control, Privacy & Sync
*   **Local-First Paradigm:** The app is fully functional without an internet connection or account.
*   **Cloud Sync:** Users can opt-in to cloud sync by signing in with Google. Their local Room database is then mirrored to Firebase Firestore.
*   **Data Export/Wipe:** The app complies with privacy best practices by offering one-tap solutions to export all local data to a file, or permanently wipe the local database (`LocalDataExportRepository`, `LocalDataResetRepository`).

---

## 4. Testing & Quality Assurance
The application maintains a high standard of code quality with established testing pipelines:
*   **Unit Testing:** JUnit 4 for testing local logic, parsers, and ViewModels (`InventoryViewModelTest`).
*   **Local Integration Testing:** Robolectric and Room-Testing for validating database queries and migrations without needing an emulator.
*   **UI & Integration Testing:** Espresso for testing Android UI interactions and `connectedDebugAndroidTest` for full device testing.
*   **Backend Rules:** Firestore security rules are tested via a dedicated npm script (`npm run test:firestore-rules`).
