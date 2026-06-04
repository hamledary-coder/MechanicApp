package com.Mechanic.Workshop.ui.task.log

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.Mechanic.Workshop.R
import com.Mechanic.Workshop.data.remote.Config
import com.Mechanic.Workshop.ui.task.dialog.SelectGroupDialog
import TaskModel
import java.text.SimpleDateFormat
import java.util.*
import android.app.TimePickerDialog
import android.content.Intent
import android.util.Log
import android.view.View
import com.Mechanic.Workshop.data.model.TaskLogModel
import com.Mechanic.Workshop.ui.task.repository.TaskLogRepository
import java.util.Calendar
import ir.hamsaa.persiandatepicker.PersianDatePickerDialog
import ir.hamsaa.persiandatepicker.api.PersianPickerDate
import ir.hamsaa.persiandatepicker.api.PersianPickerListener
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.Mechanic.Workshop.data.model.Employee
import com.Mechanic.Workshop.ui.task.detail.TaskDetailActivity
import com.Mechanic.Workshop.ui.task.dialog.WorkConditionDialog
import com.Mechanic.Workshop.utils.VolleySingleton
import com.Mechanic.Workshop.utils.UserCache
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import org.json.JSONObject


class AddLogActivity : AppCompatActivity() {

    private lateinit var etActionDescription: EditText
    private lateinit var etDate: EditText
    private lateinit var etStartTime: EditText
    private lateinit var etEndTime: EditText
    private lateinit var tvGroupValue: TextView
    private lateinit var btnEditGroup: Button
    private lateinit var spinnerNewStatus: Spinner
    private lateinit var etNotes: EditText
    private lateinit var btnSubmit: Button
    private lateinit var btnCancel: Button
    private lateinit var tvDuration: TextView
    private lateinit var tvConditionalLabel: TextView
    private lateinit var etConditionalText: EditText

    private var task: TaskModel? = null
    private var currentGroupIds: String = ""
    private lateinit var taskLogRepository: TaskLogRepository
    private lateinit var btnWorkCondition: Button
    private var currentHeat = 30
    private var currentPollution = 0
    private var currentWorkType = "fixed_equipment"

    private var isEditMode = false
    private var editingLogId: String? = null

    // متغیر برای جلوگیری از بارگذاری مکرر
    private var isUserCacheLoaded = false

    companion object {
        private const val STATUS_CONTINUE = "2"
        private const val STATUS_STOPPED = "3"
        private const val STATUS_COMPLETED = "4"

        private const val WORK_TYPE_FIXED = "1"
        private const val WORK_TYPE_ROTATING = "2"
        private const val WORK_TYPE_INSPECTION = "3"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_log)


        supportActionBar?.setDisplayHomeAsUpEnabled(true)


        taskLogRepository = TaskLogRepository(this)

        initEditMode()
        initTaskData()
        initViews()
        setupDatePicker()
        setupTimePickers()
        setupGroupSelection()
        setupStatusSpinner()
        setupButtons()
        setupWorkConditionDialog()

        // اطمینان از بارگذاری UserCache
        ensureUserCacheLoaded()

        updateGroupDisplay()

