# SmartExpApp: Smart Input Privacy & Data Usage

SmartExpApp employs smart input capabilities like image scanning (OCR) and voice recognition, alongside AI-assisted parsing, to make managing your inventory seamless. This document explicitly outlines what data is used, where it goes, and how it is protected.

## Local-First Philosophy

SmartExpApp prioritizes your privacy by keeping as much processing on your device as possible. 

### What Stays Local:
- **Optical Character Recognition (OCR):** When you use the camera or gallery to scan a receipt or product label, the text extraction runs entirely locally on your device via Google ML Kit. Images are **never** uploaded to our servers or any third-party cloud for OCR processing.
- **Speech Input:** Voice input utilizes Android's native speech-to-text APIs. Depending on your device settings and manufacturer, this may run locally or use Google's standard voice servers, but SmartExpApp does not intercept or transmit these voice recordings elsewhere.
- **Inventory Data (Default):** For guest users, all inventory items, dates, and settings remain solely in the local Room database. 

## Cloud & AI Parsing (Optional Features)

To parse messy receipt text or generate recipes, SmartExpApp uses external AI services.

- **Cloudflare AI Parsing:** When you request smart parsing of text (e.g., extracting product names and expiration dates from OCR text) or recipe generation, the text string is sent to a **Cloudflare-backed AI endpoint**.
  - **What is sent:** Only the raw text extracted from the OCR process or your manual input is sent. No images, personal identifiers, or complete inventory databases are transmitted.
  - **Data Retention:** Data sent to the Cloudflare AI endpoint is processed ephemerally to generate a response and is not stored or used to train public models.
- **Gemini Transition:** Note that direct integrations with Google's Gemini API are being phased out in favor of the controlled Cloudflare backend to ensure better privacy and rate-limiting.

## Data Sync & Cloud Storage

If you choose to create an account and sign in:
- **Firestore Sync:** Your inventory data is synchronized with our secure Firestore database to allow cross-device access and backup.
- **Access Control:** This data is strictly tied to your authenticated account and secured by Firestore Security Rules. It is not accessible by other users.
- **Data Export & Deletion:** You can export your data at any time from the app. Deleting your account will completely purge your associated inventory data from Firestore, ensuring nothing is left behind.

## What We Do NOT Do
- **No Barcode Lookups:** We explicitly **do not** offer barcode scanning or lookup services. We do not track what products you buy against global barcode databases.
- **No Data Selling:** We do not sell your inventory habits, receipt text, or email addresses to advertisers or third parties.

Your data is used strictly to provide you with the inventory tracking and recipe features you requested.
