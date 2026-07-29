package cc.meteormc.xposedkit

import cc.meteormc.xposedkit.param.PackageLoadedParam
import cc.meteormc.xposedkit.param.ProcessLoadedParam
import cc.meteormc.xposedkit.param.HotReloadParam
import cc.meteormc.xposedkit.param.SystemServerStartingParam

interface XposedModule {
    fun onProcessLoaded(param: ProcessLoadedParam) {

    }

    fun onPackageLoaded(param: PackageLoadedParam) {

    }

    fun onSystemServerStarting(param: SystemServerStartingParam) {

    }

    fun onHotReloadOld(param: HotReloadParam): Boolean {
        return false
    }

    fun onHotReloadNew(param: HotReloadParam) {

    }
}