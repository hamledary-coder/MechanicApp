package com.Mechanic.Workshop.utils

import TaskModel
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.Mechanic.Workshop.data.remote.Config
import org.json.JSONArray
import org.json.JSONObject

@SuppressLint("StaticFieldLeak")
object UserCache {

    private var userMap = mutableMapOf<String, String>()
    private var prefs: SharedPreferences? = null
    @SuppressLint("StaticFieldLeak")
    private var context: Context? = null
    private var isAllUsersLoaded = false
    private val pendingCallbacks = mutableListOf<() -> Unit>()

    // مقداردهی اولیه (در LoginActivity صدا زده می‌شود)
    fun init(appContext: Context) {
        context = appContext.applicationContext
        prefs = context?.getSharedPreferences("user_cache", Context.MODE_PRIVATE)
        loadFromPrefs()
    }

    // بارگذاری از SharedPreferences
    private fun loadFromPrefs() {
        prefs?.all?.forEach { (key, value) ->
            if (value is String && key != "all_users_loaded") {
                userMap[key] = value
            }
        }
        isAllUsersLoaded = prefs?.getBoolean("all_users_loaded", false) ?: false
    }

    // ذخیره در SharedPreferences
    private fun saveToPrefs() {
        prefs?.edit()?.apply {
            userMap.forEach { (key, value) ->
                putString(key, value)
            }
            putBoolean("all_users_loaded", isAllUsersLoaded)
            apply()
        }
    }

    // گرفتن نام کاربر (همزمان - برای استفاده در Adapter)
    fun getName(userId: String): String {
        if (userId.isEmpty() || userId == "0" || userId == "null") {
            return "نامشخص"
        }
        return userMap[userId] ?: "کاربر $userId"
    }

    // گرفتن نام کاربر با callback (برای بارگذاری تکی)
    fun getNameAsync(userId: String, callback: (String) -> Unit) {
        if (userId.isEmpty() || userId == "0" || userId == "null") {
            callback("نامشخص")
            return
        }

        // اگر از قبل داریم
        userMap[userId]?.let {
            callback(it)
            return
        }

        // درخواست از سرور
        fetchSingleUser(userId, callback)
    }

    // بارگذاری همه کاربران از سرور (با استفاده از getEmployees)
    fun loadAllUsers(onComplete: (() -> Unit)? = null) {
        // اگر قبلاً بارگذاری شده
        if (isAllUsersLoaded && userMap.isNotEmpty()) {
            onComplete?.invoke()
            return
        }

        // اگر در حال بارگذاری است
        if (pendingCallbacks.isNotEmpty()) {
            onComplete?.let { pendingCallbacks.add(it) }
            return
        }

        onComplete?.let { pendingCallbacks.add(it) }

        // استفاده از getEmployees به جای getAllUsers
        val url = "${Config.BASE_URL}?action=getEmployees"

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val jsonArray = JSONArray(response)
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        // فیلدها بر اساس پاسخ getEmployees
                        val id = obj.optString("rowId", "")      // rowId همان id کاربر است
                        var name = obj.optString("name", "")     // name همان fullname است
                        val personnelId = obj.optString("personnelId", "")

                        // اگر name خالی بود، از personnelId استفاده کن
                        if (name.isEmpty()) name = obj.optString("fullname", "")
                        if (name.isEmpty()) name = personnelId

                        if (name.isNotEmpty() && id.isNotEmpty() && id != "0") {
                            userMap[id] = name
                            // همچنین با personnelId هم ذخیره کن
                            if (personnelId.isNotEmpty() && personnelId != "0") {
                                userMap[personnelId] = name
                            }
                        }
                    }
                    isAllUsersLoaded = true
                    saveToPrefs()

                    pendingCallbacks.forEach { it.invoke() }
                    pendingCallbacks.clear()

                } catch (e: Exception) {
                    e.printStackTrace()
                    pendingCallbacks.forEach { it.invoke() }
                    pendingCallbacks.clear()
                }
            },
            { error ->
                error.printStackTrace()
                pendingCallbacks.forEach { it.invoke() }
                pendingCallbacks.clear()
            }
        )

        Volley.newRequestQueue(context).add(request)
    }

    // بارگذاری تکی یک کاربر (با استفاده از getEmployee)
    private fun fetchSingleUser(userId: String, callback: (String) -> Unit) {
        val url = "${Config.BASE_URL}?action=getEmployee&personnelId=$userId"

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    var name = json.optString("name", "")
                    if (name.isEmpty()) name = json.optString("fullname", "")

                    val finalName = if (name.isNotEmpty()) name else userId
                    userMap[userId] = finalName
                    saveToPrefs()
                    callback(finalName)
                } catch (e: Exception) {
                    callback(userId)
                }
            },
            { error ->
                callback(userId)
            }
        )

        Volley.newRequestQueue(context).add(request)
    }

    // پیش‌بارگذاری از روی لیست کار‌ها (برای Fragment و Activity)
    fun preloadFromTasks(tasks: List<TaskModel>) {
        val userIds = mutableSetOf<String>()
        tasks.forEach { task ->
            if (task.creator.isNotEmpty() && task.creator != "0") userIds.add(task.creator)
            if (task.responsible.isNotEmpty() && task.responsible != "0") userIds.add(task.responsible)
            task.assignedTo.split(",").forEach {
                val id = it.trim()
                if (id.isNotEmpty() && id != "0") userIds.add(id)
            }
        }

        // فقط اگر کاربر جدیدی وجود دارد
        val newUserIds = userIds.filter { !userMap.containsKey(it) }
        if (newUserIds.isNotEmpty()) {
            loadAllUsers()
        }
    }

    // اضافه کردن یک کاربر به کش (برای بعد از لاگین)
    fun addUser(userId: String, name: String) {
        if (userId.isNotEmpty() && userId != "0" && name.isNotEmpty()) {
            userMap[userId] = name
            saveToPrefs()
        }
    }

    // پاک کردن کش (برای لاگ اوت)
    fun clear() {
        userMap.clear()
        isAllUsersLoaded = false
        prefs?.edit()?.clear()?.apply()
    }

    // متدهای دیباگ
    fun getSize(): Int = userMap.size

    fun printCache() {
        android.util.Log.d("UserCache", "Cache size: ${userMap.size}")
        userMap.forEach { (id, name) ->
            android.util.Log.d("UserCache", "ID: $id -> Name: $name")
        }
    }

    fun hasUser(userId: String): Boolean = userMap.containsKey(userId)
}