cmake_minimum_required(VERSION 3.22.1)


project("rkllm")

add_library(${CMAKE_PROJECT_NAME} SHARED
        llm.cpp)

target_link_libraries(${CMAKE_PROJECT_NAME}
        android log
        omp
        rkllmrt)


add_library(rkllmrt SHARED IMPORTED GLOBAL)
set_target_properties(rkllmrt PROPERTIES
        INTERFACE_INCLUDE_DIRECTORIES ${CMAKE_CURRENT_SOURCE_DIR}/libs/include
        IMPORTED_NO_SONAME TRUE
        IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/libs/arm64-v8a/librkllmrt.so)

add_library(omp SHARED IMPORTED GLOBAL)
set_target_properties(omp PROPERTIES
        IMPORTED_NO_SONAME TRUE
        IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/libs/arm64-v8a/libomp.so)