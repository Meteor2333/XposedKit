module;

#include "jni.h"
#include "jvmti.h"

#include <atomic>
#include <string>
#include <vector>

export module xposedkit;

import :runtime;

import :java_primitive;
import :jni_helper;

namespace {
    jvmtiEnv* gJvmti = nullptr;
    std::atomic<jlong> gNextTag{1};
}

extern "C"
JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    xposedkit::InstallJniHelper(env);

    return JNI_VERSION_1_6;
}

extern "C"
JNIEXPORT void JNICALL
JNI_OnUnload(JavaVM *vm, void *reserved) {
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return;
    }

    xposedkit::UninstallJniHelper(env);
}

extern "C"
JNIEXPORT void JNICALL
Java_cc_meteormc_xposedkit_nativelib_NativeBridge_Init(JNIEnv *env, jclass clazz) {
    auto runtime = xposedkit::art::Runtime::Current();
    if (!runtime->EnsurePluginLoaded("libopenjdkjvmti.so", nullptr)) {
        // TODO: 支持Android7及以下无jvmti的设备
    } else {
        JavaVM* vm;
        env->GetJavaVM(&vm);
        if (vm->GetEnv(reinterpret_cast<void**>(&gJvmti), 0x40000000 | JVMTI_VERSION_1_2) != JNI_OK) {
            env->ThrowNew(env->FindClass("java/lang/RuntimeException"), "Failed to get JVMTI environment");
            return;
        }

        jvmtiCapabilities caps{};
        caps.can_tag_objects = 1;
        gJvmti->AddCapabilities(&caps);
    }
}

extern "C"
JNIEXPORT jobject JNICALL
Java_cc_meteormc_xposedkit_nativelib_NativeBridge_AllocObject(JNIEnv *env, jclass thiz,
                                                              jclass clazz) {
    if (!clazz) {
        env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"), "clazz is null");
        return nullptr;
    }

    return env->AllocObject(clazz);
}

