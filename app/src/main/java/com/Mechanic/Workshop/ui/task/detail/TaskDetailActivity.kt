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
import android.util.Log
import android.view.View
import com.Mechanic.Workshop.ui.task.complete.CompleteTaskActivity
import com.Mechanic.Workshop.utils.SeenItem
import com.Mechanic.Workshop.utils.SeenManager
import com.Mechanic.Workshop.data.remote.Config
import com.Mechanic.Workshop.utils.VolleySingleton
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest

class TaskDetailActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TaskDetailAdapter
    private lateinit var task: TaskModel
    private val logsList = mutableListOf<TaskLogModel>()
    private lateinit var taskLogRepository: TaskLogRepository


    companion object {
        private const val STATUS_REQUEST_COMPLETE = "4"
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
            status = intent.getStringExtra("STATUS") ?: "1",
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

        // دریافت وضعیت واقعی تسک از سرور
        fetchTaskStatus { taskStatus ->
            task = task.copy(status = taskStatus)
            setupAdapter()
        }
    }

    private fun fetchTaskStatus(callback: (String) -> Unit) {
        val url = "${Config.BASE_URL}?action=getTask&taskId=${task.id}"
        val request = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                callback(response.optString("status", "1"))
            },
            { error ->
                Log.e("TaskDetail", "Error fetching task status: ${error.message}")
                callback(logsList.lastOrNull()?.newStatus ?: "1")
            }
        )
        VolleySingleton.getInstance(this).add(request)
    }

    private fun setupAdapter() {
        val sharedPref = getSharedPreferences(Config.PrefKeys.USER_PREFS, MODE_PRIVATE)
        val currentUserId = sharedPref.getString(Config.PrefKeys.USER_ROW_ID, "") ?: ""
        val userRole = sharedPref.getString(Config.PrefKeys.USER_ROLE, "") ?: ""

        val hasCompletionLog = logsList.any { it.newStatus == "4" || it.newStatus == "41" }

        val isResponsible = task.responsible == currentUserId
        val isArchived = task.status == "5"

        var buttonMode = "HIDDEN"
        var onButtonClick: () -> Unit = {}

        if (!isArchived) {
            when (task.status) {
                "4" -> {
                    // فقط مسئول کار
                    if (isResponsible) {
                        buttonMode = "REQUEST_COMPLETE"
                        onButtonClick = { completeTask() }
                    }
                }
                "41" -> {
                    when {
                        userRole == Config.RoleCode.SUPERVISOR -> {
                            buttonMode = "FINAL_REVIEW"  // نارنجی - اولویت با نقش سرشیفت
                            onButtonClick = { completeTask() }
                        }
                        isResponsible -> {
                            buttonMode = "REQUEST_COMPLETE"  // سبز
                            onButtonClick = { completeTask() }
                        }
                    }
                }
                else -> { // 1,2,3,22
                    // فقط مسئول کار
                    if (isResponsible) {
                        buttonMode = "ADD_LOG"
                        onButtonClick = { startAddLogActivity() }
                    }
                }
            }
        }

        adapter = TaskDetailAdapter(
            task = task,
            logs = logsList,
            onEditLogClick = { log -> startAddLogActivity(log) },
            onDeleteLogClick = { log -> deleteLog(log) },
            onAddLogClick = { startAddLogActivity() },
            onRefreshLogs = { loadTaskLogs() },
            onCompleteTaskClick = { completeTask() },
            showCompleteButton = hasCompletionLog,
            canAddLog = true,
            currentUserId = currentUserId,
            userRole = userRole,
            buttonMode = buttonMode,
            onButtonClick = onButtonClick
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