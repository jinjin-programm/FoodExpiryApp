---
status: awaiting_human_verify
trigger: "Investigate issue: always-duck"
created: 2026-04-12T00:00:00Z
updated: 2026-04-12T03:00:00Z
---

## Current Focus
<!-- OVERWRITE on each update - reflects NOW -->

hypothesis: The contract is now structured JSON again, and the parser only accepts JSON food fields; the remaining question is whether this stricter contract restores correct classification in practice
test: rebuild the app and ask the user to retest the same fruit image against the stricter JSON contract
expecting: source/build should be clean, and the runtime should either return the correct fruit or Unknown instead of arbitrary wrong food labels
next_action: request device retest with the rebuilt app

## Symptoms
<!-- Written during gathering, then IMMUTABLE -->

expected: The model should output the correct food name for the image, not always "Duck".
actual: The model output is always "Duck" even when the image should classify to something else.
errors: No explicit error messages in the provided logcat. Relevant log lines show successful model load, inference execution, raw response="Duck", parsed food="Duck", confidence=1.0.
reproduction: Run the food classification / vision inference flow on an image; the output consistently becomes "Duck".
started: Currently reproducible based on the provided logcat; no evidence yet that it ever worked correctly in this flow.

## Eliminated
<!-- APPEND only - prevents re-investigating -->

- hypothesis: stale packaging / old JNI bundle is causing the wrong runtime to load
  evidence: `app/src/main/jniLibs/arm64-v8a/libllm.so` and related libs have fresh Apr 12 timestamps, and `build.gradle.kts` packages directly from `src/main/jniLibs`.
  timestamp: 2026-04-12T00:00:00Z

## Evidence
<!-- APPEND only - facts discovered -->

- timestamp: 2026-04-12T00:00:00Z
  checked: knowledge base
  found: Existing debug entry was unrelated (camera preview bug), so no known pattern match for this symptom.
  implication: treat this as a fresh inference-path issue.

- timestamp: 2026-04-12T00:00:00Z
  checked: Llm inference repository and engine
  found: Repository retries only when the result is generic; engine passes raw image bytes to native inference and parses whatever the native layer returns. No hardcoded "Duck" fallback exists in Kotlin.
  implication: root cause is likely in prompt/native model behavior rather than the repository/parser layer.

- timestamp: 2026-04-12T00:00:00Z
  checked: native MNN bridge and VisionScan capture path
  found: The native bridge does attach an image to MultimodalPrompt.images and uses IMREAD_COLOR, while VisionScanFragment rotates/crops a live camera bitmap before inference. No obvious code path hardcodes "Duck" or substitutes a label.
  implication: the bug is not a literal hardcoded output in the app; likely model-level behavior or stale/incorrect model bundle.

- timestamp: 2026-04-12T00:00:00Z
  checked: build docs and packaged native libs
  found: docs/MNN_BUILD.md requires rebuilding libllm.so with `-DLLM_SUPPORT_VISION=true`, and the app ships prebuilt `app/src/main/jniLibs/arm64-v8a/libllm.so` instead of building from source at runtime.
  implication: a stale or incorrectly built native bundle can persist across installs unless explicitly replaced/verified.

- timestamp: 2026-04-12T00:00:00Z
  checked: native library strings
  found: `libllm.so` exports `MultimodalPrompt`, `processImageContent`, and `tokenizer_encode` symbols, which is consistent with a vision-capable build rather than a text-only bridge.
  implication: the runtime likely does have multimodal support compiled in.

- timestamp: 2026-04-12T00:00:00Z
  checked: native prompt strings in mnn_llm_bridge.cpp
  found: The old food-classifier wording was replaced with official-style prompts: system="You are a helpful assistant.", user="<img>in_memory_image</img>What is in this image?".
  implication: prompt bias from the old food-classifier wording has been removed in source.

- timestamp: 2026-04-12T00:00:00Z
  checked: VisionScanFragment analyzer format
  found: The analyzer was explicitly set to RGBA_8888 while the local converter still reads YUV planes. That mismatch can corrupt the frame before inference.
  implication: the vision path can feed bad visual input to the model.

- timestamp: 2026-04-12T00:30:00Z
  checked: current workspace camera/image path
  found: VisionScanFragment no longer sets an RGBA_8888 output format; it uses the same ImageProxy YUV→Bitmap conversion pattern as ScanFragment and YoloScanFragment. The native bridge then JPEG-encodes that bitmap and decodes it with MNN::CV::imdecode(IMREAD_COLOR) before attaching it to the multimodal prompt.
  implication: the specific format-mismatch hypothesis is no longer supported by source; the remaining failure is downstream of capture/conversion.