extern "C"
JNIEXPORT jobject JNICALL
Java_cc_meteormc_xposedkit_nativelib_NativeBridge_CallNonvirtualMethod(JNIEnv *env, jclass thiz,
                                                                       jobject method,
                                                                       jobject instance,
                                                                       jobjectArray args) {
    if (!method) {
        env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"), "method is null");
        return nullptr;
    }

    if (!instance) {
        env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"), "instance is null");
        return nullptr;
    }

    if (!args) {
        env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"), "args is null");
        return nullptr;
    }

    auto target = env->FromReflectedMethod(method);
    auto clazz = (jclass) env->CallObjectMethod(method, xposedkit::Method_Executable_declaringClass);
    auto paramTypes = (jobjectArray) env->CallObjectMethod(method, xposedkit::Method_Executable_paramTypes);

    jsize length = env->GetArrayLength(paramTypes);
    if (length != env->GetArrayLength(args)) {
        env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"), "argument length mismatch");
        return nullptr;
    }

    std::vector<jvalue> cargs(length);
    for (jsize i = 0; i < length; i++) {
        auto arg = env->GetObjectArrayElement(args, i);
        auto paramType = (jclass) env->GetObjectArrayElement(paramTypes, i);
        cargs[i] = xposedkit::ToPrimitiveType(env, paramType, arg);
        env->DeleteLocalRef(paramType);

        if (env->ExceptionCheck())
            return nullptr;
    }

    env->DeleteLocalRef(paramTypes);

    if (env->IsInstanceOf(method, xposedkit::Class_Method)) {
        jvalue local{};
        jvalue* result = &local;
        auto returnType = xposedkit::GetPrimitiveType(
                env,
                (jclass) env->CallObjectMethod((jobject) method, xposedkit::Method_Method_returnType));
        switch (returnType) {
            case xposedkit::PrimitiveType::Object:
                return env->CallNonvirtualObjectMethodA(instance, clazz, target, cargs.data());
            case xposedkit::PrimitiveType::Boolean:
                local.z = env->CallNonvirtualBooleanMethodA(instance, clazz, target, cargs.data());
                break;
            case xposedkit::PrimitiveType::Byte:
                local.b = env->CallNonvirtualByteMethodA(instance, clazz, target, cargs.data());
                break;
            case xposedkit::PrimitiveType::Char:
                local.c = env->CallNonvirtualCharMethodA(instance, clazz, target, cargs.data());
                break;
            case xposedkit::PrimitiveType::Short:
                local.s = env->CallNonvirtualShortMethodA(instance, clazz, target, cargs.data());
                break;
            case xposedkit::PrimitiveType::Int:
                local.i = env->CallNonvirtualIntMethodA(instance, clazz, target, cargs.data());
                break;
            case xposedkit::PrimitiveType::Long:
                local.j = env->CallNonvirtualLongMethodA(instance, clazz, target, cargs.data());
                break;
            case xposedkit::PrimitiveType::Float:
                local.f = env->CallNonvirtualFloatMethodA(instance, clazz, target, cargs.data());
                break;
            case xposedkit::PrimitiveType::Double:
                local.d = env->CallNonvirtualDoubleMethodA(instance, clazz, target, cargs.data());
                break;
            case xposedkit::PrimitiveType::Void:
                result = nullptr;
                break;
        }

        if (result) {
            return xposedkit::FromPrimitiveType(env, returnType, *result);
        }
    }

    env->CallNonvirtualVoidMethodA(instance, clazz, target, cargs.data());
    return nullptr;
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_cc_meteormc_xposedkit_nativelib_NativeBridge_VisitHeapObjects(JNIEnv *env, jclass thiz,
                                                                   jclass clazz) {
    if (!clazz) {
        env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"), "clazz is null");
        return nullptr;
    }

    jvmtiError error;

    jlong tag = gNextTag.fetch_add(1, std::memory_order_relaxed);
    error = gJvmti->IterateOverInstancesOfClass(
            clazz,
            JVMTI_HEAP_OBJECT_EITHER,
            [](jlong, jlong, jlong* tag_ptr, void* user_data) {
                *tag_ptr = *static_cast<jlong*>(user_data);
                return JVMTI_ITERATION_CONTINUE;
            },
            &tag);
    if (error != JVMTI_ERROR_NONE) {
        char* name;
        std::string err;
        gJvmti->GetErrorName(error, &name);
        if (name) {
            err = name;
            gJvmti->Deallocate(reinterpret_cast<unsigned char*>(name));
        } else {
            err = "Unknown error: " + std::to_string(error);
        }

        env->ThrowNew(env->FindClass("java/lang/RuntimeException"), ("Failed to get objects with tags (" + err + ")").c_str());
        return nullptr;
    }

    jint count;
    jobject* objects;
    error = gJvmti->GetObjectsWithTags(
            1,
            &tag,
            &count,
            &objects,
            nullptr);
    if (error != JVMTI_ERROR_NONE) {
        char* name;
        std::string err;
        gJvmti->GetErrorName(error, &name);
        if (name) {
            err = name;
            gJvmti->Deallocate(reinterpret_cast<unsigned char*>(name));
        } else {
            err = "Unknown error: " + std::to_string(error);
        }

        env->ThrowNew(env->FindClass("java/lang/RuntimeException"), ("Failed to get objects with tags (" + err + ")").c_str());
        return nullptr;
    }

    auto result = env->NewObjectArray(count, clazz, nullptr);
    if (!result) return nullptr;

    for (jint i = 0; i < count; i++) {
        jobject obj = objects[i];
        gJvmti->SetTag(obj, 0);
        env->SetObjectArrayElement(result, i, obj);
        env->DeleteLocalRef(obj);

        if (env->ExceptionCheck())
            return nullptr;
    }

    gJvmti->Deallocate(reinterpret_cast<unsigned char*>(objects));
    return result;
}