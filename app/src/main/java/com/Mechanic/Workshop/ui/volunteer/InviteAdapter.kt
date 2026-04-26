package com.Mechanic.Workshop.ui.volunteer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.Mechanic.Workshop.R
import com.Mechanic.Workshop.data.model.Employee

class InviteAdapter(
    private var employees: List<Employee>,
    private val onSelectionChanged: (Employee, Boolean) -> Unit
) : RecyclerView.Adapter<InviteAdapter.ViewHolder>() {

    private val selectedIds = mutableSetOf<String>()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvEmployeeName)
        val cbSelect: CheckBox = view.findViewById(R.id.cbSelect)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_invite, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val employee = employees[position]
        holder.tvName.text = employee.name
        holder.cbSelect.isChecked = selectedIds.contains(employee.id)

        holder.cbSelect.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedIds.add(employee.id)
            } else {
                selectedIds.remove(employee.id)
            }
            onSelectionChanged(employee, isChecked)
        }

        holder.itemView.setOnClickListener {
            holder.cbSelect.isChecked = !holder.cbSelect.isChecked
        }
    }

    override fun getItemCount() = employees.size

    fun updateList(newList: List<Employee>) {
        employees = newList
        selectedIds.clear()
        notifyDataSetChanged()
    }
}