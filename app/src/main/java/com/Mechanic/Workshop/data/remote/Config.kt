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
        // وضعیت‌های اصلی
        const val UNASSIGNED = "1"      // اقدام نشده
        const val IN_PROGRESS = "2"     // در حال انجام
        const val BLOCKED = "3"         // متوقف
        const val COMPLETED = "4"       // انجام شده

        // زیروضعیت‌های در حال انجام
        const val GROUP_FORMING = "21"  // بررسی و تشکیل گروه
        const val ACTION_TAKEN = "22"   // اقدام شده

        // زیروضعیت‌های متوقف
        const val WAITING_START = "31"  // منتظر تعیین زمان شروع
        const val WAITING_OPERATOR = "32" // منتظر بهره‌بردار
        const val WAITING_PARTS = "33"  // منتظر کالا/قطعه
        const val WAITING_TEST = "34"   // منتظر تست بهره‌بردار

        // زیروضعیت‌های انجام شده
        const val FINISHED = "41"       // اتمام کار
        const val WAITING_SIGN = "42"   // منتظر امضا
        const val WAITING_REMOVE = "43" // منتظر حذف از کارتابل

        fun getText(code: String): String {
            return when (code) {
                UNASSIGNED -> "اقدام نشده"
                IN_PROGRESS -> "در حال انجام"
                GROUP_FORMING -> "بررسی و تشکیل گروه"
                ACTION_TAKEN -> "ادامه دارد"
                BLOCKED -> "متوقف"
                WAITING_START -> "منتظر تعیین زمان شروع"
                WAITING_OPERATOR -> "منتظر بهره‌بردار"
                WAITING_PARTS -> "منتظر کالا/قطعه"
                WAITING_TEST -> "منتظر تست بهره‌بردار"
                COMPLETED -> "انجام شده"
                FINISHED -> "اتمام کار"
                WAITING_SIGN -> "منتظر امضا"
                WAITING_REMOVE -> "منتظر حذف از کارتابل"
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