# Quick Task 260409-vki: Improve LLM Few-Shot Prompt with Visual Description Examples

**Date:** 2026-04-09
**Status:** Completed

## Summary

Improved the LLM food identification prompt by adding 200 visual description → food name examples to guide the model toward correct ingredient name outputs.

## Changes

### app/src/main/cpp/mnn_llm_bridge.cpp

1. **Added `buildFewShotExamples()` function** (lines 32-236)
   - Generates 200 visual description examples covering all major food categories
   - Pattern: `A [visual description] output:{"name_en":"FoodName"}`
   - Covers: fruits, vegetables, meats, seafood, dairy, grains, prepared foods, spices, nuts

2. **Updated `LlmInstance` struct**
   - Added `few_shot_examples` field to store the examples string

3. **Updated `nativeCreateLlm`**
   - Calls `buildFewShotExamples()` after model load
   - Stores examples in instance for use during inference

4. **Updated `nativeRunInference` prompt**
   - Uses the 200 examples as few-shot context
   - Guides model to describe visually then output JSON

5. **Updated `nativeRunInferenceWithHint` prompt**
   - Includes category-specific suggestions plus the examples
   - Stronger guidance when the first answer was too generic

## Testing

- Native C++ build successful: `./gradlew.bat :app:externalNativeBuildDebug`
- No compilation errors

## Expected Impact

- Model should output specific food names (e.g., "Tomato") instead of generic categories (e.g., "Vegetable")
- 200 examples provide strong pattern recognition for the 2B quantized model
- Retry prompt now has both category hints AND full examples for better recovery

## Next Steps

- Test on-device inference to validate improvement
- Monitor for any context length issues on low-memory devices