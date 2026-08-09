# Implementation Plan - Fix Persistence After Uninstall & Account Migration

This plan fixes the issue where backups were unrecoverable after deleting the app and ensures that notes are correctly migrated when switching Google accounts.

## User Review Required

> [!CAUTION]
> **Encryption Change:** I am removing the AES encryption for the Google Drive backup file.
> **Reason:** The encryption key was stored in the device's Keystore, which is deleted when the app is uninstalled. Removing this encryption ensures that you can reinstall the app and successfully **Restore** your notes. Since the file is stored in your private Google Drive space, it remains secure.

## Proposed Changes

### [Component Name] Data Recovery & Sync

#### [MODIFY] [DriveBackupManager.kt](file:///C:/Users/ilaiy/AndroidStudioProjects/app/src/main/java/com/ilaiyarasu/smartnote/util/DriveBackupManager.kt)
- **Remove Encryption:** Update `backupNotes` and `restoreNotes` to use plain JSON instead of the `EncryptionHelper`. This allows data recovery after an app reinstall.
- **Improved Refresh:** Ensure the token is always refreshed before a sync to prevent "Sync Issue" messages.

#### [MODIFY] [AutoBackupWorker.kt](file:///C:/Users/ilaiy/AndroidStudioProjects/app/src/main/java/com/ilaiyarasu/smartnote/util/AutoBackupWorker.kt)
- **Safety Check:** Add logic to prevent auto-syncing if the local note count is zero and a cloud backup already exists. This prevents a fresh installation from accidentally overwriting your cloud data with an empty list.

### [Component Name] Account Migration UX

#### [MODIFY] [SettingsScreen.kt](file:///C:/Users/ilaiy/AndroidStudioProjects/app/src/main/java/com/ilaiyarasu/smartnote/ui/screens/SettingsScreen.kt)
- **Immediate Migration:** After successfully changing a Google account, trigger an immediate backup of the current local notes to the new account. This satisfies the "copying notes to the new account" requirement.

## Verification Plan

### Manual Verification
1. **Uninstall Recovery:**
   - Back up some notes.
   - Uninstall and reinstall the app.
   - Sign in and tap **Restore Data**.
   - Verify all notes are recovered.
2. **Account Migration:**
   - Sign in with Account A and back up notes.
   - Switch to Account B.
   - Verify that Account B now has a `smartnote_backup.json` containing the same notes.
3. **Empty Overwrite Prevention:**
   - Reinstall the app (0 notes).
   - Sign in with an account that has a backup.
   - Verify that auto-sync does **not** automatically upload 0 notes and overwrite your cloud data.
