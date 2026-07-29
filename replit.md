# نظام البقالة v2.0 (Grocery Management System)

## Overview

A native Android application for grocery store management, written in Java.

## Tech Stack

- **Platform:** Android (native)
- **Language:** Java
- **Database:** SQLite (via `SQLiteOpenHelper`) — auto-created on first install, version 3
- **UI:** Android XML Layouts + AppCompat
- **Build System:** Gradle 6.5 + Android Gradle Plugin 4.1.3
- **Min SDK:** 23 (Android 6.0) — required for biometric support
- **Target SDK:** 29 (Android 10)

## Features

### Core (Fixed & Improved)
- **Product inventory** — Add, edit, delete, search with color-coded stock levels
- **Sales / Invoices** — Multi-item purchase flow, now properly deducts stock for both Cash and Debt
- **Debt management** — Track customer debts, process payments, view invoices per debtor
- **User authentication** — Username/password login with password toggle

### New Features v2.0
- **📊 Dashboard** — Real-time stats: today's sales, invoice count, total debt, low-stock count
- **👆 Fingerprint login** — Optional biometric authentication (enable from Settings)
- **⚙️ Settings page** — Beautiful settings screen with:
  - Store name customization
  - Dark/Light mode toggle
  - Fingerprint login toggle
- **📊 Reports page** — Daily & monthly sales totals, low-stock list, product count
- **⚠️ Low stock alerts** — Popup on home screen when products fall below 5 units
- **💳 Better payment UI** — Radio button selector (Cash/Debt) with color-coded confirm button
- **💡 Stock indicator** — Shows available stock when selecting a product in sales screen

### Bug Fixes
- ✅ **Stock not deducted for Debt purchases** — Fixed: stock is now always deducted regardless of payment type
- ✅ **Missing `payments` table** — Added to database schema; no more SQLite crashes
- ✅ **Missing `invoices` table** — Broken `updateInvoicesAfterPayment` method fixed to use `purchases` table
- ✅ **Duplicate `SettingsActivity`** in manifest — Removed
- ✅ **Non-existent `DebtMainActivity`** in manifest — Removed

## Project Structure

```
/
├── build.gradle          # Root Gradle config (plugin 4.1.3)
├── settings.gradle
├── gradle/wrapper/       # Gradle 6.5 wrapper
└── app/
    ├── build.gradle      # Module build config (biometric, appcompat deps)
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/mycompany/app/
        │   ├── LoginActivity.java       # AppCompatActivity + BiometricPrompt
        │   ├── HomeActivity.java        # Dashboard with stats
        │   ├── SettingsActivity.java    # NEW: Settings page
        │   ├── ReportsActivity.java     # NEW: Reports page
        │   ├── PurchaseActivity.java    # Fixed debt bug + better UI
        │   ├── AddProductActivity.java
        │   ├── EditProductActivity.java
        │   ├── ViewProductsActivity.java
        │   ├── InvoicesActivity.java
        │   ├── DebtorsListActivity.java
        │   ├── EditDeleteInvoicesActivity.java
        │   ├── PaymentActivity.java
        │   ├── DatabaseHelper.java      # v3: payments + suppliers tables
        │   └── MainActivity.java
        └── res/
            ├── layout/   # All XML UI layouts (redesigned key screens)
            ├── drawable/ # New card, stat, and button drawables
            ├── values/   # colors.xml (new), updated styles.xml
            └── menu/     # Options menus
```

## Building

```bash
# Requires Android SDK + ANDROID_HOME set
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

## Dependencies

- `androidx.appcompat:appcompat:1.3.1`
- `androidx.biometric:biometric:1.1.0`
- `androidx.cardview:cardview:1.0.0`
- `androidx.documentfile:documentfile:1.0.1`

## User Preferences

(none yet)
