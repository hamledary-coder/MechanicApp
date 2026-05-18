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
import java.util.Calendar
class AddLogActivity : AppCompatActivity() {

    private lateinit var etActionDescription: EditText
    private lateinit var etDate: EditText
    private lateinit var etStartTime: EditText
    private lateinit var etEndTime: EditText
    private lateinit var etParts: EditText
    private lateinit var tvGroupValue: TextView
    private lateinit var btnEditGroup: Button
    private lateinit var spinnerNewStatus: Spinner
    private lateinit var etNotes: EditText
    private lateinit var btnSubmit: Button
    private lateinit var btnCancel: Button

    private var task: TaskModel? = null
    private var currentGroupIds: String = ""  // ذخیره ids گروه انتخاب‌شده

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_log)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "ثبت گزارش جدید"

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
        updateGroupDisplay()
    }

    private fun initViews() {
        etActionDescription = findViewById(R.id.etActionDescription)
        etDate = findViewById(R.id.etDate)
        etStartTime = findViewById(R.id.etStartTime)
        etEndTime = findViewById(R.id.etEndTime)
        etParts = findViewById(R.id.etParts)
        tvGroupValue = findViewById(R.id.tvGroupValue)
        btnEditGroup = findViewById(R.id.btnEditGroup)
        spinnerNewStatus = findViewById(R.id.spinnerNewStatus)
        etNotes = findViewById(R.id.etNotes)
        btnSubmit = findViewById(R.id.btnSubmit)
        btnCancel = findViewById(R.id.btnCancel)
    }

    private fun setupDatePicker() {
        etDate.setOnClickListener {
            showDatePicker()
        }
        // پیش‌فرض: تاریخ امروز
        val today = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date())
        etDate.setText(today)
    }

    private fun showDatePicker() {
        // TODO: پیاده‌سازی انتخابگر تاریخ شمسی (مثل کد قبلی)
        Toast.makeText(this, "انتخابگر تاریخ شمسی در حال اضافه شدن", Toast.LENGTH_SHORT).show()
    }

    private fun setupTimePickers() {
        etStartTime.setOnClickListener { showTimePicker(etStartTime) }
        etEndTime.setOnClickListener { showTimePicker(etEndTime) }
    }

    private fun showTimePicker(editText: EditText) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePicker = TimePickerDialog(this, { _, hourOfDay, minuteOfHour ->
            val time = String.format("%02d:%02d", hourOfDay, minuteOfHour)
            editText.setText(time)
        }, hour, minute, true)
        timePicker.show()
    }

    private fun setupGroupSelection() {
        btnEditGroup.setOnClickListener {
            val dialog = SelectGroupDialog(currentGroupIds) { newGroupIds ->
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

    private fun setupStatusSpinner() {
        // لیست وضعیت‌های مجاز (بسته به وضعیت فعلی کار)
        val statusList = listOf(
            "22" to "اقدام شده",
            "31" to "منتظر تعیین زمان شروع",
            "32" to "منتظر بهره‌بردار",
            "33" to "منتظر کالا/قطعه",
            "34" to "منتظر تست بهره‌بردار",
            "41" to "اتمام کار",
            "42" to "منتظر امضا"
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, statusList.map { it.second })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerNewStatus.adapter = adapter

        // TODO: اگر کاربر مسئول یا سرشیفت است، وضعیت‌های بیشتری نشان بده
    }

    private fun setupButtons() {
        btnSubmit.setOnClickListener {
            if (validateForm()) {
                submitLog()
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
        return true
    }

    private fun submitLog() {
        val selectedStatus = when (spinnerNewStatus.selectedItemPosition) {
            0 -> "22"
            1 -> "31"
            2 -> "32"
            3 -> "33"
            4 -> "34"
            5 -> "41"
            6 -> "42"
            else -> "22"
        }

        // TODO: ارسال به سرور
        Toast.makeText(this, "گزارش با موفقیت ثبت شد", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}