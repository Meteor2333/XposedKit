cmake_minimum_required(VERSION 3.22.1)
project(openjdkmti)

add_library(openjdkjvmti_headers INTERFACE)
target_include_directories(openjdkjvmti_headers INTERFACE
        ${CMAKE_CURRENT_SOURCE_DIR}/include
)