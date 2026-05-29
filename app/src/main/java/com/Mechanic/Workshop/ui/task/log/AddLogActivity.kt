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

    // فیلدهای جدید برای کادر شرطی
    private lateinit var tvConditionalLabel: TextView
    private lateinit var etConditionalText: EditText

    private var task: TaskModel? = null
    private var currentGroupIds: String = ""
    private lateinit var taskLogRepository: TaskLogRepository
    private lateinit var btnWorkCondition: Button
    private var currentHeat = 30
    private var currentPollution = 0
    private var currentWorkType = "fixed_equipment"  // مقدار پیش‌فرض

    // برای حالت ویرایش
    private var isEditMode = false
    private var editingLogId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_log)

        taskLogRepository = TaskLogRepository(this)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // بررسی حالت ویرایش
        isEditMode = intent.getBooleanExtra("IS_EDIT_MODE", false)
        editingLogId = intent.getStringExtra("LOG_ID")

        if (isEditMode) {
            supportActionBar?.title = "ویرایش گزارش"
        } else {
            supportActionBar?.title = "ثبت گزارش جدید"
        }

        // دریافت اطلاعات کار از Intent
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

        initViews()
        setupDatePicker()
        setupTimePickers()
        setupGroupSelection()
        setupStatusSpinner()
        setupButtons()

        btnWorkCondition.setOnClickListener {
            val dialog = WorkConditionDialog(
                context = this,
                taskId = task?.id ?: "",
                onConfirm = { heat, pollution, workType ->
                    currentHeat = heat
                    currentPollution = pollution
                    currentWorkType = workType
                    updateWorkConditionButton()

                    val workTypeText = when (currentWorkType) {
                        "1" -> "تجهیزات ثابت"
                        "2" -> "عیب‌یابی تجهیزات دوار"
                        "3" -> "بررسی"
                        else -> "نامشخص"
                    }

                    Toast.makeText(this, "شرایط کار ذخیره شد: $heat°C / $pollution ppm - نوع کار: $workTypeText", Toast.LENGTH_SHORT).show()
                },
                onApplyToAll = { heat, pollution, workType ->
                    currentHeat = heat
                    currentPollution = pollution
                    currentWorkType = workType
                    updateWorkConditionButton()

                    // ارسال به سرور برای اعمال به همه گزارش‌ها
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
                            // بعد از اعمال، صفحه را رفرش کن
                            finish()
                            startActivity(intent)
                        },
                        { Toast.makeText(this, "خطا در اعمال به همه", Toast.LENGTH_SHORT).show() }
                    )
                    VolleySingleton.getInstance(this).add(request)
                }
            )
            dialog.show()
        }
        updateGroupDisplay()

        // اگر حالت ویرایش است، اطلاعات گزارش را بارگذاری کن
        if (isEditMode) {
            loadLogForEdit()
            btnSubmit.text = "ویرایش گزارش"
        }
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

        btnWorkCondition.visibility = View.VISIBLE
        updateWorkConditionButton()



        // فیلدهای شرطی
        tvConditionalLabel = findViewById(R.id.tvConditionalLabel)
        etConditionalText = findViewById(R.id.etConditionalText)

        // ✅ تنظیم ساعت پیش‌فرض
        if (!isEditMode) {
            etStartTime.setText("08:00")
            etEndTime.setText("12:00")
            calculateDuration()
        }
    }

    private fun loadLogForEdit() {
        val taskId = task?.id ?: ""
        val logId = editingLogId ?: ""

        taskLogRepository.getTaskLogs(taskId,
            onSuccess = { logs ->
                val log = logs.find { it.id == logId }
                if (log != null) {
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

                    // بازیابی متن شرطی از notes (اگر جدا ذخیره نشده باشد)
                    etConditionalText.setText(log.notes)

                    // تنظیم وضعیت جدید در Spinner
                    val statusCodes = listOf("22", "3", "41")
                    val statusIndex = statusCodes.indexOf(log.newStatus)
                    if (statusIndex >= 0) {
                        spinnerNewStatus.setSelection(statusIndex)
                        // فعال کردن کادر شرطی بر اساس وضعیت
                        updateConditionalFields(statusIndex)
                    }

                    // تنظیم notes اصلی (اگر قبلاً متنی در آن بود)
                    etNotes.setText("")

                } else {
                    runOnUiThread {
                        Toast.makeText(this, "گزارش یافت نشد", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            },
            onError = { message ->
                runOnUiThread {
                    Toast.makeText(this, "خطا در بارگذاری: $message", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        )
    }

    private fun setupDatePicker() {
        etDate.setOnClickListener {
            showDatePicker()
        }
        if (!isEditMode) {
            val today = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date())
            etDate.setText(today)
        }
    }

    private fun showDatePicker() {
        val currentDate = etDate.text.toString().split("/")
        val initYear = if (currentDate.size == 3) currentDate[0].toIntOrNull() ?: 1400 else 1400
        val initMonth = if (currentDate.size == 3) currentDate[1].toIntOrNull() ?: 1 else 1
        val initDay = if (currentDate.size == 3) currentDate[2].toIntOrNull() ?: 1 else 1

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
                    val month = persianPickerDate.persianMonth
                    val day = persianPickerDate.persianDay
                    etDate.setText("$year/$month/$day")
                }
                override fun onDismissed() { }
            })
            .show()
    }

    private fun setupTimePickers() {
        etStartTime.setOnClickListener {
            showTimePicker(etStartTime)
            calculateDuration()
        }
        etEndTime.setOnClickListener {
            showTimePicker(etEndTime)
            calculateDuration()
        }
    }

    private fun showTimePicker(editText: EditText) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        val timePickerDialog = TimePickerDialog(
            this,
            { _, hourOfDay, minuteOfHour ->
                // ذخیره ساعت و دقیقه انتخاب شده توسط کاربر
                val time = String.format("%02d:%02d", hourOfDay, minuteOfHour)
                editText.setText(time)
                calculateDuration()
            },
            hour,
            0,  // ← فقط در زمان نمایش، دقیقه را 0 قرار بده
            true
        )
        timePickerDialog.show()
    }

    // تابع محاسبه مدت زمان
    private fun calculateDuration() {
        val start = etStartTime.text.toString()
        val end = etEndTime.text.toString()

        if (start.isNotEmpty() && end.isNotEmpty()) {
            try {
                val startHour = start.split(":")[0].toInt()
                val startMinute = start.split(":")[1].toInt()
                val endHour = end.split(":")[0].toInt()
                val endMinute = end.split(":")[1].toInt()

                var durationMinutes = (endHour * 60 + endMinute) - (startHour * 60 + startMinute)
                if (durationMinutes < 0) durationMinutes += 24 * 60 // اگر پایان روز بعد باشد

                val hours = durationMinutes / 60
                val minutes = durationMinutes % 60

                tvDuration.text = "مدت زمان: $hours ساعت و $minutes دقیقه"
                tvDuration.visibility = View.VISIBLE
            } catch (e: Exception) {
                tvDuration.visibility = View.GONE
            }
        } else {
            tvDuration.visibility = View.GONE
        }
    }

    private fun setupGroupSelection() {
        btnEditGroup.setOnClickListener {
            // دریافت لیست گروه اصلی از task.assignedTo (نه currentGroupIds که ممکن است تغییر کرده باشد)
            val originalGroupIds = task?.assignedTo ?: ""
            val originalGroupIdList = originalGroupIds.split(",").filter { it.isNotEmpty() }

            // ساخت لیست کارمندانی که در گروه اصلی هستند
            val groupMembers = originalGroupIdList.mapNotNull { id ->
                Config.UserCache.userMap[id]?.let { name -> Employee(id, name, "") }
            }

            val dialog = SelectGroupDialog(
                currentGroupIds = currentGroupIds,  // ← مقدار فعلی (برای پیش‌فرض تیک‌ها)
                availableEmployees = groupMembers   // ← همه اعضای گروه اصلی
            ) { newGroupIds ->
                currentGroupIds = newGroupIds
                updateGroupDisplay()
            }
            dialog.show(supportFragmentManager, "SelectGroupDialog")
        }
    }

    private fun updateGroupDisplay() {
        if (currentGroupIds.isNotEmpty()) {
            val names = currentGroupIds.split(",").mapNotNull {
                Config.UserCache.userMap[it.trim()]
            }
            tvGroupValue.text = names.joinToString("، ")
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
            "22" to "ادامه دارد",
            "3" to "متوقف",
            "41" to "اتمام کار"
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, statusList.map { it.second })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerNewStatus.adapter = adapter

        // شنونده برای تغییر وضعیت
        spinnerNewStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                updateConditionalFields(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // مقداردهی اولیه
        updateConditionalFields(spinnerNewStatus.selectedItemPosition)
    }

    private fun setupButtons() {
        btnSubmit.setOnClickListener {
            if (validateForm()) {
                if (isEditMode) {
                    updateLog()
                } else {
                    submitLog()
                }
            }
        }

        btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun validateForm(): Boolean {
        if (etActionDescription.text.isNullOrEmpty()) {
            Toast.makeText(this, "لطفاً شرح اقدام را وارد کنید", Toast.LENGTH_SHORT).show()
            return false
        }
        if (etDate.text.isNullOrEmpty()) {
            Toast.makeText(this, "لطفاً تاریخ را انتخاب کنید", Toast.LENGTH_SHORT).show()
            return false
        }

        // اعتبارسنجی کادر شرطی در صورت نمایش
        if (etConditionalText.visibility == View.VISIBLE && etConditionalText.text.isNullOrEmpty()) {
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

    private fun submitLog() {
        val selectedStatus = when (spinnerNewStatus.selectedItemPosition) {
            0 -> "22"
            1 -> "3"
            2 -> "41"
            else -> "22"
        }

        val sharedPref = getSharedPreferences(Config.PrefKeys.USER_PREFS, MODE_PRIVATE)
        val currentUserId = sharedPref.getString(Config.PrefKeys.USER_ROW_ID, "") ?: ""
        val currentUserName = sharedPref.getString(Config.PrefKeys.USERNAME, "کاربر") ?: "کاربر"

        val conditionalText = if (etConditionalText.visibility == View.VISIBLE) {
            etConditionalText.text.toString()
        } else {
            ""
        }

        val finalNotes = if (conditionalText.isNotEmpty()) {
            conditionalText
        } else {
            etNotes.text.toString()
        }

        val newLog = TaskLogModel(
            id = System.currentTimeMillis().toString(),
            taskId = task?.id ?: "",
            userId = currentUserId,
            userName = currentUserName,
            date = etDate.text.toString(),
            startTime = etStartTime.text.toString(),
            endTime = etEndTime.text.toString(),
            actionDescription = etActionDescription.text.toString(),
            assignedUsers = currentGroupIds,
            newStatus = selectedStatus,
            attachments = "",
            notes = finalNotes,
            heatLevel = currentHeat,
            pollutionLevel = currentPollution,
            workType = currentWorkType
        )

        taskLogRepository.addTaskLog(
            log = newLog,
            onSuccess = {
                runOnUiThread {
                    Toast.makeText(this, "گزارش با موفقیت ثبت شد", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)

                    if (selectedStatus == "41") {
                        // باز کردن صفحه جزئیات کار
                        val intent = Intent(this, TaskDetailActivity::class.java)
                        intent.putExtra("TASK_ID", task?.id ?: "")
                        intent.putExtra("TITLE", task?.title ?: "")
                        intent.putExtra("DESC", task?.description ?: "")
                        intent.putExtra("CREATOR", task?.creator ?: "")
                        intent.putExtra("DATE", task?.createDate ?: "")
                        intent.putExtra("RESPONSIBLE", task?.responsible ?: "")
                        intent.putExtra("ASSIGNED_TO", task?.assignedTo ?: "")
                        intent.putExtra("UNIT", task?.unit ?: "")
                        intent.putExtra("PRIORITY", task?.priority ?: "")
                        intent.putExtra("SUB_UNIT", task?.sub_unit ?: "")
                        intent.putExtra("DECLARATION_METHOD", task?.declaration_method ?: "")
                        intent.putExtra("REQUESTER", task?.requester ?: "")
                        intent.putExtra("REQUEST_DATE", task?.request_date ?: "")
                        intent.putExtra("INITIAL_REVIEW", task?.initial_review ?: "")
                        intent.putExtra("SYSTEM_REQUEST_NUMBER", task?.system_request_number ?: "")
                        intent.putExtra("URGENCY", task?.urgency ?: "")
                        startActivity(intent)
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
        val selectedStatus = when (spinnerNewStatus.selectedItemPosition) {
            0 -> "22"
            1 -> "3"
            2 -> "41"
            else -> "22"
        }

        val sharedPref = getSharedPreferences(Config.PrefKeys.USER_PREFS, MODE_PRIVATE)
        val currentUserId = sharedPref.getString(Config.PrefKeys.USER_ROW_ID, "") ?: ""
        val currentUserName = sharedPref.getString(Config.PrefKeys.USERNAME, "کاربر") ?: "کاربر"

        // دریافت متن شرطی
        val conditionalText = if (etConditionalText.visibility == View.VISIBLE) {
            etConditionalText.text.toString()
        } else {
            ""
        }

        val finalNotes = if (conditionalText.isNotEmpty()) {
            conditionalText
        } else {
            etNotes.text.toString()
        }

        val updatedLog = TaskLogModel(
            id = editingLogId ?: "",
            taskId = task?.id ?: "",
            userId = currentUserId,
            userName = currentUserName,
            date = etDate.text.toString(),
            startTime = etStartTime.text.toString(),
            endTime = etEndTime.text.toString(),
            actionDescription = etActionDescription.text.toString(),
            assignedUsers = currentGroupIds,
            newStatus = selectedStatus,
            attachments = "",
            notes = finalNotes,
            heatLevel = currentHeat,
            pollutionLevel = currentPollution,
            workType = currentWorkType

        )

        taskLogRepository.updateTaskLog(
            log = updatedLog,
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

    private fun updateWorkConditionButton() {
        val workTypeText = when (currentWorkType) {
            "1" -> "تجهیزات ثابت"
            "2" -> "عیب‌یابی تجهیزات دوار"
            "3" -> "بررسی"
            else -> "نامشخص"
        }
        btnWorkCondition.text = "شرایط کار: $currentHeat°C / ${currentPollution}ppm - $workTypeText"
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}