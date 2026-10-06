package com.Mechanic.Workshop.data.sync

import android.content.Context
import android.util.Log
import androidx.work.*
import com.Mechanic.Workshop.data.local.AppDatabase
import com.Mechanic.Workshop.data.local.entity.OutboxEntity
import com.Mechanic.Workshop.data.remote.Config
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.RequestBody.Companion.toRequestBody

class ReportSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "ReportSyncWorker"
        private const val WORK_NAME = "ReportSyncWork"

        /**
         * trigger فوری (بعد از ثبت گزارش)
         */
        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<ReportSyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30, TimeUnit.SECONDS
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    request
                )
        }

        /**
         * trigger دوره‌ای (هر ۱۵ دقیقه)
         */
        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReportSyncWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    "${WORK_NAME}_Periodic",
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
        }
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "=== doWork started ===")

        val db = AppDatabase.getInstance(applicationContext)
        val pending = db.outboxDao().getPending()

        Log.d(TAG, "Pending items: ${pending.size}")

        if (pending.isEmpty()) {
            return Result.success()
        }

        var hasFailure = false

        for (item in pending) {
            val success = sendToServer(item)

            if (success) {
                // موفق → done
                db.outboxDao().update(item.copy(status = "done"))

                // آپدیت گزارش به synced
                if (item.action == "addTaskLog") {
                    val reportId = extractReportId(item)
                    if (reportId != null) {
                        markReportSynced(db, reportId)
                    }
                }

                Log.d(TAG, "✅ ${item.action} - ${item.targetId} sent")
            } else {
                // ناموفق → retry
                hasFailure = true
                db.outboxDao().update(
                    item.copy(
                        status = "pending",
                        retryCount = item.retryCount + 1,
                        lastAttemptAt = System.currentTimeMillis()
                    )
                )
                Log.e(TAG, "❌ ${item.action} - ${item.targetId} failed")
            }
        }

        // پاک کردن doneها
        db.outboxDao().clearDone()

        return if (hasFailure) Result.retry() else Result.success()
    }

    /**
     * ارسال یه آیتم به سرور
     */
    private suspend fun sendToServer(item: OutboxEntity): Boolean {
        return try {
            val (url, body) = when (item.action) {
                "deleteTask" -> {
                    val taskId = JSONObject(item.payload).optString("taskId", "")
                    "${Config.BASE_URL}?action=delete&id=$taskId" to null
                }
                "deleteTaskLog" -> {
                    val logId = JSONObject(item.payload).optString("logId", "")
                    "${Config.BASE_URL}?action=deleteTaskLog&logId=$logId" to null
                }
                else -> {
                    val postUrl = when (item.action) {
                        "addTaskLog" -> "${Config.BASE_URL}?action=addTaskLog"
                        "updateTaskLog" -> "${Config.BASE_URL}?action=updateTaskLog"
                        "updateTaskStatus" -> "${Config.BASE_URL}?action=updateTaskStatus"
                        "updateEvaluation" -> "${Config.BASE_URL}?action=updateEvaluation"
                        "updateTask" -> "${Config.BASE_URL}?action=update"
                        else -> return false
                    }
                    postUrl to item.payload.toRequestBody("application/json; charset=utf-8".toMediaType())
                }
            }

            val client = OkHttpClient()
            val request = if (body == null) {
                Request.Builder().url(url).get().build()
            } else {
                Request.Builder().url(url).post(body).build()
            }

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            Log.d(TAG, "Response: ${response.code} - $responseBody")

            (response.isSuccessful && responseBody.contains("success")) || response.code == 409

        } catch (e: IOException) {
            Log.e(TAG, "Network error: ${e.message}")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}")
            false
        }
    }

    private fun extractReportId(item: OutboxEntity): String? {
        return try {
            val json = JSONObject(item.payload)
            when (item.action) {
                "addTaskLog" -> json.optString("id", null)
                "updateEvaluation" -> json.optString("logId", null)
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun markReportSynced(
        db: AppDatabase,
        reportId: String
    ) {
        val report = db.reportDao().getById(reportId) ?: return
        db.reportDao().update(
            report.copy(
                syncStatus = "synced",
                lastSyncedAt = System.currentTimeMillis()
            )
        )
    }
}