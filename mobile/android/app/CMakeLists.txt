cmake_minimum_required(VERSION 3.22.1)

project(libreconnect-daemon CXX)

# Set the NDK toolchain file
set(CMAKE_SYSTEM_NAME Android)
set(CMAKE_SYSTEM_VERSION 21) # Minimum API level
set(CMAKE_ANDROID_ARCH_ABI arm64-v8a) # Or other ABIs like armeabi-v7a, x86, x86_64
set(CMAKE_ANDROID_NDK ${ANDROID_NDK})
set(CMAKE_TOOLCHAIN_FILE ${ANDROID_NDK}/build/cmake/android.toolchain.cmake)

# No Rust compilation in CMake. Rust shared library will be pre-built and placed in jniLibs.