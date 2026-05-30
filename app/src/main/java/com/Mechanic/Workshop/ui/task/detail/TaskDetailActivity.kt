package com.Mechanic.Workshop.ui.task.detail

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.Mechanic.Workshop.R
import com.Mechanic.Workshop.data.model.TaskLogModel
import com.Mechanic.Workshop.ui.task.log.AddLogActivity
import com.Mechanic.Workshop.ui.task.repository.TaskLogRepository
import TaskModel
import com.Mechanic.Workshop.ui.task.complete.CompleteTaskActivity
import com.Mechanic.Workshop.utils.SeenItem
import com.Mechanic.Workshop.utils.SeenManager
import com.Mechanic.Workshop.data.remote.Config

class TaskDetailActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TaskDetailAdapter
    private lateinit var task: TaskModel
    private val logsList = mutableListOf<TaskLogModel>()
    private lateinit var taskLogRepository: TaskLogRepository

    companion object {
        private const val STATUS_COMPLETED = "41"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_detail_new)
        setupToolbar()
        initViews()
        loadTaskData()
        markTaskAsSeen()
    }

    override fun onResume() {
        super.onResume()
        loadTaskLogs()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "جزئیات کار"
        }
        toolbar.navigationIcon?.setTint(Color.WHITE)
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerViewTaskDetail)
        recyclerView.layoutManager = LinearLayoutManager(this)
        taskLogRepository = TaskLogRepository(this)
    }

    private fun loadTaskData() {
        task = TaskModel(
            id = intent.getStringExtra("TASK_ID") ?: "",
            createDate = intent.getStringExtra("DATE") ?: "",
            title = intent.getStringExtra("TITLE") ?: "",
            description = intent.getStringExtra("DESC") ?: "",
            creator = intent.getStringExtra("CREATOR") ?: "",
            status = "1",
            assignedTo = intent.getStringExtra("ASSIGNED_TO") ?: "",
            responsible = intent.getStringExtra("RESPONSIBLE") ?: "",
            pendingInvites = "",
            unit = intent.getStringExtra("UNIT") ?: "",
            priority = intent.getStringExtra("PRIORITY") ?: "",
            sub_unit = intent.getStringExtra("SUB_UNIT") ?: "",
            declaration_method = intent.getStringExtra("DECLARATION_METHOD") ?: "",
            requester = intent.getStringExtra("REQUESTER") ?: "",
            request_date = intent.getStringExtra("REQUEST_DATE") ?: "",
            urgency = intent.getStringExtra("URGENCY") ?: "",
            initial_review = intent.getStringExtra("INITIAL_REVIEW") ?: "",
            system_request_number = intent.getStringExtra("SYSTEM_REQUEST_NUMBER") ?: "",
            referredBy = intent.getStringExtra("REFERRED_BY") ?: ""
        )
    }

    private fun markTaskAsSeen() {
        val sharedPref = getSharedPreferences(Config.PrefKeys.USER_PREFS, MODE_PRIVATE)
        val currentUserId = sharedPref.getString(Config.PrefKeys.USER_ROW_ID, "") ?: ""
        SeenManager.markAsSeen(this, currentUserId, listOf(SeenItem("TASK", task.id)))
    }

    private fun loadTaskLogs() {
        taskLogRepository.getTaskLogs(
            taskId = task.id,
            onSuccess = { logs ->
                runOnUiThread {
                    updateLogsAndStatus(logs)
                }
            },
            onError = { message ->
                runOnUiThread {
                    Toast.makeText(this, "خطا در دریافت گزارش‌ها: $message", Toast.LENGTH_SHORT).show()
                    setupAdapter()
                }
            }
        )
    }

    private fun updateLogsAndStatus(logs: List<TaskLogModel>) {
        logsList.clear()
        logsList.addAll(logs)

        if (logs.isNotEmpty()) {
            task = task.copy(status = logs.last().newStatus)
        }

        setupAdapter()
    }

    private fun setupAdapter() {
        val hasCompletionLog = logsList.any { it.newStatus == STATUS_COMPLETED }

        adapter = TaskDetailAdapter(
            task = task,
            logs = logsList,
            onEditLogClick = { log -> startAddLogActivity(log) },
            onDeleteLogClick = { log -> deleteLog(log) },
            onAddLogClick = { startAddLogActivity() },
            onRefreshLogs = { loadTaskLogs() },
            onCompleteTaskClick = { completeTask() },
            showCompleteButton = hasCompletionLog
        )
        recyclerView.adapter = adapter
    }

    private fun startAddLogActivity(log: TaskLogModel? = null) {
        val intent = Intent(this, AddLogActivity::class.java).apply {
            if (log != null) {
                putExtra("IS_EDIT_MODE", true)
                putExtra("LOG_ID", log.id)
                putExtra("TASK_ID", log.taskId)
            } else {
                putExtra("TASK_ID", task.id)
            }
            putTaskExtras(this)
        }
        startActivity(intent)
    }

    private fun putTaskExtras(intent: Intent) {
        intent.apply {
            putExtra("TITLE", task.title)
            putExtra("DESC", task.description)
            putExtra("CREATOR", task.creator)
            putExtra("DATE", task.createDate)
            putExtra("RESPONSIBLE", task.responsible)
            putExtra("ASSIGNED_TO", task.assignedTo)
            putExtra("UNIT", task.unit)
            putExtra("PRIORITY", task.priority)
            putExtra("SUB_UNIT", task.sub_unit)
            putExtra("DECLARATION_METHOD", task.declaration_method)
            putExtra("REQUESTER", task.requester)
            putExtra("REQUEST_DATE", task.request_date)
            putExtra("INITIAL_REVIEW", task.initial_review)
            putExtra("SYSTEM_REQUEST_NUMBER", task.system_request_number)
            putExtra("URGENCY", task.urgency)
        }
    }

    private fun deleteLog(log: TaskLogModel) {
        taskLogRepository.deleteTaskLog(
            logId = log.id,
            onSuccess = {
                runOnUiThread {
                    Toast.makeText(this, "گزارش حذف شد", Toast.LENGTH_SHORT).show()
                    loadTaskLogs()
                }
            },
            onError = { message ->
                runOnUiThread {
                    Toast.makeText(this, "خطا در حذف: $message", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun completeTask() {
        startActivity(Intent(this, CompleteTaskActivity::class.java).apply {
            putExtra("TASK_ID", task.id)
            putExtra("TASK_TITLE", task.title)
            putExtra("CURRENT_STATUS", task.status)
        })
    }
}