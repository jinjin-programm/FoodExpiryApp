cmake_minimum_required(VERSION 3.22.1)

project("llamajni")

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Add_compile_options for Android
add_compile_options(-stdlib=libc++)

# Find and load the llama.cpp libraries
# The pre-built libraries are in src/main/jniLibs/${ANDROID_ABI}/
set(jniLibsDir ${CMAKE_SOURCE_DIR}/src/main/jniLibs)

# Create the JNI library
add_library(llama SHARED llamajni.cpp)

# Include directories
target_include_directories(llama PRIVATE
    ${CMAKE_SOURCE_DIR}/src/main/jni
)

# Link libraries - load llama dynamically at runtime
# We don't link directly, we'll use dlopen in the JNI code
target_link_libraries(llama
    android
    log
    dl
)
