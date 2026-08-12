module;

#include <jni.h>

export module xposedkit:thread;

import :object;
import :symbol_resolver;

namespace xposedkit::art {

    export class Thread {

    private:
        inline static auto CurrentFromGdb_ = SymbolResolver::Find<Thread* (*)()>("_ZN3art6Thread14CurrentFromGdbEv");

        inline static auto DecodeJObject_ = SymbolResolver::Find<mirror::Object* (*)(Thread*, jobject)>("_ZNK3art6Thread13DecodeJObjectEP8_jobject");

    public:
        inline static Thread* Current() {
            return CurrentFromGdb_();
        }

        mirror::Object* DecodeJObject(jobject obj) {
            return DecodeJObject_(this, obj);
        }

    };

}