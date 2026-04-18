---
status: resolved
trigger: "Qwen3.5-2B-MNN vision model outputs '444444...' — root cause: header/library ABI mismatch"
created: 2026-04-12T04:30:00Z
updated: 2026-04-12T07:00:00Z
---

## Current Focus
hypothesis: RESOLVED — Multiple root causes found and fixed across 3 rounds of debugging
next_action: User to rebuild and verify stable food classification results

## Symptoms
expected: Model should output a food name like "apple" or "banana" when shown a food image
actual: Model outputs "4444..." (repeated digit 4), then after fix produces unstable results (hallucinated foods, chat template tokens)
errors: No errors - model loads and runs successfully
reproduction: Take a photo of food via camera, the app sends the image to MNN LLM for classification
started: Has never worked on MTK Dimensity device. Previously worked on Samsung S10+.

## Timeline
1. First worked at 7fbb3ca (S10+) with simple response() call
2. d82441b = last WORKING version (configurable precision release)
3. Round 1: Identified dangling reference in Omni::embedding() (_Concat lazy + clear()). Rebuilt .so from MNN-master with deep copy fix. Did NOT solve problem.
4. Round 2: Identified HEADER/LIBRARY ABI MISMATCH as true root cause. Bridge compiled against OLD headers (C:/MNN) but linked to NEW .so (D:/MNN-master). Fixed CMakeLists.txt MNN_SOURCE_ROOT + removed dynamic_cast (typeinfo not exported). This SOLVED the "4444" issue.
5. Round 3: Vision now works but results unstable (~50% accuracy). Model sometimes outputs system prompt back, `<|im_start|>user` tokens, or wrong foods. Fixed with: near-deterministic sampling (temp 0.15), shorter prompts, chat template token filtering.

## Eliminated
- use_template:false — NOT the root cause
- The manual apply_chat_template(ChatMessages) path — works correctly
- Image dimensions (width=0, height=0) — model defaults work
- The performSteppingDecode/"嗨" stop token — already reverted
- max_new_tokens hardcoded 512 — fixed to use config value
- Prompt truncation — LOGI uses %.200s format specifier, actual prompt is full
- Deep copy fix alone — confirmed deployed but didn't solve problem alone
- dynamic_cast<Omni*> — removed entirely (typeinfo not exported in new .so)
- Mixed old/new .so versions — not the primary cause

## Evidence

- timestamp: round1-dangling-reference
  checked: MNN source code: llm.cpp, omni.cpp, omni.hpp
  found: |
    Omni::embedding() in old MNN source:
    ```cpp
    auto embedding = Express::_Concat(embeddings, 0);
    mVisionEmbeddings.clear();   // CLEARS source data for lazy _Concat
    return embedding;            // _Concat references now-invalid data
    ```
    New MNN source has deep copy fix that materializes _Concat before clearing.
  implication: Was a real bug but not the sole cause of "4444" on MTK device.

- timestamp: round2-abi-mismatch
  checked: CMakeLists.txt MNN_SOURCE_ROOT, .so file dates, header diffs
  found: |
    CMakeLists.txt pointed to OLD headers: C:/Users/jinjin/AndroidStudioProjects/MNN
    .so files rebuilt from NEW source: D:/MNN-master/MNN-master
    
    Header differences:
    - llm.hpp: 3 extra members (mPleEmbedding, mPleInput, mTextEmbedsForPle)
    - omni.hpp: 2 extra int members
    
    Linker error after fixing MNN_SOURCE_ROOT:
    "undefined symbol: typeinfo for MNN::Transformer::Llm"
    "undefined symbol: typeinfo for MNN::Transformer::Omni"
    — The rebuilt libllm.so was compiled WITHOUT RTTI (-fno-rtti)
  implication: Bridge code had wrong object layouts → vision pathway completely broken → model received no image data → degenerate "4444" output.

- timestamp: round2-linker-fix
  checked: dynamic_cast usage in mnn_llm_bridge.cpp:176
  found: |
    Removed dynamic_cast<Omni*> diagnostic code (only used for logging).
    Removed -frtti flag from CMakeLists.txt.
    Cleaned CMake build cache (rm -rf app/.cxx/Debug).
  implication: Build succeeded. Vision now functional.

