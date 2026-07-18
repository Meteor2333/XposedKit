package cc.meteormc.xposedkit.hook

abstract class BaseHooker<T : HookerContext> {
    var hooked = false
        private set

    protected abstract fun T.hook()

    protected open fun T.unhook() {

    }

    fun installHook(context: T) {
        context.hook()
        hooked = true
    }

    fun uninstallHook(context: T) {
        context.unhook()
        hooked = false
    }
}