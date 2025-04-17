

import android.app.Activity
import java.lang.ref.WeakReference

object ActivityProvider {
    private var currentActivityRef: WeakReference<Activity>? = null

    fun setCurrentActivity(activity: Activity) {
        currentActivityRef = WeakReference(activity)
    }

    fun getCurrentActivity(): Activity? {
        return currentActivityRef?.get()
    }

    fun clearReference() {
        currentActivityRef = null
    }
}