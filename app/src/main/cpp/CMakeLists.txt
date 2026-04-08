cmake_minimum_required(VERSION 3.22.1)
project("mnn_llm_bridge")

add_library(mnn_llm_bridge SHARED
        mnn_llm_bridge.cpp
)

find_library(log-lib log)

target_link_libraries(mnn_llm_bridge
        ${log-lib}
        # TODO: Link against MNN LLM shared library when full implementation is ready
        # llm (from MNN's prebuilt libs)
)
