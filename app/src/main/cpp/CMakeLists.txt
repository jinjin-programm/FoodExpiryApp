cmake_minimum_required(VERSION 3.22.1)
project("foodexpiryapp")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS}")

if(NOT DEFINED MNN_SOURCE_ROOT)
    set(MNN_SOURCE_ROOT "$ENV{MNN_SOURCE_ROOT}")
endif()

set(MNN_LIB_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/arm64-v8a")

if(MNN_SOURCE_ROOT AND EXISTS "${MNN_SOURCE_ROOT}/include/MNN/Interpreter.hpp")
    include_directories(${MNN_SOURCE_ROOT}/include)
    include_directories(${MNN_SOURCE_ROOT}/include/MNN)
    include_directories(${MNN_SOURCE_ROOT}/transformers/llm/engine/include)
    include_directories(${MNN_SOURCE_ROOT}/transformers/llm/engine/src)
    include_directories(${MNN_SOURCE_ROOT}/tools/cv/include)

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

    add_library(mnn_llm_bridge SHARED mnn_llm_bridge.cpp)

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

    add_library(mnn_yolo_bridge SHARED mnn_yolo_bridge.cpp)

    target_link_libraries(mnn_yolo_bridge
        MNN
        MNN_Express
        MNNOpenCV
        log
        android
        jnigraphics
    )

    target_link_options(mnn_llm_bridge PRIVATE "-Wl,-z,max-page-size=16384")
    target_link_options(mnn_yolo_bridge PRIVATE "-Wl,-z,max-page-size=16384")

    message(STATUS "MNN found at ${MNN_SOURCE_ROOT} — building bridge libraries")
else()
    message(STATUS "MNN source not found — skipping bridge library compilation")
endif()
