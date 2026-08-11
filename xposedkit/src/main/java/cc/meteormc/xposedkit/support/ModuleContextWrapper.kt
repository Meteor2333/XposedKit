package cc.meteormc.xposedkit.support

import android.app.Activity
import android.app.ActivityThread
import android.app.Application
import android.app.Instrumentation
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PersistableBundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import cc.meteormc.xposedkit.XLog
import cc.meteormc.xposedkit.XposedKit
import cc.meteormc.xposedkit.call
import cc.meteormc.xposedkit.get
import cc.meteormc.xposedkit.reflect

@Suppress("DEPRECATION")
@ExperimentalStdlibApi
open class ModuleContextWrapper(
    base: Context,
    val mResources: Resources,
    val mThemeId: Int = XposedKit.moduleAppInfo.theme
) : ContextWrapper(base) {
    companion object {
        private const val TAG = "ModuleContextWrapper"

        private val sImplClass by lazy {
            Context::class.java
                .classLoader!!
                .reflect("android.app.ContextImpl")
                ?.type
        }
    }

    private var mImpl: Context? = null
    private var mThread: ActivityThread? = null
    private var mProxyActivity: ComponentName? = null
    private val mInflater by lazy {
        LayoutInflater.from(baseContext).cloneInContext(this)
    }
    private val mTheme by lazy {
        resources.newTheme()
    }

    init {
        findContextImpl(base)?.let {
            this.mImpl = it.apply {
                // 可能会适得其反 在某些情况下反而导致找不到资源
                // XposedKit.addAssetPathToResources(
                //     it.resources,
                //     XposedKit.moduleSource
                // )
            }
            this.mThread = sImplClass?.reflect {
                field("mMainThread")?.get(it) as? ActivityThread?
            }
        }
    }

    private fun findContextImpl(wrapper: Context): Context? {
        var contextImpl = wrapper
        while (contextImpl is ContextWrapper) {
            contextImpl = contextImpl.baseContext
        }

        return contextImpl.takeIf { sImplClass?.isInstance(it) == true }
    }

    override fun getAssets(): AssetManager {
        return mResources.assets
    }

    override fun getResources(): Resources {
        return mResources
    }

    override fun getApplicationContext(): Context {
        return baseContext
    }

    override fun setTheme(resid: Int) {

    }

    /*override*/ fun getThemeResId(): Int {
        return mThemeId
    }

    override fun getTheme(): Resources.Theme {
        mTheme.applyStyle(mThemeId, true)
        return mTheme
    }

    override fun getClassLoader(): ClassLoader {
        return XposedKit.impl.javaClass.classLoader!!
    }

    override fun getPackageName(): String {
        return XposedKit.modulePackageName
    }

    // 替换ApplicationInfo可能有问题 (已知比如startActivity后无法返回)
//    override fun getApplicationInfo(): ApplicationInfo {
//        return XposedKit.moduleAppInfo
//    }

    override fun getPackageResourcePath(): String? {
        return XposedKit.moduleSource
    }

    override fun getPackageCodePath(): String? {
        return XposedKit.moduleSource
    }

    override fun getSystemService(name: String): Any? {
        if (name == LAYOUT_INFLATER_SERVICE) {
            return mInflater
        }

        return super.getSystemService(name)
    }

    override fun startActivity(intent: Intent) {
        startActivity(intent, null)
    }

    override fun startActivity(intent: Intent, options: Bundle?) {
        val targetActivityInfo = XposedKit.moduleActivities.firstOrNull {
            intent.component == ComponentName(it.packageName, it.name)
        }

        if (targetActivityInfo == null) {
            super.startActivity(intent, options)
            return
        }

        val runningActivities = ActivityThread::class.reflect {
            val records = field("mActivities")?.get<Map<*, ActivityThread.ActivityClientRecord>>(mThread)
                ?.values
                ?: return@reflect null
            ActivityThread.ActivityClientRecord::class.reflect {
                records.filter {
                    field("paused")?.get<Boolean>(it) == false
                }.mapNotNull {
                    field("activity")?.get<Activity>(it)?.javaClass?.name
                }
            }
        } ?: throw IllegalStateException("Failed to get running activities from ActivityThread!")

        fun List<ActivityInfo>.filterVaild(): List<ActivityInfo>? {
            return filter {
                // 过滤activity-alias
                if (it.targetActivity != null) return@filter false
                // 过滤已经在运行的Activity
                if (it.name in runningActivities) return@filter false
                // 过滤launchMode与targetInfo不一致的Activity
                if (it.launchMode != targetActivityInfo.launchMode) return@filter false
                return@filter true
            }.ifEmpty { null }
        }

        val pm = baseContext.packageManager
        val basePackage = baseContext.packageName
        if (mProxyActivity == null) {
            val pendingActivities = pm.queryIntentActivities(
                // 先查Launcher Activity
                Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    setPackage(basePackage)
                },
                PackageManager.MATCH_ALL
            ).map { it.activityInfo }.filterVaild() ?: pm.queryIntentActivities(
                // 再查主Activity
                Intent(Intent.ACTION_MAIN).apply {
                    setPackage(basePackage)
                },
                PackageManager.MATCH_ALL
            ).map { it.activityInfo }.filterVaild() ?: pm.getPackageInfo(
                // 最后查找全部
                basePackage,
                PackageManager.GET_ACTIVITIES
            ).activities?.toList()?.filterVaild() ?: throw IllegalStateException(
                "Failed to find any valid Activity in package '$basePackage'!"
            )
            val info = pendingActivities.firstOrNull {
                // 优先取exported=true并且未设置taskAffinity的Activity
                it.exported && it.taskAffinity == null
            } ?: pendingActivities.first()
            mProxyActivity = ComponentName(info.packageName, info.name)
        }

        val newIntent = Intent()
        newIntent.flags = intent.flags
        newIntent.putExtra("is_proxy_activity", true)
        newIntent.putExtra("intent_wrapper", intent)
        newIntent.putExtra("target_activity_info", targetActivityInfo)
        newIntent.setComponent(mProxyActivity)

        val activityThread = mThread!!
        var instrumentation = activityThread.instrumentation
        if (instrumentation !is InstrumentationWrapper) {
            instrumentation = object : InstrumentationWrapper(instrumentation) {
                override fun callActivityOnCreate(activity: Activity?, icicle: Bundle?) {
                    prepareActivity(activity, icicle)
                    super.callActivityOnCreate(activity, icicle)
                }

                override fun callActivityOnCreate(activity: Activity?, icicle: Bundle?, persistentState: PersistableBundle?) {
                    prepareActivity(activity, icicle)
                    super.callActivityOnCreate(activity, icicle, persistentState)
                }

                override fun newActivity(
                    cl: ClassLoader?,
                    className: String?,
                    intent: Intent?
                ): Activity? {
                    return tryNewModuleActivity(intent) ?: super.newActivity(cl, className, intent)
                }

                override fun newActivity(
                    clazz: Class<*>?,
                    context: Context?,
                    token: IBinder?,
                    application: Application?,
                    intent: Intent?,
                    info: ActivityInfo?,
                    title: CharSequence?,
                    parent: Activity?,
                    id: String?,
                    lastNonConfigurationInstance: Any?
                ): Activity? {
                    return tryNewModuleActivity(intent) ?: super.newActivity(
                        clazz,
                        context,
                        token,
                        application,
                        intent,
                        info,
                        title,
                        parent,
                        id,
                        lastNonConfigurationInstance
                    )
                }

                override fun onException(obj: Any?, e: Throwable?): Boolean {
                    if (obj !is Activity) return super.onException(obj, e)

                    val intent = obj.intent
                    val component = intent.component ?: return false
                    if (!this.isProxyActivity(intent) && component.packageName != this@ModuleContextWrapper.packageName) {
                        return super.onException(obj, e)
                    }

                    XLog.e(
                        TAG,
                        "Unable to start activity '${this.getRealIntent(intent)?.component ?: component}'" +
                                " from module '${this@ModuleContextWrapper.packageName}'" +
                                " in package '${baseContext.packageName}'",
                        e
                    )
                    return true
                }

                private fun prepareActivity(activity: Activity?, icicle: Bundle?) {
                    if (activity == null) return

                    val intent = activity.intent
                    if (!this.isProxyActivity(intent)) return

                    val moduleContext = this@ModuleContextWrapper
                    icicle?.classLoader = moduleContext.classLoader

                    activity.intent = this.getRealIntent(intent) ?: intent
                    ContextThemeWrapper::class.reflect {
                        field("mBase")?.set(activity, moduleContext)
                        field("mResources")?.set(activity, mResources)
                    }

                    val targetActivityInfo = this.getTargetActivityInfo(intent)
                    var targetTheme = targetActivityInfo?.theme ?: 0
                    if (targetTheme == 0 || targetTheme == moduleContext.mThemeId) {
                        targetTheme = moduleContext.mThemeId
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            activity.theme = moduleContext.theme
                        } else {
                            ContextThemeWrapper::class.reflect {
                                field("mTheme")?.set(activity, moduleContext.theme)
                            }
                        }
                    }
                    activity.setTheme(targetTheme)
                }

                private fun tryNewModuleActivity(intent: Intent?): Activity? {
                    if (intent == null) return null
                    if (!this.isProxyActivity(intent)) return null

                    val realContext = this@ModuleContextWrapper
                    val realClassLoader = realContext.classLoader
                    val realIntent = this.getRealIntent(intent) ?: return null
                    val realComponent = realIntent.component ?: return null

                    var exception: Throwable? = null
                    val className = runCatching {
                        realContext.packageManager.getActivityInfo(
                            realComponent,
                            PackageManager.GET_META_DATA
                        )
                    }.onFailure {
                        exception = it
                    }.map {
                        it.targetActivity ?: it.name
                    }.getOrElse {
                        realComponent.className
                    }

                    return try {
                        super.newActivity(
                            realClassLoader,
                            className,
                            realIntent
                        )
                    } catch (e: ClassNotFoundException) {
                        if (exception != null) {
                            exception.addSuppressed(e)
                            XLog.e(
                                TAG,
                                "Failed to find Activity '$className'" +
                                        " from module '${realContext.packageName}'" +
                                        " in package '${baseContext.packageName}'," +
                                        " and the subsequent operation also failed",
                                exception
                            )
                        }

                        null
                    }
                }

                private fun isProxyActivity(intent: Intent): Boolean {
                    return intent.getBooleanExtra("is_proxy_activity", false)
                }

                private fun getRealIntent(intent: Intent): Intent? {
                    return intent.getParcelableExtra("intent_wrapper")
                }

                private fun getTargetActivityInfo(intent: Intent): ActivityInfo? {
                    return intent.getParcelableExtra("target_activity_info")
                }
            }

            ActivityThread::class.reflect {
                field("mInstrumentation")?.set(activityThread, instrumentation)
            }
        }

        Instrumentation::class.reflect {
            method(
                "execStartActivity",
                Context::class.java,
                IBinder::class.java,
                IBinder::class.java,
                Activity::class.java,
                Intent::class.java,
                Int::class.javaPrimitiveType!!,
                Bundle::class.java
            )!!
        }.call<Unit>(
            instrumentation,
            this,
            activityThread.applicationThread,
            null,
            null,
            newIntent,
            -1,
            options
        )
    }

    override fun startActivities(intents: Array<Intent>) {
        startActivities(intents, null)
    }

    override fun startActivities(intents: Array<Intent>, options: Bundle?) {
        throw UnsupportedOperationException("startActivities is not supported in ModuleContextWrapper")
    }
}