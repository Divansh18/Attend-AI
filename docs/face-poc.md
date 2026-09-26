# AttendAI — Milestone 2A face recognition proof of concept

## Status and scope

Implemented a debug-only camera → face detection → alignment → real embedding → comparison flow. Reference embeddings and the latest aligned preview stay in memory. Leaving the screen clears its ViewModel; process death also clears the reference. No Room, staff enrollment, location, attendance records, or selfie files were added. MainActivity is unchanged.

Pixel 7 emulator validation includes actual CameraX capture, real ML Kit detection, and real LiteRT inference on photographic test fixtures. **Fixture measurements are not live selfie measurements.** The user subsequently approved Milestone 2A after live testing with the Mac webcam through the Pixel 7 emulator. No physical-device validation is claimed.

### User-reported live validation (Milestone 2A approval)

| Live comparison | Cosine | Angle | Result |
|---|---:|---:|---|
| Same person | 0.97061 | 13.93° | MATCH |
| Different person | 0.08060 | 85.38° | NO MATCH |
| Original person tested again | Not supplied | Not supplied | MATCH |

These are validation observations supplied by the user, not scientific calibration or production-grade accuracy evidence. The threshold remains provisional and unchanged.

## Exact bundled model

| Property | Value |
|---|---|
| Model/export | Qualcomm AI Hub MobileFaceNet, float TFLite export **v0.63.0** |
| Original model | foamliu/MobileFaceNet; `weights/mobilefacenet.pt`, asset version 1 in export recipe |
| License | Model card identifies Apache-2.0; upstream LICENSE bundled. Qualcomm export wrapper BSD-3-Clause notice also bundled. |
| File | `app/src/main/assets/models/mobile_facenet/mobile_facenet.tflite` |
| Size | 3,988,392 bytes |
| SHA-256 | `4a5cfa4491ca0b02d4ea1552b808568b1569921d999b36c82b79def393d116ea` |
| Inputs | **Two** FLOAT32 tensors (`img1`, `img2`), each **[1, 3, 112, 112]**, NCHW |
| Pixel preprocessing | Aligned RGB; channel planes; each channel / 255.0 to [0,1] |
| Internal preprocessing | Graph applies ImageNet mean [0.485,0.456,0.406] and std [0.229,0.224,0.225]; original + horizontally flipped inference is summed internally. Do not apply these again. |
| Output | FLOAT32 **[2,128]**, one 128-dimensional embedding per input |
| External normalization | L2-normalize the returned embedding; reject zero/non-finite vectors |
| Metrics | Cosine similarity; angle = acos(cosine) in degrees; normalized Euclidean distance = sqrt(2−2cosine) |
| Initial verdict | Upstream rule **angle < 74.18°**, explicitly provisional in UI |

