cmake_minimum_required(VERSION 3.22.1)

project("llamajni")

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Add_compile_options for Android
add_compile_options(-stdlib=libc++)
add_definitions(-D_CRT_SECURE_NO_WARNINGS)

# Include directories from llama.cpp
set(LLAMA_INCLUDE_DIR "C:/llama.cpp/include")
set(GGML_INCLUDE_DIR "C:/llama.cpp/ggml/include")
set(LLAMA_COMMON_DIR "C:/llama.cpp/common")

# The pre-built libraries are in src/main/jniLibs/${ANDROID_ABI}/
set(jniLibsDir ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI})

# Import prebuilt libraries
add_library(llama SHARED IMPORTED)
set_target_properties(llama PROPERTIES IMPORTED_LOCATION ${jniLibsDir}/libllama.so)

add_library(ggml SHARED IMPORTED)
set_target_properties(ggml PROPERTIES IMPORTED_LOCATION ${jniLibsDir}/libggml.so)

add_library(ggml-base SHARED IMPORTED)
set_target_properties(ggml-base PROPERTIES IMPORTED_LOCATION ${jniLibsDir}/libggml-base.so)

add_library(ggml-cpu SHARED IMPORTED)
set_target_properties(ggml-cpu PROPERTIES IMPORTED_LOCATION ${jniLibsDir}/libggml-cpu.so)

# Create the JNI library
add_library(llama_jni SHARED llamajni.cpp)

# Include directories
target_include_directories(llama_jni PRIVATE
    ${CMAKE_SOURCE_DIR}
    ${LLAMA_INCLUDE_DIR}
    ${GGML_INCLUDE_DIR}
    ${LLAMA_COMMON_DIR}
)

# Link libraries
target_link_libraries(llama_jni
    llama
    ggml
    ggml-base
    ggml-cpu
    android
    log
)
