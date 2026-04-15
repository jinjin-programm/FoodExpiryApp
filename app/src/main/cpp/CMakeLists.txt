cmake_minimum_required(VERSION 3.22.1)
project("foodexpiryapp")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS}")

# 1. 設定 MNN 標頭檔路徑 (指向你 git clone 的位置)
set(MNN_SOURCE_ROOT "D:/MNN-master/MNN-master")
include_directories(${MNN_SOURCE_ROOT}/include)
include_directories(${MNN_SOURCE_ROOT}/include/MNN)
include_directories(${MNN_SOURCE_ROOT}/transformers/llm/engine/include)
include_directories(${MNN_SOURCE_ROOT}/transformers/llm/engine/src)
include_directories(${MNN_SOURCE_ROOT}/tools/cv/include)

# 2. 導入你手動編譯並複製到 jniLibs 的 .so 檔案
set(MNN_LIB_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/arm64-v8a")

add_library(MNN SHARED IMPORTED)
set_target_properties(MNN PROPERTIES IMPORTED_LOCATION ${MNN_LIB_DIR}/libMNN.so)

add_library(MNN_Express SHARED IMPORTED)
set_target_properties(MNN_Express PROPERTIES IMPORTED_LOCATION ${MNN_LIB_DIR}/libMNN_Express.so)

add_library(llm SHARED IMPORTED)
set_target_properties(llm PROPERTIES IMPORTED_LOCATION ${MNN_LIB_DIR}/libllm.so)

add_library(MNN_CL SHARED IMPORTED)
set_target_properties(MNN_CL PROPERTIES IMPORTED_LOCATION ${MNN_LIB_DIR}/libMNN_CL.so)

add_library(MNN_Vulkan SHARED IMPORTED)
set_target_properties(MNN_Vulkan PROPERTIES IMPORTED_LOCATION ${MNN_LIB_DIR}/libMNN_Vulkan.so)

add_library(MNNOpenCV SHARED IMPORTED)
set_target_properties(MNNOpenCV PROPERTIES IMPORTED_LOCATION ${MNN_LIB_DIR}/libMNNOpenCV.so)

# 3. 建立你自己的 C++ 實作檔
# 庫名必須與 MnnLlmNative.kt 中的 System.loadLibrary("mnn_llm_bridge") 一致
add_library(mnn_llm_bridge SHARED mnn_llm_bridge.cpp)

# 4. 連結所有庫
target_link_libraries(mnn_llm_bridge
    MNN
    MNN_Express
    llm
    MNN_CL
    MNN_Vulkan
    MNNOpenCV
    log
    android
    jnigraphics
)

# 5. YOLO bridge library (same MNN deps, NO llm dependency)
add_library(mnn_yolo_bridge SHARED mnn_yolo_bridge.cpp)

target_link_libraries(mnn_yolo_bridge
    MNN
    MNN_Express
    MNNOpenCV
    log
    android
    jnigraphics
)

# 6. 支援 Android 15+ 的 16KB 頁面大小
target_link_options(mnn_llm_bridge PRIVATE "-Wl,-z,max-page-size=16384")
target_link_options(mnn_yolo_bridge PRIVATE "-Wl,-z,max-page-size=16384")
