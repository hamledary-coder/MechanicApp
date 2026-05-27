package com.Mechanic.Workshop.ui.task.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.Mechanic.Workshop.R
import com.Mechanic.Workshop.data.model.Employee
import com.Mechanic.Workshop.data.remote.Config
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SelectGroupDialog(
    private val currentGroupIds: String,
    private val availableEmployees: List<Employee>,  // ← لیست کارمندانی که در گروه فعلی هستند
    private val onGroupSelected: (String) -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var btnConfirm: Button
    private lateinit var btnCancel: Button
    private lateinit var adapter: GroupSelectionAdapter
    private val selectedIds = mutableSetOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_select_group, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerViewGroup)
        btnConfirm = view.findViewById(R.id.btnConfirm)
        btnCancel = view.findViewById(R.id.btnCancel)

        // پر کردن selectedIds از مقدار فعلی
        if (currentGroupIds.isNotEmpty()) {
            selectedIds.addAll(currentGroupIds.split(",").map { it.trim() })
        }

        // استفاده از availableEmployees (فقط اعضای گروه فعلی)
        val users = availableEmployees.map { employee ->
            UserItem(employee.id, employee.name, selectedIds.contains(employee.id))
        }.sortedBy { it.name }

        adapter = GroupSelectionAdapter(users) { userId, isChecked ->
            if (isChecked) {
                selectedIds.add(userId)
            } else {
                selectedIds.remove(userId)
            }
        }
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        btnConfirm.setOnClickListener {
            val newGroupIds = selectedIds.joinToString(",")
            onGroupSelected(newGroupIds)
            dismiss()
        }

        btnCancel.setOnClickListener {
            dismiss()
        }
    }

    data class UserItem(val id: String, val name: String, var isSelected: Boolean)

    inner class GroupSelectionAdapter(
        private val users: List<UserItem>,
        private val onCheckedChange: (userId: String, isChecked: Boolean) -> Unit
    ) : RecyclerView.Adapter<GroupSelectionAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_group_selection, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val user = users[position]
            holder.bind(user)
        }

        override fun getItemCount() = users.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val cbSelect: CheckBox = itemView.findViewById(R.id.cbSelect)
            private val tvName: TextView = itemView.findViewById(R.id.tvName)

            fun bind(user: UserItem) {
                tvName.text = user.name
                cbSelect.isChecked = user.isSelected

                cbSelect.setOnCheckedChangeListener { _, isChecked ->
                    user.isSelected = isChecked
                    onCheckedChange(user.id, isChecked)
                }

                itemView.setOnClickListener {
                    cbSelect.isChecked = !cbSelect.isChecked
                }
            }
        }
    }
}