module;

#include "jni.h"

export module xposedkit:jni_helper;

namespace xposedkit {

jclass Class_Boolean;
jclass Class_Byte;
jclass Class_Char;
jclass Class_Short;
jclass Class_Int;
jclass Class_Long;
jclass Class_Float;
jclass Class_Double;
jclass Class_Void;

jclass Class_Executable;
jclass Class_Method;

jmethodID Method_Boolean_boxing;
jmethodID Method_Byte_boxing;
jmethodID Method_Char_boxing;
jmethodID Method_Short_boxing;
jmethodID Method_Int_boxing;
jmethodID Method_Long_boxing;
jmethodID Method_Float_boxing;
jmethodID Method_Double_boxing;

jmethodID Method_Boolean_unboxing;
jmethodID Method_Byte_unboxing;
jmethodID Method_Char_unboxing;
jmethodID Method_Short_unboxing;
jmethodID Method_Int_unboxing;
jmethodID Method_Long_unboxing;
jmethodID Method_Float_unboxing;
jmethodID Method_Double_unboxing;

jmethodID Method_Executable_declaringClass;
jmethodID Method_Executable_paramTypes;
jmethodID Method_Method_returnType;

void InstallJniHelper(JNIEnv* env) {
    Class_Boolean                        = (jclass) env->NewGlobalRef(env->FindClass("java/lang/Boolean"));
    Class_Byte                           = (jclass) env->NewGlobalRef(env->FindClass("java/lang/Byte"));
    Class_Char                           = (jclass) env->NewGlobalRef(env->FindClass("java/lang/Character"));
    Class_Short                          = (jclass) env->NewGlobalRef(env->FindClass("java/lang/Short"));
    Class_Int                            = (jclass) env->NewGlobalRef(env->FindClass("java/lang/Integer"));
    Class_Long                           = (jclass) env->NewGlobalRef(env->FindClass("java/lang/Long"));
    Class_Float                          = (jclass) env->NewGlobalRef(env->FindClass("java/lang/Float"));
    Class_Double                         = (jclass) env->NewGlobalRef(env->FindClass("java/lang/Double"));
    Class_Void                           = (jclass) env->NewGlobalRef(env->FindClass("java/lang/Void"));

    Class_Executable                     = (jclass) env->NewGlobalRef(env->FindClass("java/lang/reflect/Executable"));
    Class_Method                         = (jclass) env->NewGlobalRef(env->FindClass("java/lang/reflect/Method"));

    Method_Boolean_boxing                = env->GetStaticMethodID(Class_Boolean, "valueOf", "(Z)Ljava/lang/Boolean;");
    Method_Byte_boxing                   = env->GetStaticMethodID(Class_Byte, "valueOf", "(B)Ljava/lang/Byte;");
    Method_Char_boxing                   = env->GetStaticMethodID(Class_Char, "valueOf", "(C)Ljava/lang/Character;");
    Method_Short_boxing                  = env->GetStaticMethodID(Class_Short, "valueOf", "(S)Ljava/lang/Short;");
    Method_Int_boxing                    = env->GetStaticMethodID(Class_Int, "valueOf", "(I)Ljava/lang/Integer;");
    Method_Long_boxing                   = env->GetStaticMethodID(Class_Long, "valueOf", "(J)Ljava/lang/Long;");
    Method_Float_boxing                  = env->GetStaticMethodID(Class_Float, "valueOf", "(F)Ljava/lang/Float;");
    Method_Double_boxing                 = env->GetStaticMethodID(Class_Double, "valueOf", "(D)Ljava/lang/Double;");

    Method_Boolean_unboxing              = env->GetMethodID(Class_Boolean, "booleanValue", "()Z");
    Method_Byte_unboxing                 = env->GetMethodID(Class_Byte, "byteValue", "()B");
    Method_Char_unboxing                 = env->GetMethodID(Class_Char, "charValue", "()C");
    Method_Short_unboxing                = env->GetMethodID(Class_Short, "shortValue", "()S");
    Method_Int_unboxing                  = env->GetMethodID(Class_Int, "intValue", "()I");
    Method_Long_unboxing                 = env->GetMethodID(Class_Long, "longValue", "()J");
    Method_Float_unboxing                = env->GetMethodID(Class_Float, "floatValue", "()F");
    Method_Double_unboxing               = env->GetMethodID(Class_Double, "doubleValue", "()D");

    Method_Executable_declaringClass     = env->GetMethodID(Class_Executable, "getDeclaringClass", "()Ljava/lang/Class;");
    Method_Executable_paramTypes         = env->GetMethodID(Class_Executable, "getParameterTypes", "()[Ljava/lang/Class;");
    Method_Method_returnType             = env->GetMethodID(Class_Method, "getReturnType", "()Ljava/lang/Class;");
}

void UninstallJniHelper(JNIEnv* env) {
    env->DeleteGlobalRef(Class_Boolean);
    env->DeleteGlobalRef(Class_Byte);
    env->DeleteGlobalRef(Class_Char);
    env->DeleteGlobalRef(Class_Short);
    env->DeleteGlobalRef(Class_Int);
    env->DeleteGlobalRef(Class_Long);
    env->DeleteGlobalRef(Class_Float);
    env->DeleteGlobalRef(Class_Double);
    env->DeleteGlobalRef(Class_Void);

    env->DeleteGlobalRef(Class_Executable);
    env->DeleteGlobalRef(Class_Method);
}

}