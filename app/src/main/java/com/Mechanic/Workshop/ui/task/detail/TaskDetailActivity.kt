package com.Mechanic.Workshop.ui.task.detail

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.Mechanic.Workshop.R
import com.Mechanic.Workshop.data.model.TaskLogModel
import com.Mechanic.Workshop.data.remote.Config
import TaskModel
import android.content.Intent
import com.Mechanic.Workshop.ui.task.log.AddLogActivity

class TaskDetailActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TaskDetailAdapter
    private lateinit var task: TaskModel
    private val logsList = mutableListOf<TaskLogModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_detail_new)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "جزئیات کار"

        recyclerView = findViewById(R.id.recyclerViewTaskDetail)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // دریافت اطلاعات کار از Intent
        task = TaskModel(
            id = intent.getStringExtra("TASK_ID") ?: "",
            createDate = intent.getStringExtra("DATE") ?: "",
            title = intent.getStringExtra("TITLE") ?: "",
            description = intent.getStringExtra("DESC") ?: "",
            creator = intent.getStringExtra("CREATOR") ?: "",
            status = "1",  // مقدار پیش‌فرض، بعداً از سرور می‌آید
            assignedTo = "",
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
            system_request_number = intent.getStringExtra("SYSTEM_REQUEST_NUMBER") ?: ""
        )

        // TODO: دریافت لیست گزارش‌ها از سرور (فعلاً mock)
        loadMockLogs()

        adapter = TaskDetailAdapter(
            task = task,
            logs = logsList,
            onEditLogClick = { log -> editLog(log) },
            onDeleteLogClick = { log -> deleteLog(log) },
            onAddLogClick = { addNewLog() }
        )

        recyclerView.adapter = adapter
    }

    private fun loadMockLogs() {
        // داده‌های آزمایشی (بعداً با API واقعی جایگزین می‌شود)
        logsList.add(
            TaskLogModel(
                id = "1",
                taskId = task.id,
                userId = "101",
                userName = "علی رضایی",
                date = "۱۴۰۴/۰۲/۲۵",
                startTime = "09:00",
                endTime = "12:00",
                actionDescription = "بررسی اولیه پمپ و تعویض واشر",
                consumedParts = "واشر آب‌بندی 2 عدد",
                assignedUsers = "102,103",
                newStatus = "22",  // اقدام شده
                attachments = "",
                notes = "نیاز به قطعه یدکی دارد",
                canEditDelete = true
            )
        )

        logsList.add(
            TaskLogModel(
                id = "2",
                taskId = task.id,
                userId = "102",
                userName = "محمد کریمی",
                date = "۱۴۰۴/۰۲/۲۶",
                startTime = "08:30",
                endTime = "10:00",
                actionDescription = "تست پمپ پس از تعمیرات",
                consumedParts = "",
                assignedUsers = "103",
                newStatus = "41",  // اتمام کار
                attachments = "",
                notes = "عملیات موفقیت‌آمیز بود",
                canEditDelete = false
            )
        )
    }

    private fun editLog(log: TaskLogModel) {
        Toast.makeText(this, "ویرایش گزارش ${log.id}", Toast.LENGTH_SHORT).show()
        // TODO: باز کردن صفحه یا دیالوگ ویرایش گزارش
    }

    private fun deleteLog(log: TaskLogModel) {
        Toast.makeText(this, "حذف گزارش ${log.id}", Toast.LENGTH_SHORT).show()
        // TODO: درخواست حذف به سرور و حذف از لیست
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

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}