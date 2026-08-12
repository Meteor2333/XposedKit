module;

#include "jni.h"

export module xposedkit:class_linker;

import :art_method;
import :symbol_resolver;

namespace xposedkit::art {

    export class ClassLinker {

    private:
        inline static auto SetEntryPointsToInterpreter_ = SymbolResolver::Find<void (*)(ClassLinker*, ArtMethod*)>("_ZNK3art11ClassLinker27SetEntryPointsToInterpreterEPNS_9ArtMethodE");

    public:
        inline static bool SetEntryPointsToInterpreter(ArtMethod* method) {
            // 仅支持Android12及以下
            if (SetEntryPointsToInterpreter_) {
                SetEntryPointsToInterpreter_(nullptr, method);
                return true;
            }

            return false;
        }

    };

}