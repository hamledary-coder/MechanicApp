package com.Mechanic.Workshop.ui.task.repository

import android.content.Context
import android.util.Log
import com.Mechanic.Workshop.data.model.TaskLogModel
import com.Mechanic.Workshop.data.remote.Config
import com.Mechanic.Workshop.utils.VolleySingleton
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class TaskLogRepository(private val context: Context) {

    // 1. دریافت لیست گزارش‌ها (Volley)
    fun getTaskLogs(taskId: String, onSuccess: (List<TaskLogModel>) -> Unit, onError: (String) -> Unit) {
        val timestamp = System.currentTimeMillis()
        val sharedPref = context.getSharedPreferences(Config.PrefKeys.USER_PREFS, Context.MODE_PRIVATE)
        val currentUserId = sharedPref.getString(Config.PrefKeys.USER_ROW_ID, "") ?: ""
        val url = "${Config.BASE_URL}?action=getTaskLogs&taskId=$taskId&_=$timestamp&userId=$currentUserId"

        val request = object : StringRequest(
            Request.Method.GET, url,
            { response ->

                try {
                    val jsonArray = JSONArray(response)
                    val logs = mutableListOf<TaskLogModel>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)

                        // ✅ خواندن seen_by
                        val seenBy = mutableListOf<String>()
                        val seenByStr = obj.optString("seen_by", "[]")
                        if (seenByStr.isNotEmpty() && seenByStr != "[]") {
                            try {
                                val seenByArray = JSONArray(seenByStr)
                                for (j in 0 until seenByArray.length()) {
                                    seenBy.add(seenByArray.getString(j))
                                }
                            } catch (e: Exception) {
                                Log.e("TaskLogRepo", "Error parsing seen_by: ${e.message}")
                            }
                        }

                        logs.add(TaskLogModel(
                            id = obj.getString("id"),
                            taskId = obj.getString("task_id"),
                            userId = obj.getString("user_id"),
                            userName = obj.optString("user_name", ""),
                            date = obj.optString("date", ""),
                            startTime = obj.optString("start_time", ""),
                            endTime = obj.optString("end_time", ""),
                            actionDescription = obj.optString("action_description", ""),
                            assignedUsers = obj.optString("assigned_users", ""),
                            newStatus = obj.optString("new_status", ""),
                            attachments = "",
                            notes = obj.optString("notes", ""),
                            duration = "",
                            comments = obj.optString("comments", "[]"),
                            heatLevel = obj.optInt("heat_level", 30),
                            pollutionLevel = obj.optInt("pollution_level", 0),
                            workType = obj.optString("work_type", "fixed_equipment"),
                            seenBy = seenBy  // ← اضافه شد
                        ))
                    }
                    onSuccess(logs)
                } catch (e: Exception) {
                    onError("خطا در پردازش داده: ${e.message}")
                }
            },
            { error ->
                onError("خطا در اتصال به شبکه: ${error.message}")
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                return hashMapOf(
                    "Cache-Control" to "no-cache, no-store, must-revalidate",
                    "Pragma" to "no-cache",
                    "Expires" to "0"
                )
            }
        }

        VolleySingleton.getInstance(context).add(request)
    }

    // 2. ثبت گزارش جدید (OkHttp)
    fun addTaskLog(log: TaskLogModel, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val client = OkHttpClient()
        val url = "${Config.BASE_URL}?action=addTaskLog"

        val jsonObject = JSONObject().apply {
            put("taskId", log.taskId)
            put("userId", log.userId)
            put("userName", log.userName)
            put("date", log.date)
            put("startTime", log.startTime)
            put("endTime", log.endTime)
            put("actionDescription", log.actionDescription)
            put("assignedUsers", log.assignedUsers)
            put("newStatus", log.newStatus)
            put("notes", log.notes)
            put("heatLevel",log.heatLevel)
            put("pollutionLevel", log.pollutionLevel)
            put("workType", log.workType)
        }

        val jsonString = jsonObject.toString()
        Log.d("TaskLogRepo", "OkHttp Sending: $jsonString")

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = jsonString.toRequestBody(mediaType)

        val request = okhttp3.Request.Builder()
            .url(url)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("TaskLogRepo", "OkHttp Error: ${e.message}")
                onError(e.message ?: "Network error")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string() ?: ""
                Log.d("TaskLogRepo", "OkHttp Response: $responseBody")
                if (response.isSuccessful) {
                    try {
                        val jsonResponse = JSONObject(responseBody)
                        if (jsonResponse.optString("status") == "success") {
                            onSuccess()
                        } else {
                            onError(jsonResponse.optString("message", "Unknown error"))
                        }
                    } catch (e: Exception) {
                        onError("Parse error: ${e.message}")
                    }
                } else {
                    onError("HTTP ${response.code}: $responseBody")
                }
            }
        })
    }

    // 3. ویرایش گزارش (Volley)
    fun updateTaskLog(log: TaskLogModel, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val url = "${Config.BASE_URL}?action=updateTaskLog"
        val jsonObject = JSONObject().apply {
            put("logId", log.id)
            put("date", log.date)
            put("startTime", log.startTime)
            put("endTime", log.endTime)
            put("actionDescription", log.actionDescription)
            put("assignedUsers", log.assignedUsers)
            put("newStatus", log.newStatus)
            put("notes", log.notes)
            put("heatLevel",log.heatLevel)
            put("pollutionLevel", log.pollutionLevel)
            put("workType", log.workType)
        }

        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonObject,
            { response ->
                if (response.optString("status") == "success") {
                    onSuccess()
                } else {
                    onError(response.optString("message", "خطا در ویرایش گزارش"))
                }
            },
            { error ->
                onError("خطا در اتصال به شبکه: ${error.message}")
            }
        )

        VolleySingleton.getInstance(context).add(request)
    }

    // 4. حذف گزارش (Volley)
    fun deleteTaskLog(logId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val url = "${Config.BASE_URL}?action=deleteTaskLog&logId=$logId"
        Log.d("DELETE", "URL: $url")  // ← لاگ

        val request = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                Log.d("DELETE", "Response: $response")  // ← لاگ
                if (response.optString("status") == "success") {
                    onSuccess()
                } else {
                    onError(response.optString("message", "خطا در حذف گزارش"))
                }
            },
            { error ->
                Log.e("DELETE", "Error: ${error.message}")  // ← لاگ
                onError("خطا در اتصال به شبکه: ${error.message}")
            }
        )
        VolleySingleton.getInstance(context).add(request)
    }
}