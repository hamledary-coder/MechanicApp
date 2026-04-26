package com.Mechanic.Workshop.ui.task.list

import TaskModel
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.Mechanic.Workshop.R
import com.Mechanic.Workshop.ui.task.list.TaskAdapter
import com.Mechanic.Workshop.ui.task.list.TaskDetailAdapter
import com.Mechanic.Workshop.data.remote.Config
import com.Mechanic.Workshop.ui.task.detail.TaskDetailActivity
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray

class WorkListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RecyclerView.Adapter<*>  // تغییر به کلاس پدر
    private var status: String? = null
    private var taskList = mutableListOf<TaskModel>()
    private lateinit var loadingLayout: LinearLayout
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    companion object {
        @JvmStatic
        fun newInstance(status: String) = WorkListFragment().apply {
            arguments = Bundle().apply {
                putString("status", status)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        status = arguments?.getString("status")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_work_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadingLayout = view.findViewById(R.id.loadingLayout)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        recyclerView = view.findViewById(R.id.recyclerViewTasks)

        recyclerView.layoutManager = LinearLayoutManager(context)

        swipeRefreshLayout.setOnRefreshListener {
            fetchTasksFromGoogleSheet()
        }
    }

    override fun onResume() {
        super.onResume()
        fetchTasksFromGoogleSheet()
    }

    private fun fetchTasksFromGoogleSheet() {
        val url = Config.Endpoints.TASKS
        hideEmptyState()

        val sharedPref = requireContext().getSharedPreferences(Config.PrefKeys.USER_PREFS, Context.MODE_PRIVATE)
        val currentUserRowId = sharedPref.getString(Config.PrefKeys.USER_ROW_ID, "") ?: ""

        if (!swipeRefreshLayout.isRefreshing) {
            loadingLayout.visibility = View.VISIBLE
        }

        val stringRequest = StringRequest(
            Request.Method.GET, url,
            { response ->
                requireActivity().runOnUiThread {
                    loadingLayout.visibility = View.GONE
                    swipeRefreshLayout.isRefreshing = false

                    try {
                        val jsonArray = JSONArray(response)

                        if (status?.trim() == "کارتابل من" || status?.trim() == "در حال انجام") {
                            fetchAndCacheUsers()
                        }

                        taskList.clear()

                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            val task = TaskModel(
                                id = obj.getString("id"),
                                createDate = obj.getString("createDate"),
                                title = obj.getString("title"),
                                description = obj.getString("description"),
                                creator = obj.getString("creator"),
                                status = obj.getString("status"),
                                assignedTo = obj.optString("assignedTo", ""),
                                responsible = obj.optString("responsible", ""),
                                pendingInvites = obj.optString("pendingInvites", ""),
                                unit = obj.optString("unit", ""),        // ✅ این خط هست؟
                                priority = obj.optString("priority", "") // ✅ این خط هست؟
                            )

                            when (status?.trim()) {
                                "انتخاب نشده" -> {
                                    if (task.status == "1") {
                                        taskList.add(task)
                                    }
                                }

                                "در حال انجام" -> {
                                    if (task.status.startsWith("2")) {
                                        taskList.add(task)
                                    }
                                }

                                "کارتابل من" -> {
                                    if (task.assignedTo.isNotEmpty()) {
                                        val assignedList =
                                            task.assignedTo.split(",").map { it.trim() }
                                        if (assignedList.contains(currentUserRowId)) {
                                            taskList.add(task)
                                        }
                                    }
                                }
                            }
                        }

                        taskList.reverse()

                        // ✅ نمایش پیام در صورت خالی بودن لیست
                        if (taskList.isEmpty()) {
                            showEmptyState()
                        } else {
                            hideEmptyState()

                            when (status?.trim()) {
                                "انتخاب نشده" -> {
                                    adapter = TaskAdapter(taskList) { clickedTask ->
                                        openTaskDetail(clickedTask)
                                    }
                                }

                                else -> {
                                    adapter = TaskDetailAdapter(taskList) { clickedTask ->
                                        openTaskDetail(clickedTask)
                                    }
                                }
                            }
                            recyclerView.adapter = adapter
                            adapter.notifyDataSetChanged()
                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(context, "خطا در پردازش اطلاعات", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            { error ->
                requireActivity().runOnUiThread {
                    loadingLayout.visibility = View.GONE
                    swipeRefreshLayout.isRefreshing = false
                    Toast.makeText(context, "خطا در اتصال به شبکه", Toast.LENGTH_SHORT).show()
                }
            })

        Volley.newRequestQueue(requireContext()).add(stringRequest)
    }

    // ✅ توابع جدید برای نمایش پیام خالی
    private fun showEmptyState() {
        val emptyTextView = view?.findViewById<TextView>(R.id.emptyStateText)
        if (emptyTextView == null) {
            // اگر TextView وجود نداره، ایجاد کن
            val emptyView = LayoutInflater.from(context).inflate(R.layout.empty_state, recyclerView.parent as ViewGroup, false)
            (recyclerView.parent as ViewGroup).addView(emptyView)
        } else {
            emptyTextView.visibility = View.VISIBLE
        }
        recyclerView.visibility = View.GONE
    }

    private fun hideEmptyState() {
        view?.findViewById<TextView>(R.id.emptyStateText)?.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
    }

    private fun openTaskDetail(task: TaskModel) {
        val intent = Intent(requireContext(), TaskDetailActivity::class.java)
        intent.putExtra("TITLE", task.title)
        intent.putExtra("DESC", task.description)
        intent.putExtra("CREATOR", task.creator)  // اینو فعلاً بذار بمونه
        intent.putExtra("DATE", task.createDate)
        intent.putExtra("TASK_ID", task.id)
        intent.putExtra("RESPONSIBLE", task.responsible)
        intent.putExtra("UNIT", task.unit)
        intent.putExtra("PRIORITY", task.priority)
        startActivity(intent)
    }

    private fun fetchAndCacheUsers() {
        val url = "${Config.Endpoints.TASKS}?action=getEmployees"

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val usersArray = JSONArray(response)
                    for (i in 0 until usersArray.length()) {
                        val obj = usersArray.getJSONObject(i)
                        val rowId = obj.getString("rowId")
                        val name = obj.getString("name")
                        Config.UserCache.userMap[rowId] = name
                    }
                } catch (e: Exception) {
                    Log.e("UserCache", "Error caching users: ${e.message}")
                }
            },
            { error ->
                Log.e("UserCache", "Network error: ${error.message}")
            })

        Volley.newRequestQueue(requireContext()).add(request)
    }
}