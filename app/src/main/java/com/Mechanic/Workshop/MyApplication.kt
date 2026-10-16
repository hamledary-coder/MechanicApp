package com.Mechanic.Workshop

import android.app.Application
import com.Mechanic.Workshop.notifications.NotificationPermissionManager
import com.Mechanic.Workshop.notifications.NotificationPoller

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // ساخت کانال نوتیفیکیشن
        NotificationPermissionManager.createNotificationChannel(this)  // ← مفرد

        // شروع Polling
        NotificationPoller.start(this)
    }
}