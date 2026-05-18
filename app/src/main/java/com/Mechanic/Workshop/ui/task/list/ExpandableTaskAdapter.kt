package com.Mechanic.Workshop.ui.task.list

import TaskModel
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.Mechanic.Workshop.R
import com.Mechanic.Workshop.data.remote.Config

class ExpandableTaskAdapter(
    private val tasks: List<TaskModel>,
    private val tabType: String,  // "unassigned", "inProgress", "myCartable"
    private val onEditClick: (TaskModel) -> Unit,
    private val onDeleteClick: (TaskModel) -> Unit,
    private val onReferClick: (TaskModel) -> Unit,
    private val onVolunteerClick: (TaskModel) -> Unit,
    private val onItemClick: ((TaskModel) -> Unit)? = null
) : RecyclerView.Adapter<ExpandableTaskAdapter.ViewHolder>() {

    private val expandedPosition = mutableSetOf<Int>()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Header
        val tvId: TextView = itemView.findViewById(R.id.tvTaskId)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTaskTitle)
        val ivExpand: ImageView = itemView.findViewById(R.id.ivExpand)
        val divider: View = itemView.findViewById(R.id.divider)
        val detailLayout: View = itemView.findViewById(R.id.detailLayout)
        //val headerLayout: View = itemView.findViewById(R.id.headerLayout)

        // Detail - فیلدهای اصلی
        val tvDescription: TextView = itemView.findViewById(R.id.tvTaskDescription)
        val tvResponsible: TextView = itemView.findViewById(R.id.tvTaskResponsible)
        val tvAssignees: TextView = itemView.findViewById(R.id.tvTaskAssignees)
        val tvUnit: TextView = itemView.findViewById(R.id.tvTaskUnit)
        val tvPriority: TextView = itemView.findViewById(R.id.tvTaskPriority)

        // Detail - فیلدهای جدید
        val tvSubUnit: TextView = itemView.findViewById(R.id.tvTaskSubUnit)
        val tvRequestDate: TextView = itemView.findViewById(R.id.tvRequestDate)
        val tvRequester: TextView = itemView.findViewById(R.id.tvRequester)
        val tvDeclarationMethod: TextView = itemView.findViewById(R.id.tvDeclarationMethod)
        val tvSystemNumber: TextView = itemView.findViewById(R.id.tvSystemNumber)
        val tvInitialReview: TextView = itemView.findViewById(R.id.tvInitialReview)

        // آیکون‌های عملیاتی
        val icEdit: ImageView = itemView.findViewById(R.id.icEdit)
        val icDelete: ImageView = itemView.findViewById(R.id.icDelete)
        val icRefer: ImageView = itemView.findViewById(R.id.icRefer)
        val icVolunteer: ImageView = itemView.findViewById(R.id.icVolunteer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task_expandable, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val task = tasks[position]
        val isExpanded = expandedPosition.contains(position)

        // تنظیم اطلاعات header
        holder.tvId.text = "#${task.id}"
        holder.tvTitle.text = task.title

        if (isExpanded) {
            // حالت باز
            holder.ivExpand.setImageResource(R.drawable.ic_chevron_up)
            holder.divider.visibility = View.VISIBLE
            holder.detailLayout.visibility = View.VISIBLE

            // آیکون‌های عملیاتی را نمایش بده
            holder.icEdit.visibility = View.VISIBLE
            holder.icDelete.visibility = View.VISIBLE
            holder.icRefer.visibility = View.VISIBLE
            holder.icVolunteer.visibility = View.VISIBLE

            showAllFields(holder, task)

        } else {
            // حالت بسته
            holder.ivExpand.setImageResource(R.drawable.ic_chevron_down)
            holder.divider.visibility = View.VISIBLE

            // آیکون‌های عملیاتی را مخفی کن
            holder.icEdit.visibility = View.GONE
            holder.icDelete.visibility = View.GONE
            holder.icRefer.visibility = View.GONE
            holder.icVolunteer.visibility = View.GONE

            when (tabType) {
                "unassigned" -> {
                    holder.detailLayout.visibility = View.GONE
                }
                "inProgress", "myCartable" -> {
                    holder.detailLayout.visibility = View.VISIBLE
                    showCollapsedFields(holder, task)
                }
                else -> {
                    holder.detailLayout.visibility = View.GONE
                }
            }
        }

        // آیکون‌های عملیاتی (کلیک‌ها)
        holder.icEdit.setOnClickListener { onEditClick(task) }
        holder.icDelete.setOnClickListener { onDeleteClick(task) }
        holder.icRefer.setOnClickListener { onReferClick(task) }
        holder.icVolunteer.setOnClickListener { onVolunteerClick(task) }

        // ✅ فقط کلیک روی هدر (headerLayout) برای باز و بسته شدن
        holder.itemView.findViewById<View>(R.id.headerLayout).setOnClickListener {
            if (isExpanded) {
                expandedPosition.remove(position)
            } else {
                expandedPosition.clear()
                expandedPosition.add(position)
            }
            notifyDataSetChanged()
        }

        // کلیک روی کل آیتم (فقط برای تب‌های در حال انجام و کارتابل من)
        if (tabType == "inProgress" || tabType == "myCartable") {
            holder.itemView.setOnClickListener {
                onItemClick?.invoke(task)
            }
        }
    }

    /**
     * نمایش فیلدهای محدود در حالت بسته (برای تب‌های در حال انجام و کارتابل من)
     */
    private fun showCollapsedFields(holder: ViewHolder, task: TaskModel) {
        // مخفی کردن همه فیلدها اول
        holder.tvDescription.visibility = View.GONE
        holder.tvAssignees.visibility = View.GONE
        holder.tvSubUnit.visibility = View.GONE
        holder.tvRequestDate.visibility = View.GONE
        holder.tvRequester.visibility = View.GONE
        holder.tvDeclarationMethod.visibility = View.GONE
        holder.tvSystemNumber.visibility = View.GONE
        holder.tvInitialReview.visibility = View.GONE

        // فقط مسئول و واحد و اولویت را نشان بده
        // مسئول
        if (task.responsible.isNotEmpty()) {
            val responsibleName = Config.UserCache.userMap[task.responsible] ?: "کاربر ${task.responsible}"
            holder.tvResponsible.text = "مسئول: $responsibleName"
            holder.tvResponsible.visibility = View.VISIBLE
        } else {
            holder.tvResponsible.visibility = View.GONE
        }

        // انجام‌دهندگان
        if (task.assignedTo.isNotEmpty()) {
            val assigneeNames = task.assignedTo.split(",").map {
                Config.UserCache.userMap[it.trim()] ?: "کاربر $it"
            }
            holder.tvAssignees.text = "گروه: ${assigneeNames.joinToString("، ")}"
            holder.tvAssignees.visibility = View.VISIBLE
        } else {
            holder.tvAssignees.visibility = View.GONE
        }

        //---واحد---
        if (task.unit.isNotEmpty() && task.unit != "null") {
            val unitText = Config.UnitCode.getText(task.unit)
            if (unitText.isNotEmpty()) {
                holder.tvUnit.text = "واحد: $unitText"
                holder.tvUnit.visibility = View.VISIBLE
            } else {
                holder.tvUnit.visibility = View.GONE
            }
        } else {
            holder.tvUnit.visibility = View.GONE
        }

        // فوریت (urgency) - فقط مقادیر غیر از "عادی" را نمایش بده
        if (task.urgency.isNotEmpty() && task.urgency != "null" && task.urgency != "عادی") {
            holder.tvPriority.text = "فوریت: ${task.urgency}"
            holder.tvPriority.visibility = View.VISIBLE
        } else {
            holder.tvPriority.visibility = View.GONE
        }
    }

    /**
     * نمایش همه فیلدها در حالت باز (برای همه تب‌ها)
     */
    private fun showAllFields(holder: ViewHolder, task: TaskModel) {
        // توضیحات
        holder.tvDescription.text = task.description.ifEmpty { "توضیحاتی وارد نشده" }
        holder.tvDescription.visibility = View.VISIBLE

        // مسئول
        if (task.responsible.isNotEmpty()) {
            val responsibleName = Config.UserCache.userMap[task.responsible] ?: "کاربر ${task.responsible}"
            holder.tvResponsible.text = "مسئول انجام کار: $responsibleName"
            holder.tvResponsible.visibility = View.VISIBLE
        } else {
            holder.tvResponsible.visibility = View.GONE
        }

        // انجام‌دهندگان
        if (task.assignedTo.isNotEmpty()) {
            val assigneeNames = task.assignedTo.split(",").map {
                Config.UserCache.userMap[it.trim()] ?: "کاربر $it"
            }
            holder.tvAssignees.text = "گروه انجام دهنده: ${assigneeNames.joinToString("، ")}"
            holder.tvAssignees.visibility = View.VISIBLE
        } else {
            holder.tvAssignees.visibility = View.GONE
        }

        // واحد
        if (task.unit.isNotEmpty() && task.unit != "null") {
            val unitText = Config.UnitCode.getText(task.unit)
            if (unitText.isNotEmpty()) {
                holder.tvUnit.text = "واحد: $unitText"
                holder.tvUnit.visibility = View.VISIBLE
            } else {
                holder.tvUnit.visibility = View.GONE
            }
        } else {
            holder.tvUnit.visibility = View.GONE
        }

        // زیرمجموعه
        if (task.sub_unit.isNotEmpty()) {
            holder.tvSubUnit.text = "زیرمجموعه: ${task.sub_unit}"
            holder.tvSubUnit.visibility = View.VISIBLE
        } else {
            holder.tvSubUnit.visibility = View.GONE
        }

        // فوریت (urgency) - فقط مقادیر غیر از "عادی" را نمایش بده
        if (task.urgency.isNotEmpty() && task.urgency != "null" && task.urgency != "عادی") {
            holder.tvPriority.text = "فوریت: ${task.urgency}"
            holder.tvPriority.visibility = View.VISIBLE
        } else {
            holder.tvPriority.visibility = View.GONE
        }

        // تاریخ اعلام
        if (task.request_date.isNotEmpty()) {
            val displayDate = task.request_date.replace("-", "/")
            holder.tvRequestDate.text = "تاریخ اعلام: $displayDate"
            holder.tvRequestDate.visibility = View.VISIBLE
        } else {
            holder.tvRequestDate.visibility = View.GONE
        }

        // صادرکننده
        if (task.requester.isNotEmpty()) {
            holder.tvRequester.text = "صادرکننده: ${task.requester}"
            holder.tvRequester.visibility = View.VISIBLE
        } else {
            holder.tvRequester.visibility = View.GONE
        }

        // نحوه اعلام
        if (task.declaration_method.isNotEmpty()) {
            holder.tvDeclarationMethod.text = "نحوه اعلام: ${task.declaration_method}"
            holder.tvDeclarationMethod.visibility = View.VISIBLE
        } else {
            holder.tvDeclarationMethod.visibility = View.GONE
        }

        // شماره سامانه
        if (task.system_request_number.isNotEmpty()) {
            holder.tvSystemNumber.text = "شماره سامانه: ${task.system_request_number}"
            holder.tvSystemNumber.visibility = View.VISIBLE
        } else {
            holder.tvSystemNumber.visibility = View.GONE
        }

        // بررسی اولیه
        if (task.initial_review.isNotEmpty()) {
            holder.tvInitialReview.text = "بررسی اولیه: ${task.initial_review}"
            holder.tvInitialReview.visibility = View.VISIBLE
        } else {
            holder.tvInitialReview.visibility = View.GONE
        }

    }
    override fun getItemCount() = tasks.size

    private fun setPriorityColor(textView: TextView, priorityCode: String) {
        val color = when (priorityCode) {
            Config.PriorityCode.EMERGENCY -> "#F44336"
            Config.PriorityCode.HIGH -> "#FF9800"
            Config.PriorityCode.LOW -> "#4CAF50"
            else -> "#9E9E9E"
        }
        textView.setTextColor(android.graphics.Color.parseColor(color))
    }
}