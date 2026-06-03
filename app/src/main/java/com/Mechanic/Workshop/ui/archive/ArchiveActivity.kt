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
import TaskModel
import android.util.Log
import org.json.JSONArray

class ArchiveActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ExpandableTaskAdapter
    private val tasksList = mutableListOf<TaskModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_archive)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)  // ← این درست است
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "بایگانی"

        recyclerView = findViewById(R.id.recyclerViewArchive)
        recyclerView.layoutManager = LinearLayoutManager(this)

        loadArchivedTasks()
    }

    private fun loadArchivedTasks() {
        val url = "${Config.BASE_URL}?action=getTasks&status=5"

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                Log.d("Archive", "Response: $response")
                try {
                    val jsonArray = JSONArray(response)
                    tasksList.clear()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val task = TaskModel(
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
                            referredBy = obj.optString("referred_by", "")
                        )
                        tasksList.add(task)
                    }
                    setupAdapter()
                } catch (e: Exception) {
                    Log.e("Archive", "JSON error: ${e.message}")
                    Toast.makeText(this, "خطا در پردازش داده", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("Archive", "Network error: ${error.message}")
                Toast.makeText(this, "خطا در اتصال به شبکه", Toast.LENGTH_SHORT).show()
            }
        )
        Volley.newRequestQueue(this).add(request)
    }

    private fun setupAdapter() {
        Log.d("Archive", "setupAdapter called, tasksList size: ${tasksList.size}")
        adapter = ExpandableTaskAdapter(
            tasks = tasksList,
            tabType = "archived",
            onEditClick = { },
            onDeleteClick = { },
            onReferClick = { },
            onVolunteerClick = { },
            onItemClick = { task -> openTaskDetail(task) }
        )
        recyclerView.adapter = adapter
    }

    private fun openTaskDetail(task: TaskModel) {
        val intent = Intent(this, TaskDetailActivity::class.java)
        intent.putExtra("TASK_ID", task.id)
        intent.putExtra("TITLE", task.title)
        intent.putExtra("STATUS", task.status)
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
        intent.putExtra("REFERRED_BY", task.referredBy)
        startActivity(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}