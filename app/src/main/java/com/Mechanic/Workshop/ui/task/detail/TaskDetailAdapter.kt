package com.Mechanic.Workshop.ui.task.detail

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.PopupMenu
import android.widget.Toast
import android.widget.LinearLayout
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.Mechanic.Workshop.R
import com.Mechanic.Workshop.data.model.TaskLogModel
import com.Mechanic.Workshop.data.remote.Config
import com.Mechanic.Workshop.ui.task.log.AddLogActivity
import com.Mechanic.Workshop.utils.VolleySingleton
import TaskModel
import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.Gravity
import android.graphics.Color
import androidx.cardview.widget.CardView
import com.Mechanic.Workshop.utils.SeenItem
import com.Mechanic.Workshop.utils.SeenManager
import org.json.JSONArray
import org.json.JSONObject
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest


class TaskDetailAdapter(
    private val task: TaskModel,
    private val logs: List<TaskLogModel>,
    private val onEditLogClick: (TaskLogModel) -> Unit,
    private val onDeleteLogClick: (TaskLogModel) -> Unit,
    private val onAddLogClick: () -> Unit,
    private val onRefreshLogs: () -> Unit,
    private val onCompleteTaskClick: () -> Unit,     // ← جدید
    private val showCompleteButton: Boolean = false,  // ← جدید
    private val canAddLog: Boolean = true,
    private val currentUserId: String = "",     // ✅ اضافه کن
    private val userRole: String = "",
    private val buttonMode: String = "HIDDEN",
    private val onButtonClick: () -> Unit = {}
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

    override fun getItemCount(): Int = logs.size + 2

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
                AddLogViewHolder(
                    view,
                    onAddLogClick,
                    onCompleteTaskClick,
                    showCompleteButton,
                    canAddLog,
                    buttonMode,
                    onButtonClick
                )
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is TaskInfoViewHolder -> holder.bind(task)
            is TaskLogViewHolder -> {
                val log = logs[position - 1]
                val sharedPref = holder.itemView.context.getSharedPreferences(
                    Config.PrefKeys.USER_PREFS,
                    Context.MODE_PRIVATE
                )
                val currentUserId = sharedPref.getString(Config.PrefKeys.USER_ROW_ID, "") ?: ""
                val userRole = sharedPref.getString(Config.PrefKeys.USER_ROLE, "") ?: ""
                holder.bind(log, task, currentUserId, userRole, onDeleteLogClick) {
                    onRefreshLogs()
                }
            }
        }
    }

    // ViewHolder برای شرح کار
    class TaskInfoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvId: TextView = itemView.findViewById(R.id.tvTaskId)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTaskTitle)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvTaskDescription)
        private val tvResponsible: TextView = itemView.findViewById(R.id.tvTaskResponsible)
        private val tvAssignees: TextView = itemView.findViewById(R.id.tvTaskAssignees)
        private val tvUnit: TextView = itemView.findViewById(R.id.tvTaskUnit)

        //private val tvPUrgency: TextView = itemView.findViewById(R.id.tvTaskUrgency)
        private val tvRequestDate: TextView = itemView.findViewById(R.id.tvRequestDate)
        private val tvRequester: TextView = itemView.findViewById(R.id.tvRequester)
        private val tvDeclarationMethod: TextView = itemView.findViewById(R.id.tvDeclarationMethod)
        private val tvSystemNumber: TextView = itemView.findViewById(R.id.tvSystemNumber)
        private val tvInitialReview: TextView = itemView.findViewById(R.id.tvInitialReview)
        private val tvUrgency: TextView = itemView.findViewById(R.id.tvTaskUrgency)
        private val tvReferredBy: TextView = itemView.findViewById(R.id.tvReferredBy)


        fun bind(task: TaskModel) {
            tvId.text = "شماره کار: ${task.id}"
            tvTitle.text = "عنوان: ${task.title}"
            tvDescription.text = "شرح: ${task.description.ifEmpty { "توضیحاتی وارد نشده" }}"

            if (task.responsible.isNotEmpty()) {
                val responsibleName =
                    Config.UserCache.userMap[task.responsible] ?: "کاربر ${task.responsible}"
                tvResponsible.text = "مسئول: $responsibleName"
                tvResponsible.visibility = View.VISIBLE
            } else {
                tvResponsible.visibility = View.GONE
            }

            if (task.assignedTo.isNotEmpty()) {
                val assigneeNames = task.assignedTo.split(",").map {
                    Config.UserCache.userMap[it.trim()] ?: "کاربر $it"
                }
                tvAssignees.text = "گروه: ${assigneeNames.joinToString("، ")}"
                tvAssignees.visibility = View.VISIBLE
            } else {
                tvAssignees.visibility = View.GONE
            }

            // فوریت (urgency)
            if (task.urgency.isNotEmpty() && task.urgency != "عادی") {
                tvUrgency.text = "فوریت: ${task.urgency}"
                tvUrgency.visibility = View.VISIBLE
            } else {
                tvUrgency.visibility = View.GONE
            }

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


            if (task.request_date.isNotEmpty()) {
                val displayDate = task.request_date.replace("-", "/")
                tvRequestDate.text = "تاریخ اعلام: $displayDate"
                tvRequestDate.visibility = View.VISIBLE
            } else {
                tvRequestDate.visibility = View.GONE
            }

            if (task.requester.isNotEmpty()) {
                tvRequester.text = "صادرکننده: ${task.requester}"
                tvRequester.visibility = View.VISIBLE
            } else {
                tvRequester.visibility = View.GONE
            }

            if (task.declaration_method.isNotEmpty()) {
                tvDeclarationMethod.text = "نحوه اعلام: ${task.declaration_method}"
                tvDeclarationMethod.visibility = View.VISIBLE
            } else {
                tvDeclarationMethod.visibility = View.GONE
            }

            if (task.system_request_number.isNotEmpty()) {
                tvSystemNumber.text = "شماره سامانه: ${task.system_request_number}"
                tvSystemNumber.visibility = View.VISIBLE
            } else {
                tvSystemNumber.visibility = View.GONE
            }

            if (task.initial_review.isNotEmpty()) {
                tvInitialReview.text = "بررسی اولیه: ${task.initial_review}"
                tvInitialReview.visibility = View.VISIBLE
            } else {
                tvInitialReview.visibility = View.GONE
            }

            if (task.referredBy.isNotEmpty()) {
                val referredByName =
                    Config.UserCache.userMap[task.referredBy] ?: "کاربر ${task.referredBy}"
                tvReferredBy.text = "ارجاع‌دهنده: $referredByName"
                tvReferredBy.visibility = View.VISIBLE
            } else {
                tvReferredBy.visibility = View.GONE
            }
        }
    }

    // ViewHolder برای هر گزارش
    class TaskLogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvLogSummary: TextView = itemView.findViewById(R.id.tvLogSummary)
        private val ivExpand: ImageView = itemView.findViewById(R.id.ivExpand)
        private val ivMenu: ImageView = itemView.findViewById(R.id.ivMenu)
        private val divider: View = itemView.findViewById(R.id.divider)
        private val detailLayout: View = itemView.findViewById(R.id.detailLayout)
        private val tvActionDescription: TextView = itemView.findViewById(R.id.tvActionDescription)
        private val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
        private val tvWorkers: TextView = itemView.findViewById(R.id.tvWorkers)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        private val tvNewStatus: TextView = itemView.findViewById(R.id.tvNewStatus)
        private val tvLogNotes: TextView = itemView.findViewById(R.id.tvLogNotes)
        private val tvCommentLabel: TextView = itemView.findViewById(R.id.tvCommentLabel)
        private val commentsContainer: LinearLayout = itemView.findViewById(R.id.commentsContainer)
        private val btnAddComment: Button = itemView.findViewById(R.id.btnAddComment)
        private val cardLog: CardView = itemView.findViewById(R.id.cardLog)
        private var isExpanded = false
        private lateinit var currentLog: TaskLogModel
        private lateinit var currentTask: TaskModel
        private lateinit var onRefreshCallback: () -> Unit
        private lateinit var currentUserId: String
        private lateinit var onDeleteCallback: (TaskLogModel) -> Unit


        @SuppressLint("SetTextI18n")
        fun bind(
            log: TaskLogModel,
            task: TaskModel,
            currentUserId: String,
            userRole: String,
            onDelete: (TaskLogModel) -> Unit,
            onRefresh: () -> Unit
        ) {
            this.currentLog = log
            this.currentTask = task
            this.currentUserId = currentUserId
            this.onDeleteCallback = onDelete
            this.onRefreshCallback = onRefresh

            /// رنگ کردن هدر گزارش برای گزارش‌های جدید (فقط مدیر و سرشیفت)
            val isSeenByCurrentUser = currentLog.seenBy.contains(currentUserId)
            if (!isSeenByCurrentUser && (userRole == Config.RoleCode.SUPERVISOR || userRole == Config.RoleCode.MANAGER)) {
                cardLog.setCardBackgroundColor(Color.parseColor("#E3F2FD"))  // آبی کمرنگ
                isExpanded = true  // اکسپند خودکار
            } else {
                cardLog.setCardBackgroundColor(Color.WHITE)
                isExpanded = false
            }

            if (log.actionDescription.contains("برگشت داده شد")) {
                tvActionDescription.setTextColor(Color.RED)
            } else {
                tvActionDescription.setTextColor(Color.BLACK)
            }

            // ساخت متن هدر با ساعت
            val timeRange = if (log.startTime.isNotEmpty() || log.endTime.isNotEmpty()) {
                " (${log.startTime} - ${log.endTime})"
            } else {
                ""
            }
            tvLogSummary.text = "گزارش ${log.date} - ${log.userName}$timeRange"

            // ✅ نمایش تیک‌های رنگی کاربرانی که دیده‌اند
            displaySeenBy(log.seenBy)

            tvActionDescription.text = "شرح اقدام: ${log.actionDescription}"

            if (log.assignedUsers.isNotEmpty()) {
                val workerNames = log.assignedUsers.split(",").mapNotNull {
                    Config.UserCache.userMap[it.trim()]
                }
                tvWorkers.text = "گروه انجام دهتده: ${workerNames.joinToString("، ")}"
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

            val durationText = if (log.startTime.isNotEmpty() && log.endTime.isNotEmpty()) {
                try {
                    val startHour = log.startTime.split(":")[0].toInt()
                    val startMinute = log.startTime.split(":")[1].toInt()
                    val endHour = log.endTime.split(":")[0].toInt()
                    val endMinute = log.endTime.split(":")[1].toInt()

                    var durationMinutes =
                        (endHour * 60 + endMinute) - (startHour * 60 + startMinute)
                    if (durationMinutes < 0) durationMinutes += 24 * 60

                    val hours = durationMinutes / 60
                    val minutes = durationMinutes % 60
                    "مدت زمان: $hours ساعت و $minutes دقیقه"
                } catch (e: Exception) {
                    ""
                }
            } else {
                ""
            }

            if (durationText.isNotEmpty()) {
                tvDuration.text = durationText
                tvDuration.visibility = View.VISIBLE
            } else {
                tvDuration.visibility = View.GONE
            }

            if (log.newStatus.isNotEmpty()) {
                tvNewStatus.text = "آخرین وضعیت: ${Config.StatusCode.getText(log.newStatus)}"
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

            // نمایش نظرات
            displayComments(log.comments, currentUserId)

            // دکمه ثبت نظر
            // دکمه ثبت نظر (برای همه آزاد است)
            btnAddComment.visibility = View.VISIBLE
            btnAddComment.setOnClickListener {
                showAddCommentDialog()
            }

            setExpanded(isExpanded)

            itemView.findViewById<View>(R.id.headerLayout).setOnClickListener {
                isExpanded = !isExpanded
                setExpanded(isExpanded)
            }

            // سه نقطه با PopupMenu - ارسال به AddLogActivity در حالت ویرایش
            val canEditDelete = log.userId == currentUserId

            if (canEditDelete) {
                ivMenu.visibility = View.VISIBLE
                ivMenu.setOnClickListener { view ->
                    PopupMenu(view.context, view).apply {
                        menu.add(0, 1, 0, "ویرایش")
                        menu.add(0, 2, 0, "حذف")
                        setOnMenuItemClickListener { menuItem ->
                            when (menuItem.itemId) {
                                1 -> {
                                    // باز کردن AddLogActivity در حالت ویرایش
                                    val context = view.context
                                    val intent = Intent(context, AddLogActivity::class.java).apply {
                                        putExtra("IS_EDIT_MODE", true)
                                        putExtra("LOG_ID", log.id)
                                        putExtra("TASK_ID", log.taskId)
                                        putExtra("TITLE", task.title)
                                        putExtra("DESC", task.description)
                                        putExtra("CREATOR", task.creator)
                                        putExtra("DATE", task.createDate)
                                        putExtra("RESPONSIBLE", task.responsible)
                                        putExtra("ASSIGNED_TO", task.assignedTo)
                                        putExtra("UNIT", task.unit)
                                        putExtra("PRIORITY", task.priority)
                                        putExtra("SUB_UNIT", task.sub_unit)
                                        putExtra("DECLARATION_METHOD", task.declaration_method)
                                        putExtra("REQUESTER", task.requester)
                                        putExtra("REQUEST_DATE", task.request_date)
                                        putExtra("INITIAL_REVIEW", task.initial_review)
                                        putExtra(
                                            "SYSTEM_REQUEST_NUMBER",
                                            task.system_request_number
                                        )
                                        putExtra("URGENCY", task.urgency)
                                    }
                                    context.startActivity(intent)
                                }

                                2 -> {
                                    Toast.makeText(
                                        view.context,
                                        "حذف گزارش ${log.id}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    onDeleteCallback(log)
                                }
                            }
                            true
                        }
                        show()
                    }
                }
            } else {
                ivMenu.visibility = View.GONE
            }

        }

        private fun setExpanded(expanded: Boolean) {
            if (expanded) {
                ivExpand.setImageResource(R.drawable.ic_chevron_up)
                divider.visibility = View.VISIBLE
                detailLayout.visibility = View.VISIBLE

                // ✅ ثبت دیده شدن گزارش
                if (::currentUserId.isInitialized && currentUserId.isNotEmpty()) {
                    SeenManager.markAsSeen(
                        itemView.context,
                        currentUserId,
                        listOf(SeenItem("TASK_LOG", currentLog.id))
                    )
                }
            } else {
                ivExpand.setImageResource(R.drawable.ic_chevron_down)
                divider.visibility = View.GONE
                detailLayout.visibility = View.GONE
            }
        }

        private fun displayComments(commentsJson: String, currentUserId: String) {
            commentsContainer.removeAllViews()

            if (commentsJson.isEmpty() || commentsJson == "[]") {
                tvCommentLabel.visibility = View.GONE
                commentsContainer.visibility = View.GONE
                return
            }

            tvCommentLabel.visibility = View.VISIBLE
            commentsContainer.visibility = View.VISIBLE

            try {
                val jsonArray = JSONArray(commentsJson)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val commentId = obj.getString("id")
                    val userId = obj.getString("userId")
                    val userName = obj.getString("userName")
                    val timestamp = obj.getString("timestamp")
                    val text = obj.getString("text")

                    val commentView = LayoutInflater.from(itemView.context)
                        .inflate(R.layout.item_comment, commentsContainer, false)

                    val tvHeader = commentView.findViewById<TextView>(R.id.tvCommentHeader)
                    val tvText = commentView.findViewById<TextView>(R.id.tvCommentText)
                    val tvExpand = commentView.findViewById<TextView>(R.id.tvCommentExpand)
                    val ivDelete = commentView.findViewById<ImageView>(R.id.ivDeleteComment)

                    tvHeader.text = "$userName - $timestamp"
                    tvText.text = text

                    // قابلیت اکسپند (باز و بسته شدن)
                    if (text.length > 100) {
                        tvExpand.visibility = View.VISIBLE
                        tvExpand.setOnClickListener {
                            if (tvText.maxLines == 2) {
                                tvText.maxLines = Int.MAX_VALUE
                                tvExpand.text = "بستن"
                            } else {
                                tvText.maxLines = 2
                                tvExpand.text = "بیشتر"
                            }
                        }
                    }

                    // دکمه حذف فقط برای نویسنده نظر
                    if (userId == currentUserId) {
                        ivDelete.visibility = View.VISIBLE
                        ivDelete.setOnClickListener {
                            deleteComment(commentsJson, commentId)
                        }
                    }

                    commentsContainer.addView(commentView)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun deleteComment(commentsJson: String, commentId: String) {
            try {
                val jsonArray = JSONArray(commentsJson)
                val newArray = JSONArray()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    if (obj.getString("id") != commentId) {
                        newArray.put(obj)
                    }
                }
                updateLogComments(currentLog.id, newArray.toString())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun showAddCommentDialog() {
            val editText = EditText(itemView.context)
            editText.hint = "نظر خود را وارد کنید..."
            editText.inputType = android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            editText.setLines(3)

            AlertDialog.Builder(itemView.context)
                .setTitle("ثبت نظر")
                .setView(editText)
                .setPositiveButton("ثبت") { _, _ ->
                    val newCommentText = editText.text.toString().trim()
                    if (newCommentText.isNotEmpty()) {
                        addNewComment(newCommentText)
                    }
                }
                .setNegativeButton("انصراف", null)
                .show()
        }

        private fun addNewComment(text: String) {
            try {
                val sharedPref = itemView.context.getSharedPreferences(
                    Config.PrefKeys.USER_PREFS,
                    Context.MODE_PRIVATE
                )
                val currentUserId = sharedPref.getString(Config.PrefKeys.USER_ROW_ID, "") ?: ""
                val currentUserName =
                    sharedPref.getString(Config.PrefKeys.USERNAME, "کاربر") ?: "کاربر"
                val timestamp =
                    java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault())
                        .format(java.util.Date())
                val commentId = System.currentTimeMillis().toString()

                val newComment = JSONObject().apply {
                    put("id", commentId)
                    put("userId", currentUserId)
                    put("userName", currentUserName)
                    put("timestamp", timestamp)
                    put("text", text)
                }

                // ✅ بررسی null بودن currentLog.comments
                val commentsStr = currentLog.comments ?: "[]"
                val currentComments = if (commentsStr.isNotEmpty() && commentsStr != "[]") {
                    try {
                        JSONArray(commentsStr)
                    } catch (e: Exception) {
                        JSONArray()
                    }
                } else {
                    JSONArray()
                }
                currentComments.put(newComment)

                btnAddComment.isEnabled = false
                btnAddComment.text = "در حال ثبت..."

                updateLogComments(currentLog.id, currentComments.toString())
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(itemView.context, "خطا در ثبت نظر: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
                btnAddComment.isEnabled = true
                btnAddComment.text = "ثبت نظر"
            }
        }

        private fun updateLogComments(logId: String, commentsJson: String) {
            val url = "${Config.BASE_URL}?action=updateLogComments"
            val jsonObject = JSONObject().apply {
                put("logId", logId)
                put("comments", commentsJson)
            }

            val request = JsonObjectRequest(
                Request.Method.POST, url, jsonObject,
                { response ->
                    if (response.optString("status") == "success") {
                        btnAddComment.isEnabled = true
                        btnAddComment.text = "ثبت نظر"
                        onRefreshCallback()  // ← رفرش کامل صفحه
                    } else {
                        btnAddComment.isEnabled = true
                        btnAddComment.text = "ثبت نظر"
                        Toast.makeText(itemView.context, "خطا در ثبت نظر", Toast.LENGTH_SHORT)
                            .show()
                    }
                },
                { error ->
                    btnAddComment.isEnabled = true
                    btnAddComment.text = "ثبت نظر"
                    error.printStackTrace()
                    Toast.makeText(itemView.context, "خطا در اتصال به شبکه", Toast.LENGTH_SHORT)
                        .show()
                }
            )
            VolleySingleton.getInstance(itemView.context).add(request)
        }

        private fun displaySeenBy(seenBy: List<String>) {
            val container = itemView.findViewById<LinearLayout>(R.id.seenByContainer)
            container.removeAllViews()

            val maxDisplay = 6
            val toShow = seenBy.take(maxDisplay)
            val remaining = seenBy.size - maxDisplay

            toShow.forEach { userId ->
                val userColor = getColorForUserId(userId)

                val tickView = ImageView(itemView.context).apply {
                    setImageResource(R.drawable.ic_check)
                    setColorFilter(userColor, android.graphics.PorterDuff.Mode.SRC_IN)
                    layoutParams = LinearLayout.LayoutParams(12.dpToPx(), 12.dpToPx()).apply {
                        marginEnd = 0  // ← فاصله صفر
                    }
                }
                container.addView(tickView)
            }

            if (remaining > 0) {
                val moreView = TextView(itemView.context).apply {
                    text = "+$remaining"
                    textSize = 10f
                    setTextColor(Color.BLACK)
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        gravity = Gravity.CENTER_VERTICAL
                        marginEnd = 2.dpToPx()
                    }
                }
                container.addView(moreView)
            }
        }

        private fun getColorForUserId(userId: String): Int {
            val colors = listOf(
                "#E91E63", "#9C27B0", "#673AB7", "#3F51B5",
                "#2196F3", "#03A9F4", "#00BCD4", "#009688",
                "#4CAF50", "#8BC34A", "#CDDC39", "#FFEB3B",
                "#FFC107", "#FF9800", "#FF5722", "#795548"
            )
            val index = userId.hashCode().mod(colors.size)
            return Color.parseColor(colors[index])
        }

        fun Int.dpToPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()
    }


    class AddLogViewHolder(
        itemView: View,
        onAddLogClick: () -> Unit,
        onCompleteTaskClick: () -> Unit,
        showCompleteButton: Boolean,
        canAddLog: Boolean,
        private val buttonMode: String,
        private val onButtonClick: () -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        init {
            val btn = itemView.findViewById<Button>(R.id.btnAddLog)

            when (buttonMode) {
                "ADD_LOG" -> {
                    btn.text = "ثبت گزارش جدید"
                    btn.setOnClickListener { onAddLogClick() }
                    btn.visibility = View.VISIBLE
                    btn.backgroundTintList = android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#9C27B0")
                    )
                }
                "REQUEST_COMPLETE" -> {
                    btn.text = "اعلام اتمام کار"
                    btn.setOnClickListener { onCompleteTaskClick() }
                    btn.visibility = View.VISIBLE
                    btn.backgroundTintList = android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#2E7D32")
                    )
                }
                "FINAL_REVIEW" -> {
                    btn.text = "بررسی و تأیید نهایی"
                    btn.setOnClickListener { onButtonClick() }
                    btn.visibility = View.VISIBLE
                    btn.backgroundTintList = android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#FF9800")
                    )
                }
                else -> {
                    btn.visibility = View.GONE
                }
            }
        }
    }
}


