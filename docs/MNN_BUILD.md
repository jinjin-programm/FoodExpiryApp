# MNN Native Build Instructions

This project requires MNN native libraries built from source for the LLM JNI bridge.

## Prerequisites

- Android NDK (version matching `app/build.gradle.kts`)
- Git
- CMake 3.22.1+
- Unix-like shell (WSL on Windows, or native Linux/macOS)

## Build Steps

### 1. Clone MNN Repository

Clone MNN as a sibling directory to this project:

```bash
cd C:\Users\jinjin\AndroidStudioProjects
git clone https://github.com/alibaba/MNN.git
cd MNN
git checkout 3.5.0  # Use the latest stable release
```

### 2. Set NDK Path

```bash
# Windows (WSL or PowerShell)
export ANDROID_NDK=/path/to/Android/Sdk/ndk/27.2.12479018
```

### 3. Build MNN for Android (arm64-v8a)

On WSL or Linux/macOS:

```bash
cd project/android
mkdir build_64
cd build_64
../build_64.sh -DMNN_LOW_MEMORY=true \
  -DMNN_CPU_WEIGHT_DEQUANT_GEMM=true \
  -DMNN_BUILD_LLM=true \
  -DMNN_SUPPORT_TRANSFORMER_FUSE=true \
  -DMNN_ARM82=true \
  -DMNN_USE_LOGCAT=true \
  -DMNN_OPENCL=true \
  -DLLM_SUPPORT_VISION=true \
  -DCMAKE_INSTALL_PREFIX=.
make -j$(nproc)
make install
```

### 4. Verify Build Output

After successful build, verify these files exist:

```
MNN/project/android/build_64/lib/
├── libMNN.so
├── libllm.so
├── libMNN_Express.so
```

### 5. Headers

The headers are in:
```
MNN/include/           # Core MNN headers
MNN/transformers/llm/engine/include/llm/  # LLM headers (llm.hpp)
```

## CMakeLists.txt Configuration

The project's CMakeLists.txt is configured to look for MNN at:
- **MNN_SOURCE_ROOT**: `C:/Users/jinjin/AndroidStudioProjects/MNN` (sibling directory)
- **MNN_INSTALL_ROOT**: `{MNN_SOURCE_ROOT}/project/android/build_64`

If you placed MNN elsewhere, update `CMAKE_MNN_SOURCE_ROOT` in the Gradle build config.

## Troubleshooting

### "libMNN_Express.so not found"
The LLM module requires `libMNN_Express.so`. Ensure `-DMNN_BUILD_LLM=true` was passed to the build script.

### "llm.hpp not found"
Ensure the MNN checkout includes `transformers/llm/engine/include/llm/llm.hpp`. This is present in MNN v3.5.0+.

### Build fails with "undefined reference to MNN::Transformer::Llm"
Link against `libllm.so` in CMakeLists.txt. This library contains the LLM C++ API.

## Alternative: Prebuilt Libraries

If building from source is not feasible, you can:
1. Download the MNN Chat APK from https://github.com/alibaba/MNN/releases
2. Extract `libMNN.so`, `libllm.so`, `libMNN_Express.so` from the APK
3. Place them in `app/src/main/jniLibs/arm64-v8a/`
4. Download headers from the MNN GitHub repository manually

Note: Prebuilt libraries may not be ABI-compatible with your NDK version.