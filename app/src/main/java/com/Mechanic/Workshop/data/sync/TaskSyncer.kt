package com.Mechanic.Workshop.data.sync

import TaskModel
import android.content.Context
import android.util.Log
import com.Mechanic.Workshop.data.local.AppDatabase
import com.Mechanic.Workshop.data.remote.Config
import com.Mechanic.Workshop.utils.VolleySingleton
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray


/**
 * مسئول sync کارها از سرور به Room
 * فقط خواندن — چیزی به سرور نمی‌فرسته
 */
object TaskSyncer {

    private const val TAG = "TaskSyncer"

    /**
     * کارها رو از سرور می‌گیره و توی Room می‌ریزه
     * @param onComplete با (تعداد taskهای sync شده) صدا زده می‌شه
     */
    fun syncTasksFromServer(
        context: Context,
        userId: String = "",
        onComplete: ((Int) -> Unit)? = null
    ) {
        DeltaSyncer.syncDelta(context) { result ->
            Log.d(TAG, "Delta sync: ${result.tasksCount} tasks, ${result.reportsCount} reports, ${result.deletedCount} deleted")

            // ← کاربران هم sync شن
            UserSyncer.syncUsersFromServer(context) { userCount ->
                Log.d(TAG, "Synced $userCount users")
                onComplete?.invoke(result.tasksCount)
            }
        }
    }

    /**
     * تبدیل JSONObject سرور به TaskModel
     * دقیقاً همون کد fetchTasks توی WorkListFragment
     */
    private fun parseTask(obj: org.json.JSONObject): TaskModel {
        val hasUnseenReport = obj.optInt("has_unseen_report", 0) == 1

        return TaskModel(
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
            referredBy = obj.optString("referred_by", ""),
            hasUnseenReport = hasUnseenReport
        )
    }
}