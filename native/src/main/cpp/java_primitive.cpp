#include "include/java_primitive.h"

namespace xposedkit {

static jclass booleanClass;
static jclass byteClass;
static jclass charClass;
static jclass shortClass;
static jclass intClass;
static jclass longClass;
static jclass floatClass;
static jclass doubleClass;
static jclass voidClass;

static jmethodID booleanBoxingMid;
static jmethodID byteBoxingMid;
static jmethodID charBoxingMid;
static jmethodID shortBoxingMid;
static jmethodID intBoxingMid;
static jmethodID longBoxingMid;
static jmethodID floatBoxingMid;
static jmethodID doubleBoxingMid;

static jmethodID booleanUnboxingMid;
static jmethodID byteUnboxingMid;
static jmethodID charUnboxingMid;
static jmethodID shortUnboxingMid;
static jmethodID intUnboxingMid;
static jmethodID longUnboxingMid;
static jmethodID floatUnboxingMid;
static jmethodID doubleUnboxingMid;

void InitJavaPrimitive(JNIEnv* env) {
    auto load = [&](const char* wrapper) -> jclass {
        jclass cls = env->FindClass(wrapper);
        jfieldID fid = env->GetStaticFieldID(cls, "TYPE", "Ljava/lang/Class;");
        jobject primitive = env->GetStaticObjectField(cls, fid);
        return (jclass) env->NewGlobalRef(primitive);
    };

    booleanClass = load("java/lang/Boolean");
    byteClass    = load("java/lang/Byte");
    charClass    = load("java/lang/Character");
    shortClass   = load("java/lang/Short");
    intClass     = load("java/lang/Integer");
    longClass    = load("java/lang/Long");
    floatClass   = load("java/lang/Float");
    doubleClass  = load("java/lang/Double");
    voidClass    = load("java/lang/Void");

    booleanBoxingMid = env->GetStaticMethodID(env->FindClass("java/lang/Boolean"), "valueOf", "(Z)Ljava/lang/Boolean;");
    byteBoxingMid = env->GetStaticMethodID(env->FindClass("java/lang/Byte"), "valueOf", "(B)Ljava/lang/Byte;");
    charBoxingMid = env->GetStaticMethodID(env->FindClass("java/lang/Character"), "valueOf", "(C)Ljava/lang/Character;");
    shortBoxingMid = env->GetStaticMethodID(env->FindClass("java/lang/Short"), "valueOf", "(S)Ljava/lang/Short;");
    intBoxingMid = env->GetStaticMethodID(env->FindClass("java/lang/Integer"), "valueOf", "(I)Ljava/lang/Integer;");
    longBoxingMid = env->GetStaticMethodID(env->FindClass("java/lang/Long"), "valueOf", "(J)Ljava/lang/Long;");
    floatBoxingMid = env->GetStaticMethodID(env->FindClass("java/lang/Float"), "valueOf", "(F)Ljava/lang/Float;");
    doubleBoxingMid = env->GetStaticMethodID(env->FindClass("java/lang/Double"), "valueOf", "(D)Ljava/lang/Double;");

    booleanUnboxingMid = env->GetMethodID(env->FindClass("java/lang/Boolean"), "booleanValue", "()Z");
    byteUnboxingMid = env->GetMethodID(env->FindClass("java/lang/Byte"), "byteValue", "()B");
    charUnboxingMid = env->GetMethodID(env->FindClass("java/lang/Character"), "charValue", "()C");
    shortUnboxingMid = env->GetMethodID(env->FindClass("java/lang/Short"), "shortValue", "()S");
    intUnboxingMid = env->GetMethodID(env->FindClass("java/lang/Integer"), "intValue", "()I");
    longUnboxingMid = env->GetMethodID(env->FindClass("java/lang/Long"), "longValue", "()J");
    floatUnboxingMid = env->GetMethodID(env->FindClass("java/lang/Float"), "floatValue", "()F");
    doubleUnboxingMid = env->GetMethodID(env->FindClass("java/lang/Double"), "doubleValue", "()D");
}

PrimitiveType GetPrimitiveType(JNIEnv* env, jclass clazz) {
    if (env->IsSameObject(clazz, booleanClass))
        return PrimitiveType::Boolean;

    if (env->IsSameObject(clazz, byteClass))
        return PrimitiveType::Byte;

    if (env->IsSameObject(clazz, charClass))
        return PrimitiveType::Char;

    if (env->IsSameObject(clazz, shortClass))
        return PrimitiveType::Short;

    if (env->IsSameObject(clazz, intClass))
        return PrimitiveType::Int;

    if (env->IsSameObject(clazz, longClass))
        return PrimitiveType::Long;

    if (env->IsSameObject(clazz, floatClass))
        return PrimitiveType::Float;

    if (env->IsSameObject(clazz, doubleClass))
        return PrimitiveType::Double;

    if (env->IsSameObject(clazz, voidClass))
        return PrimitiveType::Void;

    return PrimitiveType::Object;
}

jobject FromPrimitiveType(JNIEnv* env, PrimitiveType type, jvalue value) {
    switch (type) {
        case PrimitiveType::Object:
            return value.l;
        case PrimitiveType::Boolean:
            return env->CallStaticObjectMethod(booleanClass, booleanBoxingMid, value.z);
        case PrimitiveType::Byte:
            return env->CallStaticObjectMethod(byteClass, byteBoxingMid, value.b);
        case PrimitiveType::Char:
            return env->CallStaticObjectMethod(charClass, charBoxingMid, value.c);
        case PrimitiveType::Short:
            return env->CallStaticObjectMethod(shortClass, shortBoxingMid, value.s);
        case PrimitiveType::Int:
            return env->CallStaticObjectMethod(intClass, intBoxingMid, value.i);
        case PrimitiveType::Long:
            return env->CallStaticObjectMethod(longClass, longBoxingMid, value.j);
        case PrimitiveType::Float:
            return env->CallStaticObjectMethod(floatClass, floatBoxingMid, value.f);
        case PrimitiveType::Double:
            return env->CallStaticObjectMethod(doubleClass, doubleBoxingMid, value.d);
        case PrimitiveType::Void:
            return nullptr;
    }
}

jobject FromPrimitiveType(JNIEnv* env, jclass clazz, jvalue value) {
    return FromPrimitiveType(env, GetPrimitiveType(env, clazz), value);
}

jvalue ToPrimitiveType(JNIEnv* env, PrimitiveType type, jobject value) {
    jvalue jv{};
    switch (type) {
        case PrimitiveType::Object:
            jv.l = value;
            break;
        case PrimitiveType::Boolean:
            jv.z = env->CallBooleanMethod(value, booleanUnboxingMid);
            break;
        case PrimitiveType::Byte:
            jv.b = env->CallByteMethod(value, byteUnboxingMid);
            break;
        case PrimitiveType::Char:
            jv.c = env->CallCharMethod(value, charUnboxingMid);
            break;
        case PrimitiveType::Short:
            jv.s = env->CallShortMethod(value, shortUnboxingMid);
            break;
        case PrimitiveType::Int:
            jv.i = env->CallIntMethod(value, intUnboxingMid);
            break;
        case PrimitiveType::Long:
            jv.j = env->CallLongMethod(value, longUnboxingMid);
            break;
        case PrimitiveType::Float:
            jv.f = env->CallFloatMethod(value, floatUnboxingMid);
            break;
        case PrimitiveType::Double:
            jv.d = env->CallDoubleMethod(value, doubleUnboxingMid);
            break;
        case PrimitiveType::Void:
            break;
    }

    return jv;
}

jvalue ToPrimitiveType(JNIEnv* env, jclass clazz, jobject value) {
    return ToPrimitiveType(env, GetPrimitiveType(env, clazz), value);
}

}