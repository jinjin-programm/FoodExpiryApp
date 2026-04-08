cmake_minimum_required(VERSION 3.22.1)
project("mnn_llm_bridge")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# MNN paths - adjust if MNN is in a different location
# Default: MNN is a sibling directory to FoodExpiryApp
set(MNN_SOURCE_ROOT "${CMAKE_CURRENT_SOURCE_DIR}/../../../../MNN")
set(MNN_INSTALL_ROOT "${MNN_SOURCE_ROOT}/project/android/build_64")
set(MNN_LIB_PATH "${MNN_INSTALL_ROOT}/lib")

# Check if MNN exists
if(NOT EXISTS "${MNN_SOURCE_ROOT}/include")
    message(WARNING "MNN source not found at ${MNN_SOURCE_ROOT}. "
                    "Please clone MNN and build it following docs/MNN_BUILD.md")
endif()

# Include directories
include_directories("${MNN_SOURCE_ROOT}/include/")
include_directories("${MNN_SOURCE_ROOT}/transformers/llm/engine/include")
include_directories("${MNN_SOURCE_ROOT}/source/")
include_directories("${MNN_SOURCE_ROOT}/3rd_party/")

# Source files
add_library(mnn_llm_bridge SHARED
    mnn_llm_bridge.cpp
)

# Find system libraries
find_library(log-lib log)
find_library(android-lib android)

# MNN prebuilt shared libraries (IMPORTED)
if(EXISTS "${MNN_LIB_PATH}/libMNN.so")
    add_library(MNN SHARED IMPORTED)
    set_target_properties(MNN PROPERTIES
        IMPORTED_LOCATION "${MNN_LIB_PATH}/libMNN.so"
    )
    
    add_library(llm SHARED IMPORTED)
    set_target_properties(llm PROPERTIES
        IMPORTED_LOCATION "${MNN_LIB_PATH}/libllm.so"
    )
    
    add_library(MNN_Express SHARED IMPORTED)
    set_target_properties(MNN_Express PROPERTIES
        IMPORTED_LOCATION "${MNN_LIB_PATH}/libMNN_Express.so"
    )
    
    target_link_libraries(mnn_llm_bridge
        ${log-lib}
        ${android-lib}
        MNN
        llm
        MNN_Express
    )
    
    message(STATUS "MNN libraries found at ${MNN_LIB_PATH}")
else()
    # Fallback: stub library for development without MNN built
    message(WARNING "MNN libraries not found at ${MNN_LIB_PATH}. "
                    "Building stub library. Please build MNN following docs/MNN_BUILD.md")
    target_link_libraries(mnn_llm_bridge
        ${log-lib}
        ${android-lib}
    )
endif()

# Enable 16KB page size support for Android 15+
target_link_options(mnn_llm_bridge PRIVATE "-Wl,-z,max-page-size=16384")