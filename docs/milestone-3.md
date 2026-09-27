# Milestone 3 implementation and acceptance-test report

## Delivered workflow and credentials

**Staff login → own staff profile/enrollment → CameraX selfie → exactly one face → existing MobileFaceNet verification → Fused Location → private JPEG + Room attendance → clear success.**

Admin credentials remain `admin` / `admin123`. Staff choose the **Staff** role and use an existing **Employee ID** with shared demo password **`staff123`**, shown on the login screen. Employee ID lookup trims whitespace and uppercases with Locale.ROOT, matching staff creation. Unknown IDs and incorrect passwords do not authenticate; database lookup failures produce a retryable error. Sessions remain in memory and sign out on process restart. A logout during a pending lookup cannot restore that session.

`UserSession.Staff(staffId)` identifies the exact database row. The attendance ViewModel obtains that ID from the authenticated session through the manual factory; it is not supplied by a staff-editable field or arbitrary navigation argument. The staff navigation graph exposes no Admin destinations. Staff can sign in without enrollment, but see “Contact Admin to enroll your face before marking attendance” and cannot capture/mark attendance.

## Room schema version 2

The existing `staff` table and its data are unchanged. **Migration 1→2** creates `attendance` and its indices without destructive fallback. Both schema versions are retained in `app/schemas`.

| Attendance column | SQLite type | Purpose |
|---|---|---|
| `id` | INTEGER PRIMARY KEY AUTOINCREMENT | Attendance identifier |
| `staffId` | INTEGER NOT NULL | Foreign key to `staff.id`, indexed, deletion restricted |
| `timestamp` | INTEGER NOT NULL | Recording time, Unix epoch milliseconds |
| `selfieFileName` | TEXT NOT NULL, unique index | Relative private JPEG filename, not image bytes |
| `latitude` | REAL NOT NULL | Location latitude |
| `longitude` | REAL NOT NULL | Location longitude |
| `accuracyMeters` | REAL, nullable | Reported horizontal accuracy when available |

`AttendanceDao` inserts rows, observes the latest attendance for one staff ID and obtains referenced selfie filenames for recovery. `AttendanceRepository` is the ViewModel boundary. The last recorded attendance remains visible after signing in again. No history/admin reporting feature or daily attendance restriction was added.

## Verification and persistence ordering

1. Reject concurrent taps and unenrolled/missing staff.
2. Reload **only the signed-in staff ID** and verify its model fingerprint matches the existing bundled model.
3. Capture through the existing CameraX implementation.
4. Reuse the existing rotation/mirroring handling, ML Kit exactly-one-face check, alignment, preprocessing, embedding inference and FaceMatcher.
5. A mismatch stops here: no location request, JPEG file or attendance row.
6. On a match, encode the upright full selfie to an in-memory JPEG, then request location.
7. If location succeeds, write the JPEG and insert the attendance row.
8. Display “Attendance recorded,” timestamp, coordinates and accuracy. A deliberate **Record another attendance** action is required before another attempt.

The existing angle `< 74.18°` rule is unchanged and explicitly described as **provisional, not production calibrated**. User-reported webcam observations from Milestone 2A remain observations, not calibration. There is no liveness detection.

The only required face-engine integration change is optional retention of an upright full selfie and a verification method. The same extraction code still serves POC/enrollment. Source/crop/selfie bitmaps are recycled on success, mismatch, exception and cancellation. JPEG compression and image inference run off the main thread. `AttendanceFaceVerifier` is a narrow unit-test seam; production always receives `FaceRecognitionEngine`.

## Selfie storage and failure handling

Files are stored under:

`context.filesDir/attendance_selfies/<UUID>.jpg`

On this application, that corresponds to `/data/user/0/com.divanshgandhi.attendai/files/attendance_selfies/`. Room stores only `<UUID>.jpg`. Files are private to the app, not gallery/media-store entries; no storage permission is needed. JPEG quality is 90; pixels are already upright and unmirrored, so no EXIF rotation is required.

No failed face or failed location attempt creates a permanent file. The repository writes a temporary `.jpg.tmp`, flushes it, then renames it. A failed database insertion deletes the newly written JPEG. Disk operations and Room insertion run off the UI thread and are serialized around file cleanup/commit. Once the short disk commit starts, it completes or rolls back even if the screen closes; a completed row is available on the next login.

A filesystem and SQLite commit cannot be one transaction. If the process dies between the file write and row insert, the next attendance screen load/save reconciles unreferenced files, including temporary files. Referenced successful selfies are retained. Backup/device-transfer exclusions cover attendance selfies as well as Room data; app backup remains disabled. Uninstall or clearing app data removes local records/files.

## Location strategy

Added `play-services-location:21.4.0` through the existing version catalog. The manifest requests foreground `ACCESS_COARSE_LOCATION` and `ACCESS_FINE_LOCATION`, with no background location permission.

The UI requests both Android runtime permissions together and accepts either approximate or precise location. It provides app-permission settings recovery and a device Location settings action. **The location request itself runs only after the face matches.**

`FusedLocationProvider` checks permission and device Location enablement, then calls `getCurrentLocation` using a high-accuracy request, maximum cached-fix age **30 seconds**, provider request duration **15 seconds**, and coroutine timeout **20 seconds**. Returned age is checked again using elapsed realtime. There is no unchecked `lastLocation` fallback and no fabricated location. Accuracy is stored if supplied, including the actual uncertainty of approximate locations. Permission removal, disabled location, null/stale fixes, timeout and provider failures block attendance and display useful errors. Cancellation cancels the provider request.

