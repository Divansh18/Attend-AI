# Bundled face embedding model

Model: Qualcomm AI Hub MobileFaceNet, float TFLite export v0.63.0.
File is unmodified from the publisher archive (3,988,392 bytes).

SHA-256: `4a5cfa4491ca0b02d4ea1552b808568b1569921d999b36c82b79def393d116ea`

Download: https://qaihub-public-assets.s3.us-west-2.amazonaws.com/qai-hub-models/models/mobile_facenet/releases/v0.63.0/mobile_facenet-tflite-float.zip

Publisher model card: https://huggingface.co/qualcomm/MobileFaceNet

Model provenance: foamliu/MobileFaceNet, `weights/mobilefacenet.pt`, asset version 1
in Qualcomm's export recipe. Trained on refined MS-Celeb-1M according to the upstream
README. This is an assignment proof of concept, not a deployment or accuracy certification.

The publisher model card identifies the model as Apache-2.0 and links to
https://github.com/foamliu/MobileFaceNet/blob/master/LICENSE (included as LICENSE).
The original repository revision inspected was a6cc9032a659b615f477833e1a70b5e7931bcccc.

Qualcomm's wrapper/export code is BSD-3-Clause, copyright 2025 Qualcomm
Technologies, Inc. and/or its subsidiaries (included as LICENSE.qualcomm).
https://github.com/qualcomm/ai-hub-models/blob/v0.63.0/LICENSE

Pinned preprocessing, augmentation and threshold references:
https://github.com/qualcomm/ai-hub-models/blob/v0.63.0/src/qai_hub_models/models/mobile_facenet/model.py
https://github.com/qualcomm/ai-hub-models/blob/v0.63.0/src/qai_hub_models/models/mobile_facenet/app.py
https://github.com/qualcomm/ai-hub-models/blob/v0.63.0/src/qai_hub_models/utils/image_processing.py
https://github.com/foamliu/MobileFaceNet/blob/a6cc9032a659b615f477833e1a70b5e7931bcccc/align_faces.py

Two inputs, img1/img2: FLOAT32 [1,3,112,112], RGB NCHW in [0,1].
The graph applies ImageNet mean [0.485,0.456,0.406] / std [0.229,0.224,0.225]
and original-plus-horizontal-flip augmentation internally. Do not apply these twice.
Output: FLOAT32 [2,128], one embedding per input. L2-normalize outside the graph.
AttendAI feeds the same crop to both inputs and retains row 0, avoiding retention
of the raw reference photo. The duplicate computation is acceptable for this POC.

Comparison: cosine similarity, angular distance in degrees and normalized L2.
Upstream provisional rule: angle < 74.18 degrees (not calibrated for our ML Kit
landmarks/camera pipeline). The UI explicitly identifies the verdict as provisional.
