package cc.meteormc.xposedkit.param

import android.os.Bundle

data class HotReloadingParam(
    val extras: Bundle?,
    var savedData: Any?
)