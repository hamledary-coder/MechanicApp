package com.Mechanic.Workshop.utils

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {

    private val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun formatMessageTime(dateStr: String): String {
        return try {
            val date = inputFormat.parse(dateStr) ?: Date()
            timeFormat.format(date)
        } catch (e: Exception) {
            dateStr
        }
    }

    // ========== توابع تبدیل تاریخ ==========

    /**
     * تبدیل تاریخ میلادی به شمسی (بدون کتابخانه)
     * دقت: برای سال‌های ۱۳۰۰ تا ۱۴۵۰ دقیق است
     * @param calendar Calendar (اختیاری، پیش‌فرض: امروز)
     * @return تاریخ شمسی به صورت "yyyy/MM/dd" با اعداد انگلیسی
     */
    fun gregorianToShamsi(calendar: Calendar = Calendar.getInstance()): String {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        var shamsiYear = year - 621
        var shamsiMonth = 0
        var shamsiDay = day

        val shamsiMonths = intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)

        val daysInGregorian = when (month) {
            1 -> day
            2 -> 31 + day
            3 -> 59 + day
            4 -> 90 + day
            5 -> 120 + day
            6 -> 151 + day
            7 -> 181 + day
            8 -> 212 + day
            9 -> 243 + day
            10 -> 273 + day
            11 -> 304 + day
            12 -> 334 + day
            else -> day
        }

        val isLeap = (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
        val daysOffset = if (isLeap) 80 else 79

        var totalShamsiDays = daysInGregorian - daysOffset

        if (totalShamsiDays <= 0) {
            shamsiYear--
            totalShamsiDays += 365 + if (isLeap) 1 else 0
        }

        for (i in 0 until 12) {
            if (totalShamsiDays <= shamsiMonths[i]) {
                shamsiMonth = i + 1
                shamsiDay = totalShamsiDays
                break
            }
            totalShamsiDays -= shamsiMonths[i]
        }

        return String.format(Locale.ENGLISH, "%d/%02d/%02d", shamsiYear, shamsiMonth, shamsiDay)
    }

    /**
     * دریافت نام روز هفته به فارسی
     * @param calendar Calendar (اختیاری، پیش‌فرض: امروز)
     * @return نام روز هفته به فارسی
     */
    fun getDayName(calendar: Calendar = Calendar.getInstance()): String {
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY -> "شنبه"
            Calendar.SUNDAY -> "یک‌شنبه"
            Calendar.MONDAY -> "دوشنبه"
            Calendar.TUESDAY -> "سه‌شنبه"
            Calendar.WEDNESDAY -> "چهارشنبه"
            Calendar.THURSDAY -> "پنج‌شنبه"
            Calendar.FRIDAY -> "جمعه"
            else -> ""
        }
    }

    /**
     * تبدیل اعداد انگلیسی به فارسی برای نمایش
     */
    fun toPersianNumber(number: String): String {
        val map = mapOf(
            '0' to '۰', '1' to '۱', '2' to '۲', '3' to '۳',
            '4' to '۴', '5' to '۵', '6' to '۶', '7' to '۷',
            '8' to '۸', '9' to '۹'
        )
        return number.map { map[it] ?: it }.joinToString("")
    }

    /**
     * تبدیل اعداد فارسی به انگلیسی
     */
    fun toEnglishNumber(number: String): String {
        val map = mapOf(
            '۰' to '0', '۱' to '1', '۲' to '2', '۳' to '3',
            '۴' to '4', '۵' to '5', '۶' to '6', '۷' to '7',
            '۸' to '8', '۹' to '9'
        )
        return number.map { map[it] ?: it }.joinToString("")
    }
}