Sources: [publisher model card](https://huggingface.co/qualcomm/MobileFaceNet), [exact download](https://qaihub-public-assets.s3.us-west-2.amazonaws.com/qai-hub-models/models/mobile_facenet/releases/v0.63.0/mobile_facenet-tflite-float.zip), [pinned export implementation](https://github.com/qualcomm/ai-hub-models/blob/v0.63.0/src/qai_hub_models/models/mobile_facenet/model.py), [pinned preprocessing and threshold](https://github.com/qualcomm/ai-hub-models/blob/v0.63.0/src/qai_hub_models/models/mobile_facenet/app.py), [alignment helper](https://github.com/qualcomm/ai-hub-models/blob/v0.63.0/src/qai_hub_models/utils/image_processing.py), [upstream license](https://github.com/foamliu/MobileFaceNet/blob/a6cc9032a659b615f477833e1a70b5e7931bcccc/LICENSE), [Qualcomm license](https://github.com/qualcomm/ai-hub-models/blob/v0.63.0/LICENSE).

The actual bundled flatbuffer tensor shapes were inspected and are checked again at runtime, together with its SHA-256. This is a small mobile embedding network with documented preprocessing, a downloadable Android-compatible export and explicit licensing. It avoids training/conversion infrastructure within a two-day assignment. Its pair graph performs redundant computation: the same crop is supplied to both inputs, and row 0 is retained. This keeps each stored reference independent of raw photos and is acceptable for this POC.

The original training dataset is refined MS-Celeb-1M according to upstream documentation. A couple of public photographic fixtures cannot establish generalization, demographic performance, or a deployment threshold. The publisher pipeline uses RetinaFace landmarks; this POC uses ML Kit landmarks, so its documented threshold has **not** been calibrated for AttendAI.

## Architecture and image contract

- `camera/CameraCapture`: screen-owned CameraX Preview and in-memory ImageCapture, front camera, lifecycle binding, capture executor and image proxy cleanup. Requests 1280×960 with nearest-supported resolution fallback.
- `camera/CapturedPhoto`: bitmap, clockwise rotation metadata and explicit mirrored-pixel flag; ownership transfers to the engine.
- `face/ImageOrientation`: apply CameraX rotation once. CameraX in-memory capture is unmirrored; PreviewView mirrors its front-camera preview only. Mirrored inputs can explicitly be corrected before detection. Preview pixels never enter inference.
- `face/MlKitFaceDetector`: bundled detector, accurate mode, landmarks, default minimum face width ratio 0.1, no contour-only mode. Reject zero or multiple returned faces. Require visible landmarks, at least 100×100 face box, and near-frontal pose (yaw/pitch within 25°). These are practical capture-quality limits, not calibrated identity thresholds.
- `face/FaceAlignment` and `FacePreprocessor`: least-squares similarity transform from eyes, nose and mouth corners to the upstream 112×112 five-point template. Eye/mouth ordering is image-left to image-right. Bilinear bitmap sampling, black borders, RGB float NCHW.
- `face/TfliteFaceEmbedder`: lazy local asset load, checksum/tensor validation, CPU LiteRT interpreter with two threads, actual model execution and L2 normalization.
- `face/FaceMatcher`: deterministic normalized-vector comparison and provisional verdict.
- `face/FaceRecognitionEngine`: owns detector/interpreter, serializes work with a Mutex on Dispatchers.Default, recycles source bitmaps and releases native resources. Waits for non-cancellable ML Kit native work before recycling its input.
- `ui/facepoc`: temporary Material 3 screen and ViewModel, reference in memory, busy/error/result state, permission handling and repeated captures. Manual AppContainer/ViewModel factory wiring is retained. Authentication still uses the existing repository.

Camera decode happens on its executor; image processing, model loading and inference run off the UI thread. No network model download or Play Services model installation is required. Added dependencies: CameraX core/camera2/lifecycle/view **1.6.2**, bundled ML Kit face detection **16.1.7**, LiteRT **1.4.2**, coroutines-play-services **1.9.0** for Task.await. LiteRT's Interpreter API keeps this POC small and matches the published TFLite graph.

Failures shown include permission denial/settings recovery, missing front camera/binding failure, capture failure/timeout, no face, multiple faces, inadequate landmarks/pose, model load failure and inference failure. Rapid duplicate capture requests are ignored while busy. A failed comparison leaves the valid reference available for retry.

## Observed emulator measurements

Pixel_7 AVD, Android 17/API 37 family, arm64, real CPU LiteRT inference. Images resized proportionally to a maximum side of 1280 for tests. Values rounded below. Test photos are separately licensed and only packaged in the instrumentation APK; see their attribution file.

| Comparison against person A / 2009 portrait | Cosine | Angle (degrees) | Normalized L2 | Provisional verdict |
|---|---:|---:|---:|---|
| Same exact photo, three repeat runs | 1.00000000 | 0.000000 | 0.00000000 | Match |
| Same person, independent 2012 portrait | 0.81476303 | 35.436062 | 0.60866570 | Match |
| Different person, 2013 portrait | 0.15870343 | 80.868353 | 1.29714808 | No match |
| Same photo, brightness ×0.8 +15 | 0.98549140 | 9.771851 | 0.17034434 | Match |
| 90°, 180°, 270° rotation then correction | 1.00000000 | 0.000000 | 0.00000000 | Match |
| Mirrored photo then correction | 1.00000000 | 0.000000 | 0.00000000 | Match |

Warm detection/alignment/inference took approximately 214–241 ms per fixture in the measured run; this excludes camera capture and is not a physical-device benchmark. Repeated identical pixels demonstrate determinism, not repeated live capture accuracy. Rotation tests exercise exact pixel correction; physical camera metadata still needs device validation.

An initial 0.05 minimum face ratio produced two overlapping detections on one portrait. Restoring ML Kit's default 0.1 ratio resolved it; the app still rejects every result containing multiple detected faces. The explicit two-person composition test passes. No detections are silently dropped to force a match.

## Final verification

`assembleDebug`, `testDebugUnitTest`, `lintDebug`, and `connectedDebugAndroidTest` all passed on the final source. **18 JVM tests and 8 emulator tests passed.** Lint reports **0 errors, 23 warnings** (dependency update notices and existing template resources/label); no lint baseline or suppression was added. Manual emulator permission denial and subsequent grant recovery were also verified. The final debug APK is installed on Pixel 7.

Exact final-run logs are preserved in [face-poc-observed-scores.txt](face-poc-observed-scores.txt).

## Reproduce automated checks

From the project root, with the Pixel 7 AVD running:

```sh
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
ANDROID_SERIAL=emulator-5554 \
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest --console=plain
```

The original synthetic-camera smoke test was updated in Milestone 2B to accept successful enrollment or a clear face-quality rejection with either synthetic or webcam input. It skips physical hardware. To run just the deterministic face fixture class:

```sh
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.divanshgandhi.attendai.face.FacePipelineTest
```

Fixture tests use the production detector, preprocessing and interpreter—no embedding mocks. JVM tests cover RGB layout/range, alignment, normalization, comparison, invalid values and the strict threshold boundary. Instrumentation covers photographic inference, orientation, no/multiple faces, model-loading/input errors and repeat camera capture; existing login/navigation tests remain included.

## Manual Pixel 7 / physical-device testing

1. Android Studio → Device Manager → start Pixel 7. Select the `app` run configuration and **debug** build variant, select Pixel 7, then Run. Alternatively install `app/build/outputs/apk/debug/app-debug.apk` with adb.
2. Sign in as Admin using **admin / admin123**. Tap **Face recognition · Debug POC** on the Admin placeholder.
3. Tap **Grant camera access**. First deny permission: the screen should remain usable and explain that permission is needed. Tap again to grant, or use **Open app settings → Permissions → Camera → Allow only while using the app** if Android no longer prompts; return to the app.
4. With the default synthetic front camera, tap **Capture reference**. Expect “No face detected…”; repeat and confirm capture remains usable. There should be no fabricated embedding or match.
5. To test real faces in the emulator, stop the AVD, edit it in Device Manager, expand advanced settings and set **Front camera → Webcam0** (if available), save and cold boot. Allow macOS camera access for the emulator. Use a physical device if webcam passthrough is unavailable. The virtual rear-camera scene is not a selfie source.
6. With one consenting participant facing the front camera in good light, tap **Capture reference**. Expect “Reference embedding is held in memory,” an upright aligned face crop and processing time. Capture/compare 5–10 new frames of that person with small changes of distance/expression/light. Record all cosine, angle and L2 values, including rejected attempts.
7. Keep that reference and have a second consenting participant replace the first. Capture/compare 5–10 times. Record scores and provisional verdicts. Compare same-person minimum similarity against different-person maximum; do not claim a validated threshold from this small sample.
8. Test an empty frame and two visible people: expect explicit rejection. Test turning away/covering facial features and retrying. Rotate the device between portrait and landscape and confirm the aligned crop is upright; the preview may look mirrored while inference crop is unmirrored.
9. Tap **Clear reference**: compare disables. Re-enroll and repeat. Press Back, reopen POC: reference must be gone. Force-stop/relaunch also clears it. No selfie or staff record should appear anywhere.
10. For physical Android: enable Developer options → USB debugging, connect USB and accept the device trust prompt, select it in Android Studio and Run. Repeat steps 2–9. Airplane mode can be used to confirm local operation after installation.

Record device/Android version, lighting, participant aliases, reference attempt and each candidate score. The user-reported live observations are recorded above; these do not establish a calibrated threshold. More extensive calibration and physical-device testing remain outside the POC evidence.

## Files

See [the complete created/modified file inventory](milestone-2a-files.md). Build outputs are generated artifacts, not source changes.
