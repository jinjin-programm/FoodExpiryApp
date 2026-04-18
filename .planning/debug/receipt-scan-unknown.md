---
status: verifying
trigger: "User used receipt scan on watermelon, expecting Qwen3.5-2b to return 'Watermelon', but it returns 'Unknown︮'."
created: 2026-04-10T00:00:00Z
updated: 2026-04-10T21:00:00Z
---

## Current Focus
hypothesis: Custom prompt/template handling diverged from official MNN Chat, causing persistent hallucination and misidentification.
test: Restore official MNN Chat defaults (use_template=true, default system prompt, stepping generate loop) and verify on device.
expecting: Model output quality matches official MNN Chat app with Qwen3.5-2B-MNN.
next_action: Build with Android NDK and test on device.

## Symptoms
expected: Qwen3.5-2b should return 'Watermelon' (or similar) when scanning a watermelon
actual: Always shows 'Unknown︮' (with garbled characters/len=10), or later "I'm unable to see or identify objects from your images."
errors: Logcat shows `nativeRunInference: response=I'm unable to see or identify objects from your images...`.
reproduction: Open Receipt Scan -> Point at Watermelon -> Click Scan
started: Never worked

## Eliminated
- Hypothesis: Qwen3.5-2b is a text-only model. Evidence: `visual.mnn` exists, making it a multimodal model. It was just lacking a text prompt in the multimodal request, leading it to return "Unknown", and the stop token `\xef\xb8\xae` wasn't stripped. (PARTIALLY TRUE: Stop token was an issue, prompt was an issue, but model is indeed multimodal).
- Hypothesis: MNN's engine handles `<img>path</img>` automatically on Android. Evidence: `MNN_IMGCODECS` is disabled in the Android build, so `Omni::visionProcess(const std::string& file)` returns 0 tokens.

## Evidence
- The MNN native call was sending `<img>[path]</img>` without any question.
- The `\xef\xb8\xae` stop token was hardcoded into the MNN response stream termination, but not stripped from the resulting string in `stripThinkingProcess`.
- When a prompt was added, the model replied "I'm unable to see or identify objects from your images."
- `libllm.so` does not export `MNN_IMGCODECS` functionalities natively within the `llm.cpp` execution.
- `Omni::multimodeProcess` returns 0 tokens if `MNN_IMGCODECS` is disabled.
- `libMNNOpenCV.so` is included in the project, so `MNN::CV::imread` can be used explicitly in the JNI bridge to decode the image into a `VARP` before passing to MNN.
- When `image_part.width` and `height` were 0, the MNN internal vision processor failed to correctly partition the image patches, leading to a zero-sized visual token stream.
- Zero-shot classification (outputting only the name) causes the small 2B model to hallucinate (Banana -> Pancake). Instructing it to reason first improves accuracy.
- The model outputs `The food is pepper (specifically a red or orange variety).︮` and `The food is tomato` without the brackets we specified in the prompt.
- The UI contains a `focusRectangle` meant to guide the user, but `VisionScanFragment` passes the uncropped `latestBitmap` from the `imageAnalyzer` directly to `AskAiInference()`. The LLM identifies objects outside the `focusRectangle`.
- The user reports hallucinated foods ("chicken breast", "pineapple") on a cropped Banana. The custom `ImageProxy.toBitmap()` naively concatenates YUV buffers, ignoring `rowStride`. On most devices, this scrambles the image data entirely into pink/green noise, explaining the complete LLM hallucination.
- After fixing the garbage image, the model outputs completely random *but clear* objects like "orange" or "sugar". The `toBitmap()` method returns an unrotated bitmap (e.g. 640x480 landscape), meaning applying the `FILL_CENTER` portrait UI bounding box math grabs a completely wrong area of the image (e.g. table background).
- The image passes cleanly uncropped and rotated (480x640), but the 2B model ignores the "brief description first" instruction and immediately jumps to the final string ("The food is Grape"), causing zero-shot hallucination. Pre-filling the assistant's response to start with `<|im_start|>assistant\nDescription of the image:` forces the Chain-of-Thought required to prevent hallucination in small models.
- **(2026-04-10) Critical Bug Found: `imdecode(img_data, 1)` decodes image as Grayscale (1 channel).** Official MNN Chat demo uses RGB (3 channels). Vision encoder expects RGB input. Grayscale input causes the model to see meaningless tensor data, explaining why it says "no image attached" or hallucinates random objects. Comparison with official MNN iOS demo (`LLMInferenceEngineWrapper.mm`) and Android demo (`llm_session.cpp`) confirmed they always pass RGB images.
- **(2026-04-10) Critical Bug Found: Stop token `\xef\xb8\xae` (U+FEAE, Arabic Presentation Form-B) is incorrect.** The model's `llm_config.json` declares `"eos": "<|im_end|>"`. The official MNN Chat demo uses `"<|end_of_sentence|>"` or the model's configured EOS token. Wrong stop token causes premature termination or garbled output with trailing Unicode artifacts.
- **(2026-04-10) Bug Found: Double chat template application.** The bridge manually called `apply_chat_template(chat)` then passed the result to `response(MultimodalPrompt)`. But `Llm::response(MultimodalPrompt)` internally calls `apply_chat_template()` again when `use_template=true` (the default), wrapping the already-templated text in another `<|im_start|>user\n...<|im_end|>` layer. Fix: set `"use_template":false` in config.
- **(2026-04-10) Bug Found: Duplicate assistant prefix.** After `apply_chat_template()`, the Qwen3.5 template appends `<|im_start|>assistant\n` followed by thinking tags. The bridge checked if template ended with `<|im_start|>assistant\n` — it didn't (ended with thinking tags), so it appended another `<|im_start|>assistant\nDescription of the image:`, creating a duplicate. Fix: use `rfind` to locate the last `<|im_start|>assistant` marker, truncate after it, then append cleanly.

