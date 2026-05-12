package com.Mechanic.Workshop.ui.task.detail

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.Mechanic.Workshop.R
import com.Mechanic.Workshop.data.remote.Config
import com.Mechanic.Workshop.ui.cartable.CartableActivity
import com.Mechanic.Workshop.ui.task.create.CreateTaskActivity
import com.Mechanic.Workshop.ui.volunteer.InviteAdapter
import com.Mechanic.Workshop.ui.referral.ReferDialog
import com.Mechanic.Workshop.data.model.Employee
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class TaskDetailActivity : AppCompatActivity() {

    private lateinit var btnEdit: Button
    private lateinit var btnDelete: Button
    private lateinit var btnRefer: Button
    private lateinit var loadingProgress: ProgressBar
    private lateinit var btnVolunteer: Button
    private var taskId: String? = null
    private var taskResponsible: String = ""
    private val client = OkHttpClient()

    private val startEditForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_detail)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        btnEdit = findViewById(R.id.btnEditTask)
        btnDelete = findViewById(R.id.btnDeleteTask)
        btnRefer = findViewById(R.id.btnReferTask)
        loadingProgress = findViewById(R.id.loadingProgress)
        btnVolunteer = findViewById(R.id.btnVolunteer)

        taskId = intent.getStringExtra("TASK_ID")
        taskResponsible = intent.getStringExtra("RESPONSIBLE") ?: ""
        val title = intent.getStringExtra("TITLE")
        val desc = intent.getStringExtra("DESC")
        val creator = intent.getStringExtra("CREATOR")
        val rawDate = intent.getStringExtra("DATE") ?: ""
        val unit = intent.getStringExtra("UNIT") ?: ""
        val priority = intent.getStringExtra("PRIORITY") ?: ""

        val cleanDate = if (rawDate.contains("GMT")) {
            rawDate.substring(0, rawDate.indexOf("GMT")).trim()
        } else if (rawDate.length > 20) {
            rawDate.substring(0, 16)
        } else {
            rawDate
        }
        setupTabs(cleanDate)

        btnEdit.visibility = View.VISIBLE
        btnDelete.visibility = View.VISIBLE
        btnRefer.visibility = View.VISIBLE

        // حذف با OkHttp
        btnDelete.setOnClickListener {
            val currentTaskId = taskId ?: intent.getStringExtra("TASK_ID") ?: ""

            AlertDialog.Builder(this)
                .setTitle("حذف کار")
                .setMessage("آیا مطمئن هستید که این کار حذف شود؟")
                .setPositiveButton("بله، حذف کن") { _, _ ->
                    val url = "${Config.Endpoints.DELETE_TASK}?action=delete&id=$currentTaskId"

                    loadingProgress.visibility = View.VISIBLE
                    btnDelete.isEnabled = false
                    btnEdit.isEnabled = false
                    btnRefer.isEnabled = false

                    val request = Request.Builder()
                        .url(url)
                        .get()
                        .addHeader("Cache-Control", "no-cache")
                        .build()

                    client.newCall(request).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            runOnUiThread {
                                loadingProgress.visibility = View.GONE
                                btnDelete.isEnabled = true
                                btnEdit.isEnabled = true
                                btnRefer.isEnabled = true
                                Toast.makeText(this@TaskDetailActivity, "خطا در اتصال: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }

                        override fun onResponse(call: Call, response: Response) {
                            val responseBody = response.body?.string() ?: ""
                            runOnUiThread {
                                loadingProgress.visibility = View.GONE
                                if (response.isSuccessful && responseBody.contains("success")) {
                                    Toast.makeText(this@TaskDetailActivity, "با موفقیت حذف شد", Toast.LENGTH_SHORT).show()
                                    finish()
                                } else {
                                    btnDelete.isEnabled = true
                                    btnEdit.isEnabled = true
                                    btnRefer.isEnabled = true
                                    Toast.makeText(this@TaskDetailActivity, "خطا: $responseBody", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    })
                }
                .setNegativeButton("انصراف", null)
                .show()
        }

        btnEdit.setOnClickListener {
            val editIntent = Intent(this, CreateTaskActivity::class.java)
            editIntent.putExtra("IS_EDIT", true)
            editIntent.putExtra("TASK_ID", taskId)
            editIntent.putExtra("TITLE", title)
            editIntent.putExtra("DESC", desc)
            editIntent.putExtra("UNIT", unit)
            editIntent.putExtra("PRIORITY", priority)
            startEditForResult.launch(editIntent)
        }

        btnRefer.setOnClickListener {
            val sharedPref = getSharedPreferences(Config.PrefKeys.USER_PREFS, MODE_PRIVATE)
            val userRole = sharedPref.getString(Config.PrefKeys.USER_ROLE, "")

            if (userRole != Config.RoleCode.SUPERVISOR && userRole != Config.RoleCode.MANAGER) {
                Toast.makeText(this, "فقط سرشیفت و مدیر می‌توانند ارجاع دهند", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val currentTaskId = taskId ?: intent.getStringExtra("TASK_ID") ?: ""
            if (currentTaskId.isEmpty()) {
                Toast.makeText(this, "شناسه کار نامعتبر", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val dialog = ReferDialog(this, currentTaskId, intent.getStringExtra("TITLE") ?: "")
            dialog.setOnReferSubmitListener { assigneeIds, referralType, responsibleId ->
                sendReferralRequest(currentTaskId, assigneeIds, referralType, responsibleId)
            }
            dialog.show()
        }

        btnVolunteer.setOnClickListener {
            showVolunteerDialog()
        }
        val btnBack = findViewById<Button>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }
    }

    // ارجاع کار با OkHttp
    private fun sendReferralRequest(taskId: String, assigneeIds: String, referralType: String, responsibleId: String?) {
        val url = Config.Endpoints.TASKS

        val jsonObject = JSONObject().apply {
            put("action", "assignTask")
            put("taskId", taskId)
            put("assigneeIds", assigneeIds)
            put("referralType", referralType)
            responsibleId?.let { put("responsibleId", it) }
        }

        val body = RequestBody.create(
            "application/json; charset=utf-8".toMediaType(),
            jsonObject.toString()
        )

        val request = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Cache-Control", "no-cache")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@TaskDetailActivity, "خطای شبکه: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string() ?: ""
                runOnUiThread {
                    if (response.isSuccessful && responseBody.contains("success")) {
                        Toast.makeText(this@TaskDetailActivity, "ارجاع با موفقیت ثبت شد", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@TaskDetailActivity, "خطا: $responseBody", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun showVolunteerDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_volunteer, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        val btnResponsibility = dialogView.findViewById<Button>(R.id.btnAcceptResponsibility)
        val btnAssignee = dialogView.findViewById<Button>(R.id.btnAcceptAssignee)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)

        if (taskResponsible.isNotEmpty()) {
            btnResponsibility.isEnabled = false
            btnResponsibility.alpha = 0.5f
            btnResponsibility.text = "مسئول انجام کار تعیین شده"
        }

        btnResponsibility.setOnClickListener {
            acceptAsResponsible()
            dialog.dismiss()
        }

        btnAssignee.setOnClickListener {
            dialogView.findViewById<ProgressBar>(R.id.progressBar).visibility = View.VISIBLE
            btnResponsibility.isEnabled = false
            btnAssignee.isEnabled = false
            btnCancel.isEnabled = false
            btnAssignee.alpha = 0.5f
            btnCancel.alpha = 0.5f
            btnResponsibility.alpha = 0.5f
            acceptAsAssignee()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun acceptAsResponsible() {
        val sharedPref = getSharedPreferences(Config.PrefKeys.USER_PREFS, MODE_PRIVATE)
        val currentUserRowId = sharedPref.getString(Config.PrefKeys.USER_ROW_ID, "") ?: ""

        showInviteDialogWithCallback { inviteeIds ->
            if (inviteeIds.isNotEmpty()) {
                sendVolunteerRequest(
                    assigneeIds = currentUserRowId,
                    responsibleId = currentUserRowId,
                    showFinish = false
                )
                sendInviteRequest(taskId ?: "", inviteeIds)
                val intent = Intent(this, CartableActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "ثبت مسئولیت لغو شد", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showInviteDialogWithCallback(onResult: (String) -> Unit) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_invite, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerEmployees)
        val btnSubmit = dialogView.findViewById<Button>(R.id.btnSubmit)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.progressBar)

        recyclerView.layoutManager = LinearLayoutManager(this)

        val selectedEmployees = mutableSetOf<Employee>()

        val adapter = InviteAdapter(emptyList()) { employee, isSelected ->
            if (isSelected) {
                selectedEmployees.add(employee)
            } else {
                selectedEmployees.remove(employee)
            }
            btnSubmit.isEnabled = selectedEmployees.isNotEmpty()
            btnSubmit.text = "ارسال پیشنهاد به ${selectedEmployees.size} نفر"
        }
        recyclerView.adapter = adapter

        loadCollaboratorsForInvite(adapter, progressBar)

        btnSubmit.setOnClickListener {
            dialogView.findViewById<ProgressBar>(R.id.progressBar).visibility = View.VISIBLE
            btnSubmit.isEnabled = false
            btnCancel.isEnabled = false
            btnCancel.alpha = 0.5f
            btnSubmit.alpha = 0.5f
            val inviteeIds = selectedEmployees.joinToString(",") { it.id }
            onResult.invoke(inviteeIds)
        }

        btnCancel.setOnClickListener {
            onResult.invoke("")
            dialog.dismiss()
        }

        dialog.show()
    }

    // دریافت لیست کاربران برای دعوت با OkHttp
    private fun loadCollaboratorsForInvite(adapter: InviteAdapter, progressBar: ProgressBar) {
        val sharedPref = getSharedPreferences(Config.PrefKeys.USER_PREFS, MODE_PRIVATE)
        val currentUserRowId = sharedPref.getString(Config.PrefKeys.USER_ROW_ID, "") ?: ""

        progressBar.visibility = View.VISIBLE
        val url = "${Config.Endpoints.TASKS}?action=getEmployees"

        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("Cache-Control", "no-cache")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@TaskDetailActivity, "خطای شبکه: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string() ?: "[]"
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    try {
                        val jsonArray = JSONArray(responseBody)
                        val employees = mutableListOf<Employee>()
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            val role = obj.getString("role")
                            val rowId = obj.getString("rowId")
                            if (rowId != currentUserRowId &&
                                (role == Config.RoleCode.EMPLOYEE || role == Config.RoleCode.SUPERVISOR)) {
                                employees.add(
                                    Employee(
                                        id = rowId,
                                        name = obj.getString("name"),
                                        role = role
                                    )
                                )
                            }
                        }
                        adapter.updateList(employees)
                    } catch (e: Exception) {
                        Toast.makeText(this@TaskDetailActivity, "خطا در دریافت لیست", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    // ارسال دعوتنامه با OkHttp
    private fun sendInviteRequest(taskId: String, inviteeIds: String) {
        val url = Config.Endpoints.TASKS

        val jsonObject = JSONObject().apply {
            put("action", "sendInvite")
            put("taskId", taskId)
            put("inviteeIds", inviteeIds)
        }

        val body = RequestBody.create(
            "application/json; charset=utf-8".toMediaType(),
            jsonObject.toString()
        )

        val request = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Cache-Control", "no-cache")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@TaskDetailActivity, "خطای شبکه: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string() ?: ""
                runOnUiThread {
                    if (response.isSuccessful && responseBody.contains("success")) {
                        Toast.makeText(this@TaskDetailActivity, "پیشنهاد همکاری ارسال شد. مسئول انجام کار ثبت شد.", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@TaskDetailActivity, CartableActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@TaskDetailActivity, "خطا: $responseBody", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun acceptAsAssignee() {
        val sharedPref = getSharedPreferences(Config.PrefKeys.USER_PREFS, MODE_PRIVATE)
        val currentUserRowId = sharedPref.getString(Config.PrefKeys.USER_ROW_ID, "") ?: ""

        loadingProgress.visibility = View.VISIBLE
        btnVolunteer.isEnabled = false

        sendVolunteerRequest(
            assigneeIds = currentUserRowId,
            responsibleId = null,
            showFinish = true
        )
    }

    // داوطلب شدن با OkHttp
    private fun sendVolunteerRequest(assigneeIds: String, responsibleId: String?, showFinish: Boolean) {
        val url = Config.Endpoints.TASKS

        val jsonObject = JSONObject().apply {
            put("action", "volunteer")
            put("taskId", taskId ?: "")
            put("assigneeIds", assigneeIds)
            responsibleId?.let { put("responsibleId", it) }
        }

        val body = RequestBody.create(
            "application/json; charset=utf-8".toMediaType(),
            jsonObject.toString()
        )

        val request = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Cache-Control", "no-cache")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    loadingProgress.visibility = View.GONE
                    btnVolunteer.isEnabled = true
                    Toast.makeText(this@TaskDetailActivity, "خطای شبکه: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string() ?: ""
                runOnUiThread {
                    loadingProgress.visibility = View.GONE
                    btnVolunteer.isEnabled = true
                    if (response.isSuccessful && responseBody.contains("success")) {
                        Toast.makeText(this@TaskDetailActivity, "شما در لیست گروه انجام دهنده این کار قرار گرفتید", Toast.LENGTH_SHORT).show()
                        if (showFinish) {
                            finish()
                        }
                    } else {
                        Toast.makeText(this@TaskDetailActivity, "خطا: $responseBody", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun setupTabs(cleanDate: String) {
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)

        val adapter = TaskDetailPagerAdapter(
            this,
            taskId ?: "",
            intent.getStringExtra("TITLE") ?: "",
            intent.getStringExtra("DESC") ?: "",
            intent.getStringExtra("CREATOR") ?: "",
            cleanDate,
            taskResponsible,
            intent.getStringExtra("UNIT") ?: "",
            intent.getStringExtra("PRIORITY") ?: ""
        )
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = "شرح کار"
                1 -> tab.text = "ثبت سوابق"
            }
        }.attach()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}

class TaskDetailPagerAdapter(
    activity: AppCompatActivity,
    private val taskId: String,
    private val title: String,
    private val desc: String,
    private val creator: String,
    private val date: String,
    private val responsible: String,
    private val unit: String,
    private val priority: String
) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> TaskInfoFragment.newInstance(
                taskId, title, desc, creator, date, responsible, unit, priority
            )
            1 -> TaskLogFragment.newInstance(taskId)
            else -> TaskInfoFragment.newInstance(
                taskId, title, desc, creator, date, responsible, unit, priority
            )
        }
    }
}