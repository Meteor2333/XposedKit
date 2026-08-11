package cc.meteormc.xposedkit

import android.util.Log

object XLog {
    /**
     * %level% - 完整的日志等级 (如INFO WARN等)
     * %level_short% - 简短的日志等级 (如I W等)
     * %module_package% - 模块包名
     * %tag% - 日志标签
     * %message% - 日志内容
     */
    var pattern = "(%module_package%)[%tag%|%level_short%] %message%"

    fun v(tag: String, msg: String, tr: Throwable? = null) {
        print(Log.VERBOSE, tag, msg, tr)
    }

    fun d(tag: String, msg: String, tr: Throwable? = null) {
        print(Log.DEBUG, tag, msg, tr)
    }

    fun i(tag: String, msg: String, tr: Throwable? = null) {
        print(Log.INFO, tag, msg, tr)
    }

    fun w(tag: String, msg: String, tr: Throwable? = null) {
        print(Log.WARN, tag, msg, tr)
    }

    fun e(tag: String, msg: String, tr: Throwable? = null) {
        print(Log.ERROR, tag, msg, tr)
    }

    private fun print(priority: Int, tag: String, msg: String, tr: Throwable?) {
        XposedKit.impl.printLog(priority, tag, msg, tr)
    }
}