package com.Mechanic.Workshop.ui.referral

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.Mechanic.Workshop.ui.referral.EmployeeSelectionAdapter
import com.Mechanic.Workshop.R
import com.Mechanic.Workshop.data.model.Employee
import com.Mechanic.Workshop.data.remote.Config
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray

class ReferDialog(private val context: Context, private val taskId: String, private val taskTitle: String) {
    private var dialog: AlertDialog? = null
    private lateinit var adapter: EmployeeSelectionAdapter
    private val selectedEmployees = mutableSetOf<Employee>()
    private var responsibleEmployee: Employee? = null

    private var onReferSubmit: ((String, String, String?) -> Unit)? = null

    fun setOnReferSubmitListener(listener: (String, String, String?) -> Unit) {
        this.onReferSubmit = listener
    }

    fun show() {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_refer, null)

        // تنظیم عنوان
        view.findViewById<TextView>(R.id.tvDialogTitle).text = "ارجاع کار: $taskTitle"

        // پیدا کردن ویوها
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewEmployees)
        val btnSubmit = view.findViewById<Button>(R.id.btnSubmit)
        val btnCancel = view.findViewById<Button>(R.id.btnCancel)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)

        // تنظیم RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = EmployeeSelectionAdapter(emptyList()) { employee, isSelected, isResponsible ->
            if (isSelected) {
                selectedEmployees.add(employee)
            } else {
                selectedEmployees.remove(employee)
                if (responsibleEmployee?.id == employee.id) {
                    responsibleEmployee = null
                }
            }
            if (isResponsible) {
                responsibleEmployee = employee
            } else if (responsibleEmployee?.id == employee.id) {
                // 🔴 اگر رادیوباتن این شخص خاموش شد (isResponsible = false)
                // ولی این شخص مسئول بود، مسئولیتش رو حذف کن
                responsibleEmployee = null
            }
            updateSubmitButton(btnSubmit)
        }
        recyclerView.adapter = adapter

        // دکمه‌ها
        btnCancel.setOnClickListener { dialog?.dismiss() }

        btnSubmit.setOnClickListener {
            if (selectedEmployees.isEmpty()) {
                Toast.makeText(context, "حداقل یک نفر را انتخاب کنید", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ✅ ProgressBar رو نشون بده
            progressBar.visibility = View.VISIBLE
            btnSubmit.isEnabled = false
            btnCancel.isEnabled = false

            val assigneeIds = selectedEmployees.map { it.id }.distinct().joinToString(",")
            val responsibleId = responsibleEmployee?.id

            // همیشه ارجاع با مسئول است (RESPONSIBLE)
            onReferSubmit?.invoke(assigneeIds, Config.ReferralType.RESPONSIBLE, responsibleId)
            //dialog?.dismiss()
        }

        // ساختن دیالوگ
        dialog = AlertDialog.Builder(context)
            .setView(view)
            .setCancelable(false)
            .create()

        // بارگذاری لیست کارمندان
        loadEmployees(adapter, progressBar)


        dialog?.show()
    }

    // در ReferDialog.kt - تابع loadEmployees

    private fun loadEmployees(adapter: EmployeeSelectionAdapter, progressBar: ProgressBar) {
        progressBar.visibility = View.VISIBLE

        val url = "${Config.Endpoints.TASKS}?action=getEmployees"

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                progressBar.visibility = View.GONE

                try {
                    val jsonArray = JSONArray(response)
                    val employees = mutableListOf<Employee>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val role = obj.getString("role")
                        if (role == Config.RoleCode.EMPLOYEE || role == Config.RoleCode.SUPERVISOR) {
                            employees.add(
                                Employee(
                                    id = obj.getString("rowId"),
                                    name = obj.getString("name"),
                                    role = role
                                )
                            )
                        }
                    }
                    adapter.updateList(employees)
                } catch (e: Exception) {
                    Toast.makeText(context, "خطا در دریافت لیست", Toast.LENGTH_SHORT).show()

                }
            },
            { error ->
                progressBar.visibility = View.GONE
                Toast.makeText(context, "خطای شبکه", Toast.LENGTH_SHORT).show()

            })

        Volley.newRequestQueue(context).add(request)
    }



    private fun updateSubmitButton(btnSubmit: Button) {
        val hasSelection = selectedEmployees.isNotEmpty()

        btnSubmit.isEnabled = hasSelection

        btnSubmit.text = when {
            !btnSubmit.isEnabled -> "ارجاع کار"
            responsibleEmployee != null -> "ارجاع (مسئول: ${responsibleEmployee?.name})"
            else -> "ارجاع به ${selectedEmployees.size} نفر"
        }
    }
}