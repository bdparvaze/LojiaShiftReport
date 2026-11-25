# Backup Device Verification Checklist

- [ ] After a restore to a new phone, the vault must be gone or unreadable, and the app must ask for the password again.

## Pre-Release On-Device Checks (`a`–`k`)

- [ ] **a. Passwordless export / cloud block:** Before setting a backup password, tap File Export, OneDrive, and Google Drive; confirm all are blocked with the *"Set a backup password first"* dialog and action button opening password setup.
- [ ] **b. Auto snapshot format & rotation:** Set a backup password and leave the app running past an AutoBackup trigger (`AutoBackupWorker`); run `adb shell run-as com.lojia.shiftreport head -c 4 files/backups/<file>` on a snapshot file and confirm magic bytes are `LSB2` (never `{` plain JSON) and at most 3 `device_snapshot_*.lsb` files are kept.
- [ ] **c. Manual portable export & wrong-password restore:** Create a portable file backup, transfer it to another device or folder, attempt restore with a wrong password, confirm failure displays *"Wrong password or damaged backup"*, then restore with the correct password and verify all records return intact.
- [ ] **d. Reinstall vault invalidation:** Record a portable backup, uninstall the app, reinstall cleanly, confirm the old Keystore-wrapped `backup_password.vault` (if any copy survived) is invalid and cleared on check, and confirm cloud/file export prompts to set a backup password again before exporting.
- [ ] **e. Cross-device portable restore:** Copy the `.lsb` portable backup to a second physical device (without the original Keystore), enter the backup password when prompted, and confirm restore succeeds cleanly with all shift reports and sessions present.
- [ ] **f. Google Drive & OneDrive portable round-trip:** Upload a backup to Google Drive / OneDrive, delete local app data or switch device, restore from the cloud list/Drive restore flow, enter password at prompt, and verify restore completes without writing plain JSON.
- [ ] **g. Legacy backup backward compatibility:** Import an older plain-JSON (`{...}`) backup file and an older `LOJIA_ENC_V1` encrypted backup file via restore picker; confirm both restore cleanly without requiring a new-format password prompt unless portable `LSB2`.
- [ ] **h. Password change forward compatibility:** Change the backup password in admin-gated Settings, create a new portable backup, confirm older backups still decrypt with the earlier password (*"Earlier backups keep the password they were made with."*) and new backups use the new password.
- [ ] **i. Logcat / SharedPreferences privacy check:** Filter `adb logcat` while setting/changing password, exporting, and restoring; confirm zero plaintext passwords, passphrases, or legacy secret strings appear in logs, toasts, SharedPreferences, or disk files.
- [ ] **j. Android cloud backup exclusion check:** Verify Android cloud backup & device transfer exclusions remain intact in manifest & XML rules:
  - `AndroidManifest.xml`: `android:allowBackup="false"`, `android:dataExtractionRules="@xml/data_extraction_rules"`, `android:fullBackupContent="@xml/backup_rules"`
  - `res/xml/data_extraction_rules.xml`: excludes `database="."`, `sharedpref="."`, `file="."`, `file="backups/"`, and `file="backup_password.vault"` under both `<cloud-backup>` and `<device-transfer>`
  - `res/xml/backup_rules.xml`: excludes `database="."`, `sharedpref="."`, `file="."`, `file="backups/"`, and `file="backup_password.vault"`
  - On device, run `adb shell bmgr backupnow com.lojia.shiftreport` / simulate restore and confirm `files/backups/` and `files/backup_password.vault` are not restored.
- [ ] **k. Device & Build Sign-off:**
  - Device Model: `________________`
  - Android OS Version: `________________`
  - App Version: `1.1` (`versionCode = 2`, `applicationId = com.lojia.shiftreport`)
  - Tester / Date: `________________`

