module;

#include "jni.h"

export module xposedkit:art_method;

import :jni_helper;
import :symbol_resolver;

namespace xposedkit::art {

    export class ArtMethod {

    private:
        inline static size_t art_method_size = 0;

    public:
        static void Init(JNIEnv *env) {
            auto constructors = (jobjectArray) env->CallObjectMethod(xposedkit::Class_Throwable, xposedkit::Method_Class_declaredConstructors);
            if (env->ExceptionCheck()) return;
            if (!constructors) return;
            if (env->GetArrayLength(constructors) < 2) {
                env->DeleteLocalRef(constructors);
                return;
            }

            auto element0 = env->GetObjectArrayElement(constructors, 0);
            auto element1 = env->GetObjectArrayElement(constructors, 1);
            auto first = env->FromReflectedMethod(element0);
            auto second = env->FromReflectedMethod(element1);
            art_method_size = reinterpret_cast<uintptr_t>(second) - reinterpret_cast<uintptr_t>(first);

            env->DeleteLocalRef(element0);
            env->DeleteLocalRef(element1);
            env->DeleteLocalRef(constructors);
        }

    };

}