# Milestone 2A file inventory

Compared with the Milestone 1 snapshot taken before implementation. No existing source files were deleted. MainActivity is unchanged. Generated Gradle build outputs are excluded.

## Modified

- [app/src/main/AndroidManifest.xml](../app/src/main/AndroidManifest.xml)
- [app/src/main/java/com/divanshgandhi/attendai/AttendAiApplication.kt](../app/src/main/java/com/divanshgandhi/attendai/AttendAiApplication.kt)
- [app/src/main/java/com/divanshgandhi/attendai/di/AppContainer.kt](../app/src/main/java/com/divanshgandhi/attendai/di/AppContainer.kt)
- [app/src/main/java/com/divanshgandhi/attendai/di/AppViewModelFactory.kt](../app/src/main/java/com/divanshgandhi/attendai/di/AppViewModelFactory.kt)
- [app/src/main/java/com/divanshgandhi/attendai/navigation/AttendAiNavHost.kt](../app/src/main/java/com/divanshgandhi/attendai/navigation/AttendAiNavHost.kt)
- [app/src/main/java/com/divanshgandhi/attendai/navigation/Routes.kt](../app/src/main/java/com/divanshgandhi/attendai/navigation/Routes.kt)
- [app/src/main/java/com/divanshgandhi/attendai/ui/admin/stafflist/StaffListScreen.kt](../app/src/main/java/com/divanshgandhi/attendai/ui/admin/stafflist/StaffListScreen.kt)
- [app/src/main/java/com/divanshgandhi/attendai/ui/components/PlaceholderScreen.kt](../app/src/main/java/com/divanshgandhi/attendai/ui/components/PlaceholderScreen.kt)
- [app/src/main/res/values/strings.xml](../app/src/main/res/values/strings.xml)
- [app/build.gradle.kts](../app/build.gradle.kts)
- [gradle/libs.versions.toml](../gradle/libs.versions.toml)

## Created

- [app/src/androidTest/assets/faces/ATTRIBUTION.md](../app/src/androidTest/assets/faces/ATTRIBUTION.md)
- [app/src/androidTest/assets/faces/person_a_2009.jpg](../app/src/androidTest/assets/faces/person_a_2009.jpg)
- [app/src/androidTest/assets/faces/person_a_2012.jpg](../app/src/androidTest/assets/faces/person_a_2012.jpg)
- [app/src/androidTest/assets/faces/person_b_2013.jpg](../app/src/androidTest/assets/faces/person_b_2013.jpg)
- [app/src/androidTest/java/com/divanshgandhi/attendai/FaceCameraSmokeTest.kt](../app/src/androidTest/java/com/divanshgandhi/attendai/FaceCameraSmokeTest.kt)
- [app/src/androidTest/java/com/divanshgandhi/attendai/face/FacePipelineTest.kt](../app/src/androidTest/java/com/divanshgandhi/attendai/face/FacePipelineTest.kt)
- [app/src/main/assets/models/mobile_facenet/LICENSE](../app/src/main/assets/models/mobile_facenet/LICENSE)
- [app/src/main/assets/models/mobile_facenet/LICENSE.qualcomm](../app/src/main/assets/models/mobile_facenet/LICENSE.qualcomm)
- [app/src/main/assets/models/mobile_facenet/NOTICE.md](../app/src/main/assets/models/mobile_facenet/NOTICE.md)
- [app/src/main/assets/models/mobile_facenet/metadata.json](../app/src/main/assets/models/mobile_facenet/metadata.json)
- [app/src/main/assets/models/mobile_facenet/mobile_facenet.tflite](../app/src/main/assets/models/mobile_facenet/mobile_facenet.tflite)
- [app/src/main/java/com/divanshgandhi/attendai/camera/CameraCapture.kt](../app/src/main/java/com/divanshgandhi/attendai/camera/CameraCapture.kt)
- [app/src/main/java/com/divanshgandhi/attendai/camera/CapturedPhoto.kt](../app/src/main/java/com/divanshgandhi/attendai/camera/CapturedPhoto.kt)
- [app/src/main/java/com/divanshgandhi/attendai/face/FaceAlignment.kt](../app/src/main/java/com/divanshgandhi/attendai/face/FaceAlignment.kt)
- [app/src/main/java/com/divanshgandhi/attendai/face/FaceMatcher.kt](../app/src/main/java/com/divanshgandhi/attendai/face/FaceMatcher.kt)
- [app/src/main/java/com/divanshgandhi/attendai/face/FacePreprocessor.kt](../app/src/main/java/com/divanshgandhi/attendai/face/FacePreprocessor.kt)
- [app/src/main/java/com/divanshgandhi/attendai/face/FaceProcessingException.kt](../app/src/main/java/com/divanshgandhi/attendai/face/FaceProcessingException.kt)
- [app/src/main/java/com/divanshgandhi/attendai/face/FaceRecognitionEngine.kt](../app/src/main/java/com/divanshgandhi/attendai/face/FaceRecognitionEngine.kt)
- [app/src/main/java/com/divanshgandhi/attendai/face/ImageOrientation.kt](../app/src/main/java/com/divanshgandhi/attendai/face/ImageOrientation.kt)
- [app/src/main/java/com/divanshgandhi/attendai/face/MlKitFaceDetector.kt](../app/src/main/java/com/divanshgandhi/attendai/face/MlKitFaceDetector.kt)
- [app/src/main/java/com/divanshgandhi/attendai/face/TfliteFaceEmbedder.kt](../app/src/main/java/com/divanshgandhi/attendai/face/TfliteFaceEmbedder.kt)
- [app/src/main/java/com/divanshgandhi/attendai/ui/facepoc/FacePocScreen.kt](../app/src/main/java/com/divanshgandhi/attendai/ui/facepoc/FacePocScreen.kt)
- [app/src/main/java/com/divanshgandhi/attendai/ui/facepoc/FacePocViewModel.kt](../app/src/main/java/com/divanshgandhi/attendai/ui/facepoc/FacePocViewModel.kt)
- [app/src/test/java/com/divanshgandhi/attendai/face/FaceMathTest.kt](../app/src/test/java/com/divanshgandhi/attendai/face/FaceMathTest.kt)
- [docs/face-poc.md](../docs/face-poc.md)
- [docs/milestone-2a-files.md](../docs/milestone-2a-files.md)
- [docs/face-poc-observed-scores.txt](../docs/face-poc-observed-scores.txt)
