#ifndef XPOSEDKIT_JAVA_PRIMITIVE_H
#define XPOSEDKIT_JAVA_PRIMITIVE_H

#include <jni.h>

namespace xposedkit {

enum class PrimitiveType {
    Object,
    Boolean,
    Byte,
    Char,
    Short,
    Int,
    Long,
    Float,
    Double,
    Void,
};

void InitJavaPrimitive(JNIEnv* env);

PrimitiveType GetPrimitiveType(JNIEnv* env, jclass clazz);

jobject FromPrimitiveType(JNIEnv* env, PrimitiveType type, jvalue value);

jobject FromPrimitiveType(JNIEnv* env, jclass clazz, jvalue value);

jvalue ToPrimitiveType(JNIEnv* env, PrimitiveType type, jobject value);

jvalue ToPrimitiveType(JNIEnv* env, jclass clazz, jobject value);

}

#endif //XPOSEDKIT_JAVA_PRIMITIVE_H
