package com.Mechanic.Workshop.data.remote

object Config {
    // آدرس اسکریپت گوگل
    const val BASE_URL = "https://mechanicbb1.ir/api.php"

    object Endpoints {
        const val LOGIN = BASE_URL
        const val TASKS = "$BASE_URL?action=getTasks"
        const val CREATE_TASK = BASE_URL
        const val DELETE_TASK = BASE_URL
    }

    // ============== کدهای وضعیت (دو رقمی) ==============
    object StatusCode {
        const val NOT_ACTIONED = "1"
        const val IN_PROGRESS = "2"
        const val BLOCKED = "3"
        const val REQUEST_COMPLETE = "4"      // اضافه کن
        const val SENT_TO_SUPERVISOR = "41"   // اضافه کن
        const val ARCHIVED = "5"              // اضافه کن

        fun getText(code: String): String {
            return when (code) {
                NOT_ACTIONED -> "اقدام نشده"
                IN_PROGRESS -> "در حال انجام"
                BLOCKED -> "متوقف"
                REQUEST_COMPLETE -> "درخواست اتمام"
                SENT_TO_SUPERVISOR -> "ارسال به سرشیفت"
                ARCHIVED -> "بایگانی"
                else -> "نامشخص"
            }
        }
    }

    // ============== کدهای نقش ==============
    object RoleCode {
        const val MANAGER = "1"     // مدیر - نظارت
        const val SUPERVISOR = "2"  // سرشیفت - ارجاع کار
        const val EMPLOYEE = "3"    // کارمند - انجام کار

        fun getText(code: String): String = when (code) {
            SUPERVISOR -> "Supervisor"
            MANAGER -> "Manager"
            EMPLOYEE -> "employee"
            else -> "employee"
        }

        fun getCode(text: String): String = when (text) {
            "Supervisor" -> SUPERVISOR
            "Manager" -> MANAGER
            "employee" -> EMPLOYEE
            else -> EMPLOYEE
        }
    }

    // ============== کدهای نوع ارجاع ==============
    object ReferralType {
        const val ASSIGNEE = "1"
        const val RESPONSIBLE = "2"
    }

    // ============== کش کاربران (ردیف -> نام) ==============
    object UserCache {
        val userMap = mutableMapOf<String, String>()  // "1" -> "علی رضایی"

        fun getName(rowId: String): String {
            return userMap[rowId] ?: "کاربر $rowId"
        }
    }

    // ============== کلیدهای SharedPreferences ==============
    object PrefKeys {
        const val USER_PREFS = "UserPrefs"
        const val PERSONNEL_ID = "pId"
        const val USERNAME = "username"
        const val USER_ROLE = "userRole"
        const val USER_ROW_ID = "userRowId"  // کد ردیف کاربر در شیت Users
    }

    // ============== کدهای واحد مربوطه ==============
    object UnitCode {
        const val GAS = "1"          // گاز
        const val OPERATION = "2"    // بهره‌برداری
        const val DESALTER = "3"     // نمکزدایی
        const val COMPLEX = "4"     // مجموعه ها

        fun getText(code: String): String = when (code) {
            GAS -> "گاز"
            OPERATION -> "بهره‌برداری"
            DESALTER -> "نمکزدایی"
            COMPLEX -> "مجموعه ها"
            else -> code
        }
    }

    // ============== کدهای درجه اهمیت ==============
    object PriorityCode {
        const val EMERGENCY = "1"    // اورژانسی
        const val HIGH = "2"         // بالا
        const val LOW = "3"          // کم

        fun getText(code: String): String = when (code) {
            EMERGENCY -> "اورژانسی"
            HIGH -> "بالا"
            LOW -> "کم"
            else -> "نامشخص"
        }
    }
}