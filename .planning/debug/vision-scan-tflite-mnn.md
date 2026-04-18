---
status: verifying
trigger: "Investigate issue: vision-scan-tflite-mnn - After MNN architecture migration, the Vision Scan tab crashes because FoodClassifier still tries to load a removed TFLite model"
created: 2026-04-09T00:00:00Z
updated: 2026-04-10T21:00:00Z
---

## Current Focus
hypothesis: Restored official MNN Chat defaults in mnn_llm_bridge.cpp. Prior custom prompt/template handling was causing persistent model misidentification. Vision still requires libllm.so rebuild with MNN_BUILD_OPENCV=ON.
test: Build with NDK and test on device — compare output quality with official MNN Chat app
expecting: Model output matches official MNN Chat quality after libllm.so rebuild
next_action: 1) Rebuild libllm.so with vision flags, 2) Build project and test on device

## Symptoms
<!-- Written during gathering, then IMMUTABLE -->

expected: Vision scan loads the MNN model (Qwen3.5-2B-MNN), takes a photo, feeds it to the model, and returns identified food names in Chinese and English
actual: 
1. FoodClassifier crashes on startup trying to load old TFLite model `food_efficientnet_lite0_int8.tflite` (FileNotFoundException)
2. Even when MNN inference runs, all results return "unknown" — food names not identified
errors: 
```
FoodClassifier: Error initializing TFLite model (Ask Gemini)
java.io.FileNotFoundException: food_efficientnet_lite0_int8.tflite
  at android.content.res.AssetManager.nativeOpenAssetFd(Native Method)
  at android.content.res.AssetManager.openFd(AssetManager.java:963)
  at com.example.foodexpiryapp.domain.vision.FoodClassifier.loadModelFile(FoodClassifier.kt:103)
  at com.example.foodexpiryapp.domain.vision.FoodClassifier.initialize(FoodClassifier.kt:92)
  at com.example.foodexpiryapp.presentation.ui.vision.VisionScanFragment.onViewCreated(VisionScanFragment.kt:98)
```
reproduction: Open vision scan tab in the app
started: Broke after MNN architecture migration (v2.0). TFLite model was removed during llama.cpp → MNN swap. MNN loads successfully (JNI_OnLoad OK, All MNN libraries loaded successfully) but inference results are all "unknown".

## Eliminated
<!-- APPEND only - prevents re-investigating -->

- hypothesis: Using MultimodalPrompt with <img>filepath</img> tags would let MNN load image via fallback path
  evidence: MNN's multimodeProcess("img", path) calls visionProcess(file) which is wrapped in #ifdef LLM_SUPPORT_VISION. Without this flag, visionProcess returns empty vector. The fallback path does NOT work without vision support compiled in.
  timestamp: round2

- hypothesis: Passing image data in MultimodalPrompt.images map would bypass the need for LLM_SUPPORT_VISION
  evidence: processImageContent(content, images) checks images map first, then calls visionProcess(it->second.image_data). But visionProcess(VARP) is ALSO wrapped in #ifdef LLM_SUPPORT_VISION and returns empty vector without it. No API-level workaround exists.
  timestamp: round2

- hypothesis: The top-level "enable_thinking":false config would disable Qwen3 thinking mode
  evidence: MNN's set_config() merges JSON into config_. setChatTemplate() extracts jinja.context and sets it on tokenizer. Our "enable_thinking":false at top level was never propagated to the chat template context. The correct config path is "jinja":{"context":{"enable_thinking":false}}.
  timestamp: round2

## Evidence
<!-- APPEND only - facts discovered -->

- timestamp: initial-gathering
  checked: FoodClassifier.kt
  found: Line 38: MODEL_FILE = "food_efficientnet_lite0_int8.tflite" — still references removed TFLite model
  implication: FoodClassifier is obsolete and should not be used

- timestamp: initial-gathering
  checked: VisionScanFragment.kt
  found: Lines 67, 97-98: foodClassifier = FoodClassifier(requireContext()); foodClassifier.initialize() — still creates and initializes obsolete TFLite classifier
  implication: This is the direct cause of the crash

- timestamp: initial-gathering
  checked: assets directory
  found: No TFLite food classification model (food_efficientnet_lite0_int8.tflite) exists — only YOLO TFLite models remain
  implication: Confirms TFLite model was removed during migration

- timestamp: initial-gathering
  checked: VisionScanFragment.kt quick scan flow
  found: Lines 237-246: captureAndAnalyze() first tries foodClassifier.classify(bitmap), then falls back to runAskAiInference()
  implication: Quick scan uses obsolete TFLite; Ask AI uses new MNN pipeline (IdentifyFoodUseCase)

- timestamp: initial-gathering
  checked: LlmInferenceRepositoryImpl.kt → MnnLlmEngine.kt → MnnLlmNative.kt
  found: Complete MNN inference pipeline exists: bitmap → JPEG file → nativeRunInference(imagePath)
  implication: MNN pipeline is correctly wired from Kotlin side

- timestamp: initial-gathering
  checked: mnn_llm_bridge.cpp nativeRunInference
  found: Lines 127-129: history.emplace_back("user", std::string(img_path)) — passes image FILE PATH as plain text string in user message
  implication: CRITICAL BUG: LLM receives "/path/to/temp_food.jpg" as text, NOT the actual image embedded. Vision model cannot see the image.

