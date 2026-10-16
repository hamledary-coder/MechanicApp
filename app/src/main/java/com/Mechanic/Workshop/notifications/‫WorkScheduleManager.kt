package com.Mechanic.Workshop.notifications

import android.content.Context
import com.Mechanic.Workshop.data.remote.Config
import java.util.Calendar

object WorkScheduleManager {

    // ========== بازه‌های کاری ==========
    // صبح: ۶ تا ۸
    private const val MORNING_START = 6
    private const val MORNING_END = 8

    // ظهر تا شب: ۱۲ تا ۲۴
    private const val AFTERNOON_START = 12
    private const val AFTERNOON_END = 24

    // ========== آیا الان باید Polling انجام بشه؟ ==========
    fun shouldPollNow(context: Context): Boolean {
        // ۱. چک کن هفته کاری هست یا استراحت
        if (!isActiveWeek(context)) {
            return false
        }

        // ۲. چک کن تو یکی از بازه‌های کاری هستیم
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

        val isMorning = currentHour in MORNING_START until MORNING_END
        val isAfternoon = currentHour in AFTERNOON_START until AFTERNOON_END

        return isMorning || isAfternoon
    }

    // ========== آیا هفته کاریه؟ ==========
    private fun isActiveWeek(context: Context): Boolean {
        val sharedPref = context.getSharedPreferences(Config.PrefKeys.USER_PREFS, Context.MODE_PRIVATE)
        return sharedPref.getBoolean("is_active_week", true)
    }

    // ========== تنظیم وضعیت هفته ==========
    fun setActiveWeek(context: Context, isActive: Boolean) {
        val sharedPref = context.getSharedPreferences(Config.PrefKeys.USER_PREFS, Context.MODE_PRIVATE)
        sharedPref.edit().putBoolean("is_active_week", isActive).apply()
    }

    // ========== زمان‌بندی بعدی Polling ==========
    fun getNextPollingDelayMinutes(context: Context): Long {
        if (!isActiveWeek(context)) {
            // هفته استراحت: ۲۴ ساعت صبر کن
            return 60L * 24
        }

        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        // اگه تو بازه کاری هستیم → ۱۵ دقیقه
        if (isInWorkHours(currentHour)) {
            return 15L
        }

        // خارج از بازه کاری → تا شروع بازه بعدی صبر کن
        val minutesUntilNext = calculateMinutesUntilNextWorkHour(currentHour, currentMinute)
        return minutesUntilNext
    }

    private fun isInWorkHours(hour: Int): Boolean {
        val isMorning = hour in MORNING_START until MORNING_END
        val isAfternoon = hour in AFTERNOON_START until AFTERNOON_END
        return isMorning || isAfternoon
    }

    private fun calculateMinutesUntilNextWorkHour(currentHour: Int, currentMinute: Int): Long {
        // محاسبه دقیقه تا شروع بازه بعدی
        val nextStartHour = when {
            currentHour < MORNING_START -> MORNING_START
            currentHour < AFTERNOON_START -> AFTERNOON_START
            else -> MORNING_START + 24  // فردا صبح
        }

        val hoursUntil = nextStartHour - currentHour
        val minutesUntil = hoursUntil * 60 - currentMinute

        return minutesUntil.toLong().coerceAtLeast(15L)
    }
}