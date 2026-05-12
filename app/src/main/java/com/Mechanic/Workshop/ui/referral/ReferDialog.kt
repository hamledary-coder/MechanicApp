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
import com.Mechanic.Workshop.R
import com.Mechanic.Workshop.data.model.Employee
import com.Mechanic.Workshop.data.remote.Config
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.IOException

class ReferDialog(private val context: Context, private val taskId: String, private val taskTitle: String) {
    private var dialog: AlertDialog? = null
    private lateinit var adapter: EmployeeSelectionAdapter
    private val selectedEmployees = mutableSetOf<Employee>()
    private var responsibleEmployee: Employee? = null
    private val client = OkHttpClient()

    private var onReferSubmit: ((String, String, String?) -> Unit)? = null

    fun setOnReferSubmitListener(listener: (String, String, String?) -> Unit) {
        this.onReferSubmit = listener
    }

    fun show() {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_refer, null)

        view.findViewById<TextView>(R.id.tvDialogTitle).text = "ارجاع کار: $taskTitle"

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewEmployees)
        val btnSubmit = view.findViewById<Button>(R.id.btnSubmit)
        val btnCancel = view.findViewById<Button>(R.id.btnCancel)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)

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
                responsibleEmployee = null
            }
            updateSubmitButton(btnSubmit)
        }
        recyclerView.adapter = adapter

        btnCancel.setOnClickListener { dialog?.dismiss() }

        btnSubmit.setOnClickListener {
            if (selectedEmployees.isEmpty()) {
                Toast.makeText(context, "حداقل یک نفر را انتخاب کنید", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            btnSubmit.isEnabled = false
            btnCancel.isEnabled = false

            val assigneeIds = selectedEmployees.map { it.id }.distinct().joinToString(",")
            val responsibleId = responsibleEmployee?.id

            onReferSubmit?.invoke(assigneeIds, Config.ReferralType.RESPONSIBLE, responsibleId)

            // بستن دیالوگ بعد از ارجاع
            dialog?.dismiss()
        }

        dialog = AlertDialog.Builder(context)
            .setView(view)
            .setCancelable(false)
            .create()

        loadEmployees(adapter, progressBar)

        dialog?.show()
    }

    private fun loadEmployees(adapter: EmployeeSelectionAdapter, progressBar: ProgressBar) {
        progressBar.visibility = View.VISIBLE

        val url = "${Config.BASE_URL}?action=getEmployees"

        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("Cache-Control", "no-cache")
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                (context as? android.app.Activity)?.runOnUiThread {
                    progressBar.visibility = View.GONE
                    Toast.makeText(context, "خطای شبکه: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val responseBody = response.body?.string() ?: "[]"

                android.util.Log.d("REFER_DIALOG", "Raw response: $responseBody")

                (context as? android.app.Activity)?.runOnUiThread {
                    progressBar.visibility = View.GONE
                    try {
                        val cleanResponse = responseBody.trim().replace("\uFEFF", "")
                        val jsonArray = JSONArray(cleanResponse)

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
                        if (employees.isEmpty()) {
                            Toast.makeText(context, "کارمندی برای ارجاع وجود ندارد", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("REFER_DIALOG", "JSON error: ${e.message}")
                        Toast.makeText(context, "خطا در دریافت لیست: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
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