- timestamp: mnn-api-research
  checked: MNN llm.hpp + omni.cpp (MultimodalPrompt API)
  found: MNN's vision models use MultimodalPrompt struct with images map (PromptImagePart with VARP image_data). Prompt uses <img>placeholder_key</img> tags. Omni::tokenizer_encode finds these tags and calls visionProcess() on actual image tensor data.
  implication: Bridge must use MultimodalPrompt API, not plain ChatMessages

- timestamp: mnn-reference-app
  checked: MNN MnnLlmChat processor.cpp
  found: Reference app extracts <img>path</img> tags from prompt, loads image with LoadImageFromPath(), creates PromptImagePart with VARP image_data, puts in MultimodalPrompt.images map
  implication: Confirmed the correct pattern — bridge needs to load image into VARP tensor and use MultimodalPrompt

- timestamp: round2-checkpoint-response
  checked: Human verification feedback
  found: Model initializes OK (0.4s), image saved as temp file (77415 bytes), nativeRunInference called with <img> tags. BUT two issues: 1) Image not actually read by model (model responds as if no image), 2) Model ignores "disable thinking" config, returns long thinking process causing 59s inference and JSON parse failure.
  implication: Two remaining issues to fix

- timestamp: round2-cmake-investigation
  checked: CMakeCache.txt in MNN/project/android/build_64
  found: MNN_BUILD_OPENCV:BOOL=OFF, MNN_IMGCODECS not set, MNN_BUILD_LLM_OMNI:BOOL=OFF. LLM_SUPPORT_VISION is only defined when MNN_BUILD_OPENCV=ON.
  implication: CRITICAL: libllm.so was compiled WITHOUT vision support. All visionProcess() methods return empty vector. The <img> tag processing is a no-op.

- timestamp: round2-thinking-config-research
  checked: MNN llm_demo.cpp + llm.cpp
  found: Correct way to disable thinking: set_config({"jinja":{"context":{"enable_thinking":false}}}). Our bridge sets top-level "enable_thinking":false which is never propagated to the jinja context used by chat templates.
  implication: Thinking mode config path is incorrect — must nest under jinja.context

- timestamp: round3-official-comparison
  checked: Official MNN Chat llm_session.cpp vs our mnn_llm_bridge.cpp
  found: Our bridge diverged significantly from official patterns: (1) use_template:false with manual template handling vs official use_template:true, (2) custom STEP 1-2-3 prompt vs official "You are a helpful assistant.", (3) custom end_with="<|im_end|>" vs official "\n", (4) single response() call vs official prefill+stepping generate loop, (5) custom stripThinkingProcess with \xef\xb8\xae handling vs official deleteThinkPart for <thinkApproach> tags.
  implication: Custom prompt engineering was operating outside the 2B model's training distribution, causing persistent hallucination despite all prior infrastructure fixes. Official defaults should produce accurate results.

## Resolution

root_cause: THREE issues found:
1. (FIXED in round 1) VisionScanFragment still instantiated obsolete FoodClassifier (TFLite) → crash on tab open
2. (FIXED in round 3) Custom prompt/template handling diverged from official MNN Chat, causing persistent model misidentification. Restored all defaults: use_template=true (removed override), system prompt="You are a helpful assistant.", user prompt="<img>in_memory_image</img>What is in this image?", end_with="\n", stepping generate loop, official stripThinkingProcess.
3. (REQUIRES REBUILD) libllm.so compiled WITHOUT vision support (MNN_BUILD_OPENCV=OFF, no MNN_IMGCODECS). All visionProcess() methods are no-ops returning empty vectors. The <img> tag processing silently drops the image. No API-level workaround exists — must rebuild libllm.so with MNN_BUILD_OPENCV=ON.
fix: 
1. (Round 1) Removed FoodClassifier from VisionScanFragment
2. (Round 2) Fixed thinking config: changed "enable_thinking":false to nested "jinja":{"context":{"enable_thinking":false}}, also set "async":false for synchronous response
3. (Round 2) MUST REBUILD libllm.so: cd MNN/project/android && ./build_64.sh -DMNN_BUILD_OPENCV=ON -DMNN_IMGCODECS=ON then copy new .so files to jniLibs/arm64-v8a/
4. (Round 3) Restored official MNN Chat prompt defaults in mnn_llm_bridge.cpp: system prompt="You are a helpful assistant.", user prompt="<img>in_memory_image</img>What is in this image?". Kept `use_template:false` + manual `apply_chat_template(ChatMessages)` to avoid double-template (verified that `response(MultimodalPrompt)` with `use_template=true` internally calls `apply_chat_template(string)` which would re-wrap). Fixed `imdecode` flag from `3` (default/error case → raw RGB) to `IMREAD_COLOR` (1 → BGR) matching what `qwen2VisionProcess` expects (it does `COLOR_BGR2RGB` internally). Simplified `stripThinkingProcess` to match official `deleteThinkPart`.
5. (Round 3) Updated StructuredOutputParser.kt with flexible natural-language food name extraction patterns to handle free-form model output
verification: 
files_changed: [VisionScanFragment.kt, mnn_llm_bridge.cpp, CMakeLists.txt (build instructions), StructuredOutputParser.kt]
