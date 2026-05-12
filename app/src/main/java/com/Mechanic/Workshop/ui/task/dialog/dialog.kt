package com.Mechanic.Workshop.ui.task.dialog

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ProgressBar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.Mechanic.Workshop.R
import com.Mechanic.Workshop.data.model.Employee
import com.Mechanic.Workshop.ui.volunteer.InviteAdapter
import com.Mechanic.Workshop.ui.task.repository.TaskRepository
import org.json.JSONArray

class InviteDialog(
    private val context: Context,
    private val taskRepository: TaskRepository,
    private val onInviteSent: (String) -> Unit
) {

    fun show() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_invite, null)
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerEmployees)
        val btnSubmit = dialogView.findViewById<Button>(R.id.btnSubmit)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.progressBar)

        recyclerView.layoutManager = LinearLayoutManager(context)

        val selectedEmployees = mutableSetOf<Employee>()
        val adapter = InviteAdapter(emptyList()) { employee, isSelected ->
            if (isSelected) selectedEmployees.add(employee) else selectedEmployees.remove(employee)
            btnSubmit.isEnabled = selectedEmployees.isNotEmpty()
            btnSubmit.text = "ارسال پیشنهاد به ${selectedEmployees.size} نفر"
        }
        recyclerView.adapter = adapter

        loadEmployees(adapter, progressBar)

        btnSubmit.setOnClickListener {
            progressBar.visibility = android.view.View.VISIBLE
            btnSubmit.isEnabled = false
            btnCancel.isEnabled = false
            val inviteeIds = selectedEmployees.joinToString(",") { it.id }
            onInviteSent(inviteeIds)
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            onInviteSent("")
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun loadEmployees(adapter: InviteAdapter, progressBar: ProgressBar) {
        progressBar.visibility = android.view.View.VISIBLE
        taskRepository.getEmployees(
            onSuccess = { response ->
                progressBar.visibility = android.view.View.GONE
                try {
                    val jsonArray = JSONArray(response)
                    val employees = mutableListOf<Employee>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        employees.add(Employee(obj.getString("rowId"), obj.getString("name"), obj.getString("role")))
                    }
                    adapter.updateList(employees)
                } catch (e: Exception) {
                    adapter.updateList(emptyList())
                }
            },
            onError = {
                progressBar.visibility = android.view.View.GONE
                adapter.updateList(emptyList())
            }
        )
    }
}