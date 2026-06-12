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
import com.Mechanic.Workshop.ui.task.log.TaskLogDetailActivity
import com.Mechanic.Workshop.utils.SeenItem
import com.Mechanic.Workshop.utils.SeenManager
import com.Mechanic.Workshop.utils.UserCache
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
    private val onCompleteTaskClick: () -> Unit,
    private val showCompleteButton: Boolean = false,
    private val canAddLog: Boolean = true,
    private val currentUserId: String = "",
    private val userRole: String = "",
    private val buttonMode: String = "HIDDEN",
    private val onButtonClick: () -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_TASK_INFO = 0
        private const val TYPE_ADD_LOG = 1      // ← جابه‌جا شد
        private const val TYPE_TASK_LOG = 2
    }

    override fun getItemCount(): Int {
        val hasAddLog = buttonMode != "HIDDEN"
        return 1 + (if (hasAddLog) 1 else 0) + logs.size  // info + addLog + logs
    }

    override fun getItemViewType(position: Int): Int {
        val hasAddLog = buttonMode != "HIDDEN"

        return when (position) {
            0 -> TYPE_TASK_INFO
            1 -> {
                if (hasAddLog) TYPE_ADD_LOG else TYPE_TASK_LOG
            }
            else -> {
                if (hasAddLog) {
                    // اگر addLog وجود داره، لاگ‌ها از ایندکس 2 شروع می‌شن
                    TYPE_TASK_LOG
                } else {
                    // اگر addLog وجود نداره، لاگ‌ها از ایندکس 1 شروع می‌شن
                    TYPE_TASK_LOG
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_TASK_INFO -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_task_info_static, parent, false)
                TaskInfoViewHolder(view)
            }
            TYPE_ADD_LOG -> {
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
            TYPE_TASK_LOG -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_task_log, parent, false)
                TaskLogViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val hasAddLog = buttonMode != "HIDDEN"

        when (holder) {
            is TaskInfoViewHolder -> holder.bind(task)
            is AddLogViewHolder -> {
                // کاری نداره، دکمه قبلاً در onCreateViewHolder ست شده
            }
            is TaskLogViewHolder -> {
                // محاسبه ایندکس صحیح لاگ
                val addLogOffset = if (hasAddLog) 1 else 0
                val logPosition = position - 1 - addLogOffset
                if (logPosition in logs.indices) {
                    val log = logs[logPosition]
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
    }


    // ViewHolder برای شرح کار
    class TaskInfoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // TextView‌ها
        private val tvTaskId: TextView = itemView.findViewById(R.id.tvTaskId)
        private val tvTaskTitle: TextView = itemView.findViewById(R.id.tvTaskTitle)
        private val tvTaskDescription: TextView = itemView.findViewById(R.id.tvTaskDescription)
        private val tvTaskResponsible: TextView = itemView.findViewById(R.id.tvTaskResponsible)
        private val tvTaskAssignees: TextView = itemView.findViewById(R.id.tvTaskAssignees)
        private val tvTaskUnit: TextView = itemView.findViewById(R.id.tvTaskUnit)
        private val tvTaskUrgency: TextView = itemView.findViewById(R.id.tvTaskUrgency)
        private val tvRequestDate: TextView = itemView.findViewById(R.id.tvRequestDate)
        private val tvRequester: TextView = itemView.findViewById(R.id.tvRequester)
        private val tvDeclarationMethod: TextView = itemView.findViewById(R.id.tvDeclarationMethod)
        private val tvSystemNumber: TextView = itemView.findViewById(R.id.tvSystemNumber)
        private val tvInitialReview: TextView = itemView.findViewById(R.id.tvInitialReview)
        private val tvReferredBy: TextView = itemView.findViewById(R.id.tvReferredBy)
        private val seenByContainer: LinearLayout = itemView.findViewById(R.id.seenByContainer)

        // Layoutهای جدید برای کنترل visibility
        private val layoutResponsible: LinearLayout = itemView.findViewById(R.id.layoutResponsible)
        private val layoutAssignees: LinearLayout = itemView.findViewById(R.id.layoutAssignees)
        private val layoutUnit: LinearLayout = itemView.findViewById(R.id.layoutUnit)
        private val layoutUrgency: LinearLayout = itemView.findViewById(R.id.layoutUrgency)
        private val layoutRequestDate: LinearLayout = itemView.findViewById(R.id.layoutRequestDate)
        private val layoutRequester: LinearLayout = itemView.findViewById(R.id.layoutRequester)
        private val layoutDeclarationMethod: LinearLayout = itemView.findViewById(R.id.layoutDeclarationMethod)
        private val layoutSystemNumber: LinearLayout = itemView.findViewById(R.id.layoutSystemNumber)
        private val layoutReferredBy: LinearLayout = itemView.findViewById(R.id.layoutReferredBy)

        // CardView‌ها
        private val cardDescription: CardView = itemView.findViewById(R.id.cardDescription)
        private val cardInitialReview: CardView = itemView.findViewById(R.id.cardInitialReview)

        fun bind(task: TaskModel) {
            tvTaskId.text = "#${task.id}"
            tvTaskTitle.text = task.title

            // توضیحات
            if (task.description.isNotEmpty()) {
                tvTaskDescription.text = task.description
                cardDescription.visibility = View.VISIBLE
            } else {
                cardDescription.visibility = View.GONE
            }

            // مسئول
            if (task.responsible.isNotEmpty() && task.responsible != "0") {
                val responsibleName = UserCache.getName(task.responsible)
                tvTaskResponsible.text = responsibleName
                layoutResponsible.visibility = View.VISIBLE
            } else {
                layoutResponsible.visibility = View.GONE
            }

            // گروه انجام‌دهنده
            if (task.assignedTo.isNotEmpty()) {
                val assigneeNames = task.assignedTo.split(",").map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .map { UserCache.getName(it) }
                tvTaskAssignees.text = assigneeNames.joinToString("، ")
                layoutAssignees.visibility = View.VISIBLE
            } else {
                layoutAssignees.visibility = View.GONE
            }

            // واحد
            if (task.unit.isNotEmpty() && task.unit != "null") {
                val unitText = Config.UnitCode.getText(task.unit)
                if (unitText.isNotEmpty()) {
                    tvTaskUnit.text = unitText
                    layoutUnit.visibility = View.VISIBLE
                } else {
                    layoutUnit.visibility = View.GONE
                }
            } else {
                layoutUnit.visibility = View.GONE
            }

            // فوریت
            if (task.urgency.isNotEmpty() && task.urgency != "عادی") {
                tvTaskUrgency.text = task.urgency
                layoutUrgency.visibility = View.VISIBLE
                // رنگ متن فوریت
                when (task.urgency) {
                    "خیلی زیاد" -> tvTaskUrgency.setTextColor(Color.parseColor("#D32F2F"))
                    "زیاد" -> tvTaskUrgency.setTextColor(Color.parseColor("#FF9800"))
                    else -> tvTaskUrgency.setTextColor(Color.parseColor("#333333"))
                }
            } else {
                layoutUrgency.visibility = View.GONE
            }

            // تاریخ اعلام
            if (task.request_date.isNotEmpty()) {
                val displayDate = task.request_date.replace("-", "/")
                tvRequestDate.text = displayDate
                layoutRequestDate.visibility = View.VISIBLE
            } else {
                layoutRequestDate.visibility = View.GONE
            }

            // صادرکننده
            if (task.requester.isNotEmpty()) {
                tvRequester.text = task.requester
                layoutRequester.visibility = View.VISIBLE
            } else {
                layoutRequester.visibility = View.GONE
            }

            // نحوه اعلام
            if (task.declaration_method.isNotEmpty()) {
                tvDeclarationMethod.text = task.declaration_method
                layoutDeclarationMethod.visibility = View.VISIBLE
            } else {
                layoutDeclarationMethod.visibility = View.GONE
            }

            // شماره سامانه
            if (task.system_request_number.isNotEmpty()) {
                tvSystemNumber.text = task.system_request_number
                layoutSystemNumber.visibility = View.VISIBLE
            } else {
                layoutSystemNumber.visibility = View.GONE
            }

            // بررسی اولیه
            if (task.initial_review.isNotEmpty()) {
                tvInitialReview.text = task.initial_review
                cardInitialReview.visibility = View.VISIBLE
            } else {
                cardInitialReview.visibility = View.GONE
            }

            // ارجاع‌دهنده
            if (task.referredBy.isNotEmpty()) {
                val referredByName = UserCache.getName(task.referredBy)
                tvReferredBy.text = referredByName
                layoutReferredBy.visibility = View.VISIBLE
            } else {
                layoutReferredBy.visibility = View.GONE
            }

            // مشاهده‌کنندگان
            displaySeenBy(task.seenBy)
        }


        private fun displaySeenBy(seenBy: List<String>) {
            seenByContainer.removeAllViews()

            if (seenBy.isEmpty()) {
                seenByContainer.visibility = View.GONE
                return
            }

            seenByContainer.visibility = View.VISIBLE

            val maxDisplay = 6
            val toShow = seenBy.take(maxDisplay)
            val remaining = seenBy.size - maxDisplay

            toShow.forEach { userId ->
                val userName = UserCache.getName(userId)
                val firstLetter = if (userName.isNotEmpty() && userName != "نامشخص")
                    userName.firstOrNull()?.toString()?.uppercase() ?: "?"
                else "?"

                val circleView = createCircleWithLetter(firstLetter, userId)
                seenByContainer.addView(circleView)
            }

            if (remaining > 0) {
                val moreView = TextView(itemView.context).apply {
                    text = "+$remaining"
                    textSize = 10f
                    setTextColor(Color.GRAY)
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        gravity = Gravity.CENTER_VERTICAL
                        marginStart = 4.dpToPx()
                    }
                }
                seenByContainer.addView(moreView)
            }
        }

        private fun createCircleWithLetter(letter: String, userId: String): View {
            return TextView(itemView.context).apply {
                text = letter
                textSize = 10f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER

                val size = 20.dpToPx()
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    marginEnd = 4.dpToPx()
                }

                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(getColorForUserId(userId))
                }

                // اضافه کردن Tooltip برای نمایش نام کامل وقتی طولانی نگه می‌دارند
                val userName = UserCache.getName(userId)
                if (userName.isNotEmpty() && userName != "نامشخص") {
                    setOnLongClickListener {
                        Toast.makeText(context, userName, Toast.LENGTH_SHORT).show()
                        true
                    }
                }
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

        private fun Int.dpToPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()
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
        //private val tvTime: TextView = itemView.findViewById(R.id.tvTime)
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

            // رنگ کردن هدر گزارش برای گزارش‌های جدید
            val isSeenByCurrentUser = currentLog.seenBy.contains(currentUserId)
            if (!isSeenByCurrentUser && (userRole == Config.RoleCode.SUPERVISOR || userRole == Config.RoleCode.MANAGER)) {
                cardLog.setCardBackgroundColor(Color.parseColor("#E3F2FD"))
                isExpanded = true
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

            // نمایش تیک‌های رنگی کاربرانی که دیده‌اند
            displaySeenBy(log.seenBy)

            tvActionDescription.text = "شرح اقدام: ${log.actionDescription}"

            // گروه انجام دهنده - استفاده از UserCache.getName
            if (log.assignedUsers.isNotEmpty()) {
                val workerNames = log.assignedUsers.split(",").mapNotNull {
                    val name = UserCache.getName(it.trim())
                    if (name != "نامشخص" && !name.startsWith("کاربر")) name else null
                }
                if (workerNames.isNotEmpty()) {
                    tvWorkers.text = "گروه انجام دهنده: ${workerNames.joinToString("، ")}"
                    tvWorkers.visibility = View.VISIBLE
                } else {
                    tvWorkers.visibility = View.GONE
                }
            } else {
                tvWorkers.visibility = View.GONE
            }

            //tvTime.visibility = View.GONE

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

                    // ساخت متن مدت زمان بدون نمایش "و صفر دقیقه"
                    when {
                        hours > 0 && minutes > 0 -> "مدت زمان: $hours ساعت و $minutes دقیقه"
                        hours > 0 && minutes == 0 -> "مدت زمان: $hours ساعت"
                        hours == 0 && minutes > 0 -> "مدت زمان: $minutes دقیقه"
                        else -> ""
                    }
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

            // دکمه ثبت نظر فقط برای کارهای غیر بایگانی
            val isArchived = task.status == "5"
            if (!isArchived) {
                btnAddComment.visibility = View.VISIBLE
                btnAddComment.setOnClickListener {
                    showAddCommentDialog()
                }
            } else {
                btnAddComment.visibility = View.GONE
            }

            setExpanded(isExpanded)

            itemView.findViewById<View>(R.id.headerLayout).setOnClickListener {
                isExpanded = !isExpanded
                setExpanded(isExpanded)
            }

            // منوی سه نقطه - فقط اگر کار بایگانی نشده باشد و کاربر خودش باشد
            val canEditDelete = !isArchived && log.userId == currentUserId
            if (canEditDelete) {
                ivMenu.visibility = View.VISIBLE
                ivMenu.setOnClickListener { view ->
                    PopupMenu(view.context, view).apply {
                        menu.add(0, 1, 0, "ویرایش")
                        menu.add(0, 2, 0, "حذف")
                        setOnMenuItemClickListener { menuItem ->
                            when (menuItem.itemId) {
                                1 -> {
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
                                        putExtra("SYSTEM_REQUEST_NUMBER", task.system_request_number)
                                        putExtra("URGENCY", task.urgency)
                                    }
                                    context.startActivity(intent)
                                }
                                2 -> {
                                    Toast.makeText(view.context, "حذف گزارش", Toast.LENGTH_SHORT).show()
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

            // بعد از setExpanded(isExpanded) یا در انتهای متد bind
            itemView.setOnClickListener {
                val context = itemView.context
                val intent = Intent(context, TaskLogDetailActivity::class.java).apply {
                    putExtra("TASK_ID", task.id)
                    putExtra("LOG_ID", log.id)
                    putExtra("IS_TASK_ARCHIVED", task.status == "5")
                }
                context.startActivity(intent)
            }
        }

        private fun setExpanded(expanded: Boolean) {
            if (expanded) {
                ivExpand.setImageResource(R.drawable.ic_chevron_up)
                divider.visibility = View.VISIBLE
                detailLayout.visibility = View.VISIBLE

                displaySeenBy(currentLog.seenBy)

                // ثبت دیده شدن گزارش
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
                val currentUserName = sharedPref.getString(Config.PrefKeys.USERNAME, "کاربر") ?: "کاربر"
                val timestamp = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault())
                    .format(java.util.Date())
                val commentId = System.currentTimeMillis().toString()

                val newComment = JSONObject().apply {
                    put("id", commentId)
                    put("userId", currentUserId)
                    put("userName", currentUserName)
                    put("timestamp", timestamp)
                    put("text", text)
                }

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
                Toast.makeText(itemView.context, "خطا در ثبت نظر: ${e.message}", Toast.LENGTH_SHORT).show()
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
                        onRefreshCallback()
                    } else {
                        btnAddComment.isEnabled = true
                        btnAddComment.text = "ثبت نظر"
                        Toast.makeText(itemView.context, "خطا در ثبت نظر", Toast.LENGTH_SHORT).show()
                    }
                },
                { error ->
                    btnAddComment.isEnabled = true
                    btnAddComment.text = "ثبت نظر"
                    error.printStackTrace()
                    Toast.makeText(itemView.context, "خطا در اتصال به شبکه", Toast.LENGTH_SHORT).show()
                }
            )
            VolleySingleton.getInstance(itemView.context).add(request)
        }

        private fun displaySeenBy(seenBy: List<String>) {
            val container = itemView.findViewById<LinearLayout>(R.id.seenByContainer)
            container.removeAllViews()

            if (seenBy.isEmpty()) return

            val maxDisplay = 6
            val toShow = seenBy.take(maxDisplay)
            val remaining = seenBy.size - maxDisplay

            toShow.forEach { userId ->
                val userName = UserCache.getName(userId)
                val firstLetter = if (userName.isNotEmpty() && userName != "نامشخص")
                    userName.firstOrNull()?.toString()?.uppercase() ?: "?"
                else "?"

                val circleView = createSmallCircleWithLetter(firstLetter, userId)
                container.addView(circleView)
            }

            if (remaining > 0) {
                val moreView = TextView(itemView.context).apply {
                    text = "+$remaining"
                    textSize = 9f
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

        private fun createSmallCircleWithLetter(letter: String, userId: String): View {
            return TextView(itemView.context).apply {
                text = letter
                textSize = 8f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER

                val size = 16.dpToPx()
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    marginEnd = 2.dpToPx()
                }

                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(getColorForUserId(userId))
                }
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