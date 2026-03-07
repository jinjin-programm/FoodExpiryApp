# AI Vision Scanner - Technical Documentation

## Overview

The AI Vision Scanner is an on-device food recognition system that uses **Qwen3-VL-2B**, a vision-language model (VLM) running locally on Android devices via `llama.cpp`. It captures food images through the camera and identifies:
- **Food item name**
- **Expiry date** (if visible on packaging)

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Android Application                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐       │
│  │   CameraX    │───▶│   Bitmap     │───▶│   Resize     │       │
│  │  (Preview)   │    │  (640x480)   │    │  (320x240)   │       │
│  └──────────────┘    └──────────────┘    └──────────────┘       │
│                                                 │                │
│                                                 ▼                │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                    VisionScanFragment                     │   │
│  │  - Camera preview                                         │   │
│  │  - Progress indicator (Step 1/2/3)                        │   │
│  │  - Result display                                         │   │
│  └──────────────────────────────────────────────────────────┘   │
│                               │                                  │
│                               ▼                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                     LlamaBridge (Kotlin)                  │   │
│  │  - loadModel(path, contextSize, threads)                  │   │
│  │  - loadMmproj(path)                                       │   │
│  │  - generateWithImage(prompt, bitmap)                      │   │
│  └──────────────────────────────────────────────────────────┘   │
│                               │                                  │
│                               ▼ (JNI)                            │
├─────────────────────────────────────────────────────────────────┤
│                      Native Layer (C++)                          │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                    llamajni.cpp                           │   │
│  │  - JNI bindings for llama.cpp                             │   │
│  │  - Memory management (KV cache clearing)                  │   │
│  │  - Image bitmap processing                                │   │
│  └──────────────────────────────────────────────────────────┘   │
│                               │                                  │
│                               ▼                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │               llama.cpp + mtmd (Multimodal)               │   │
│  │  - GGUF model loading                                     │   │
│  │  - Image tokenization (CLIP/ViT encoder)                  │   │
│  │  - Text generation (transformer inference)                │   │
│  └──────────────────────────────────────────────────────────┘   │
│                               │                                  │
│                               ▼                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                    GGML Backend                           │   │
│  │  - CPU inference (ARM NEON optimized)                     │   │
│  │  - Quantized operations (Q4_K_M, Q8_0)                    │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

---

## Model Components

### 1. Language Model (model.gguf)
- **File**: `Qwen3VL-2B-Instruct-Q4_K_M.gguf`
- **Size**: ~1.1 GB
- **Quantization**: Q4_K_M (4-bit)
- **Purpose**: Text understanding and generation
- **Embedding dimension**: 1024

### 2. Vision Encoder (mmproj.gguf)
- **File**: `mmproj-Qwen3VL-2B-Instruct-Q8_0.gguf`
- **Size**: ~445 MB
- **Quantization**: Q8_0 (8-bit)
- **Purpose**: Convert image pixels to token embeddings
- **Must match model's embedding dimension**

### Why Two Files?
```
Text Model (model.gguf):
├── Token embeddings
├── Transformer layers (24 layers)
├── Attention heads
└── Output projection

Vision Encoder (mmproj.gguf):
├── Image patch encoder (ViT)
├── Projection layer (image → text embedding space)
└── Position embeddings

The mmproj projects image features into the same
embedding space as text tokens, allowing the model
to "see" images as if they were a sequence of words.
```

---

## Processing Pipeline

### Step 1: Image Capture
```
CameraX → ImageAnalysis → YUV_420_888 → NV21 → JPEG → Bitmap
                                    (640x480)
```

### Step 2: Image Preprocessing
```kotlin
// Resize for faster processing
val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 320, 240, true)

// Convert to RGB byte array
val rgbData = ByteArray(width * height * 3)
for (pixel in pixels) {
    rgbData[i++] = (pixel >> 16) and 0xFF  // R
    rgbData[i++] = (pixel >> 8) and 0xFF   // G
    rgbData[i++] = pixel and 0xFF          // B
}
```

### Step 3: Image Tokenization (Native)
```cpp
// Create bitmap from RGB data
mtmd_bitmap* bitmap = mtmd_bitmap_init(width, height, rgbData);

// Tokenize image into patches (10x8 = 80 tokens for 320x240)
mtmd_input_chunks* chunks = mtmd_input_chunks_init();
mtmd_tokenize(ctx, chunks, &text, &bitmap, 1);

// Result: Image becomes ~80 tokens (like 80 words)
```

### Step 4: Prompt Construction
```
<|vision_start|>
[80 image tokens representing the food photo]
<|vision_end|>

Identify the food item and expiry date in this image.

Respond in exactly this format:
FOOD: [food name]
EXPIRY: [date or "not visible"]
```

### Step 5: Inference
```
Token IDs → Transformer Layers → Logits → Sampling → Token IDs → Text
           (24 layers)                     (greedy)
```

### Step 6: Response Parsing
```kotlin
// Regex finds FOOD: and EXPIRY: patterns
val foodRegex = Regex("""FOOD:\s*\[?([^\[\]\n]+)\]?"?""")
val expiryRegex = Regex("""EXPIRY:\s*\[?([^\[\]\n]+)\]?"?""")

// Takes LAST match (model often repeats correct answer)
val foodName = foodRegex.findAll(response).lastOrNull()?.value
```

---

## Performance Metrics

| Stage | Time (CPU) | Memory |
|-------|------------|--------|
| Image encoding | ~5s | ~50MB |
| Image decoding | ~6s | ~100MB |
| Text generation | ~40s | ~1.5GB |
| **Total** | **~51s** | **~1.5GB** |

