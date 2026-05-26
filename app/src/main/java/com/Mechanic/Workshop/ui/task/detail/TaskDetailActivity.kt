package com.Mechanic.Workshop.ui.task.detail

import android.content.Intent
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
import android.graphics.Color
import android.util.Log

class TaskDetailActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TaskDetailAdapter
    private lateinit var task: TaskModel
    private val logsList = mutableListOf<TaskLogModel>()
        private lateinit var taskLogRepository: TaskLogRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_detail_new)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "جزئیات کار"
        toolbar.navigationIcon?.setTint(Color.WHITE)

        recyclerView = findViewById(R.id.recyclerViewTaskDetail)
        recyclerView.layoutManager = LinearLayoutManager(this)

        taskLogRepository = TaskLogRepository(this)

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

        loadTaskLogs()
    }

    private fun loadTaskLogs() {
        taskLogRepository.getTaskLogs(
            taskId = task.id,
            onSuccess = { logs ->
                runOnUiThread {
                    logsList.clear()
                    logsList.addAll(logs)
                    setupAdapter()
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

    private fun setupAdapter() {
        adapter = TaskDetailAdapter(
            task = task,
            logs = logsList,
            onEditLogClick = { log -> editLog(log) },
            onDeleteLogClick = { log -> deleteLog(log) },
            onAddLogClick = { addNewLog() },
            onRefreshLogs = { refreshLogs() }
        )
        recyclerView.adapter = adapter
    }

    private fun editLog(log: TaskLogModel) {
        val intent = Intent(this, AddLogActivity::class.java).apply {
            putExtra("IS_EDIT_MODE", true)
            putExtra("LOG_ID", log.id)
            putExtra("TASK_ID", log.taskId)
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
        startActivity(intent)
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

    private fun addNewLog() {
        val intent = Intent(this, AddLogActivity::class.java)
        intent.putExtra("TASK_ID", task.id)
        intent.putExtra("TITLE", task.title)
        intent.putExtra("DESC", task.description)
        intent.putExtra("CREATOR", task.creator)
        intent.putExtra("DATE", task.createDate)
        intent.putExtra("RESPONSIBLE", task.responsible)
        intent.putExtra("ASSIGNED_TO", task.assignedTo)
        intent.putExtra("UNIT", task.unit)
        intent.putExtra("PRIORITY", task.priority)
        intent.putExtra("SUB_UNIT", task.sub_unit)
        intent.putExtra("DECLARATION_METHOD", task.declaration_method)
        intent.putExtra("REQUESTER", task.requester)
        intent.putExtra("REQUEST_DATE", task.request_date)
        intent.putExtra("INITIAL_REVIEW", task.initial_review)
        intent.putExtra("SYSTEM_REQUEST_NUMBER", task.system_request_number)
        intent.putExtra("URGENCY", task.urgency)
        startActivity(intent)
    }

    private fun refreshLogs() {
        loadTaskLogs()
    }

    override fun onResume() {
        super.onResume()
        loadTaskLogs()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}