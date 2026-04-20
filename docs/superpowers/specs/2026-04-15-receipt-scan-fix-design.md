# Receipt Scan Fix - Design Document

## Problem Statement

Receipt scan (Vision Scan) produces completely hallucinated results. When scanning a strawberry, the model outputs "a large group of people wearing black clothing and holding umbrellas".

## Root Cause Analysis

1. **`enable_thinking=false` (line 73-80 in mnn_llm_bridge.cpp)**
   - Forces the 2B model to skip reasoning, which it needs for vision tasks
   - Small models hallucinate without chain-of-thought
   
2. **Zero-shot brief prompt**
   - Current: "Reply with only the single best food name in English, or Unknown..."
   - Too short for 2B model to reason about image content
   
3. **`max_new_tokens=128` too low**
   - With thinking enabled + STEP prompt, model needs ~256 tokens to complete

4. **`stripThinkingProcess` removes all content**
   - Deletes everything between `<think>` tags including useful reasoning

## Solution: Hybrid A+B

### Changes

#### 1. mnn_llm_bridge.cpp

**Remove disable thinking (line 73-80):**
```cpp
// DELETE THIS BLOCK:
instance->llm->set_config(R"({
    "jinja": {
        "context": {
            "enable_thinking": false
        }
    }
})");
LOGI("nativeCreateLlm: disabled Qwen3 thinking mode (enable_thinking=false)");
```

**Replace prompt (line 128):**
```cpp
// BEFORE:
mm_prompt.prompt_template = "<img>image_0</img>Reply with only the single best food name...";

// AFTER:
mm_prompt.prompt_template = R"(
<img>image_0</img>
STEP 1: Is this image showing food? Answer Yes or No.
STEP 2: If Yes, describe what food you see and explain your reasoning.
STEP 3: State the food name in this format: The food is [FOOD_NAME].
)";
```

**Fix stripThinkingProcess (line 299-328):**
```cpp
static std::string stripThinkingProcess(const std::string& text) {
    std::string result = text;
    
    // Remove <think> and </think> tags but KEEP content between them
    auto removeTagsOnly = [](std::string s, const std::string& openTag, 
                              const std::string& closeTag) -> std::string {
        size_t pos = 0;
        while ((pos = s.find(openTag, pos)) != std::string::npos) {
            s.erase(pos, openTag.length());
            size_t closePos = s.find(closeTag, pos);
            if (closePos != std::string::npos) {
                s.erase(closePos, closeTag.length());
            }
        }
        return s;
    };
    
    result = removeTagsOnly(result, "<think>", "</think>");
    result = removeTagsOnly(result, "<think|>", "</think|>");
    result = removeTagsOnly(result, "<thought>", "</thought>");
    
    // Trim whitespace
    size_t first = result.find_first_not_of(" \t\n\r");
    size_t last = result.find_last_not_of(" \t\n\r");
    if (first != std::string::npos && last != std::string::npos) {
        return result.substr(first, last - first + 1);
    }
    return "";
}
```

**Same changes for nativeRunInferenceWithHint (line 215):**
```cpp
std::string user_msg = "<img>image_0</img>\n" 
    "STEP 1: Is this image showing food? Answer Yes or No.\n"
    "STEP 2: If Yes, describe what food you see and explain your reasoning.\n"
    "STEP 3: State the food name in this format: The food is [FOOD_NAME].\n"
    "Hint: It might be a " + std::string(hint_str);
```

#### 2. MnnLlmConfig.kt (line 11)
```kotlin
val maxNewTokens: Int = 256  // was 128
```

#### 3. StructuredOutputParser.kt
Enhance to extract from "The food is X" format:
```kotlin
// Add pattern for chain-of-thought output
private val CHAIN_OF_THOUGHT_RE = Regex(
    """(?i)(?:the\s+)?food\s+is\s+([a-zA-Z]+)""",
    RegexOption.IGNORE_CASE
)

fun parse(rawResponse: String?): FoodIdentification? {
    // ... existing logic ...
    
    // Try chain-of-thought extraction
    CHAIN_OF_THOUGHT_RE.find(cleaned)?.let { match ->
        return FoodIdentification(
            name = match.groupValues[1].trim(),
            nameZh = match.groupValues[1].trim(),
            confidence = 1.0f,
            rawResponse = rawResponse
        )
    }
    
    // ... fallback to existing first-line logic ...
}
```

## Files to Modify

1. `app/src/main/cpp/mnn_llm_bridge.cpp`
2. `app/src/main/java/com/example/foodexpiryapp/inference/mnn/MnnLlmConfig.kt`
3. `app/src/main/java/com/example/foodexpiryapp/inference/mnn/StructuredOutputParser.kt`

## Verification Plan

1. Rebuild APK with NDK
2. Test with strawberry image → expect "Strawberry"
3. Test with various food images
4. Check logcat for thinking process output

## Risk Assessment

- **Medium Risk**: 2B model may still hallucinate on STEP 2, but chain-of-thought is proven more stable than zero-shot
- **Low Risk**: Increased token count may slow inference slightly

## Reference

- Debug doc: `.planning/debug/receipt-scan-unknown.md`
- Previous session: `.planning/debug/qwen-vision-hallucination-4444.md`
