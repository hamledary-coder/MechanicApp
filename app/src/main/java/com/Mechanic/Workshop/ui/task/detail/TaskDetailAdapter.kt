package com.Mechanic.Workshop.ui.task.detail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.Mechanic.Workshop.R
import com.Mechanic.Workshop.data.model.TaskLogModel
import com.Mechanic.Workshop.data.remote.Config
import TaskModel
import android.widget.PopupMenu

class TaskDetailAdapter(
    private val task: TaskModel,                        // اطلاعات ثابت کار
    private val logs: List<TaskLogModel>,               // لیست گزارش‌ها
    private val onEditLogClick: (TaskLogModel) -> Unit, // ویرایش گزارش
    private val onDeleteLogClick: (TaskLogModel) -> Unit, // حذف گزارش
    private val onAddLogClick: () -> Unit               // ثبت گزارش جدید
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_TASK_INFO = 0
        private const val TYPE_TASK_LOG = 1
        private const val TYPE_ADD_LOG = 2
    }

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> TYPE_TASK_INFO
            logs.size + 1 -> TYPE_ADD_LOG
            else -> TYPE_TASK_LOG
        }
    }

    override fun getItemCount(): Int = logs.size + 2  // 1 (info) + تعداد گزارش‌ها + 1 (دکمه اضافه)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_TASK_INFO -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_task_info_static, parent, false)
                TaskInfoViewHolder(view)
            }
            TYPE_TASK_LOG -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_task_log, parent, false)
                TaskLogViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_add_log, parent, false)
                AddLogViewHolder(view, onAddLogClick)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is TaskInfoViewHolder -> holder.bind(task)
            is TaskLogViewHolder -> {
                val log = logs[position - 1]  // زیرا position 0 مربوط به info است
                holder.bind(log, onEditLogClick, onDeleteLogClick)
            }
            // AddLogViewHolder نیازی به bind ندارد (کلیک در سازنده تنظیم شده)
        }
    }

    // ViewHolder برای شرح کار (بدون باکس)
    class TaskInfoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvId: TextView = itemView.findViewById(R.id.tvTaskId)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTaskTitle)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvTaskDescription)
        private val tvResponsible: TextView = itemView.findViewById(R.id.tvTaskResponsible)
        private val tvAssignees: TextView = itemView.findViewById(R.id.tvTaskAssignees)
        private val tvUnit: TextView = itemView.findViewById(R.id.tvTaskUnit)
        private val tvPriority: TextView = itemView.findViewById(R.id.tvTaskPriority)
        private val tvRequestDate: TextView = itemView.findViewById(R.id.tvRequestDate)
        private val tvRequester: TextView = itemView.findViewById(R.id.tvRequester)
        private val tvDeclarationMethod: TextView = itemView.findViewById(R.id.tvDeclarationMethod)
        private val tvSystemNumber: TextView = itemView.findViewById(R.id.tvSystemNumber)
        private val tvInitialReview: TextView = itemView.findViewById(R.id.tvInitialReview)

        fun bind(task: TaskModel) {
            tvId.text = "شماره کار: ${task.id}"
            tvTitle.text = "عنوان: ${task.title}"
            tvDescription.text = "شرح: ${task.description.ifEmpty { "توضیحاتی وارد نشده" }}"

            // مسئول
            if (task.responsible.isNotEmpty()) {
                val responsibleName = Config.UserCache.userMap[task.responsible] ?: "کاربر ${task.responsible}"
                tvResponsible.text = "مسئول: $responsibleName"
                tvResponsible.visibility = View.VISIBLE
            } else {
                tvResponsible.visibility = View.GONE
            }

            // گروه انجام‌دهنده
            if (task.assignedTo.isNotEmpty()) {
                val assigneeNames = task.assignedTo.split(",").map {
                    Config.UserCache.userMap[it.trim()] ?: "کاربر $it"
                }
                tvAssignees.text = "گروه: ${assigneeNames.joinToString("، ")}"
                tvAssignees.visibility = View.VISIBLE
            } else {
                tvAssignees.visibility = View.GONE
            }

            // واحد
            if (task.unit.isNotEmpty() && task.unit != "null") {
                val unitText = Config.UnitCode.getText(task.unit)
                if (unitText.isNotEmpty()) {
                    tvUnit.text = "واحد: $unitText"
                    tvUnit.visibility = View.VISIBLE
                } else {
                    tvUnit.visibility = View.GONE
                }
            } else {
                tvUnit.visibility = View.GONE
            }

            // اولویت
            if (task.priority.isNotEmpty()) {
                tvPriority.text = "اهمیت: ${Config.PriorityCode.getText(task.priority)}"
                tvPriority.visibility = View.VISIBLE
            } else {
                tvPriority.visibility = View.GONE
            }

            // تاریخ اعلام
            if (task.request_date.isNotEmpty()) {
                val displayDate = task.request_date.replace("-", "/")
                tvRequestDate.text = "تاریخ اعلام: $displayDate"
                tvRequestDate.visibility = View.VISIBLE
            } else {
                tvRequestDate.visibility = View.GONE
            }

            // صادرکننده
            if (task.requester.isNotEmpty()) {
                tvRequester.text = "صادرکننده: ${task.requester}"
                tvRequester.visibility = View.VISIBLE
            } else {
                tvRequester.visibility = View.GONE
            }

            // نحوه اعلام
            if (task.declaration_method.isNotEmpty()) {
                tvDeclarationMethod.text = "نحوه اعلام: ${task.declaration_method}"
                tvDeclarationMethod.visibility = View.VISIBLE
            } else {
                tvDeclarationMethod.visibility = View.GONE
            }

            // شماره سامانه
            if (task.system_request_number.isNotEmpty()) {
                tvSystemNumber.text = "شماره سامانه: ${task.system_request_number}"
                tvSystemNumber.visibility = View.VISIBLE
            } else {
                tvSystemNumber.visibility = View.GONE
            }

            // بررسی اولیه
            if (task.initial_review.isNotEmpty()) {
                tvInitialReview.text = "بررسی اولیه: ${task.initial_review}"
                tvInitialReview.visibility = View.VISIBLE
            } else {
                tvInitialReview.visibility = View.GONE
            }
        }
    }

    // ViewHolder برای هر گزارش (با قابلیت اکسپند و سه نقطه)
    class TaskLogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvLogSummary: TextView = itemView.findViewById(R.id.tvLogSummary)
        private val ivExpand: ImageView = itemView.findViewById(R.id.ivExpand)
        private val ivMenu: ImageView = itemView.findViewById(R.id.ivMenu)
        private val divider: View = itemView.findViewById(R.id.divider)
        private val detailLayout: View = itemView.findViewById(R.id.detailLayout)

        private val tvActionDescription: TextView = itemView.findViewById(R.id.tvActionDescription)
        private val tvParts: TextView = itemView.findViewById(R.id.tvParts)
        private val tvWorkers: TextView = itemView.findViewById(R.id.tvWorkers)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        private val tvNewStatus: TextView = itemView.findViewById(R.id.tvNewStatus)
        private val tvLogNotes: TextView = itemView.findViewById(R.id.tvLogNotes)

        private var isExpanded = false

        fun bind(log: TaskLogModel, onEdit: (TaskLogModel) -> Unit, onDelete: (TaskLogModel) -> Unit) {
            // خلاصه گزارش
            tvLogSummary.text = "گزارش ${log.date} - ${log.userName}"

            // پر کردن جزئیات (برای زمان باز شدن)
            tvActionDescription.text = "شرح اقدام: ${log.actionDescription}"

            if (log.consumedParts.isNotEmpty()) {
                tvParts.text = "قطعات مصرفی: ${log.consumedParts}"
                tvParts.visibility = View.VISIBLE
            } else {
                tvParts.visibility = View.GONE
            }

            if (log.assignedUsers.isNotEmpty()) {
                val workerNames = log.assignedUsers.split(",").mapNotNull {
                    Config.UserCache.userMap[it.trim()]
                }
                tvWorkers.text = "نیروی انسانی: ${workerNames.joinToString("، ")}"
                tvWorkers.visibility = View.VISIBLE
            } else {
                tvWorkers.visibility = View.GONE
            }

            if (log.startTime.isNotEmpty() || log.endTime.isNotEmpty()) {
                tvTime.text = "زمان: ${log.startTime} - ${log.endTime}"
                tvTime.visibility = View.VISIBLE
            } else {
                tvTime.visibility = View.GONE
            }

            if (log.newStatus.isNotEmpty()) {
                tvNewStatus.text = "وضعیت جدید: ${Config.StatusCode.getText(log.newStatus)}"
                tvNewStatus.visibility = View.VISIBLE
            } else {
                tvNewStatus.visibility = View.GONE
            }

            if (log.notes.isNotEmpty()) {
                tvLogNotes.text = "توضیحات: ${log.notes}"
                tvLogNotes.visibility = View.VISIBLE
            } else {
                tvLogNotes.visibility = View.GONE
            }

            // تنظیم حالت اکسپند
            setExpanded(isExpanded)

            // کلیک روی هدر برای باز و بسته شدن
            itemView.findViewById<View>(R.id.headerLayout).setOnClickListener {
                isExpanded = !isExpanded
                setExpanded(isExpanded)
            }

            // سه نقطه - همیشه نمایش داده می‌شود با PopupMenu
            ivMenu.visibility = View.VISIBLE
            ivMenu.setOnClickListener { view ->
                PopupMenu(view.context, view).apply {
                    menu.add(0, 1, 0, "ویرایش")
                    menu.add(0, 2, 0, "حذف")
                    setOnMenuItemClickListener { menuItem ->
                        when (menuItem.itemId) {
                            1 -> onEdit(log)
                            2 -> onDelete(log)
                        }
                        true
                    }
                    show()
                }
            }
        }

        private fun setExpanded(expanded: Boolean) {
            if (expanded) {
                ivExpand.setImageResource(R.drawable.ic_chevron_up)
                divider.visibility = View.VISIBLE
                detailLayout.visibility = View.VISIBLE
            } else {
                ivExpand.setImageResource(R.drawable.ic_chevron_down)
                divider.visibility = View.GONE
                detailLayout.visibility = View.GONE
            }
        }
    }

    // ViewHolder برای دکمه ثبت گزارش جدید
    class AddLogViewHolder(itemView: View, onClick: () -> Unit) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.setOnClickListener { onClick() }
        }
    }
}