# Smartnote – AI Notepad & Smart Organizer 📝✨

[![Google Play](https://img.shields.io/badge/Google_Play-Live-brightgreen?logo=googleplay)](https://play.google.com/store/apps/details?id=com.ilaiyarasu.smartnoteapp)
[![Kotlin](https://img.shields.io/badge/Kotlin-100%25-7F52FF?logo=kotlin)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20(Material3)-4285F4?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![ML Kit](https://img.shields.io/badge/ML%20Kit-On--Device%20OCR-FBBC04?logo=google)](https://developers.google.com/ml-kit)
[![Gemini AI](https://img.shields.io/badge/AI-Gemini%20API-4285F4?logo=googlegemini)](https://ai.google.dev/)

An offline-first, native Android productivity app designed to bridge fast daily note-taking with intelligent generative AI processing and on-device computer vision.

👉 **Get it on Google Play:** [Smartnote on Google Play Store](https://play.google.com/store/apps/details?id=com.ilaiyarasu.smartnoteapp)

---

## 🚀 Key Features

- **Modern Jetpack Compose UI:** 100% declarative UI built with Material Design 3, dynamic theming (Dark & Light modes), and responsive layouts.
- **On-Device OCR Scanner:** Powered by Google ML Kit to extract text instantly from camera captures, documents, receipts, and whiteboard images without external cloud processing.
- **Generative AI Assistant:** Gemini AI integration providing one-tap summaries, bulleted action items, tone formatting, and English ⇄ Tamil translation.
- **Offline-First Persistence:** Jetpack Room Database (SQLite) with Kotlin Coroutines and StateFlow for immediate, responsive state management.
- **Privacy & Security:** App-level biometric authentication (Fingerprint / Face Unlock) and fallback PIN support.
- **Cloud Backup:** Automated WorkManager background synchronisation to personal Google Drive accounts.

---

## 🛠 Tech Stack & Architecture

- **Language:** Kotlin
- **UI Framework:** Jetpack Compose (Material 3)
- **Architecture:** MVVM (Model-View-ViewModel) + Clean Architecture principles
- **Local Persistence:** Room Database, Jetpack DataStore
- **Asynchronous Flow:** Coroutines, StateFlow, SharedFlow
- **Machine Learning:** Google ML Kit Text Recognition
- **AI Integration:** Google Gemini API
- **Background Tasks:** WorkManager

---

## 📦 Getting Started

### Prerequisites
- Android Studio Ladybug or later
- JDK 17+
- Android SDK 34+

### Installation & Run
1. Clone the repository:
   ```bash
   git clone [https://github.com/ilaiyarasu78/Smartnote.git](https://github.com/ilaiyarasu78/Smartnote.git)
