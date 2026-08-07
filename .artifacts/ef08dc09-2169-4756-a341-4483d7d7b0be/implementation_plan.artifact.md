# Secure Cloud Backup & Auto Sync Implementation Plan

As a Senior Android Developer and UX Designer, I have architected a robust and secure synchronization system. This plan details the migration to Firebase Authentication, the integration of data encryption, and a polished Material 3 user experience.

## User Review Required

> [!IMPORTANT]
> **Firebase Setup Required:** You must add the `google-services.json` file to your `app/` directory and enable **Google Sign-In** in the Firebase Console.
> **Database Reset:** This implementation will build upon the existing UUID-based schema (Version 4).

## Proposed Changes

### [Component Name] Authentication & Security
- **Firebase Auth:** Integrate Firebase Authentication for secure token management and user identification.
- **Encryption Utility:** Add `EncryptionHelper.kt` to encrypt backup data using AES-GCM before uploading to Google Drive.
- **Auth Manager:** Create `AuthManager.kt` to encapsulate Firebase and Google Sign-In logic.

### [Component Name] Data Layer & Logic
- **Drive Manager:** Update `DriveBackupManager.kt` to:
    - Support encrypted payloads.
    - Use Firebase user tokens for authentication where applicable.
- **Sync Scheduler:** Enhance `AutoSyncScheduler.kt` to support user-controlled auto-sync toggles.
- **Worker:** Update `AutoBackupWorker.kt` to report detailed status (Syncing, Success, Error) back to the UI.

### [Component Name] UI & UX (Material 3)
- **Settings Screen:** Complete redesign with:
    - **User Profile:** Displays avatar and email.
    - **Live Status:** Visual indicators for "Last Synced" and current status (Progress bar, green check, or red error).
    - **Advanced Controls:** "Change Backup Account", "Backup Now", "Restore", and "Sign Out".
    - **Auto-Sync Toggle:** User control over background activity.

### [Component Name] Dependencies
#### [MODIFY] [build.gradle.kts (Module :app)](file:///C:/Users/ilaiy/AndroidStudioProjects/app/build.gradle.kts)
- Add Firebase Auth and Google Services dependencies.
- Add Security Crypto library for encryption.

## Verification Plan

### Automated Tests
- Run `gradle_build("app:assembleDebug")` to ensure all components are integrated.

### Manual Verification
1. **Security:** Intercept the `smartnote_backup.json` on Google Drive and verify the content is unreadable (encrypted).
2. **Account Switching:** Use "Change Backup Account" to switch Gmail accounts and verify notes sync to the new account's Drive space.
3. **UX Flow:**
    - Verify the sync icon animates during background tasks.
    - Verify "Last Backed Up" time updates immediately after a manual or automatic backup.
    - Test "Restore" on a fresh install to verify end-to-end recovery.
