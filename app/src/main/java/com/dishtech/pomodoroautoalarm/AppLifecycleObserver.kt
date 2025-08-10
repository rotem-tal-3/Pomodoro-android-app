package com.dishtech.pomodoroautoalarm

import android.app.Activity
import android.app.Application
import android.os.Bundle

/**
 * AppLifecycleObserver is a class that implements the Application.ActivityLifecycleCallbacks
 * interface and used to track the app's life cycle.
 */
class AppLifecycleObserver(application: Application) : Application.ActivityLifecycleCallbacks {

    private var isAppInBackground = false

    init {
        application.registerActivityLifecycleCallbacks(this)
    }

    /**
     * Returns true if the app is in the background, false otherwise.
     */
    fun isAppInForeground(): Boolean {
        return !isAppInBackground
    }

    override fun onActivityResumed(activity: Activity) {
        isAppInBackground = false
    }

    override fun onActivityPaused(activity: Activity) {
        isAppInBackground = true
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityDestroyed(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityStopped(activity: Activity) {}
}