### Bottlenecks
1. **Text generation**: 78% of total time
2. **Image encoding**: 10% of total time
3. **Image decoding**: 12% of total time

### Optimization Opportunities
1. **GPU/Vulkan**: 3-5x speedup (requires shader compilation)
2. **Smaller mmproj (Q4_K_M)**: 30% less memory, 15% faster
3. **Batch processing**: Process multiple images in parallel
4. **KV cache reuse**: Keep model loaded between scans

---

## Key Technical Concepts

### 1. Quantization
```
FP16 (original) → Q4_K_M (4-bit) → 75% size reduction
                 Q8_0 (8-bit)   → 50% size reduction

Trade-off: Slight accuracy loss for much faster inference
```

### 2. Context Window
```
Context size: 4096 tokens

Allocation:
- System prompt: ~50 tokens
- Image tokens: 80-256 tokens (depends on image size)
- User prompt: ~30 tokens
- Response: ~100-512 tokens

Total must be < 4096 or model crashes
```

### 3. KV Cache
```cpp
// Clear between independent generations
llama_memory_clear(mem, false);

// Without clearing, the model would try to
// continue the previous conversation, causing
// context overflow and crashes
```

### 4. Multimodal Architecture
```
                    ┌─────────────┐
                    │   Image     │
                    │  (320x240)  │
                    └──────┬──────┘
                           │
                           ▼
              ┌────────────────────────┐
              │   Vision Transformer   │
              │   (mmproj.gguf)        │
              │   - Patch extraction   │
              │   - Feature encoding   │
              └────────────┬───────────┘
                           │
                           ▼
              ┌────────────────────────┐
              │   Projection Layer     │
              │   (image → text space) │
              └────────────┬───────────┘
                           │
                           ▼
         ┌─────────────────────────────────────┐
         │     Shared Embedding Space          │
         │  [img_token_1] [img_token_2] ...    │
         │  [text_token_1] [text_token_2] ...  │
         └─────────────────────────────────────┘
                           │
                           ▼
              ┌────────────────────────┐
              │   Language Model       │
              │   (model.gguf)         │
              │   - Self-attention     │
              │   - FFN layers         │
              └────────────┬───────────┘
                           │
                           ▼
                    ┌─────────────┐
                    │ Response    │
                    │ "FOOD: ..." │
                    └─────────────┘
```

---

## File Structure

```
app/src/main/
├── assets/llm/
│   ├── model.gguf          # Qwen3-VL-2B Q4_K_M (1.1GB)
│   └── mmproj.gguf         # Vision encoder Q8_0 (445MB)
│
├── java/.../vision/
│   └── VisionScanFragment.kt    # Camera + UI + parsing
│
├── java/.../llm/
│   └── LlamaBridge.kt          # Kotlin JNI wrapper
│
├── jni/
│   ├── llamajni.cpp            # Native implementation
│   └── CMakeLists.txt          # Build configuration
│
└── jniLibs/arm64-v8a/
    ├── libllama.so             # LLM engine (31MB)
    ├── libmtmd.so              # Multimodal support (8MB)
    ├── libggml.so              # Tensor library
    ├── libggml-base.so         # Base operations
    └── libggml-cpu.so          # CPU backend
```

---

## Prompt Engineering

### Current Prompt
```
Identify the food item and expiry date in this image.

Respond in exactly this format with no other text:
FOOD: [food name]
EXPIRY: [date in DD/MM/YYYY format, or "not visible" if not shown]
```

### Why This Works
1. **Direct instruction**: Clear task definition
2. **Format specification**: Exact output format required
3. **Fallback handling**: "not visible" for missing dates

### Handling Chatty Output
The model sometimes adds preambles:
```
"The image shows a red apple with a yellowish tint...
FOOD: Apple
EXPIRY: not visible"
```

**Solution**: Regex parsing takes the LAST match
```kotlin
foodRegex.findAll(response).lastOrNull()  // Gets "Apple"
```

---

## Limitations

1. **Speed**: 50+ seconds per scan (CPU-only)
2. **Memory**: Requires 2GB+ RAM
3. **Storage**: Model files take 1.5GB
4. **Accuracy**: 
   - Works well for: Clear food items, visible text
   - Struggles with: Blurry images, handwritten dates, non-food items

---

## Future Improvements

### Short-term
- [ ] Download Q4_K_M mmproj for faster inference
- [ ] Add confidence score display
- [ ] Support batch scanning

### Medium-term
- [ ] Vulkan GPU acceleration (3-5x speedup)
- [ ] Multiple model support (switch between models)
- [ ] OCR overlay for date detection

### Long-term
- [ ] Fine-tuned food recognition model
- [ ] Voice output of results
- [ ] Automatic expiry tracking integration

---

## Troubleshooting

### "Model not loaded"
- Check model files exist in `assets/llm/`
- Verify files are valid GGUF format
- Clear app data and reinstall

### "Vision not available"
- Ensure `mmproj.gguf` is present
- Check mmproj matches model (same embedding dimension)
- Look for "n_embd mismatch" in logcat

### App crashes during scan
- Context size too small → Increase to 4096
- Out of memory → Close other apps
- Native crash → Check logcat for `ggml_abort`

### Slow performance
- Normal for CPU-only inference
- Consider smaller model (Q3_K_M)
- Reduce image size further (256x192)

---

## References

- [Qwen3-VL Model](https://huggingface.co/Qwen/Qwen3-VL-2B-Instruct-GGUF)
- [llama.cpp](https://github.com/ggml-org/llama.cpp)
- [Multimodal Documentation](https://github.com/ggml-org/llama.cpp/blob/master/docs/multimodal.md)
- [Android CameraX](https://developer.android.com/training/camerax)