- **(2026-04-10) Bug Found: `enable_thinking:false` disabled model reasoning.** The 2B model needs the thinking chain to reason about image content. Disabling it prevented the model from properly analyzing images. Fix: removed the `jinja.context.enable_thinking` override to use model default (true).
- **(2026-04-10) Overly restrictive prompt design.** The original prompt had multiple issues: (a) too long and specific for a 2B model to follow, (b) `The food is [UNKNOWN]` provided an escape route the model defaulted to, (c) forced `Description of the image:` assistant prefix consumed output tokens. Fix: redesigned prompt using STEP 1-2-3 structure (Categorization → Description & Reasoning → Output) inspired by the foodApp reference project (github.com/jinjin-programm/foodApp). Removed UNKNOWN option, removed forced assistant prefix, simplified to natural language instructions.
- **(2026-04-10) `max_new_tokens=128` too low.** With thinking enabled and STEP 1-2-3 structure, the model needs more tokens to complete its reasoning chain. Kept at 128 for now but may need increase if truncated.
- **(2026-04-10) Persistent hallucination despite all prior fixes.** After Sessions 1-3, model still outputs incorrect food names and reasoning. Testing with official MNN Chat app using same Qwen3.5-2B-MNN model produces accurate results. Root cause: our custom prompt engineering (`use_template:false`, manual template handling, STEP 1-2-3 prompt, custom `end_with`, single `response()` call) diverged significantly from official MNN Chat patterns. The 2B model was trained with standard chat templates and prompts — our customizations caused it to operate outside its training distribution.
- **(2026-04-10) Official MNN Chat stepping pattern.** Official demo uses `response(..., 0)` for prefill only, then loops `generate(1)` up to `max_new_tokens`. Our code was passing `max_new_tokens=256` directly to `response()`. The stepping pattern may affect generation quality on Android.

## Resolution
root_cause: Multiple compounding bugs in `mnn_llm_bridge.cpp` prevented the Qwen3.5-2B vision model from correctly processing images:

1. **Grayscale image decoding (CRITICAL):** `MNN::CV::imdecode(img_data, 1)` decoded images as 1-channel grayscale instead of 3-channel RGB. The vision encoder received incorrect tensor format, making the image content invisible to the model.

2. **Wrong stop token (CRITICAL):** `\xef\xb8\xae` (Arabic Unicode) was used instead of the model's actual EOS `<|im_end|>` (defined in `llm_config.json`). This caused garbled trailing characters in model output.

3. **Double chat template (MAJOR):** The bridge manually applied chat template, then `response(MultimodalPrompt)` applied it again internally (via `use_template=true` default), producing garbled prompt structure where image tokens were buried in doubly-wrapped chat markers.

4. **Duplicate assistant prefix (MAJOR):** The Qwen3.5 template ends with thinking tags after `<|im_start|>assistant\n`. The bridge's suffix check failed to detect this, appending a second `<|im_start|>assistant\n`.

5. **Previous fixes (from earlier sessions):** Zero width/height on `PromptImagePart`, buggy `ImageProxy.toBitmap()` YUV conversion, wrong crop math, zero-shot hallucination in 2B model.
6. **(2026-04-10) max_new_tokens=128 too low:** With thinking enabled and STEP 1-2-3 prompt, the model ran out of tokens at ~593 chars during STEP 2, never reaching STEP 3 output (`The food is [name]`). Fix: increased to 256.
7. **(2026-04-10) stripThinkingProcess deleted all content:** The function erased everything between `<think|>...</think|>` tags including the STEP 1-2-3 reasoning. For the hint retry, all output was inside thinking tags, resulting in empty string (len=0). Fix: changed `removeTag` to only remove the tags themselves while preserving content between them.
8. **(2026-04-10) Image too small (512px maxSide):** Kotlin layer resized to max 512px causing the model to see only partial food objects, leading to misidentification (e.g., identifying a lime as mandarin orange based on partial skin texture). Fix: increased maxSide to 1024.

