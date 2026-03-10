cmake_minimum_required(VERSION 3.22.1)

project("llamajni")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_compile_options(-stdlib=libc++)
add_definitions(-D_CRT_SECURE_NO_WARNINGS)
add_definitions(-DLLAMA_SHARED)

set(LLAMA_DIR "C:/llama.cpp")
set(LLAMA_INCLUDE_DIR "${LLAMA_DIR}/include")
set(GGML_INCLUDE_DIR "${LLAMA_DIR}/ggml/include")
set(MTMD_DIR "${LLAMA_DIR}/tools/mtmd")

set(jniLibsDir ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI})

add_library(llama SHARED IMPORTED)
set_target_properties(llama PROPERTIES IMPORTED_LOCATION ${jniLibsDir}/libllama.so)

add_library(ggml SHARED IMPORTED)
set_target_properties(ggml PROPERTIES IMPORTED_LOCATION ${jniLibsDir}/libggml.so)

add_library(ggml-base SHARED IMPORTED)
set_target_properties(ggml-base PROPERTIES IMPORTED_LOCATION ${jniLibsDir}/libggml-base.so)

add_library(ggml-cpu SHARED IMPORTED)
set_target_properties(ggml-cpu PROPERTIES IMPORTED_LOCATION ${jniLibsDir}/libggml-cpu.so)

# Multimodal tool library
add_library(mtmd SHARED IMPORTED)
set_target_properties(mtmd PROPERTIES IMPORTED_LOCATION ${jniLibsDir}/libmtmd.so)

add_library(llama_jni SHARED llamajni.cpp)

target_include_directories(llama_jni PRIVATE
    ${CMAKE_SOURCE_DIR}
    ${LLAMA_INCLUDE_DIR}
    ${GGML_INCLUDE_DIR}
    ${MTMD_DIR}
)

if (NOT MSVC)
    target_compile_options(llama_jni PRIVATE -Wno-cast-qual)
endif()

target_link_libraries(llama_jni
    llama
    ggml
    ggml-base
    ggml-cpu
    mtmd
    android
    log
)