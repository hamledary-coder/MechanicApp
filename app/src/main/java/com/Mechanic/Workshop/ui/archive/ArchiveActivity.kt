package com.Mechanic.Workshop.ui.archive

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.Mechanic.Workshop.R
import com.Mechanic.Workshop.data.remote.Config
import com.Mechanic.Workshop.ui.task.detail.TaskDetailActivity
import com.Mechanic.Workshop.ui.task.list.ExpandableTaskAdapter
import com.Mechanic.Workshop.utils.UserCache
import TaskModel
import android.util.Log
import org.json.JSONArray
import org.json.JSONException

class ArchiveActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ExpandableTaskAdapter
    private val tasksList = mutableListOf<TaskModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_archive)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "بایگانی"

        recyclerView = findViewById(R.id.recyclerViewArchive)
        recyclerView.layoutManager = LinearLayoutManager(this)

        loadArchivedTasks()
    }

    private fun loadArchivedTasks() {
        // اضافه کردن تایم‌استمپ برای جلوگیری از کش
        val url = "${Config.BASE_URL}?action=getTasks&status=5&_=${System.currentTimeMillis()}"

        val request = object : StringRequest(
            Request.Method.GET, url,
            { response ->
                Log.d("Archive", "Response: $response")
                parseAndDisplayTasks(response)
            },
            { error ->
                Log.e("Archive", "Network error: ${error.message}")
                Toast.makeText(this, "خطا در اتصال به شبکه", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Cache-Control"] = "no-cache, no-store, must-revalidate"
                headers["Pragma"] = "no-cache"
                headers["Expires"] = "0"
                return headers
            }
        }

        Volley.newRequestQueue(this).apply {
            cache?.clear()
        }.add(request)
    }

    private fun parseAndDisplayTasks(response: String) {
        try {
            val jsonArray = JSONArray(response)
            tasksList.clear()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)

                val task = TaskModel(
                    id = obj.optString("id", ""),
                    createDate = obj.optString("createDate", ""),
                    title = obj.optString("title", "بدون عنوان"),
                    description = obj.optString("description", ""),
                    creator = obj.optString("creator", ""),
                    status = obj.optString("status", "5"),
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
                tasksList.add(task)
            }

            Log.d("Archive", "Tasks loaded: ${tasksList.size}")

            // پیش‌بارگذاری اسامی کاربران در UserCache
            UserCache.preloadFromTasks(tasksList)

            if (tasksList.isEmpty()) {
                Toast.makeText(this, "هیچ تسک بایگانی شده‌ای یافت نشد", Toast.LENGTH_SHORT).show()
            }

            setupAdapter()

        } catch (e: JSONException) {
            Log.e("Archive", "JSON parsing error: ${e.message}")
            Toast.makeText(this, "خطا در پردازش داده‌های دریافتی", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e("Archive", "Unexpected error: ${e.message}")
            Toast.makeText(this, "خطای غیرمنتظره رخ داد", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupAdapter() {
        Log.d("Archive", "setupAdapter called, tasksList size: ${tasksList.size}")

        adapter = ExpandableTaskAdapter(
            tasks = tasksList,
            tabType = "archived",
            onEditClick = { task ->
                Toast.makeText(this, "تسک‌های بایگانی شده قابل ویرایش نیستند", Toast.LENGTH_SHORT).show()
            },
            onDeleteClick = { task ->
                Toast.makeText(this, "تسک‌های بایگانی شده قابل حذف نیستند", Toast.LENGTH_SHORT).show()
            },
            onReferClick = { task ->
                Toast.makeText(this, "تسک‌های بایگانی شده قابل ارجاع نیستند", Toast.LENGTH_SHORT).show()
            },
            onVolunteerClick = { task ->
                Toast.makeText(this, "تسک‌های بایگانی شده قابل داوطلبی نیستند", Toast.LENGTH_SHORT).show()
            },
            onItemClick = { task -> openTaskDetail(task) }
        )
        recyclerView.adapter = adapter
    }

    private fun openTaskDetail(task: TaskModel) {
        val intent = Intent(this, TaskDetailActivity::class.java).apply {
            putExtra("TASK_ID", task.id)
            putExtra("TITLE", task.title)
            putExtra("STATUS", task.status)
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
            putExtra("REFERRED_BY", task.referredBy)
        }
        startActivity(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onResume() {
        super.onResume()
        // هر بار که صفحه نمایش داده می‌شود، داده‌ها را دوباره بارگیری کن
        loadArchivedTasks()
    }
}