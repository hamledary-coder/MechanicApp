package com.Mechanic.Workshop.ui.task.quicklog

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.Mechanic.Workshop.R
import com.Mechanic.Workshop.data.remote.Config
import com.Mechanic.Workshop.ui.task.log.AddLogActivity
import com.Mechanic.Workshop.utils.VolleySingleton
import TaskModel
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import org.json.JSONObject

class QuickLogActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: QuickLogTaskAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyState: TextView
    private lateinit var tvSubtitle: TextView

    private val taskList = mutableListOf<TaskModel>()
    private var currentUserId = ""

    // متغیر برای جلوگیری از درخواست‌های همزمان
    private var isLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quick_log)

        initViews()
        setupToolbar()
        getCurrentUserInfo()
        loadTasks()
    }

    // ====== اضافه کردن onResume برای به‌روزرسانی لیست ======
    override fun onResume() {
        super.onResume()
        // وقتی از صفحه ثبت گزارش برمی‌گردد، لیست را مجدداً بارگذاری کن
        loadTasks()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerViewTasks)
        progressBar = findViewById(R.id.progressBar)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        tvSubtitle = findViewById(R.id.tvSubtitle)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = QuickLogTaskAdapter { task ->
            openAddLogForTask(task)
        }
        recyclerView.adapter = adapter
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "ثبت گزارش سریع"
        }
    }

    private fun getCurrentUserInfo() {
        val sharedPref = getSharedPreferences(Config.PrefKeys.USER_PREFS, MODE_PRIVATE)
        currentUserId = sharedPref.getString(Config.PrefKeys.USER_ROW_ID, "") ?: ""
    }

    private fun loadTasks() {
        // جلوگیری از درخواست‌های همزمان
        if (isLoading) return

        // اگر کاربر لاگین نیست، کاری نکن
        if (currentUserId.isEmpty()) {
            tvEmptyState.text = "کاربر شناسایی نشد"
            tvEmptyState.visibility = View.VISIBLE
            return
        }

        isLoading = true
        progressBar.visibility = View.VISIBLE
        tvEmptyState.visibility = View.GONE

        val url = "${Config.BASE_URL}?action=getUserResponsibleTasks&userId=$currentUserId"

        val request = JsonArrayRequest(
            Request.Method.GET, url, null,
            { response ->
                isLoading = false
                progressBar.visibility = View.GONE
                taskList.clear()

                for (i in 0 until response.length()) {
                    try {
                        val obj = response.getJSONObject(i)
                        val task = parseTask(obj)
                        taskList.add(task)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                adapter.submitList(taskList)
                updateEmptyState()
            },
            { error ->
                isLoading = false
                progressBar.visibility = View.GONE
                tvEmptyState.text = "خطا در بارگذاری: ${error.message}"
                tvEmptyState.visibility = View.VISIBLE
            }
        )

        VolleySingleton.getInstance(this).add(request)
    }

    private fun parseTask(obj: JSONObject): TaskModel {
        return TaskModel(
            id = obj.optString("id", ""),
            createDate = obj.optString("createDate", ""),
            title = obj.optString("title", ""),
            description = obj.optString("description", ""),
            creator = obj.optString("creator", ""),
            status = obj.optString("status", "1"),
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
            referredBy = obj.optString("referredBy", "")
        )
    }

    private fun openAddLogForTask(task: TaskModel) {
        val intent = Intent(this, AddLogActivity::class.java).apply {
            putExtra("TASK_ID", task.id)
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
            putExtra("FROM_QUICK_LOG", true)
        }
        startActivity(intent)
    }

    private fun updateEmptyState() {
        if (adapter.itemCount == 0) {
            tvEmptyState.text = "شما مسئول هیچ کاری در حال انجام نیستید"
            tvEmptyState.visibility = View.VISIBLE
        } else {
            tvEmptyState.visibility = View.GONE
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}