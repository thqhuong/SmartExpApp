# SmartExpApp: Privacy and Security Features

This document outlines the privacy and security features implemented in the SmartExpApp project. It details the design principles, data storage choices, and processing protocols that protect user data.

## 1. Local-First Philosophy & Local Storage

By default, the app is fully functional without requiring a cloud account. 
- **Local Databases:** For guest or local-only users, all inventory items, categories, expiry scans, recipe caches, and custom settings remain strictly on the device inside a Room database ([smartexp_local.db](file:///c:/Users/ADMIN/AndroidStudioProjects/SmartExpApp/app/src/main/res/xml/backup_rules.xml#L3)).
- **Local Images:** All captured or selected product photos are stored in the app's internal files directory and are not automatically uploaded to any remote storage ([LocalImageRepository](file:///c:/Users/ADMIN/AndroidStudioProjects/SmartExpApp/app/src/main/java/com/example/smartexpapp/data/LocalImageRepository.java)).

## 2. On-Device OCR & Voice Processing

- **Local OCR:** When scanning a receipt or product label, Optical Character Recognition (OCR) text extraction is processed entirely locally on the user's device using **Google ML Kit**. Original images are never transmitted to third-party clouds or external servers for OCR.
- **On-Device Voice Input:** Smart voice input leverages Android's native Speech-to-Text framework, ensuring the app itself does not intercept or transmit raw audio recordings.
- *Refer to: [smart-input-privacy.md](file:///c:/Users/ADMIN/AndroidStudioProjects/SmartExpApp/docs/smart-input-privacy.md)*

## 3. Secure Cloud Synchronization

- **Authentication-Based Access:** When signed in (using Firebase Auth), data synchronizes securely with Cloud Firestore.
- **Firestore Security Rules:** Access is strictly bounded by user identity. The database rules enforce that a signed-in user can only read, create, or update documents within their own path (`/users/{userId}`), preventing cross-user data leakage.
- *Refer to: [firestore.rules](file:///c:/Users/ADMIN/AndroidStudioProjects/SmartExpApp/firestore.rules#L141)*

## 4. Ephemeral AI Processing & Secure Proxies

- **No Direct Client Keys:** The app communicates with a Cloudflare Worker backend instead of invoking AI endpoints (like Google Gemini) directly. This keeps API keys secure and allows controlled rate-limiting.
- **Ephemeral AI Processing:** Only the raw text extracted from OCR or user queries is passed to the AI models. No personal identifiers or images are sent, and the worker processes request payloads ephemerally without using them to train public models.

## 5. Data Control, Export & Erasure

- **Local Data Reset:** Users have a "Delete Local Data" option in [AccountDetailsActivity](file:///c:/Users/ADMIN/AndroidStudioProjects/SmartExpApp/app/src/main/java/com/example/smartexpapp/AccountDetailsActivity.java#L69). This triggers [LocalDataResetRepository.reset](file:///c:/Users/ADMIN/AndroidStudioProjects/SmartExpApp/app/src/main/java/com/example/smartexpapp/data/LocalDataResetRepository.java#L97), which purges all local databases, wipes cached recipes, deletes saved images, and cancels any scheduled reminders.
- **Data Portability:** Users can export all local data to a standardized JSON format at any time using [LocalDataExportRepository](file:///c:/Users/ADMIN/AndroidStudioProjects/SmartExpApp/app/src/main/java/com/example/smartexpapp/data/LocalDataExportRepository.java).

## 6. Configured Backup & Device Transfer Rules

Safe backup filters are defined in [backup_rules.xml](file:///c:/Users/ADMIN/AndroidStudioProjects/SmartExpApp/app/src/main/res/xml/backup_rules.xml) and [data_extraction_rules.xml](file:///c:/Users/ADMIN/AndroidStudioProjects/SmartExpApp/app/src/main/res/xml/data_extraction_rules.xml). These rules control Android Auto-Backup and device transfers, ensuring only the necessary database and settings are copied, while temporary caches or secrets are excluded.
