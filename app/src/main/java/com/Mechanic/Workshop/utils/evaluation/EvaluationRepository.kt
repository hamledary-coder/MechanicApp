package com.Mechanic.Workshop.utils.evaluation

import TaskModel
import android.content.Context
import android.util.Log
import com.Mechanic.Workshop.data.model.TaskLogModel
import com.Mechanic.Workshop.data.remote.Config
import com.Mechanic.Workshop.utils.VolleySingleton
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import org.json.JSONArray
import org.json.JSONObject

/**
 * مخزن داده‌های ماژول ارزیابی
 * مسئول دریافت داده از سرور و تبدیل به مدل‌های داخلی
 */
class EvaluationRepository(private val context: Context) {

    private val TAG = "EvaluationRepository"

    /**
     * دریافت همه گزارش‌های کاری (بدون فیلتر تاریخ)
     */
    fun getAllTaskLogs(callback: (List<TaskLogModel>) -> Unit) {
        val url = "${Config.BASE_URL}?action=getAllTaskLogs"

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val jsonArray = JSONArray(response)
                    val logs = mutableListOf<TaskLogModel>()

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val log = parseTaskLog(obj)
                        logs.add(log)
                    }

                    callback(logs)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing: ${e.message}")
                    callback(emptyList())
                }
            },
            { error ->
                Log.e(TAG, "Network error: ${error.message}")
                callback(emptyList())
            }
        )

        VolleySingleton.getInstance(context).add(request)
    }

    fun getAllTasks(callback: (List<TaskModel>) -> Unit) {
        val url = "${Config.BASE_URL}?action=getTasks"

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val jsonArray = JSONArray(response)
                    val tasks = mutableListOf<TaskModel>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val task = TaskModel(
                            id = obj.getString("id"),
                            createDate = obj.getString("createDate"),
                            title = obj.getString("title"),
                            description = obj.getString("description"),
                            creator = obj.getString("creator"),
                            status = obj.getString("status"),
                            assignedTo = obj.optString("assignedTo", ""),
                            responsible = obj.optString("responsible", ""),
                            pendingInvites = obj.optString("pendingInvites", ""),
                            unit = obj.optString("unit", ""),
                            priority = obj.optString("priority", ""),
                            sub_unit = obj.optString("sub_unit", ""),
                            declaration_method = obj.optString("declaration_method", ""),
                            requester = obj.optString("requester", ""),
                            request_date = obj.optString("request_date", ""),
                            urgency = obj.optString("urgency", ""),
                            initial_review = obj.optString("initial_review", ""),
                            system_request_number = obj.optString("system_request_number", ""),
                            referredBy = obj.optString("referred_by", "")
                        )
                        tasks.add(task)
                    }
                    callback(tasks)
                } catch (e: Exception) {
                    callback(emptyList())
                }
            },
            { error ->
                callback(emptyList())
            }
        )
        VolleySingleton.getInstance(context).add(request)
    }

    fun getTasks(taskIds: List<String>, callback: (List<TaskModel>) -> Unit) {
        if (taskIds.isEmpty()) {
            callback(emptyList())
            return
        }

        val taskIdParam = taskIds.joinToString(",")
        val url = "${Config.BASE_URL}?action=getTasks&taskIds=$taskIdParam"

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val jsonArray = JSONArray(response)
                    val tasks = mutableListOf<TaskModel>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val task = TaskModel(
                            id = obj.getString("id"),
                            createDate = obj.getString("createDate"),
                            title = obj.getString("title"),
                            description = obj.getString("description"),
                            creator = obj.getString("creator"),
                            status = obj.getString("status"),
                            assignedTo = obj.optString("assignedTo", ""),
                            responsible = obj.optString("responsible", ""),
                            pendingInvites = obj.optString("pendingInvites", ""),
                            unit = obj.optString("unit", ""),
                            priority = obj.optString("priority", ""),
                            sub_unit = obj.optString("sub_unit", ""),
                            declaration_method = obj.optString("declaration_method", ""),
                            requester = obj.optString("requester", ""),
                            request_date = obj.optString("request_date", ""),
                            urgency = obj.optString("urgency", ""),
                            initial_review = obj.optString("initial_review", ""),
                            system_request_number = obj.optString("system_request_number", ""),
                            referredBy = obj.optString("referred_by", "")
                        )
                        tasks.add(task)
                    }
                    callback(tasks)
                } catch (e: Exception) {
                    Log.e("EvaluationRepo", "Error parsing tasks: ${e.message}")
                    callback(emptyList())
                }
            },
            { error ->
                Log.e("EvaluationRepo", "Network error: ${error.message}")
                callback(emptyList())
            }
        )
        VolleySingleton.getInstance(context).add(request)
    }

    /**
     * دریافت گزارش‌های یک کار خاص
     */
    fun getTaskLogs(taskId: String, callback: (List<TaskLogModel>) -> Unit) {
        val url = "${Config.BASE_URL}?action=getTaskLogs&taskId=$taskId"

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                Log.d("TaskLogRepo1", "response: $response")
                try {
                    val jsonArray = JSONArray(response)
                    val logs = mutableListOf<TaskLogModel>()

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val log = parseTaskLog(obj)
                        logs.add(log)
                    }

                    callback(logs)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing task logs: ${e.message}")
                    callback(emptyList())
                }
            },
            { error ->
                Log.e(TAG, "Network error: ${error.message}")
                callback(emptyList())
            }
        )

        VolleySingleton.getInstance(context).add(request)
    }

    /**
     * تبدیل JSONObject به TaskLogModel
     */
    private fun parseTaskLog(obj: JSONObject): TaskLogModel {
        val seenBy = mutableListOf<String>()
        val seenByStr = obj.optString("seen_by", "[]")
        if (seenByStr.isNotEmpty() && seenByStr != "[]") {
            try {
                val seenByArray = JSONArray(seenByStr)
                for (j in 0 until seenByArray.length()) {
                    seenBy.add(seenByArray.getString(j))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing seen_by: ${e.message}")
            }
        }

        return TaskLogModel(
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
            workType = obj.optString("work_type", "1"),
            seenBy = seenBy,
            physicalDifficulty = obj.optInt("physical_difficulty", 0),
            technicalComplexity = obj.optInt("technical_complexity", 0)
        )
    }
}