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
    private val onSelectionChanged: (Employee, Boolean, Boolean) -> Unit
) : RecyclerView.Adapter<EmployeeSelectionAdapter.ViewHolder>() {

    private val selectedIds = mutableSetOf<String>()
    private var responsibleId: String? = null

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

        // جدا کردن listener‌ها قبل از تنظیم وضعیت
        holder.cbSelect.setOnCheckedChangeListener(null)
        holder.rbResponsible.setOnCheckedChangeListener(null)

        // تنظیم وضعیت
        val isSelected = selectedIds.contains(employee.id)
        val isResponsible = responsibleId == employee.id

        holder.cbSelect.isChecked = isSelected
        holder.rbResponsible.isChecked = isResponsible
        holder.rbResponsible.isEnabled = isSelected

        // listener چک‌باکس
        holder.cbSelect.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedIds.add(employee.id)
                onSelectionChanged(employee, true, false)
            } else {
                selectedIds.remove(employee.id)
                // اگر مسئول بود، مسئولیتش را حذف کن
                val wasResponsible = (responsibleId == employee.id)

                if (wasResponsible) {
                    responsibleId = null
                    holder.rbResponsible.isChecked = false
                    onSelectionChanged(employee, false, false)
                    onSelectionChanged(employee, false, true) // این خط مهمه!
                } else {
                    onSelectionChanged(employee, false, false)
                }

                if (responsibleId == employee.id) {
                    responsibleId = null
                    holder.rbResponsible.isChecked = false
                    // اطلاع دهید که مسئول حذف شد
                    onSelectionChanged(employee, false, true)
                    // آپدیت کل لیست برای نمایش درست رادیوباتن‌ها
                    notifyDataSetChanged()
                } else {
                    onSelectionChanged(employee, false, false)
                }
            }
            // آپدیت وضعیت رادیوباتن
            holder.rbResponsible.isEnabled = isChecked
        }

        // listener رادیوباتن
        holder.rbResponsible.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // شخص قبلی که مسئول بود را ذخیره کن
                val prevResponsible = responsibleId
                responsibleId = employee.id

                // اگر قبلاً کسی مسئول بود، آن را آپدیت کن
                if (prevResponsible != null) {
                    notifyDataSetChanged() // کل لیست رو آپدیت کن
                }

                onSelectionChanged(employee, true, true) // این شخص مسئول شد
            } else {
                // فقط اگر این شخص مسئول فعلی است، آن را خاموش کن
                if (responsibleId == employee.id) {
                    responsibleId = null
                    onSelectionChanged(employee, true, false) // مسئول حذف شد
                }
            }
        }

        // کلیک روی کل آیتم
        holder.itemView.setOnClickListener {
            holder.cbSelect.isChecked = !holder.cbSelect.isChecked
        }
    }

    override fun getItemCount() = employees.size

    fun updateList(newList: List<Employee>) {
        employees = newList
        notifyDataSetChanged()
    }
}