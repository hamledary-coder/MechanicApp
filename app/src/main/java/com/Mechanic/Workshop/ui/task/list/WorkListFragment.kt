package com.Mechanic.Workshop.ui.task.list

import TaskModel
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
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
import com.Mechanic.Workshop.data.remote.Config
import com.Mechanic.Workshop.ui.task.create.CreateTaskActivity
import com.Mechanic.Workshop.ui.task.repository.TaskRepository
import com.Mechanic.Workshop.ui.referral.ReferDialog
import com.Mechanic.Workshop.ui.task.detail.TaskDetailActivity
import com.Mechanic.Workshop.utils.ActivityLogger
import com.Mechanic.Workshop.utils.UserCache
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray

@Suppress("DEPRECATION")
class WorkListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RecyclerView.Adapter<*>
    private var status: String? = null
    private var taskList = mutableListOf<TaskModel>()
    private lateinit var loadingLayout: LinearLayout
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var taskRepository: TaskRepository

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

        setHasOptionsMenu(true)

        loadingLayout = view.findViewById(R.id.loadingLayout)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        recyclerView = view.findViewById(R.id.recyclerViewTasks)
        recyclerView.layoutManager = LinearLayoutManager(context)

        swipeRefreshLayout.setOnRefreshListener {
            fetchTasks()
        }

        // بارگذاری کار‌ها (UserCache قبلاً در LoginActivity مقداردهی شده)
        fetchTasks()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_work_list, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sort -> {
                showSortDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showSortDialog() {
        val sortOptions = arrayOf(
            "تاریخ (جدیدترین اول)",
            "تاریخ (قدیمی‌ترین اول)",
            "اولویت (بالا به پایین)",
            "عنوان (الفبا)"
        )

        AlertDialog.Builder(requireContext())
            .setTitle("مرتب‌سازی بر اساس")
            .setItems(sortOptions) { _, which ->
                when (which) {
                    0 -> sortTasksByDateNewest()
                    1 -> sortTasksByDateOldest()
                    2 -> sortTasksByPriority()
                    3 -> sortTasksByTitle()
                }
            }
            .show()
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
        val sharedPref = requireContext().getSharedPreferences(Config.PrefKeys.USER_PREFS, Context.MODE_PRIVATE)
        val currentUserId = sharedPref.getString(Config.PrefKeys.USER_ROW_ID, "") ?: ""
        val userRole = sharedPref.getString(Config.PrefKeys.USER_ROLE, "") ?: ""

        val url = "${Config.Endpoints.TASKS}&_=$timestamp&userId=$currentUserId"
        hideEmptyState()

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
                        taskList.clear()

                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)



                            val hasUnseenReport = obj.optInt("has_unseen_report", 0) == 1

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
                                sub_unit = obj.optString("sub_unit", ""),
                                declaration_method = obj.optString("declaration_method", ""),
                                requester = obj.optString("requester", ""),
                                request_date = obj.optString("request_date", ""),
                                urgency = obj.optString("urgency", ""),
                                initial_review = obj.optString("initial_review", ""),
                                system_request_number = obj.optString("system_request_number", ""),
                                referredBy = obj.optString("referred_by", ""),
                                hasUnseenReport = hasUnseenReport
                            )

                            when (status?.trim()) {
                                "اقدام نشده" -> {
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
                                    when (userRole) {
                                        Config.RoleCode.EMPLOYEE -> {
                                            if (task.assignedTo.split(",").contains(currentUserRowId)
                                                && task.status != "5"  && task.responsible == currentUserRowId) {
                                                taskList.add(task)
                                            }
                                        }
                                        Config.RoleCode.SUPERVISOR -> {
                                            val isInvolved = task.assignedTo.split(",").contains(currentUserRowId) ||
                                                    task.responsible == currentUserRowId
                                            val isCompleted = task.status == "41"
                                            val hasNewReport = task.hasUnseenReport
                                            if ((isInvolved || isCompleted || hasNewReport) && task.status != "5") {
                                                taskList.add(task)
                                            }
                                        }
                                        Config.RoleCode.MANAGER -> {
                                            if (task.hasUnseenReport && task.status != "5") {
                                                taskList.add(task)
                                            }
                                        }
                                        else -> {
                                            if (task.assignedTo.split(",").contains(currentUserRowId)) {
                                                taskList.add(task)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // پیش‌بارگذاری اسامی کاربران در UserCache
                        if (taskList.isNotEmpty()) {
                            UserCache.preloadFromTasks(taskList)
                        }

                        if (taskList.isEmpty()) {
                            showEmptyState()
                        } else {
                            hideEmptyState()
                            setupAdapter()
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

    private fun setupAdapter() {
        when (status?.trim()) {
            "اقدام نشده" -> {
                adapter = ExpandableTaskAdapter(
                    tasks = taskList,
                    tabType = "unassigned",
                    onEditClick = { task -> openEditTask(task) },
                    onDeleteClick = { task -> deleteTask(task) },
                    onReferClick = { task -> referTask(task) }
                )
            }
            "در حال انجام" -> {
                adapter = ExpandableTaskAdapter(
                    tasks = taskList,
                    tabType = "inProgress",
                    onEditClick = { task -> openEditTask(task) },
                    onDeleteClick = { task -> deleteTask(task) },
                    onReferClick = { task -> referTask(task) },
                    onItemClick = { task -> openTaskDetail(task) }
                )
            }
            "کارتابل من" -> {
                adapter = ExpandableTaskAdapter(
                    tasks = taskList,
                    tabType = "myCartable",
                    onEditClick = { task -> openEditTask(task) },
                    onDeleteClick = { task -> deleteTask(task) },
                    onReferClick = { task -> referTask(task) },
                    onItemClick = { task -> openTaskDetail(task) }
                )
            }
            else -> {
                adapter = ExpandableTaskAdapter(
                    tasks = taskList,
                    tabType = "unassigned",
                    onEditClick = { task -> openEditTask(task) },
                    onDeleteClick = { task -> deleteTask(task) },
                    onReferClick = { task -> referTask(task) }
                )
            }
        }
        recyclerView.adapter = adapter
        adapter.notifyDataSetChanged()
    }

    // ویرایش کار
    private fun openEditTask(task: TaskModel) {
        val intent = Intent(requireContext(), CreateTaskActivity::class.java)
        intent.putExtra("IS_EDIT", true)
        intent.putExtra("TASK_ID", task.id)
        intent.putExtra("TITLE", task.title)
        intent.putExtra("DESC", task.description)
        intent.putExtra("UNIT", task.unit)
        intent.putExtra("SUB_UNIT", task.sub_unit)
        intent.putExtra("DECLARATION_METHOD", task.declaration_method)
        intent.putExtra("REQUESTER", task.requester)
        intent.putExtra("REQUEST_DATE", task.request_date)
        intent.putExtra("INITIAL_REVIEW", task.initial_review)
        intent.putExtra("SYSTEM_REQUEST_NUMBER", task.system_request_number)
        intent.putExtra("URGENCY", task.urgency)
        intent.putExtra("OLD_TITLE", task.title)
        intent.putExtra("OLD_UNIT", task.unit)
        intent.putExtra("OLD_PRIORITY", task.priority)
        startActivity(intent)
    }

    private fun openTaskDetail(task: TaskModel) {
        val intent = Intent(requireContext(), TaskDetailActivity::class.java)
        intent.putExtra("TASK_ID", task.id)
        intent.putExtra("TITLE", task.title)
        intent.putExtra("STATUS", task.status)
        intent.putExtra("DESC", task.description)
        intent.putExtra("CREATOR", task.creator)
        intent.putExtra("DATE", task.createDate)
        intent.putExtra("RESPONSIBLE", task.responsible)
        intent.putExtra("ASSIGNED_TO", task.assignedTo)
        intent.putExtra("UNIT", task.unit)
        intent.putExtra("PRIORITY", task.priority)
        intent.putExtra("SUB_UNIT", task.sub_unit)
        intent.putExtra("DECLARATION_METHOD", task.declaration_method)
        intent.putExtra("REQUESTER", task.requester)
        intent.putExtra("REQUEST_DATE", task.request_date)
        intent.putExtra("INITIAL_REVIEW", task.initial_review)
        intent.putExtra("SYSTEM_REQUEST_NUMBER", task.system_request_number)
        intent.putExtra("URGENCY", task.urgency)
        intent.putExtra("REFERRED_BY", task.referredBy)
        startActivity(intent)
    }

    // حذف کار
    private fun deleteTask(task: TaskModel) {
        AlertDialog.Builder(requireContext())
            .setTitle("حذف کار")
            .setMessage("آیا از حذف این کار مطمئن هستید؟")
            .setPositiveButton("بله") { _, _ ->
                taskRepository.deleteTask(
                    taskId = task.id,
                    onSuccess = {
                        requireActivity().runOnUiThread {
                            val sharedPref = requireContext().getSharedPreferences(Config.PrefKeys.USER_PREFS, Context.MODE_PRIVATE)
                            val userId = sharedPref.getString(Config.PrefKeys.USER_ROW_ID, "") ?: ""
                            val userName = sharedPref.getString(Config.PrefKeys.USERNAME, "کاربر") ?: "کاربر"

                            ActivityLogger.log(
                                context = requireContext(),
                                userId = userId,
                                userName = userName,
                                action = "delete",
                                targetType = "TASK",
                                targetId = task.id,
                                description = "کار شماره ${task.id} با عنوان «${task.title}» توسط کاربر $userName حذف شد"
                            )

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

    // ارجاع کار
    private fun referTask(task: TaskModel) {
        val sharedPref = requireContext().getSharedPreferences(Config.PrefKeys.USER_PREFS, Context.MODE_PRIVATE)
        val userRole = sharedPref.getString(Config.PrefKeys.USER_ROLE, "")
        val currentUserId = sharedPref.getString(Config.PrefKeys.USER_ROW_ID, "") ?: ""

        if (userRole != Config.RoleCode.SUPERVISOR && userRole != Config.RoleCode.MANAGER) {
            Toast.makeText(context, "فقط سرشیفت و مدیر می‌توانند ارجاع دهند", Toast.LENGTH_SHORT).show()
            return
        }

        val referDialog = ReferDialog(
            context = requireContext(),
            taskTitle = task.title,
            currentAssignees = task.assignedTo,
            currentResponsible = task.responsible
        )

        referDialog.setOnReferSubmitListener { assigneeIds, referralType, responsibleId ->
            val oldAssignees = task.assignedTo
            val oldResponsible = task.responsible

            taskRepository.assignTask(
                taskId = task.id,
                assigneeIds = assigneeIds,
                referralType = referralType,
                responsibleId = responsibleId,
                referredBy = currentUserId,
                onSuccess = {
                    requireActivity().runOnUiThread {
                        val userId = sharedPref.getString(Config.PrefKeys.USER_ROW_ID, "") ?: ""
                        val userName = sharedPref.getString(Config.PrefKeys.USERNAME, "کاربر") ?: "کاربر"

                        // استفاده از UserCache جدید برای گرفتن نام‌ها
                        val assigneeNames = assigneeIds.split(",").mapNotNull { UserCache.getName(it) }.joinToString("، ")
                        val responsibleName = if (responsibleId != null) UserCache.getName(responsibleId) else "تعیین نشده"
                        val oldAssigneeNames = oldAssignees.split(",").mapNotNull { UserCache.getName(it) }.joinToString("، ")
                        val oldResponsibleName = UserCache.getName(oldResponsible)

                        val description = "کار شماره ${task.id} با عنوان «${task.title}» توسط کاربر $userName ارجاع شد.\n" +
                                "گروه قبلی: [$oldAssigneeNames] - گروه جدید: [$assigneeNames]\n" +
                                "مسئول قبلی: [$oldResponsibleName] - مسئول جدید: [$responsibleName]"

                        ActivityLogger.log(
                            context = requireContext(),
                            userId = userId,
                            userName = userName,
                            action = "REFER_TASK",
                            targetType = "TASK",
                            targetId = task.id,
                            description = description
                        )

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
        Log.d("REFER_DEBUG", "ReferDialog shown")
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

    private fun sortTasksByDateNewest() {
        taskList.sortByDescending { it.createDate }
        adapter?.notifyDataSetChanged()
    }

    private fun sortTasksByDateOldest() {
        taskList.sortBy { it.createDate }
        adapter?.notifyDataSetChanged()
    }

    private fun sortTasksByPriority() {
        taskList.sortByDescending { it.priority.toIntOrNull() ?: 0 }
        adapter?.notifyDataSetChanged()
    }

    private fun sortTasksByTitle() {
        taskList.sortBy { it.title }
        adapter?.notifyDataSetChanged()
    }
}