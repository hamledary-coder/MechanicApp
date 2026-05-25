package com.Mechanic.Workshop.ui.referral

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.Mechanic.Workshop.R
import com.Mechanic.Workshop.data.model.Employee

class EmployeeSelectionAdapter(
    private var employees: List<Employee>,
    private val preSelectedIds: Set<String> = emptySet(),
    private val preSelectedResponsible: String? = null,
    private val onSelectionChanged: (Employee, Boolean, Boolean) -> Unit
) : RecyclerView.Adapter<EmployeeSelectionAdapter.ViewHolder>() {

    // این مقادیر اولیه را جداگانه نگه می‌داریم
    private val initialSelectedIds = preSelectedIds.toMutableSet()
    private val initialResponsibleId = preSelectedResponsible

    private val selectedIds = mutableSetOf<String>().apply { addAll(initialSelectedIds) }
    private var responsibleId: String? = initialResponsibleId

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvEmployeeName)
        val cbSelect: CheckBox = view.findViewById(R.id.cbSelect)
        val rbResponsible: RadioButton = view.findViewById(R.id.rbResponsible)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_employee_selection, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val employee = employees[position]
        holder.tvName.text = employee.name

        holder.cbSelect.setOnCheckedChangeListener(null)
        holder.rbResponsible.setOnCheckedChangeListener(null)

        val isSelected = selectedIds.contains(employee.id)
        val isResponsible = responsibleId == employee.id

        holder.cbSelect.isChecked = isSelected
        holder.rbResponsible.isChecked = isResponsible
        holder.rbResponsible.isEnabled = isSelected

        holder.cbSelect.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedIds.add(employee.id)
                onSelectionChanged(employee, true, false)
            } else {
                selectedIds.remove(employee.id)
                if (responsibleId == employee.id) {
                    responsibleId = null
                    holder.rbResponsible.isChecked = false
                    onSelectionChanged(employee, false, true)
                } else {
                    onSelectionChanged(employee, false, false)
                }
            }
            holder.rbResponsible.isEnabled = isChecked
        }

        holder.rbResponsible.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                val prevResponsible = responsibleId
                responsibleId = employee.id
                if (prevResponsible != null && prevResponsible != employee.id) {
                    notifyDataSetChanged()
                }
                onSelectionChanged(employee, true, true)
            } else {
                if (responsibleId == employee.id) {
                    responsibleId = null
                    onSelectionChanged(employee, true, false)
                }
            }
        }

        holder.itemView.setOnClickListener {
            holder.cbSelect.isChecked = !holder.cbSelect.isChecked
        }
    }

    override fun getItemCount() = employees.size

    fun updateList(newList: List<Employee>) {
        employees = newList

        // تنظیم مجدد selectedIds بر اساس مقادیر اولیه (نه مقادیر قبلی که ممکن است تغییر کرده باشند)
        // اگر می‌خواهی تغییرات کاربر حفظ شود، باید از selectedIds فعلی استفاده کنی
        // اما اینجا فرض می‌کنیم می‌خواهیم مقادیر اولیه (از دیتابیس) حفظ شود

        // گزینه 1: حفظ مقادیر اولیه (از دیتابیس)
        selectedIds.clear()
        selectedIds.addAll(initialSelectedIds.filter { id ->
            employees.any { it.id == id }
        })

        // گزینه 2: حفظ مقادیر انتخاب شده توسط کاربر در این جلسه (قبل از بارگذاری مجدد)
        // selectedIds.retainAll(employees.map { it.id })

        if (initialResponsibleId != null && employees.any { it.id == initialResponsibleId }) {
            responsibleId = initialResponsibleId
        } else {
            responsibleId = null
        }

        notifyDataSetChanged()
    }

    fun getSelectedIds(): Set<String> = selectedIds.toSet()

    fun getResponsibleId(): String? = responsibleId
}