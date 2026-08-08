package com.Mechanic.Workshop.ui.task.detail

import TaskModel
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
import android.util.Log
import android.view.View
import com.Mechanic.Workshop.ui.task.complete.CompleteTaskActivity
import com.Mechanic.Workshop.utils.SeenItem
import com.Mechanic.Workshop.utils.SeenManager
import com.Mechanic.Workshop.data.remote.Config
import com.Mechanic.Workshop.ui.cartable.CartableActivity
import com.Mechanic.Workshop.utils.UserCache
import com.Mechanic.Workshop.utils.VolleySingleton
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import org.json.JSONArray
import org.json.JSONObject

class TaskDetailActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TaskDetailAdapter
    private lateinit var task: TaskModel
    private val logsList = mutableListOf<TaskLogModel>()
    private lateinit var taskLogRepository: TaskLogRepository
    private lateinit var progressBar: View
    private lateinit var userRole: String

    companion object {
        private const val STATUS_IN_PROGRESS = "2"
        private const val STATUS_BLOCKED = "3"
        private const val STATUS_SENT_TO_SUPERVISOR = "41"
        private const val STATUS_ARCHIVED = "5"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_detail_new)

        if (UserCache.getSize() == 0) {
            showLoading(true)
            UserCache.loadAllUsers {
                runOnUiThread {
                    showLoading(false)
                    setupToolbar()
                    initViews()
                    loadTaskData()
                }
            }
        } else {
            setupToolbar()
            initViews()
            loadTaskData()
        }
    }

    private fun showLoading(show: Boolean) {
        if (::progressBar.isInitialized) {
            progressBar.visibility = if (show) View.VISIBLE else View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        if (::task.isInitialized) {
            fetchFullTaskFromServer()
        }
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
        progressBar = findViewById(R.id.progressBar)
    }

    private fun loadTaskData() {
        val tempTask = TaskModel(
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
        task = tempTask
        fetchFullTaskFromServer()
    }

    private fun loadTaskLogs(callback: (() -> Unit)? = null) {
        taskLogRepository.getTaskLogs(
            taskId = task.id,
            onSuccess = { logs ->
                runOnUiThread {
                    logsList.clear()
                    logsList.addAll(logs)
                    setupAdapter()
                    callback?.invoke()
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

    private fun getCurrentUserId(): String {
        val sharedPref = getSharedPreferences(Config.PrefKeys.USER_PREFS, MODE_PRIVATE)
        return sharedPref.getString(Config.PrefKeys.USER_ROW_ID, "") ?: ""
    }

    private fun fetchFullTaskFromServer() {
        val url = "${Config.BASE_URL}?action=getTask&taskId=${task.id}"
        val request = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    val seenByArray = response.optJSONArray("seenBy") ?: JSONArray()
                    val seenBy = (0 until seenByArray.length()).map { seenByArray.getString(it) }
                    val hasUnseenReport = response.optBoolean("hasUnseenReport", false)
                    val status = response.optString("status", task.status)

                    task = task.copy(
                        status = status,
                        seenBy = seenBy,
                        hasUnseenReport = hasUnseenReport
                    )

                    Log.d("TaskDetail", "Task updated from server - seenBy: $seenBy")

                    val currentUserId = getCurrentUserId()
                    val alreadySeen = task.seenBy.contains(currentUserId)

                    if (currentUserId.isNotEmpty() && !alreadySeen) {
                        SeenManager.markAsSeen(this, currentUserId, listOf(SeenItem("TASK", task.id)))
                    }

                    setupAdapter()
                    loadTaskLogs()

                } catch (e: Exception) {
                    Log.e("TaskDetail", "Error parsing task: ${e.message}")
                    setupAdapter()
                    loadTaskLogs()
                }
            },
            { error ->
                Log.e("TaskDetail", "Error fetching task: ${error.message}")
                setupAdapter()
                loadTaskLogs()
            }
        )
        VolleySingleton.getInstance(this).add(request)
    }

    private fun setupAdapter() {
        if (!::task.isInitialized) return

        val currentUserId = getCurrentUserId()
        val sharedPref = getSharedPreferences(Config.PrefKeys.USER_PREFS, MODE_PRIVATE)
        userRole = sharedPref.getString(Config.PrefKeys.USER_ROLE, "") ?: ""

        val hasCompletionLog = logsList.any { it.newStatus == STATUS_SENT_TO_SUPERVISOR }

        val isResponsible = task.responsible == currentUserId
        val isArchived = task.status == STATUS_ARCHIVED

        var buttonMode = "HIDDEN"
        var onButtonClick: () -> Unit = {}

        if (!isArchived) {
            when (task.status) {
                STATUS_SENT_TO_SUPERVISOR -> {
                    if (userRole == Config.RoleCode.SUPERVISOR) {
                        buttonMode = "FINAL_REVIEW"
                        onButtonClick = { completeTask() }
                    }
                }
                else -> {
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
                    loadTaskLogs {
                        updateTaskStatusFromLastLog()
                    }
                }
            },
            onError = { message ->
                runOnUiThread {
                    Toast.makeText(this, "خطا در حذف: $message", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun updateTaskStatusFromLastLog() {
        if (logsList.isEmpty()) {
            updateTaskStatusDirectly(STATUS_IN_PROGRESS)
            return
        }

        val lastLog = logsList.maxByOrNull {
            it.date + it.startTime
        }

        lastLog?.let {
            val newStatus = when (it.newStatus) {
                STATUS_IN_PROGRESS -> STATUS_IN_PROGRESS
                STATUS_BLOCKED -> STATUS_BLOCKED

                STATUS_SENT_TO_SUPERVISOR -> STATUS_SENT_TO_SUPERVISOR
                else -> STATUS_IN_PROGRESS
            }
            updateTaskStatusDirectly(newStatus)
        }
    }

    private fun completeTask() {
        val currentUserId = getCurrentUserId()
        val isResponsible = task.responsible == currentUserId

        when {
            isResponsible && task.status == STATUS_SENT_TO_SUPERVISOR -> {
                updateTaskStatusDirectly(STATUS_SENT_TO_SUPERVISOR) { success ->
                    if (success) {
                        Toast.makeText(this, "درخواست تأیید به سرشیفت ارسال شد", Toast.LENGTH_SHORT).show()
                        navigateToCartable()
                    } else {
                        Toast.makeText(this, "خطا در ارسال درخواست", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            userRole == Config.RoleCode.SUPERVISOR && task.status == STATUS_SENT_TO_SUPERVISOR -> {
                startActivity(Intent(this, CompleteTaskActivity::class.java).apply {
                    putExtra("TASK_ID", task.id)
                    putExtra("TASK_TITLE", task.title)
                    putExtra("CURRENT_STATUS", task.status)
                })
            }
            else -> {
                Toast.makeText(this, "شما مجوز انجام این عملیات را ندارید", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateTaskStatusDirectly(newStatus: String, callback: ((Boolean) -> Unit)? = null) {
        val url = "${Config.BASE_URL}?action=updateTaskStatus"
        val jsonObject = JSONObject().apply {
            put("taskId", task.id)
            put("status", newStatus)
        }
        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonObject,
            { _ ->
                Log.d("TaskDetail", "Task status updated to $newStatus")
                task = task.copy(status = newStatus)
                setupAdapter()
                callback?.invoke(true)
            },
            { error ->
                Log.e("TaskDetail", "Error updating task status: ${error.message}")
                callback?.invoke(false)
            }
        )
        VolleySingleton.getInstance(this).add(request)
    }

    private fun navigateToCartable() {
        val intent = Intent(this, CartableActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }
}