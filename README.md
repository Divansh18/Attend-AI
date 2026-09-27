# AttendAI

AttendAI is a local-first Android attendance application supporting Admin and Staff workflows, face enrollment, selfie-based face verification, location capture, and persistent attendance records.

## Features

### Admin

- Sign in with demo Admin credentials.
- View the staff list and each employee's face-enrollment status.
- Add staff using a name and unique employee ID.
- Open a staff profile and enroll or replace its face embedding.
- View that employee's attendance history, newest first, including date, time, coordinates, and available location accuracy.

### Staff

- Sign in with an employee ID created by the Admin.
- Mark attendance only against that employee's enrolled face.
- Capture an attendance selfie, timestamp, and current location after a successful face match.
- Store attendance records locally and display the last recorded attendance after signing in again.

The workflow reports no-face, multiple-face, face-mismatch, camera-permission, location-permission, stale-location, and unavailable-location failures. Failed face verification or missing required location data does not create an attendance record or retain a selfie. Processing guards prevent repeated taps from starting concurrent attendance attempts.

## Demo Credentials

**Admin**

- Username: `admin`
- Password: `admin123`

**Staff**

- Employee ID: an employee ID created by the Admin
- Password: `staff123`

The Admin must create the staff member and enroll their face before that employee can mark attendance.

## Tech Stack

- Kotlin 2.2.10
- Jetpack Compose with Material 3 (Compose BOM 2026.02.01)
- MVVM and Repository pattern
- Navigation Compose 2.9.4
- Room 2.8.5 with KSP
- CameraX 1.6.2
- bundled ML Kit Face Detection 16.1.7
- Qualcomm AI Hub MobileFaceNet TFLite model
- LiteRT 1.4.2 using the TensorFlow Lite `Interpreter` API
- Fused Location Provider through Google Play services Location 21.4.0
- Kotlin Coroutines and Flow 1.9.0
- Android Gradle Plugin 9.4.1 and Gradle 9.6.0

The app targets Android API 37 and supports API 24 and newer.

## Architecture

```text
Compose UI
    ↓
ViewModels
    ↓
Repositories / workflow coordination
    ↓
Room / CameraX / face recognition / location / private file storage
```

Screens observe ViewModel state and send user actions back to their ViewModels. Repositories form the persistence boundary; Compose code does not access Room DAOs directly. Camera capture, face processing, location acquisition, and private selfie storage remain separate concerns.

`AttendAiApplication` creates an application-scoped `AppContainer`. The container manually wires the database, repositories, location provider, face-engine factory, and ViewModel factory, keeping dependency setup explicit without Hilt. AttendAI intentionally has no application backend, Firebase integration, or cloud synchronization.

## Face Recognition Pipeline

```text
CameraX selfie
    → ML Kit face detection
    → exactly-one-face validation
    → orientation and mirroring correction
    → five-landmark alignment, crop, and preprocessing
    → MobileFaceNet embedding
    → L2 normalization
    → cosine/angular comparison
    → match or mismatch
```

