module;

#include <string>

export module xposedkit:runtime;

import :symbol_resolver;

namespace xposedkit::art {

export class Runtime {

private:
    inline static auto Current_ = SymbolResolver::Find<Runtime**>("_ZN3art7Runtime9instance_E");

    inline static auto EnsurePluginLoaded_ = SymbolResolver::Find<bool (*)(Runtime*, const char*, std::string*)>("_ZN3art7Runtime18EnsurePluginLoadedEPKcPNSt3__112basic_stringIcNS3_11char_traitsIcEENS3_9allocatorIcEEEE");

public:
    inline static Runtime* Current() {
        return *Current_;
    }

    bool EnsurePluginLoaded(const char* plugin_name, std::string* error_msg) {
        return EnsurePluginLoaded_(this, plugin_name, error_msg);
    }

};

}