        if (isEditMode) {
            loadLogForEdit()
            btnSubmit.text = "ویرایش گزارش"
        }
    }

    private fun ensureUserCacheLoaded() {
        // اگر کش خالی است یا هنوز بارگذاری نشده
        if (UserCache.getSize() == 0 && !isUserCacheLoaded) {
            isUserCacheLoaded = true
            UserCache.loadAllUsers {
                runOnUiThread {
                    updateGroupDisplay()
                }
            }
        }
    }

    private fun initEditMode() {
        isEditMode = intent.getBooleanExtra("IS_EDIT_MODE", false)
        editingLogId = intent.getStringExtra("LOG_ID")
        supportActionBar?.title = if (isEditMode) "ویرایش گزارش" else "ثبت گزارش جدید"
    }

    private fun initTaskData() {
        task = TaskModel(
            id = intent.getStringExtra("TASK_ID") ?: "",
            createDate = intent.getStringExtra("DATE") ?: "",
            title = intent.getStringExtra("TITLE") ?: "",
            description = intent.getStringExtra("DESC") ?: "",
            creator = intent.getStringExtra("CREATOR") ?: "",
            status = "1",
            assignedTo = intent.getStringExtra("ASSIGNED_TO") ?: "",
            responsible = intent.getStringExtra("RESPONSIBLE") ?: "",
            pendingInvites = "",
            unit = intent.getStringExtra("UNIT") ?: "",
            priority = intent.getStringExtra("PRIORITY") ?: "",
            sub_unit = intent.getStringExtra("SUB_UNIT") ?: "",
            declaration_method = intent.getStringExtra("DECLARATION_METHOD") ?: "",
            requester = intent.getStringExtra("REQUESTER") ?: "",
            request_date = intent.getStringExtra("REQUEST_DATE") ?: "",
            urgency = intent.getStringExtra("URGENCY") ?: "",
            initial_review = intent.getStringExtra("INITIAL_REVIEW") ?: "",
            system_request_number = intent.getStringExtra("SYSTEM_REQUEST_NUMBER") ?: ""
        )
        currentGroupIds = task?.assignedTo ?: ""
    }

    private fun initViews() {
        etActionDescription = findViewById(R.id.etActionDescription)
        etDate = findViewById(R.id.etDate)
        etStartTime = findViewById(R.id.etStartTime)
        etEndTime = findViewById(R.id.etEndTime)
        tvGroupValue = findViewById(R.id.tvGroupValue)
        btnEditGroup = findViewById(R.id.btnEditGroup)
        spinnerNewStatus = findViewById(R.id.spinnerNewStatus)
        etNotes = findViewById(R.id.etNotes)
        btnSubmit = findViewById(R.id.btnSubmit)
        btnCancel = findViewById(R.id.btnCancel)
        tvDuration = findViewById(R.id.tvDuration)
        btnWorkCondition = findViewById(R.id.btnWorkCondition)
        tvConditionalLabel = findViewById(R.id.tvConditionalLabel)
        etConditionalText = findViewById(R.id.etConditionalText)

        btnWorkCondition.visibility = View.VISIBLE
        updateWorkConditionButton()

        if (!isEditMode) {
            etStartTime.setText("08:00")
            etEndTime.setText("12:00")
            calculateDuration()
            val today = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date())
            etDate.setText(today)
        }
    }

    private fun setupWorkConditionDialog() {
        btnWorkCondition.setOnClickListener {
            val dialog = WorkConditionDialog(
                context = this,
                taskId = task?.id ?: "",
                onConfirm = { heat, pollution, workType ->
                    updateWorkConditionData(heat, pollution, workType, false)
                },
                onApplyToAll = { heat, pollution, workType ->
                    updateWorkConditionData(heat, pollution, workType, true)
                }
            )
            dialog.show()
        }
    }

    private fun updateWorkConditionData(heat: Int, pollution: Int, workType: String, applyToAll: Boolean) {
        currentHeat = heat
        currentPollution = pollution
        currentWorkType = workType
        updateWorkConditionButton()

        val workTypeText = getWorkTypeText(currentWorkType)
        val message = "شرایط کار ذخیره شد: $heat°C / $pollution ppm - نوع کار: $workTypeText"

        if (applyToAll) {
            applyWorkConditionToAll(heat, pollution, workType)
        } else {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun getWorkTypeText(workType: String): String = when (workType) {
        WORK_TYPE_FIXED -> "تجهیزات ثابت"
        WORK_TYPE_ROTATING -> "عیب‌یابی تجهیزات دوار"
        WORK_TYPE_INSPECTION -> "بررسی"
        else -> "نامشخص"
    }

    private fun applyWorkConditionToAll(heat: Int, pollution: Int, workType: String) {
        val json = JSONObject().apply {
            put("action", "applyWorkConditionToAll")
            put("taskId", task?.id ?: "")
            put("heatLevel", heat)
            put("pollutionLevel", pollution)
            put("workType", workType)
        }

        val request = JsonObjectRequest(
            Request.Method.POST, Config.BASE_URL, json,
            {
                Toast.makeText(this, "شرایط کار به همه گزارش‌ها اعمال شد", Toast.LENGTH_SHORT).show()
                finish()
                startActivity(intent)
            },
            { Toast.makeText(this, "خطا در اعمال به همه", Toast.LENGTH_SHORT).show() }
        )
        VolleySingleton.getInstance(this).add(request)
    }

    private fun updateWorkConditionButton() {
        val workTypeText = getWorkTypeText(currentWorkType)
        btnWorkCondition.text = "شرایط کار: $currentHeat°C / ${currentPollution}ppm - $workTypeText"
    }

    private fun loadLogForEdit() {
        val taskId = task?.id ?: ""
        val logId = editingLogId ?: ""

        taskLogRepository.getTaskLogs(taskId,
            onSuccess = { logs ->
                val log = logs.find { it.id == logId }
                if (log != null) {
                    populateLogData(log)
                } else {
                    showErrorAndFinish("گزارش یافت نشد")
                }
            },
            onError = { message ->
                showErrorAndFinish("خطا در بارگذاری: $message")
            }
        )
    }

    private fun populateLogData(log: TaskLogModel) {
        etActionDescription.setText(log.actionDescription)
        etDate.setText(log.date)
        etStartTime.setText(log.startTime)
        etEndTime.setText(log.endTime)
        calculateDuration()

        currentGroupIds = log.assignedUsers
        updateGroupDisplay()

        currentHeat = log.heatLevel
        currentPollution = log.pollutionLevel
        currentWorkType = log.workType
        updateWorkConditionButton()

        etConditionalText.setText(log.notes)

        val statusCodes = listOf(STATUS_CONTINUE, STATUS_STOPPED, STATUS_COMPLETED)
        val statusIndex = statusCodes.indexOf(log.newStatus)
        if (statusIndex >= 0) {
            spinnerNewStatus.setSelection(statusIndex)
            updateConditionalFields(statusIndex)
        }
    }

    private fun showErrorAndFinish(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupDatePicker() {
        etDate.setOnClickListener { showDatePicker() }
    }

    private fun showDatePicker() {
        val currentDate = etDate.text.toString().split("/")
        val initYear = currentDate.getOrNull(0)?.toIntOrNull() ?: 1400
        val initMonth = currentDate.getOrNull(1)?.toIntOrNull() ?: 1
        val initDay = currentDate.getOrNull(2)?.toIntOrNull() ?: 1

        PersianDatePickerDialog(this)
            .setPositiveButtonString("تأیید")
            .setNegativeButton("انصراف")
            .setTodayButton("امروز")
            .setTodayButtonVisible(true)
            .setInitDate(initYear, initMonth, initDay)
            .setMinYear(1400)
            .setMaxYear(PersianDatePickerDialog.THIS_YEAR)
            .setListener(object : PersianPickerListener {
                override fun onDateSelected(persianPickerDate: PersianPickerDate) {
                    val year = persianPickerDate.persianYear
                    val month = String.format("%02d", persianPickerDate.persianMonth)
                    val day = String.format("%02d", persianPickerDate.persianDay)
                    etDate.setText("$year/$month/$day")
                }
                override fun onDismissed() { }
            })
            .show()
    }

    private fun setupTimePickers() {
        etStartTime.setOnClickListener {
            showTimePicker(etStartTime)
        }
        etEndTime.setOnClickListener {
            showTimePicker(etEndTime)
        }
    }

    private fun showTimePicker(editText: EditText) {
        val currentTime = editText.text.toString()
        val hour = if (currentTime.isNotEmpty() && currentTime.contains(":")) {
            currentTime.split(":")[0].toIntOrNull() ?: 8
        } else {
            8
        }

        val minute = if (currentTime.isNotEmpty() && currentTime.contains(":")) {
            currentTime.split(":")[1].toIntOrNull() ?: 0
        } else {
            0
        }

        val timePickerDialog = TimePickerDialog(
            this,
            { _, hourOfDay, minuteOfHour ->
                val time = String.format("%02d:%02d", hourOfDay, minuteOfHour)
                editText.setText(time)
                calculateDuration()
            },
            hour,
            minute,
            true
        )
        timePickerDialog.show()
    }

    private fun calculateDuration() {
        val start = etStartTime.text.toString()
        val end = etEndTime.text.toString()

        if (start.isNotEmpty() && end.isNotEmpty() && start.contains(":") && end.contains(":")) {
            try {
                val startParts = start.split(":")
                val endParts = end.split(":")

                if (startParts.size == 2 && endParts.size == 2) {
                    val startHour = startParts[0].toInt()
                    val startMinute = startParts[1].toInt()
                    val endHour = endParts[0].toInt()
                    val endMinute = endParts[1].toInt()

                    var durationMinutes = (endHour * 60 + endMinute) - (startHour * 60 + startMinute)
                    if (durationMinutes < 0) durationMinutes += 24 * 60

                    val hours = durationMinutes / 60
                    val minutes = durationMinutes % 60

                    tvDuration.text = "مدت زمان: $hours ساعت و $minutes دقیقه"
                    tvDuration.visibility = View.VISIBLE
                    return
                }
            } catch (e: Exception) {
                Log.e("AddLog", "Error calculating duration", e)
            }
        }
        tvDuration.visibility = View.GONE
    }

    private fun setupGroupSelection() {
        btnEditGroup.setOnClickListener {
            val originalGroupIds = task?.assignedTo ?: ""
            val originalGroupIdList = originalGroupIds.split(",").filter { it.isNotEmpty() }

            // ساخت لیست کارمندانی که در گروه اصلی هستند با استفاده از UserCache
            val groupMembers = originalGroupIdList.mapNotNull { id ->
                val name = UserCache.getName(id)
                // اگر نام با "کاربر" شروع می‌شود، یعنی در کش نیست اما ID معتبر است
                if (name != "نامشخص" && !name.startsWith("کاربر")) {
                    Employee(id, name, "")
                } else {
                    null
                }
            }

            // اگر لیست خالی است یا کش هنوز بارگذاری نشده، ابتدا بارگذاری کن
            if (groupMembers.isEmpty() && originalGroupIdList.isNotEmpty()) {
                Toast.makeText(this, "در حال بارگذاری اسامی کاربران...", Toast.LENGTH_SHORT).show()
                UserCache.loadAllUsers {
                    runOnUiThread {
                        // بعد از بارگذاری، دوباره دیالوگ را باز کن
                        setupGroupSelection() // بازگشت به این تابع
                        btnEditGroup.performClick()
                    }
                }
                return@setOnClickListener
            }

            val dialog = SelectGroupDialog(
                currentGroupIds = currentGroupIds,
                availableEmployees = groupMembers
            ) { newGroupIds ->
                currentGroupIds = newGroupIds
                updateGroupDisplay()
            }
            dialog.show(supportFragmentManager, "SelectGroupDialog")
        }
    }

    private fun updateGroupDisplay() {
        if (currentGroupIds.isNotEmpty()) {
            val ids = currentGroupIds.split(",").map { it.trim() }.filter { it.isNotEmpty() }

            if (ids.isEmpty()) {
                tvGroupValue.text = "تعیین نشده"
                return
            }

            // استفاده از UserCache برای دریافت نام‌ها
            val names = ids.mapNotNull { id ->
                val name = UserCache.getName(id)
                // اگر نام با "کاربر" شروع می‌شود و کش پر است، یعنی کاربر در کش نیست
                if (name != "نامشخص" && !name.startsWith("کاربر $id")) {
                    name
                } else {
                    null
                }
            }

            if (names.isNotEmpty()) {
                tvGroupValue.text = names.joinToString("، ")
            } else {
                // اگر نامی پیدا نشد، IDها را نشان بده
                tvGroupValue.text = ids.joinToString("، ")
            }
        } else {
            tvGroupValue.text = "تعیین نشده"
        }
    }

    private fun updateConditionalFields(statusIndex: Int) {
        when (statusIndex) {
            0 -> { // ادامه دارد
                tvConditionalLabel.text = "شرح اقدام بعدی"
                tvConditionalLabel.visibility = View.VISIBLE
                etConditionalText.visibility = View.VISIBLE
                etConditionalText.hint = "برنامه بعدی برای این کار را وارد کنید..."
            }
            1 -> { // متوقف
                tvConditionalLabel.text = "علت توقف"
                tvConditionalLabel.visibility = View.VISIBLE
                etConditionalText.visibility = View.VISIBLE
                etConditionalText.hint = "دلیل توقف کار را وارد کنید..."
            }
            2 -> { // اتمام کار
                tvConditionalLabel.visibility = View.GONE
                etConditionalText.visibility = View.GONE
                etConditionalText.setText("")
            }
        }
    }

    private fun setupStatusSpinner() {
        val statusList = listOf(
            STATUS_CONTINUE to "ادامه دارد",
            STATUS_STOPPED to "متوقف",
            STATUS_COMPLETED to "اتمام کار"
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, statusList.map { it.second })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerNewStatus.adapter = adapter

        spinnerNewStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                updateConditionalFields(position)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        updateConditionalFields(spinnerNewStatus.selectedItemPosition)
    }

    private fun setupButtons() {
        btnSubmit.setOnClickListener {
            if (validateForm()) {
                if (isEditMode) updateLog() else submitLog()
            }
        }
        btnCancel.setOnClickListener { finish() }
    }

    private fun validateForm(): Boolean {
        if (etActionDescription.text.isNullOrBlank()) {
            Toast.makeText(this, "لطفاً شرح اقدام را وارد کنید", Toast.LENGTH_SHORT).show()
            return false
        }
        if (etDate.text.isNullOrBlank()) {
            Toast.makeText(this, "لطفاً تاریخ را انتخاب کنید", Toast.LENGTH_SHORT).show()
            return false
        }

        if (etConditionalText.visibility == View.VISIBLE && etConditionalText.text.isNullOrBlank()) {
            val message = when (spinnerNewStatus.selectedItemPosition) {
                0 -> "لطفاً شرح اقدام بعدی را وارد کنید"
                1 -> "لطفاً علت توقف را وارد کنید"
                else -> ""
            }
            if (message.isNotEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                return false
            }
        }

        return true
    }

    private fun getSelectedStatus(): String {
        return when (spinnerNewStatus.selectedItemPosition) {
            0 -> STATUS_CONTINUE
            1 -> STATUS_STOPPED
            2 -> STATUS_COMPLETED
            else -> STATUS_CONTINUE
        }
    }

    private fun getNewTaskStatus(logStatus: String): String {
        return if (logStatus == STATUS_COMPLETED) STATUS_COMPLETED else STATUS_CONTINUE
    }

    private fun getCurrentUserInfo(): Pair<String, String> {
        val sharedPref = getSharedPreferences(Config.PrefKeys.USER_PREFS, MODE_PRIVATE)
        val userId = sharedPref.getString(Config.PrefKeys.USER_ROW_ID, "") ?: ""
        val userName = sharedPref.getString(Config.PrefKeys.USERNAME, "کاربر") ?: "کاربر"
        return Pair(userId, userName)
    }

    private fun getFinalNotes(): String {
        return if (etConditionalText.visibility == View.VISIBLE && etConditionalText.text.isNotEmpty()) {
            etConditionalText.text.toString()
        } else {
            etNotes.text.toString()
        }
    }

    private fun createTaskLogModel(status: String): TaskLogModel {
        val (userId, userName) = getCurrentUserInfo()

        return TaskLogModel(
            id = if (isEditMode) editingLogId ?: "" else System.currentTimeMillis().toString(),
            taskId = task?.id ?: "",
            userId = userId,
            userName = userName,
            date = etDate.text.toString(),
            startTime = etStartTime.text.toString(),
            endTime = etEndTime.text.toString(),
            actionDescription = etActionDescription.text.toString(),
            assignedUsers = currentGroupIds,
            newStatus = status,
            attachments = "",
            notes = getFinalNotes(),
            heatLevel = currentHeat,
            pollutionLevel = currentPollution,
            workType = currentWorkType
        )
    }

    private fun submitLog() {
        val selectedStatus = getSelectedStatus()
        val newTaskStatus = getNewTaskStatus(selectedStatus)

        updateTaskStatusDirectly(newTaskStatus)

        val log = createTaskLogModel(selectedStatus)

        taskLogRepository.addTaskLog(
            log = log,
            onSuccess = {
                runOnUiThread {
                    Toast.makeText(this, "گزارش با موفقیت ثبت شد", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)

                    if (selectedStatus == STATUS_COMPLETED) {
                        navigateToTaskDetail()
                    }
                    finish()
                }
            },
            onError = { message ->
                runOnUiThread {
                    Toast.makeText(this, "خطا در ثبت: $message", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun updateLog() {
        val selectedStatus = getSelectedStatus()
        val newTaskStatus = getNewTaskStatus(selectedStatus)

        updateTaskStatusDirectly(newTaskStatus)

        val log = createTaskLogModel(selectedStatus)

        taskLogRepository.updateTaskLog(
            log = log,
            onSuccess = {
                runOnUiThread {
                    Toast.makeText(this, "گزارش با موفقیت ویرایش شد", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                }
            },
            onError = { message ->
                runOnUiThread {
                    Toast.makeText(this, "خطا در ویرایش: $message", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun updateTaskStatusDirectly(newStatus: String) {
        val url = "${Config.BASE_URL}?action=updateTaskStatus"
        val jsonObject = JSONObject().apply {
            put("taskId", task?.id ?: "")
            put("status", newStatus)
        }
        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonObject,
            { _ -> },
            { error -> Log.e("AddLog", "Error updating task status to $newStatus: ${error.message}") }
        )
        VolleySingleton.getInstance(this).add(request)
    }

    private fun navigateToTaskDetail() {
        val intent = Intent(this, TaskDetailActivity::class.java).apply {
            putExtra("TASK_ID", task?.id ?: "")
            putExtra("TITLE", task?.title ?: "")
            putExtra("DESC", task?.description ?: "")
            putExtra("CREATOR", task?.creator ?: "")
            putExtra("DATE", task?.createDate ?: "")
            putExtra("RESPONSIBLE", task?.responsible ?: "")
            putExtra("ASSIGNED_TO", task?.assignedTo ?: "")
            putExtra("UNIT", task?.unit ?: "")
            putExtra("PRIORITY", task?.priority ?: "")
            putExtra("SUB_UNIT", task?.sub_unit ?: "")
            putExtra("DECLARATION_METHOD", task?.declaration_method ?: "")
            putExtra("REQUESTER", task?.requester ?: "")
            putExtra("REQUEST_DATE", task?.request_date ?: "")
            putExtra("INITIAL_REVIEW", task?.initial_review ?: "")
            putExtra("SYSTEM_REQUEST_NUMBER", task?.system_request_number ?: "")
            putExtra("URGENCY", task?.urgency ?: "")
        }
        startActivity(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}