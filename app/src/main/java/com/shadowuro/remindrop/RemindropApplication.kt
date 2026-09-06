package com.shadowuro.remindrop

import android.Manifest
import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle
import android.content.pm.PackageManager
import com.shadowuro.remindrop.ui.MainActivity
import java.util.Collections
import java.util.WeakHashMap

class RemindropApplication : Application() {
    private val promptedActivities = Collections.newSetFromMap(
        WeakHashMap<Activity, Boolean>()
    )

    override fun onCreate() {
        super.onCreate()

        registerActivityLifecycleCallbacks(
            object : ActivityLifecycleCallbacks {
                override fun onActivityResumed(activity: Activity) {
                    if (activity !is MainActivity) return
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
                    if (activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return
                    if (!promptedActivities.add(activity)) return

                    activity.requestPermissions(
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        NOTIFICATION_PERMISSION_REQUEST,
                    )
                }

                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
                override fun onActivityStarted(activity: Activity) = Unit
                override fun onActivityPaused(activity: Activity) = Unit
                override fun onActivityStopped(activity: Activity) = Unit
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

                override fun onActivityDestroyed(activity: Activity) {
                    promptedActivities.remove(activity)
                }
            }
        )
    }

    private companion object {
        const val NOTIFICATION_PERMISSION_REQUEST = 1001
    }
}