Sources: [FusedLocationProviderClient](https://developers.google.com/android/reference/com/google/android/gms/location/FusedLocationProviderClient), [CurrentLocationRequest](https://developers.google.com/android/reference/com/google/android/gms/location/CurrentLocationRequest), [Google Play services setup](https://developers.google.com/android/guides/setup).

No backend, Firebase, upload, geofence, Hilt, background tracking or additional assignment feature was added. Location may use device/Google Play services assistance; the app has no backend dependency.

## Automated verification

Executed successfully on the Pixel 7 emulator:

```sh
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
ANDROID_SERIAL=emulator-5554 \
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:connectedDebugAndroidTest :app:lintDebug --console=plain
```

- **Debug APK:** built successfully.
- **47 unit tests:** all passed, zero skipped. Includes staff ID lookup/session/logout behavior, missing enrollment, selected-ID/reference use, mismatch preventing location/save, success, duplicate-tap protection, location error states, save failure/retry, model mismatch, last-record state, selfie rollback and orphan cleanup.
- **22 instrumented tests:** all passed, zero skipped. Includes existing Admin/camera tests, staff navigation/enrollment gating, real-model same/different-person verification, valid upright JPEG persistence, metadata/location/file survival after database reopen, foreign-key enforcement and version-1 migration preserving enrollment.
- **Lint:** 0 errors, 25 warnings. Warnings are unused template/placeholder resources, redundant activity label and dependency/tool update notices. The Fused Location call has a documented `MissingPermission` annotation because permission is checked through a local helper immediately before the request; no blanket lint baseline was added.

Tests use isolated databases/directories. Fixed test coordinates and verification doubles are used for deterministic state/persistence tests; real-model tests use the actual bundled model and photographic fixtures. **A live GPS/Fused Location fix and the complete human webcam→location UI flow are deferred to manual acceptance, as requested.** They are not represented as automated live-device results.

## Final end-to-end manual acceptance instructions

1. In Android Studio, use **app / debug / Pixel 7**. Keep the previously validated front camera set to **Webcam0** and macOS webcam permission allowed. Run the APK as an update, without uninstalling/clearing existing data, to exercise migration from Milestone 2B.
2. Turn on Android **Settings → Location → Use location**. In the emulator's **Extended controls (⋮) → Location → Single points**, choose a location and press **Set location**. For example, latitude `28.6139`, longitude `77.2090`. If using the emulator console, `adb -s emulator-5554 emu geo fix 77.2090 28.6139` uses **longitude first**. Set it again just before the attendance test so the fix is fresh.
3. Sign in as Admin using `admin` / `admin123`. Confirm existing staff and enrollments remain after migration. If needed, **Add Staff**, name `Alex Demo`, employee ID `EMP-001`, then **Enroll Face** using the original participant. Verify **Face Enrolled** and return to the list.
4. Create a second staff member `EMP-002` without enrollment. Sign out, select **Staff**, enter `EMP-002` / `staff123`. Expect the enrollment instruction and no attendance action. Sign out.
5. Test an unknown employee ID or incorrect password. Expect invalid credentials and no session. Then sign in as Staff with `emp-001` / `staff123`; confirm the displayed name and `EMP-001` identify the correct employee. Admin destinations must not be accessible.
6. Grant camera access if needed. Tap **Grant location access** and choose **While using the app**. Prefer **Precise** for this acceptance run; approximate is supported but may be less accurate or take longer. If permission was permanently denied, use **Open app settings → Permissions → Location** to grant it and return.
7. Set a fresh emulator location, keep exactly one enrolled person's face visible, and tap **Mark Attendance**. Expect stages for verifying face, getting location and saving. Rapid extra taps must not start another operation. Expect **Attendance recorded**, a timestamp, coordinates and accuracy. Capture controls are replaced by **Record another attendance**.
8. Force-stop the app from Android Settings, reopen, and sign in again as `EMP-001` / `staff123`. Verify **Last recorded attendance** shows the saved timestamp/location. Sign out and sign in as `EMP-002`: it must not show EMP-001's record.
9. Sign back in as EMP-001. With a different person alone in frame, tap **Mark Attendance**. Expect **Face mismatch**, no success state, and no new row/selfie. Then retry with the original person and a fresh location. Also test an empty frame and two faces; both must be rejected before attendance creation.
10. Test location failures with a matching face: deny the app's location permission, then separately turn device Location off. Permission denial must prevent marking until granted; disabled provider must display a useful error after face verification. Withhold a fresh emulator fix to test unavailable/timeout behavior. None of these attempts should create a row or permanent selfie. Restore settings, set a fresh location and retry.
11. Test re-enrollment: sign out, Admin login, open EMP-001 → **Re-enroll Face**, capture the intended person, then Staff login and repeat attendance. Compare only against that employee's newly saved enrollment.
12. In Android Studio **App Inspection → Database Inspector → attendai.db**, inspect:

```sql
SELECT a.id, s.employeeId, a.timestamp, a.selfieFileName,
       a.latitude, a.longitude, a.accuracyMeters
FROM attendance a JOIN staff s ON s.id = a.staffId
ORDER BY a.id DESC;
```

Use Device Explorer to open `data/data/com.divanshgandhi.attendai/files/attendance_selfies` (equivalent to the `/data/user/0/...` path). Each successful row should reference an existing viewable upright JPEG. Rejected attempts must not add rows/files. Do not mistake the displayed *last recorded attendance* for success on a new failed attempt.

On a physical Android device, select it in Android Studio, enable Location, grant foreground permissions and use an actual fresh fix outdoors if needed; omit emulator location injection. No physical-device results are claimed here.

## Files

[All created and modified files](milestone-3-files.md). MainActivity, model weights, alignment/preprocessing math and matching threshold remain unchanged. No README, presentation or screen redesign work was started.
