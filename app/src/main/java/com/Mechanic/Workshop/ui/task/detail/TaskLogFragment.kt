package com.Mechanic.Workshop.ui.task.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.Mechanic.Workshop.R

class TaskLogFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var etLog: EditText
    private lateinit var btnSubmit: Button
    private var taskId: String? = null
    private var logList = mutableListOf<TaskLog>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_task_log, container, false)

        recyclerView = view.findViewById(R.id.recyclerLogs)
        etLog = view.findViewById(R.id.etNewLog)
        btnSubmit = view.findViewById(R.id.btnSubmitLog)

        recyclerView.layoutManager = LinearLayoutManager(context)

        taskId = arguments?.getString("TASK_ID")

        loadLogs()

        btnSubmit.setOnClickListener {
            val logText = etLog.text.toString().trim()
            if (logText.isNotEmpty()) {
                submitLog(logText)
            }
        }

        return view
    }

    private fun loadLogs() {
        // TODO: دریافت گزارش‌ها از سرور
    }

    private fun submitLog(logText: String) {
        // TODO: ارسال گزارش به سرور
    }

    companion object {
        fun newInstance(taskId: String): TaskLogFragment {
            val fragment = TaskLogFragment()
            val args = Bundle()
            args.putString("TASK_ID", taskId)
            fragment.arguments = args
            return fragment
        }
    }
}

data class TaskLog(
    val id: String,
    val userId: String,
    val date: String,
    val text: String
)