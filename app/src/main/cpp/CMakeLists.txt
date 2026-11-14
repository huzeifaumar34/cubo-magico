cmake_minimum_required(VERSION 3.22.1)
project(cubomagico)
add_library(cubomagico SHARED
    native_solver.cpp
    min2phase/src/CubieCube.cpp
    min2phase/src/Search.cpp
    min2phase/src/coords.cpp
    min2phase/src/info.cpp
)
target_include_directories(cubomagico PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/min2phase/src
    ${CMAKE_CURRENT_SOURCE_DIR}/min2phase/include
)
target_compile_features(cubomagico PRIVATE cxx_std_17)
target_compile_options(cubomagico PRIVATE -O2 -fexceptions -frtti)
find_library(log-lib log)
target_link_libraries(cubomagico ${log-lib})
