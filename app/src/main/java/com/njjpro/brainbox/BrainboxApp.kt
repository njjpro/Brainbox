package com.njjpro.brainbox

import android.app.Application
import com.njjpro.brainbox.notify.Notifications

class BrainboxApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifications.ensureChannels(this)
    }
}
