#include <jni.h>

#include "java_primitive.h"

#include <string>
#include <vector>

static jclass methodClass;

static jmethodID declaringClsMid;
static jmethodID paramTypesMid;
static jmethodID returnTypeMid;

extern "C"
JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv* env;
    vm->GetEnv((void**)&env, JNI_VERSION_1_6);

    xposedkit::InitJavaPrimitive(env);

    auto execClass = env->FindClass("java/lang/reflect/Executable");
    auto methClass = env->FindClass("java/lang/reflect/Method");
    methodClass = (jclass) env->NewGlobalRef(methClass);
    declaringClsMid = env->GetMethodID(
            execClass,
            "getDeclaringClass",
            "()Ljava/lang/Class;");
    paramTypesMid = env->GetMethodID(
            execClass,
            "getParameterTypes",
            "()[Ljava/lang/Class;");
    returnTypeMid = env->GetMethodID(
            methClass,
            "getReturnType",
            "()Ljava/lang/Class;");

    return JNI_VERSION_1_6;
}

extern "C"
JNIEXPORT void JNICALL
JNI_OnUnload(JavaVM *vm, void *reserved) {
    JNIEnv* env;
    vm->GetEnv((void**)&env, JNI_VERSION_1_6);

    env->DeleteGlobalRef(methodClass);
}

extern "C"
JNIEXPORT jobject JNICALL
Java_cc_meteormc_xposedkit_nativelib_NativeBridge_AllocObject(JNIEnv *env, jclass thiz,
                                                              jclass clazz) {
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
    auto clazz = (jclass) env->CallObjectMethod(method, declaringClsMid);
    auto paramTypes = (jobjectArray) env->CallObjectMethod(method, paramTypesMid);

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

    if (env->IsInstanceOf(method, methodClass)) {
        jvalue local{};
        jvalue* result = &local;
        auto returnType = xposedkit::GetPrimitiveType(
                env,
                (jclass) env->CallObjectMethod((jobject) method, returnTypeMid));
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