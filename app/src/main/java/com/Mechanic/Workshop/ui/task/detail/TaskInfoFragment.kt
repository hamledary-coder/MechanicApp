package com.Mechanic.Workshop.ui.task.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.Mechanic.Workshop.R
import com.Mechanic.Workshop.data.remote.Config

class TaskInfoFragment : Fragment() {

    private lateinit var tvTaskId: TextView
    private lateinit var tvTaskTitle: TextView
    private lateinit var tvTaskDescription: TextView
    private lateinit var tvTaskCreator: TextView
    private lateinit var tvTaskDate: TextView
    private lateinit var tvTaskResponsible: TextView
    private lateinit var tvTaskAssignees: TextView
    private lateinit var tvTaskUnit: TextView
    private lateinit var tvTaskPriority: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_task_info, container, false)

        tvTaskId = view.findViewById(R.id.tvTaskId)
        tvTaskTitle = view.findViewById(R.id.tvTaskTitle)
        tvTaskDescription = view.findViewById(R.id.tvTaskDescription)
        tvTaskCreator = view.findViewById(R.id.tvTaskCreator)
        tvTaskDate = view.findViewById(R.id.tvTaskDate)
        tvTaskResponsible = view.findViewById(R.id.tvTaskResponsible)
        tvTaskAssignees = view.findViewById(R.id.tvTaskAssignees)
        tvTaskUnit = view.findViewById<TextView>(R.id.tvTaskUnit)
        tvTaskPriority = view.findViewById<TextView>(R.id.tvTaskPriority)


        // دریافت اطلاعات از intent
        arguments?.let {
            tvTaskId.text = "شماره کار: ${it.getString("TASK_ID")}"
            tvTaskTitle.text = it.getString("TITLE")
            tvTaskDescription.text = it.getString("DESC")

            // ✅ تبدیل کد ردیف به اسم
            val creatorRowId = it.getString("CREATOR") ?: ""
            val creatorName = Config.UserCache.userMap[creatorRowId] ?: "کاربر $creatorRowId"
            tvTaskCreator.text = "ایجاد کننده: $creatorName"  // کوچکتر شد

            tvTaskDate.text = "تاریخ ایجاد: ${it.getString("DATE")}"

            // مسئول و انجام‌دهندگان
            val responsible = it.getString("RESPONSIBLE")
            if (!responsible.isNullOrEmpty()) {
                val responsibleName = Config.UserCache.userMap[responsible] ?: "کاربر $responsible"
                tvTaskResponsible.text = "مسئول: $responsibleName"
                tvTaskResponsible.visibility = View.VISIBLE
            }

            // واحد و اهمیت
            val unitCode = it.getString("UNIT") ?: ""
            val priorityCode = it.getString("PRIORITY") ?: ""

            if (unitCode.isNotEmpty()) {
                tvTaskUnit.text = "واحد: ${Config.UnitCode.getText(unitCode)}"
                tvTaskUnit.visibility = View.VISIBLE
            } else {
                tvTaskUnit.visibility = View.GONE
            }

            if (priorityCode.isNotEmpty()) {
                tvTaskPriority.text = "اهمیت: ${Config.PriorityCode.getText(priorityCode)}"
                tvTaskPriority.visibility = View.VISIBLE
            } else {
                tvTaskPriority.visibility = View.GONE
            }
        }

        return view
    }

    companion object {
        fun newInstance(
            taskId: String,
            title: String,
            desc: String,
            creator: String,
            date: String,
            responsible: String,
            unit: String,
            priority: String
        ): TaskInfoFragment {
            val fragment = TaskInfoFragment()
            val args = Bundle()
            args.putString("TASK_ID", taskId)
            args.putString("TITLE", title)
            args.putString("DESC", desc)
            args.putString("CREATOR", creator)
            args.putString("DATE", date)
            args.putString("RESPONSIBLE", responsible)
            args.putString("UNIT", unit)
            args.putString("PRIORITY", priority)
            fragment.arguments = args
            return fragment
        }
    }
}