The bundled model is the **Qualcomm AI Hub MobileFaceNet float TFLite export v0.63.0**, sourced from the [Qualcomm model card](https://huggingface.co/qualcomm/MobileFaceNet) and its [v0.63.0 export](https://qaihub-public-assets.s3.us-west-2.amazonaws.com/qai-hub-models/models/mobile_facenet/releases/v0.63.0/mobile_facenet-tflite-float.zip). The model card identifies the upstream model as Apache-2.0; the included Qualcomm export/wrapper notice is BSD-3-Clause. Copies of both license notices and detailed provenance are bundled beside the model in `app/src/main/assets/models/mobile_facenet/`.

The graph accepts two FLOAT32 RGB NCHW inputs of shape `[1, 3, 112, 112]` in the range `[0, 1]` and returns `[2, 128]` FLOAT32 embeddings. AttendAI supplies the aligned crop to both graph inputs, retains the first 128-value embedding, and L2-normalizes it. The graph performs its documented ImageNet normalization and horizontal-flip augmentation internally.

Comparison uses cosine similarity. The app also derives angular distance with `acos(cosine)` and normalized Euclidean distance. A match currently requires an angle strictly below **74.18°**, following the upstream Qualcomm example. This threshold is provisional and has not been production-calibrated for AttendAI's CameraX, ML Kit landmark, alignment, or capture pipeline.

AttendAI does not implement liveness detection or anti-spoofing.

## Attendance Flow

```text
Admin creates staff
    → Admin enrolls the staff face
    → Staff signs in
    → Staff captures a selfie
    → Face is verified against that staff record
    → A fresh location is obtained
    → Timestamp, selfie filename, and location are persisted
    → Admin views the staff-specific attendance history
```

Only a successful face match proceeds to location acquisition. Face mismatch, detection failure, missing enrollment, or required-location failure stops the workflow without creating attendance.

## Data Storage

The Room database is at schema **version 2** and contains:

- `staff`: local ID, unique employee ID, name, normalized face embedding, face-model identifier, and enrollment timestamp.
- `attendance`: local ID, staff foreign key, timestamp, unique selfie filename, latitude, longitude, and optional accuracy.

Face embeddings are serialized for Room storage and survive process restarts. Attendance history is scoped by staff ID and ordered by descending timestamp, then descending attendance ID for deterministic ties.

Attendance JPEGs are stored in app-private storage under:

```text
files/attendance_selfies/<UUID>.jpg
```

Room stores only the filename, not image bytes. A temporary file is finalized before the database insert; an insert failure deletes the newly written JPEG. Before observing the latest attendance or recording a new one, repository reconciliation removes temporary or unreferenced files left by interrupted writes. Clearing app data or uninstalling the app removes the local database and selfies.

## Location

AttendAI uses the Fused Location Provider with foreground coarse and fine location permissions. Location is requested only after the face matches.

- High-accuracy current-location request
- Maximum accepted cached-fix age: 30 seconds
- Provider request duration: 15 seconds
- Overall coroutine timeout: 20 seconds
- Returned fix age is checked again before attendance is saved

Permission denial, disabled device location, a null or stale fix, timeout, and provider failure all block attendance creation. There is no background tracking or geofencing.

## Running the Project

1. Clone the repository and open its root directory in Android Studio.
2. Allow Android Studio to complete Gradle sync using its bundled JDK.
3. Start an Android emulator or connect a physical Android device running API 24 or newer.
4. Select the `app` run configuration and the `debug` build variant, then run the app.
5. Grant camera and foreground location permissions when requested.
6. Sign in as Admin, create a staff member, and enroll that person's face before testing the Staff flow.

For a Pixel emulator, stop the AVD before changing its camera configuration, then edit the device's advanced settings and set the front camera to `Webcam0` when host webcam passthrough is available. Allow the Android Emulator to use the host camera. Set a current simulated location from **Extended controls → Location** before marking attendance. A physical device can use its normal front camera and location provider.

No backend, API key, environment variable, or cloud service configuration is required. After dependencies have been resolved, recognition and application data operate locally; the Fused Location Provider may use the device's normal Google Play services location assistance.

Useful verification commands from the repository root are:

```sh
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest
./gradlew :app:lintDebug
```

Run the connected test command with an emulator or physical device available.

## Testing

Final automated verification completed successfully on the Pixel 7 emulator:

- `:app:assembleDebug`: passed
- `:app:testDebugUnitTest`: 50 tests passed, 0 failed, 0 skipped
- `:app:connectedDebugAndroidTest`: 25 tests passed, 0 failed, 0 skipped
- `:app:lintDebug`: passed with 0 errors and 23 warnings
- `git diff --check`: passed

The remaining lint warnings are dependency/tool update notices, a redundant activity label, and unused template resources; dependencies were intentionally not upgraded for the assignment.

Manual QA performed included:

- Admin login and staff creation
- Live webcam face enrollment through the Pixel 7 emulator
- Staff login using the created employee ID
- Successful attendance by the enrolled person
- A different face producing **Face mismatch** without recording attendance
- Rejection when no face was present
- Location-unavailable failure blocking attendance
- Successful attendance appearing in the Admin's staff-specific history

Force-stop persistence was not part of the completed manual QA. Persistence behavior is covered by the automated database and ViewModel tests.

## Assumptions and Limitations

- AttendAI is a local-only hiring-assignment implementation using shared dummy passwords.
- There is no backend, cloud synchronization, Firebase integration, or multi-device data sharing.
- The MobileFaceNet threshold is provisional and is not production-calibrated for this application.
- There is no liveness detection or anti-spoofing, so the app is not suitable for production biometric deployment as written.
- Emulator webcam behavior depends on the host and AVD configuration; emulator location may be simulated.
- There are no maps, geofences, background tracking, payroll features, or external attendance APIs.
- Staff sessions are held in memory and end when the application process ends.
- Face embeddings and attendance selfies are sensitive biometric and personal data. This assignment keeps them in app-private local storage, but a production system would require a formal privacy, consent, retention, encryption, access-control, and security review.

## Project Structure

```text
app/src/main/
├── assets/models/mobile_facenet/   # Bundled model, metadata, and licenses
├── java/com/divanshgandhi/attendai/
│   ├── camera/                     # CameraX preview and capture
│   ├── data/
│   │   ├── local/                  # Room entities, DAOs, database, migrations
│   │   └── repository/             # Authentication, staff, and attendance repositories
│   ├── di/                         # Manual AppContainer and ViewModel factory
│   ├── face/                       # Detection, alignment, preprocessing, inference, matching
│   ├── location/                   # Fused location abstraction and implementation
│   ├── model/                      # Session and role models
│   ├── navigation/                 # Typed destinations and navigation host
│   ├── storage/                    # App-private attendance selfie storage
│   └── ui/                         # Login, Admin, enrollment, attendance, and shared UI
└── res/                            # Manifest resources and Material theme assets
```

## APK

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

This is a debug build output. A final release or submission APK has not been created yet.

## AI Assistance

AI coding assistants were used during development for architecture discussion, implementation assistance, testing, and review. The resulting code and documented behavior were checked against the repository and executed verification results.
