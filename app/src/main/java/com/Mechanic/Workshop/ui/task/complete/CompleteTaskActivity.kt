package com.Mechanic.Workshop.ui.task.complete

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.Mechanic.Workshop.R
import com.Mechanic.Workshop.data.model.TaskLogModel
import com.Mechanic.Workshop.data.remote.Config
import com.Mechanic.Workshop.ui.task.repository.TaskLogRepository
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.Mechanic.Workshop.utils.VolleySingleton
import org.json.JSONObject

class CompleteTaskActivity : AppCompatActivity() {

    private lateinit var taskLogRepository: TaskLogRepository
    private var taskId: String = ""
    private var taskTitle: String = ""
    private lateinit var tvTotalDuration: TextView
    private lateinit var tvAvgHeat: TextView
    private lateinit var tvAvgPollution: TextView
    private lateinit var tableContent: LinearLayout
    private lateinit var btnConfirmComplete: Button
    private lateinit var btnBack: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_complete_task)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "اتمام کار"

        taskId = intent.getStringExtra("TASK_ID") ?: ""
        taskTitle = intent.getStringExtra("TASK_TITLE") ?: ""

        taskLogRepository = TaskLogRepository(this)

        tvTotalDuration = findViewById(R.id.tvTotalDuration)
        tvAvgHeat = findViewById(R.id.tvAvgHeat)
        tvAvgPollution = findViewById(R.id.tvAvgPollution)
        tableContent = findViewById(R.id.tableContent)
        btnConfirmComplete = findViewById(R.id.btnConfirmComplete)
        btnBack = findViewById(R.id.btnBack)

        loadTaskLogs()

        btnConfirmComplete.setOnClickListener {
            completeTask()
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun loadTaskLogs() {
        taskLogRepository.getTaskLogs(
            taskId = taskId,
            onSuccess = { logs ->
                if (logs.isNotEmpty()) {
                    calculateAndDisplay(logs)
                } else {
                    Toast.makeText(this, "گزارشی برای این کار وجود ندارد", Toast.LENGTH_SHORT).show()
                    finish()
                }
            },
            onError = { message ->
                Toast.makeText(this, "خطا در دریافت گزارش‌ها: $message", Toast.LENGTH_SHORT).show()
                finish()
            }
        )
    }

    private fun calculateAndDisplay(logs: List<TaskLogModel>) {
        var totalPersonHours = 0.0
        var totalWeightedHeat = 0.0
        var totalWeightedPollution = 0.0
        val uniqueWorkTypes = mutableSetOf<String>()

        val userStats = mutableMapOf<String, UserStats>()

        for (log in logs) {
            val duration = calculateDuration(log.startTime, log.endTime)

            // جمع‌آوری انواع کار
            if (log.workType.isNotEmpty()) {
                uniqueWorkTypes.add(log.workType)
            }

            val userIds = log.assignedUsers.split(",").filter { it.isNotEmpty() }
            for (userId in userIds) {
                val stats = userStats.getOrPut(userId) { UserStats() }
                stats.totalDuration += duration
                stats.weightedHeat += log.heatLevel * duration
                stats.weightedPollution += log.pollutionLevel * duration
                totalPersonHours += duration
            }

            totalWeightedHeat += log.heatLevel * duration * userIds.size
            totalWeightedPollution += log.pollutionLevel * duration * userIds.size
        }

        // نمایش خلاصه کل کار
        tvTotalDuration.text = "کل زمان صرف شده: ${String.format("%.1f", totalPersonHours)} نفر-ساعت"

        if (totalPersonHours > 0) {
            val avgHeat = totalWeightedHeat / totalPersonHours
            val avgPollution = totalWeightedPollution / totalPersonHours
            tvAvgHeat.text = "میانگین وزنی گرما: ${String.format("%.1f", avgHeat)} درجه"
            tvAvgPollution.text = "میانگین وزنی آلودگی: ${String.format("%.1f", avgPollution)} ppm"
        }

        // نمایش انواع کار (قبل از جدول عملکرد نفرات)
        displayWorkType(uniqueWorkTypes)

        // نمایش جدول نفرات
        displayUserTable(userStats)
    }

    private fun displayWorkType(workTypes: Set<String>) {
        if (workTypes.isEmpty()) return

        // ترجمه کدها به متن فارسی
        val workTypeNames = workTypes.map { code ->
            when (code) {
                "1" -> "تجهیزات ثابت"
                "2" -> "عیب‌یابی تجهیزات دوار"
                "3" -> "بررسی"
                else -> "نامشخص"
            }
        }.joinToString("، ")

        // پیدا کردن TextView موجود یا ایجاد یک TextView جدید با Context
        val workTypesContainer = findViewById<LinearLayout>(R.id.workTypesContainer)
        if (workTypesContainer != null) {
            workTypesContainer.removeAllViews()

            val tvWorkTypes = TextView(this)  // ← این درست است، this Context را می‌دهد
            tvWorkTypes.text = "نوع کار انجام شده: $workTypeNames"
            tvWorkTypes.textSize = 14f
            tvWorkTypes.setTextColor(ContextCompat.getColor(this, android.R.color.black))
            tvWorkTypes.setPadding(0, 8, 0, 0)

            workTypesContainer.addView(tvWorkTypes)
            workTypesContainer.visibility = View.VISIBLE
        }
    }

    private fun calculateDuration(startTime: String, endTime: String): Double {
        if (startTime.isEmpty() || endTime.isEmpty()) return 0.0
        try {
            val startParts = startTime.split(":")
            val endParts = endTime.split(":")
            val startHour = startParts[0].toInt()
            val startMinute = startParts[1].toInt()
            val endHour = endParts[0].toInt()
            val endMinute = endParts[1].toInt()

            var durationMinutes = (endHour * 60 + endMinute) - (startHour * 60 + startMinute)
            if (durationMinutes < 0) durationMinutes += 24 * 60
            return durationMinutes / 60.0
        } catch (e: Exception) {
            return 0.0
        }
    }

    private fun displayUserTable(userStats: MutableMap<String, UserStats>) {
        tableContent.removeAllViews()

        // ❌ هدر حذف شد

        for ((userId, stats) in userStats) {
            val userName = Config.UserCache.userMap[userId] ?: "کاربر $userId"
            val duration = stats.totalDuration
            val avgHeat = if (duration > 0) stats.weightedHeat / duration else 0.0
            val avgPollution = if (duration > 0) stats.weightedPollution / duration else 0.0

            val row = LayoutInflater.from(this).inflate(R.layout.item_user_stats_row, tableContent, false)

            val tvName = row.findViewById<TextView>(R.id.tvUserName)
            val tvDuration = row.findViewById<TextView>(R.id.tvUserDuration)
            val tvHeat = row.findViewById<TextView>(R.id.tvUserHeat)
            val tvPollution = row.findViewById<TextView>(R.id.tvUserPollution)

            tvName.text = userName
            tvDuration.text = String.format("%.1f", duration)
            tvHeat.text = String.format("%.1f / %.1f", avgHeat, avgPollution)
            tvPollution.visibility = View.GONE

            tableContent.addView(row)
        }
    }

    private fun completeTask() {
        val url = "${Config.BASE_URL}?action=updateTaskStatus"
        val jsonObject = JSONObject().apply {
            put("taskId", taskId)
            put("status", "4")
        }

        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonObject,
            { response ->
                if (response.optString("status") == "success") {
                    Toast.makeText(this, "کار با موفقیت به اتمام رسید", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "خطا در اتمام کار", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "خطا: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
        VolleySingleton.getInstance(this).add(request)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    data class UserStats(
        var totalDuration: Double = 0.0,
        var weightedHeat: Double = 0.0,
        var weightedPollution: Double = 0.0
    )
}