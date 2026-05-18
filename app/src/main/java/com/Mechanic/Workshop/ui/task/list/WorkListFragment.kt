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
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.Mechanic.Workshop.R
import com.Mechanic.Workshop.ui.task.list.TaskAdapter
import com.Mechanic.Workshop.data.remote.Config
import com.Mechanic.Workshop.ui.task.create.CreateTaskActivity
import com.Mechanic.Workshop.ui.task.repository.TaskRepository
import com.Mechanic.Workshop.ui.task.dialog.InviteDialog
import com.Mechanic.Workshop.ui.referral.ReferDialog
import com.Mechanic.Workshop.ui.task.detail.TaskDetailActivity
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray

class WorkListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RecyclerView.Adapter<*>
    private var status: String? = null
    private var taskList = mutableListOf<TaskModel>()
    private lateinit var loadingLayout: LinearLayout
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var taskRepository: TaskRepository
    //private val onItemClick: ((TaskModel) -> Unit)? = null

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
        taskRepository = TaskRepository(requireContext())
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
            fetchTasks()
        }
    }

    override fun onResume() {
        super.onResume()
        fetchTasks()
    }

    fun refreshTasks() {
        fetchTasks()
    }

    private fun fetchTasks() {
        val timestamp = System.currentTimeMillis()
        val url = "${Config.Endpoints.TASKS}&_=$timestamp"
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
                        fetchAndCacheUsers()

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
                                unit = obj.optString("unit", ""),
                                priority = obj.optString("priority", ""),
                                // فیلدهای جدید
                                sub_unit = obj.optString("sub_unit", ""),
                                declaration_method = obj.optString("declaration_method", ""),
                                requester = obj.optString("requester", ""),
                                request_date = obj.optString("request_date", ""),
                                urgency = obj.optString("urgency", ""),
                                initial_review = obj.optString("initial_review", ""),
                                system_request_number = obj.optString("system_request_number", "")
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
                                        val assignedList = task.assignedTo.split(",").map { it.trim() }
                                        if (assignedList.contains(currentUserRowId)) {
                                            taskList.add(task)
                                        }
                                    }
                                }
                            }
                        }

                        if (taskList.isEmpty()) {
                            showEmptyState()
                        } else {
                            hideEmptyState()

                            // انتخاب آداپتور مناسب بر اساس وضعیت (با tabType)
                            when (status?.trim()) {
                                "انتخاب نشده" -> {
                                    adapter = ExpandableTaskAdapter(
                                        tasks = taskList,
                                        tabType = "unassigned",  // ← اضافه شد
                                        onEditClick = { task -> openEditTask(task) },
                                        onDeleteClick = { task -> deleteTask(task) },
                                        onReferClick = { task -> referTask(task) },
                                        onVolunteerClick = { task -> volunteerTask(task) }
                                    )
                                }
                                "در حال انجام" -> {
                                    adapter = ExpandableTaskAdapter(
                                        tasks = taskList,
                                        tabType = "inProgress",  // ← اضافه شد
                                        onEditClick = { task -> openEditTask(task) },
                                        onDeleteClick = { task -> deleteTask(task) },
                                        onReferClick = { task -> referTask(task) },
                                        onVolunteerClick = { task -> volunteerTask(task) },
                                        onItemClick = { task -> openTaskDetail(task) }
                                    )
                                }
                                "کارتابل من" -> {
                                    adapter = ExpandableTaskAdapter(
                                        tasks = taskList,
                                        tabType = "myCartable",  // ← اضافه شد
                                        onEditClick = { task -> openEditTask(task) },
                                        onDeleteClick = { task -> deleteTask(task) },
                                        onReferClick = { task -> referTask(task) },
                                        onVolunteerClick = { task -> volunteerTask(task) },
                                        onItemClick = { task -> openTaskDetail(task) }
                                    )
                                }
                                else -> {
                                    // حالت پیش‌فرض (نباید اجرا شود)
                                    adapter = ExpandableTaskAdapter(
                                        tasks = taskList,
                                        tabType = "unassigned",
                                        onEditClick = { task -> openEditTask(task) },
                                        onDeleteClick = { task -> deleteTask(task) },
                                        onReferClick = { task -> referTask(task) },
                                        onVolunteerClick = { task -> volunteerTask(task) }
                                    )
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

    // ویرایش کار
    private fun openEditTask(task: TaskModel) {
        val intent = Intent(requireContext(), CreateTaskActivity::class.java)
        intent.putExtra("IS_EDIT", true)
        intent.putExtra("TASK_ID", task.id)
        intent.putExtra("TITLE", task.title)
        intent.putExtra("DESC", task.description)
        intent.putExtra("UNIT", task.unit)
        // فیلدهای جدید
        intent.putExtra("SUB_UNIT", task.sub_unit)
        intent.putExtra("DECLARATION_METHOD", task.declaration_method)
        intent.putExtra("REQUESTER", task.requester)
        intent.putExtra("REQUEST_DATE", task.request_date)
        intent.putExtra("INITIAL_REVIEW", task.initial_review)
        intent.putExtra("SYSTEM_REQUEST_NUMBER", task.system_request_number)
        intent.putExtra("URGENCY", task.urgency)
        startActivity(intent)
    }

    private fun openTaskDetail(task: TaskModel) {
        val intent = Intent(requireContext(), TaskDetailActivity::class.java)
        intent.putExtra("TASK_ID", task.id)
        intent.putExtra("TITLE", task.title)
        intent.putExtra("DESC", task.description)
        intent.putExtra("CREATOR", task.creator)
        intent.putExtra("DATE", task.createDate)
        intent.putExtra("RESPONSIBLE", task.responsible)
        intent.putExtra("UNIT", task.unit)
        intent.putExtra("PRIORITY", task.priority)
        intent.putExtra("SUB_UNIT", task.sub_unit)
        intent.putExtra("DECLARATION_METHOD", task.declaration_method)
        intent.putExtra("REQUESTER", task.requester)
        intent.putExtra("REQUEST_DATE", task.request_date)
        intent.putExtra("INITIAL_REVIEW", task.initial_review)
        intent.putExtra("SYSTEM_REQUEST_NUMBER", task.system_request_number)
        intent.putExtra("URGENCY", task.urgency)
        startActivity(intent)
    }

    // حذف کار (اصلاح شده با runOnUiThread)
    private fun deleteTask(task: TaskModel) {
        AlertDialog.Builder(requireContext())
            .setTitle("حذف کار")
            .setMessage("آیا از حذف این کار مطمئن هستید؟")
            .setPositiveButton("بله") { _, _ ->
                taskRepository.deleteTask(
                    taskId = task.id,
                    onSuccess = {
                        requireActivity().runOnUiThread {
                            Toast.makeText(context, "کار حذف شد", Toast.LENGTH_SHORT).show()
                            fetchTasks()
                        }
                    },
                    onError = { message ->
                        requireActivity().runOnUiThread {
                            Toast.makeText(context, "خطا: $message", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
            .setNegativeButton("خیر", null)
            .show()
    }

    // ارجاع کار (اصلاح شده با runOnUiThread)
    private fun referTask(task: TaskModel) {
        Log.d("REFER_DEBUG", "1. referTask called for task ${task.id}")

        val sharedPref = requireContext().getSharedPreferences(Config.PrefKeys.USER_PREFS, Context.MODE_PRIVATE)
        val userRole = sharedPref.getString(Config.PrefKeys.USER_ROLE, "")

        Log.d("REFER_DEBUG", "2. User role: $userRole")

        if (userRole != Config.RoleCode.SUPERVISOR && userRole != Config.RoleCode.MANAGER) {
            Toast.makeText(context, "فقط سرشیفت و مدیر می‌توانند ارجاع دهند", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("REFER_DEBUG", "3. Creating ReferDialog")
        val referDialog = ReferDialog(requireContext(), task.id, task.title)

        referDialog.setOnReferSubmitListener { assigneeIds, referralType, responsibleId ->
            Log.d("REFER_DEBUG", "4. OnSubmit called: assigneeIds=$assigneeIds, responsibleId=$responsibleId")
            taskRepository.assignTask(
                taskId = task.id,
                assigneeIds = assigneeIds,
                referralType = referralType,
                responsibleId = responsibleId,
                onSuccess = {
                    requireActivity().runOnUiThread {
                        Toast.makeText(context, "ارجاع با موفقیت ثبت شد", Toast.LENGTH_SHORT).show()
                        fetchTasks()
                    }
                },
                onError = { message ->
                    requireActivity().runOnUiThread {
                        Toast.makeText(context, "خطا: $message", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
        referDialog.show()
        Log.d("REFER_DEBUG", "5. ReferDialog shown")
    }

    // داوطلب شدن (اصلاح شده با runOnUiThread)
    private fun volunteerTask(task: TaskModel) {
        val sharedPref = requireContext().getSharedPreferences(Config.PrefKeys.USER_PREFS, Context.MODE_PRIVATE)
        val currentUserRowId = sharedPref.getString(Config.PrefKeys.USER_ROW_ID, "") ?: ""

        // اگر مسئول دارد و کاربر فعلی مسئول نیست → فقط عضو گروه شود
        if (task.responsible.isNotEmpty() && task.responsible != currentUserRowId) {
            taskRepository.volunteer(
                taskId = task.id,
                assigneeIds = currentUserRowId,
                responsibleId = null,
                onSuccess = {
                    requireActivity().runOnUiThread {
                        Toast.makeText(context, "شما به گروه انجام‌دهندگان اضافه شدید", Toast.LENGTH_SHORT).show()
                        fetchTasks()
                    }
                },
                onError = { message ->
                    requireActivity().runOnUiThread {
                        Toast.makeText(context, "خطا: $message", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
        // اگر مسئول ندارد → خودش مسئول شود و دعوتنامه بفرستد
        else if (task.responsible.isEmpty()) {
            val inviteDialog = InviteDialog(requireContext(), taskRepository) { inviteeIds ->
                if (inviteeIds.isNotEmpty()) {
                    taskRepository.volunteer(
                        taskId = task.id,
                        assigneeIds = currentUserRowId,
                        responsibleId = currentUserRowId,
                        onSuccess = {
                            taskRepository.sendInvite(
                                taskId = task.id,
                                inviteeIds = inviteeIds,
                                onSuccess = {
                                    requireActivity().runOnUiThread {
                                        Toast.makeText(context, "مسئولیت ثبت شد و دعوتنامه ارسال گردید", Toast.LENGTH_SHORT).show()
                                        fetchTasks()
                                    }
                                },
                                onError = { message ->
                                    requireActivity().runOnUiThread {
                                        Toast.makeText(context, "دعوتنامه ارسال نشد: $message", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        },
                        onError = { message ->
                            requireActivity().runOnUiThread {
                                Toast.makeText(context, "خطا در ثبت مسئولیت: $message", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
            inviteDialog.show()
        } else {
            requireActivity().runOnUiThread {
                Toast.makeText(context, "شما قبلاً مسئول این کار هستید", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showEmptyState() {
        val emptyTextView = view?.findViewById<TextView>(R.id.emptyStateText)
        if (emptyTextView == null) {
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

    private fun fetchAndCacheUsers() {
        val url = "${Config.BASE_URL}?action=getEmployees"

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