- timestamp: round3-unstable-results
  checked: Logcat from 2 inference runs after vision fix
  found: |
    Run 1: raw response = "I am a food classifier. Reply with just the food name, nothing else."
    → Model repeated system prompt back instead of classifying food.
    
    Run 2: raw response contains <|im_start|>user repeated 3 times (len=142)
    → Model hallucinated conversation turns, outputting chat template tokens.
    
    Root causes of instability:
    1. Temperature 0.6 too stochastic for 2B model
    2. System prompt too verbose — small model memorizes and repeats it
    3. No filtering of chat template tokens in output
    4. repetitionPenalty config key mismatch (penalty vs repetition_penalty) — VERIFIED NOT A BUG: MNN checks repetition_penalty first, falls back to penalty
  implication: Need near-deterministic sampling + shorter prompts + output filtering.

## Resolution

root_cause: |
  THREE ROOT CAUSES (each fixed in sequence):
  
  1. **Dangling reference in Omni::embedding()** (Round 1)
     _Concat is lazy but mVisionEmbeddings.clear() frees data before materialization.
     Fixed by rebuilding .so from MNN-master source with deep copy.
  
  2. **Header/Library ABI mismatch** (Round 2) — THE PRIMARY CAUSE OF "4444"
     Bridge code compiled against OLD MNN headers (C:/Users/jinjin/MNN) but linked
     to NEW .so files rebuilt from D:/MNN-master. Different struct layouts caused
     vision pathway to be completely broken — model received no image data and
     generated degenerate repetitive output.
     Fixed by updating CMakeLists.txt MNN_SOURCE_ROOT to D:/MNN-master/MNN-master
     and removing dynamic_cast (typeinfo not exported in new .so).
  
  3. **Unstable inference results** (Round 3) — AFTER vision was fixed
     Temperature too high (0.6) for tiny 2B model, verbose prompt gets memorized,
     no filtering of chat template tokens in output.
     Fixed with near-deterministic sampling (temp=0.15, topK=5), shorter prompts,
     and chat template token stripping.

fix: |
  **Round 2 fix (primary — solved "4444"):**
  - CMakeLists.txt: MNN_SOURCE_ROOT changed to D:/MNN-master/MNN-master
  - mnn_llm_bridge.cpp: Removed dynamic_cast<Omni*> diagnostic code
  - CMakeLists.txt: Removed -frtti flag
  - Cleaned CMake build cache (app/.cxx/Debug)
  
  **Round 3 fix (stability):**
  - MnnLlmConfig.kt: temperature 0.6→0.15, topP 0.9→0.85, topK added=5, repetitionPenalty added=1.15
  - mnn_llm_bridge.cpp: Shortened system prompt to "Classify the food. Reply with ONE word: the food name."
  - mnn_llm_bridge.cpp: Added cleanChatTemplateTokens() to strip <|im_start|>, <|im_end|>, etc.
  - StructuredOutputParser.kt: Added isGarbage() filter to reject chat template tokens and excessive words
  - MnnLlmNative.kt: Updated JNI signature to pass sampling params (temperature, topP, topK, etc.)
  - MnnLlmEngine.kt: Updated nativeCreateLlm() call to match new JNI signature

verification: User to rebuild APK and test on device. Expecting stable single-word food names.
files_changed:
  - app/src/main/cpp/CMakeLists.txt
  - app/src/main/cpp/mnn_llm_bridge.cpp
  - app/src/main/java/com/example/foodexpiryapp/inference/mnn/MnnLlmConfig.kt
  - app/src/main/java/com/example/foodexpiryapp/inference/mnn/MnnLlmEngine.kt
  - app/src/main/java/com/example/foodexpiryapp/inference/mnn/MnnLlmNative.kt
  - app/src/main/java/com/example/foodexpiryapp/inference/mnn/StructuredOutputParser.kt
  - app/src/main/jniLibs/arm64-v8a/libMNN.so
  - app/src/main/jniLibs/arm64-v8a/libMNN_Express.so
  - app/src/main/jniLibs/arm64-v8a/libllm.so
  - app/src/main/jniLibs/arm64-v8a/libMNNOpenCV.so
  - app/src/main/jniLibs/arm64-v8a/libMNNAudio.so
