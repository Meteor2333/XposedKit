package cc.meteormc.xposedkit

import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.os.ParcelFileDescriptor
import android.util.Log

internal interface XposedInterface {
    companion object {
        private const val TAG = "XposedKitEntry"
    }

    val apiVer: Int

    val frameworkLabel: String

    val frameworkVer: String

    val frameworkVerCode: Long

    val moduleSource: String

    val moduleAppInfo: ApplicationInfo

    fun getRemotePrefs(name: String): SharedPreferences

    fun getRemoteFile(name: String): ParcelFileDescriptor

    fun getRemoteFiles(): List<String>

    fun printLog(
        priority: Int,
        tag: String,
        msg: String,
        tr: Throwable?
    )

    fun formatLog(priority: Int, tag: String, msg: String): String {
        val level = when (priority) {
            Log.VERBOSE -> "VERBOSE"
            Log.DEBUG -> "DEBUG"
            Log.INFO -> "INFO"
            Log.WARN -> "WARN"
            Log.ERROR -> "ERROR"
            Log.ASSERT -> "ASSERT"
            else -> ""
        }
        val values = mapOf(
            "level" to level,
            "level_short" to (level.firstOrNull() ?: "").toString(),
            "module_package" to XposedKit.modulePackageName,
            "tag" to tag,
            "message" to msg
        )
        return "%(\\w+)%".toRegex().replace(XLog.pattern) {
            values[it.groupValues[1]] ?: it.value
        }
    }
}