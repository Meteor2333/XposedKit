package cc.meteormc.xposedkit.hook

fun interface InvokeCallback {
    operator fun invoke(info: InvokeInfo)
}