#include <jni.h>
#include <mutex>
#include <string>
#include "Search.h"
#include "coords.h"
#include "info.h"

namespace {
std::once_flag solverInit;
void initializeSolver() {
    min2phase::info::init();
    min2phase::coords::init();
}
std::string fromJava(JNIEnv* env, jstring value) {
    if (value == nullptr) return {};
    const char* chars = env->GetStringUTFChars(value, nullptr);
    if (chars == nullptr) return {};
    std::string result(chars);
    env->ReleaseStringUTFChars(value, chars);
    return result;
}
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_huzeifa_cubomagico_solver_NativeSolver_nativeSolve(
        JNIEnv* env, jclass, jstring facelets) {
    try {
        const std::string state = fromJava(env, facelets);
        if (state.size() != 54) {
            return env->NewStringUTF("Erro: o estado precisa de 54 adesivos.");
        }
        const std::string solved = "UUUUUUUUURRRRRRRRRFFFFFFFFFDDDDDDDDDLLLLLLLLLBBBBBBBBB";
        if (state == solved) return env->NewStringUTF("");
        std::call_once(solverInit, initializeSolver);
        std::string result = min2phase::Search().solve(state, 21, 1000000, 0, 0, nullptr);
        if (result.empty()) result = "Erro: estado inválido ou sem solução.";
        return env->NewStringUTF(result.c_str());
    } catch (const std::exception& error) {
        return env->NewStringUTF(error.what());
    } catch (...) {
        return env->NewStringUTF("Erro interno no núcleo de resolução.");
    }
}
