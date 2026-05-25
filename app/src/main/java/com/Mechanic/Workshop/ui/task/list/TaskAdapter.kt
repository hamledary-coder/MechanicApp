package com.Mechanic.Workshop.ui.task.list

import TaskModel
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.Mechanic.Workshop.R
import com.Mechanic.Workshop.data.remote.Config

class TaskAdapter(
    private val tasks: List<TaskModel>,
    private val onItemClick: (TaskModel) -> Unit

) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvId: TextView = view.findViewById(R.id.tvTaskId)
        val tvTitle: TextView = view.findViewById(R.id.tvTaskTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task_unassigned, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.tvId.text = task.id
        holder.tvTitle.text = task.title
        holder.itemView.setOnClickListener { onItemClick(task) }
    }

    override fun getItemCount() = tasks.size
}

class TaskDetailAdapter(
    private val tasks: List<TaskModel>,
    private val onItemClick: (TaskModel) -> Unit
) : RecyclerView.Adapter<TaskDetailAdapter.TaskDetailViewHolder>() {

    class TaskDetailViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // ✅ آیدی‌ها دقیقاً مطابق XML
        val tvTaskId: TextView = view.findViewById(R.id.tvTaskId)
        val tvTaskTitle: TextView = view.findViewById(R.id.tvTaskTitle)
        val tvTaskResponsible: TextView = view.findViewById(R.id.tvTaskResponsible)
        val tvTaskAssignees: TextView = view.findViewById(R.id.tvTaskAssignees)
        val tvTaskStatus: TextView = view.findViewById(R.id.tvTaskStatus)
        val tvTaskUnit: TextView = view.findViewById(R.id.tvTaskUnit)
        val tvTaskPriority: TextView = view.findViewById(R.id.tvTaskPriority)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskDetailViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task_detail, parent, false)
        return TaskDetailViewHolder(view)
    }

    // در TaskDetailAdapter.kt - تابع onBindViewHolder

    override fun onBindViewHolder(holder: TaskDetailViewHolder, position: Int) {
        val task = tasks[position]

        holder.tvTaskId.text = task.id
        holder.tvTaskTitle.text = task.title

        // مسئول انجام کار - تبدیل کد ردیف به اسم
        if (task.responsible.isNotEmpty()) {
            val responsibleName = Config.UserCache.userMap[task.responsible]
            if (responsibleName != null) {
                holder.tvTaskResponsible.text = responsibleName
            } else {
                holder.tvTaskResponsible.text = "کاربر ${task.responsible}"
                // TODO: بعداً fetchUserName اضافه میشه
            }
        } else {
            holder.tvTaskResponsible.text = "تعیین نشده"
        }

        // گروه انجام دهنده - تبدیل کدهای ردیف به اسم
        if (task.assignedTo.isNotEmpty()) {
            val assigneeIds = task.assignedTo.split(",").map { it.trim() }
            val assigneeNames = assigneeIds.map {
                Config.UserCache.userMap[it] ?: "کاربر $it"
            }
            holder.tvTaskAssignees.text = assigneeNames.joinToString("، ")
        } else {
            holder.tvTaskAssignees.text = "تعیین نشده"
        }

        // آخرین وضعیت - تبدیل کد به متن
        holder.tvTaskStatus.text = Config.StatusCode.getText(task.status)
        setStatusColor(holder.tvTaskStatus, task.status)

        // واحد مربوطه
        if (task.unit.isNotEmpty()) {
            holder.tvTaskUnit.text = Config.UnitCode.getText(task.unit)
            holder.tvTaskUnit.visibility = View.VISIBLE
        } else {
            holder.tvTaskUnit.visibility = View.GONE
        }

        // درجه اهمیت
        if (task.priority.isNotEmpty()) {
            holder.tvTaskPriority.text = Config.PriorityCode.getText(task.priority)
            holder.tvTaskPriority.visibility = View.VISIBLE
            setPriorityColor(holder.tvTaskPriority, task.priority)
        } else {
            holder.tvTaskPriority.visibility = View.GONE
        }
        // رنگ‌آمیزی پس‌زمینه کل آیتم بر اساس priority
        when (task.priority) {
            Config.PriorityCode.EMERGENCY -> {
                holder.itemView.setBackgroundColor(Color.parseColor("#FFEBEE"))  // قرمز خیلی کم‌رنگ
            }
            Config.PriorityCode.HIGH -> {
                holder.itemView.setBackgroundColor(Color.parseColor("#FFF3E0"))  // نارنجی کم‌رنگ
            }
            Config.PriorityCode.LOW -> {
                holder.itemView.setBackgroundColor(Color.parseColor("#E8F5E9"))  // سبز کم‌رنگ
            }
            else -> {
                holder.itemView.setBackgroundColor(Color.WHITE)  // سفید
            }
        }
    }

    private fun setStatusColor(textView: TextView, statusCode: String) {
        val background = textView.background
        when (statusCode.firstOrNull()) {
            '2' -> background.setTint(Color.parseColor("#2196F3"))  // در حال انجام
            '3' -> background.setTint(Color.parseColor("#FF9800"))  // متوقف
            '4' -> background.setTint(Color.parseColor("#4CAF50"))  // انجام شده
            else -> background.setTint(Color.parseColor("#757575")) // اقدام نشده
        }
    }

    private fun setPriorityColor(textView: TextView, priorityCode: String) {
        val background = textView.background
        when (priorityCode) {
            Config.PriorityCode.EMERGENCY -> background.setTint(Color.parseColor("#F44336"))  // قرمز
            Config.PriorityCode.HIGH -> background.setTint(Color.parseColor("#FF9800"))       // نارنجی
            Config.PriorityCode.LOW -> background.setTint(Color.parseColor("#4CAF50"))        // سبز
            else -> background.setTint(Color.parseColor("#9E9E9E"))                           // خاکستری
        }
    }

    override fun getItemCount() = tasks.size
}