fix: Applied changes to `mnn_llm_bridge.cpp` across two sessions:

Session 1 (infrastructure fixes):
- `imdecode(img_data, 1)` → `imdecode(img_data, 3)` (RGB) in both `nativeRunInference` and `nativeRunInferenceWithHint`
- `"\xef\xb8\xae"` → `"<|im_end|>"` as stop token in both functions
- Added `"use_template":false` to config JSON to prevent double template application
- Replaced brittle assistant prefix suffix-check with `rfind("<|im_start|>assistant")` truncation logic in both functions

Session 2 (prompt & reasoning fixes):
- Removed `"jinja":{"context":{"enable_thinking":false}}` config override — model now uses default thinking mode
- Replaced complex prompt with STEP 1-2-3 structured prompt (Categorization → Description & Reasoning → Output)
- Removed `The food is [UNKNOWN]` escape route from prompt
- Removed forced `Description of the image:` assistant prefix — model now generates freely from STEP 1
- Both `nativeRunInference` and `nativeRunInferenceWithHint` updated with identical system prompt
- Hint version appends hint to user message: `What food is in this image? Follow the steps above. Hint: [hint]`

Session 3 (token budget, image size, thinking output fixes):
- `max_new_tokens` 128 → 256 to allow model to complete STEP 1-2-3 without truncation
- `MnnLlmEngine.kt` `maxSide` 512 → 1024 for better image detail preservation
- `stripThinkingProcess` rewritten: now removes only the `<think|>`/`</think|>` tags while preserving content between them (previously deleted all content inside tags, causing empty output for hint retries)

verification: Code changes verified via grep — no traces of old prompt, enable_thinking, Description prefix, or UNKNOWN option remain. Build requires Android NDK — to be verified on device.

Session 4 (restore official MNN Chat defaults — 2026-04-10):
- **Root cause analysis**: Compared our `mnn_llm_bridge.cpp` with official MNN Chat demo (`llm_session.cpp`). Found our implementation diverged significantly from official patterns, causing persistent misidentification despite earlier fixes.
- **Key differences found**:
  1. Custom STEP 1-2-3 system prompt (~30 lines) — official uses `"You are a helpful assistant."`
  2. `end_with="<|im_end|>"` — official uses `"\n"` (default)
  3. Custom `stripThinkingProcess` with `\xef\xb8\xae` handling — official uses `deleteThinkPart` for `<thinkApproach>` tags
  4. `imdecode(img_data, 3)` — wrong flag, hits `default` case in `buildImgVARP`; should be `IMREAD_COLOR` (1) → BGR output matching `qwen2VisionProcess` which does `COLOR_BGR2RGB` internally
- **Changes to `mnn_llm_bridge.cpp`**:
  - Replaced STEP 1-2-3 system prompt with `"You are a helpful assistant."` (official default)
  - Simplified user prompt to `"<img>in_memory_image</img>What is in this image?"` (official style)
  - `imdecode(img_data, 3)` → `imdecode(img_data, MNN::CV::IMREAD_COLOR)` — correct BGR output for qwen2VisionProcess pipeline
  - Simplified `stripThinkingProcess`: removed `\xef\xb8\xae` stop token handling, rewrote to match official `deleteThinkPart` pattern (removes `<thinkApproach>` tags and content between them)
- **Template approach**: Kept `use_template:false` with manual `apply_chat_template(ChatMessages)` because `response(MultimodalPrompt)` internally calls `apply_chat_template(string)` when `use_template=true`, which would double-apply the template. Verified: `apply_chat_template(string)` wraps content as `[system, user]` messages → template applied → our already-templated text gets re-wrapped.
  - Hint version: `"<img>in_memory_image</img>What is in this image? It might be a [hint]."`
- **Changes to `StructuredOutputParser.kt`**:
  - Added flexible regex patterns to extract food names from natural language responses (e.g., "This is a watermelon.", "I can see a red apple in the image.")
  - Added STOP_WORDS filter for fallback word extraction
  - Added broader unknown-detection patterns (unable to, cannot, sorry, etc.)
  - Retained JSON parsing as primary method, natural language parsing as fallback

files_changed: ["app/src/main/cpp/mnn_llm_bridge.cpp", "app/src/main/java/com/example/foodexpiryapp/inference/mnn/StructuredOutputParser.kt"]
