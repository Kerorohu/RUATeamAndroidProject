
cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_VERBOSE_MAKEFILE on)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++11")

include_directories(D:/Project/AndroidProject/UXSDKDemo/app/src/main/cpp/include)

add_library(lib_opencv STATIC IMPORTED)

set_target_properties(lib_opencv PROPERTIES IMPORTED_LOCATION
        D:/Project/AndroidProject/UXSDKDemo/app/libs/${ANDROID_ABI}/libopencv_java3.so)

add_library( # Sets the name of the library.
        native-lib
        # Sets the library as a shared library.
        SHARED
        # Provides a relative path to your source file(s).
        native-lib.cpp
        )

find_library( # Sets the name of the path variable.
        log-lib
        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)
target_link_libraries( # Specifies the target library.
        native-lib
        lib_opencv
        android -ljnigraphics
        # Links the target library to the log library
        # included in the NDK.
        ${log-lib})