- timestamp: 2026-04-12T00:30:00Z
  checked: model config and repository flow
  found: Sampling is already near-deterministic in MnnLlmConfig (temperature 0.15, topP 0.85, topK 5, repetitionPenalty 1.15), the bridge uses a neutral prompt, and StructuredOutputParser rejects non-food hallucinations. The repository retries only if the parsed result is generic.
  implication: prompt/sampling/parser are intentionally constrained; the remaining candidate is the specific model bundle or its multimodal behavior, not a loose app-side prompt bug.

- timestamp: 2026-04-12T01:00:00Z
  checked: VisionScanFragment capture path
  found: The analyzer stores the latest rotated frame only (`latestBitmap = Bitmap.createBitmap(..., rotation)`), `captureAndAnalyze()` stops the camera before inference, and `runAskAiInference(bitmap)` forwards the captured bitmap directly to the use case without extra crop/resize. The standalone crop helper is only used when analyzing the live preview without a clicked capture.
  implication: no obvious stale-frame race or extra image transformation exists in the captured-path flow; the remaining uncertainty is model/runtime behavior, not capture state.

- timestamp: 2026-04-12T02:15:00Z
  checked: runtime retest + historical planning docs
  found: User retest shows the model loads from the expected `/storage/emulated/0/Android/data/com.example.foodexpiryapp/files/mnn_models` directory, uses temp=0.15/topP=0.85/topK=5, and still hallucinates a non-food object. Historical STATE.md says the breakthrough fix was a **food-specific JSON-only prompt** that replaced the generic `"What is in this image?"` prompt, while the current bridge still uses `system="You are a helpful assistant."` and `user="<img>in_memory_image</img>What is in this image?"`.
  implication: the runtime evidence rules out camera corruption and points at a prompt-contract regression versus the previously working food-classification path.

- timestamp: 2026-04-12T02:30:00Z
  checked: debug build verification
  found: `./gradlew assembleDebug` completed successfully after updating the bridge prompt to explicitly request food-only output.
  implication: the fix compiles and is ready for on-device retest.

- timestamp: 2026-04-12T03:00:00Z
  checked: parser contract update + rebuild
  found: `StructuredOutputParser` now accepts only JSON food objects (`name`, `name_zh`, `confidence`) and rejects free-form guesses; `./gradlew assembleDebug` passed after the change.
  implication: the app now enforces a structured output contract again, matching the historical working path.

- timestamp: 2026-04-12T03:30:00Z
  checked: active build tree and captured-image flow
  found: `settings.gradle.kts` includes only `:app`, while the nested `app/app` tree is a stale duplicate copy. In the active `app` module, `VisionScanFragment` captures the latest YUV frame, rotates it, and `runAskAiInference()` now always center-crops before inference so the model sees the food instead of the whole preview frame.
  implication: the working build should be using the outer `app` sources only; the next retest should tell us whether the crop change improves the fruit result.

- timestamp: 2026-04-12T02:45:00Z
  checked: human retest on strawberry
  found: Model returned `Peanuts`; bridge prompt is food-specific, image decode is still correct (BGR/IMREAD_COLOR), and the raw response was accepted by `StructuredOutputParser` as a valid food name.
  implication: the prompt is still too loose and the parser is too permissive; structured output enforcement is needed.

## Resolution
<!-- OVERWRITE as understanding evolves -->

root_cause: The current strongest hypothesis is that the inference input was too wide/loose for the food crop path, and the capture flow was not consistently focusing the food item before inference. The stale duplicate `app/app` tree also created confusion about which source was active.
fix: Active build tree verified as outer `:app`; `VisionScanFragment` now crops the captured frame before inference, and the bridge/parser remain on the structured JSON contract.
verification: `./gradlew :app:assembleDebug` passed after the crop change; awaiting on-device retest.
files_changed: [app/src/main/java/com/example/foodexpiryapp/presentation/ui/vision/VisionScanFragment.kt, app/src/main/java/com/example/foodexpiryapp/inference/mnn/MnnLlmEngine.kt, app/src/main/cpp/mnn_llm_bridge.cpp, app/src/main/java/com/example/foodexpiryapp/inference/mnn/StructuredOutputParser.kt]
