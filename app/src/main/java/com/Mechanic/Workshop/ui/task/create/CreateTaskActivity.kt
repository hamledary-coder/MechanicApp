package com.Mechanic.Workshop.ui.task.create

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.Mechanic.Workshop.R
import com.Mechanic.Workshop.data.remote.Config
import com.Mechanic.Workshop.ui.task.repository.TaskRepository
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.io.IOException
import java.util.Calendar
import ir.hamsaa.persiandatepicker.PersianDatePickerDialog
import ir.hamsaa.persiandatepicker.api.PersianPickerDate
import ir.hamsaa.persiandatepicker.api.PersianPickerListener

class CreateTaskActivity : AppCompatActivity() {

    private var isEdit = false
    private var taskId: String? = null
    private val client = OkHttpClient.Builder()
        .cache(null)
        .build()
    private lateinit var taskRepository: TaskRepository

    // لیست‌های اسپینر
    private val units = arrayOf("", "بهره‌برداری", "مجموعه‌ها", "نمکزدایی", "تقویت فشار گاز", "داخلی")
    private val subUnits = arrayOf("", "11", "12", "145", "63", "13")
    private val declarationMethods = arrayOf("", "سامانه تعمیرات", "تلفنی")

    // ویوها
    private lateinit var btnSubmit: Button
    private lateinit var etTitle: EditText
    private lateinit var etTaskDescription: EditText
    private lateinit var spinnerUnit: Spinner
    private lateinit var spinnerSubUnit: Spinner
    private lateinit var spinnerDeclarationMethod: Spinner
    private lateinit var etRequester: EditText
    private lateinit var etRequestDate: EditText
    private lateinit var etInitialReview: EditText
    private lateinit var etSystemRequestNumber: EditText
    private lateinit var layoutSubUnit: LinearLayout
    private lateinit var layoutSystemNumber: LinearLayout
    private lateinit var rgUrgency: RadioGroup
    private lateinit var layoutDeclarationMethod: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_task)
        taskRepository = TaskRepository(this)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        initViews()
        setupSpinners()
        setupDatePicker()
        setupConditionalFields()
        loadEditDataIfNeeded()

        btnSubmit.setOnClickListener {
            if (validateFields()) {
                sendCreateTaskRequest()
            }
        }
    }

    private fun initViews() {
        btnSubmit = findViewById(R.id.btnSubmitTask)
        etTitle = findViewById(R.id.etTaskTitle)
        etTaskDescription = findViewById(R.id.etTaskDescription)
        spinnerUnit = findViewById(R.id.spinnerUnit)
        spinnerSubUnit = findViewById(R.id.spinnerSubUnit)
        spinnerDeclarationMethod = findViewById(R.id.spinnerDeclarationMethod)
        etRequester = findViewById(R.id.etRequester)
        etRequestDate = findViewById(R.id.etRequestDate)
        etInitialReview = findViewById(R.id.etInitialReview)
        etSystemRequestNumber = findViewById(R.id.etSystemRequestNumber)
        layoutSubUnit = findViewById(R.id.layoutSubUnit)
        layoutSystemNumber = findViewById(R.id.layoutSystemNumber)
        rgUrgency = findViewById(R.id.rgUrgency)
        layoutDeclarationMethod = findViewById(R.id.layoutDeclarationMethod)
    }

    private fun setupSpinners() {
        val unitAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, units)
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerUnit.adapter = unitAdapter

        val subUnitAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, subUnits)
        subUnitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSubUnit.adapter = subUnitAdapter

        val methodAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, declarationMethods)
        methodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDeclarationMethod.adapter = methodAdapter
    }

    private fun setupDatePicker() {
        val btnCalendar = findViewById<ImageButton>(R.id.btnCalendar)
        btnCalendar.setOnClickListener { showPersianDatePicker() }
        etRequestDate.setOnClickListener { showPersianDatePicker() }

        // تاریخ پیش‌فرض = امروز (میلادی)
        val calendar = Calendar.getInstance()
        etRequestDate.setText("${calendar.get(Calendar.YEAR)}/${calendar.get(Calendar.MONTH) + 1}/${calendar.get(Calendar.DAY_OF_MONTH)}")
    }

    private fun showPersianDatePicker() {
        val currentDate = etRequestDate.text.toString().split("/")
        val initYear = if (currentDate.size == 3) currentDate[0].toIntOrNull() ?: 1400 else 1400
        val initMonth = if (currentDate.size == 3) currentDate[1].toIntOrNull() ?: 1 else 1
        val initDay = if (currentDate.size == 3) currentDate[2].toIntOrNull() ?: 1 else 1

        PersianDatePickerDialog(this)
            .setPositiveButtonString("تأیید")
            .setNegativeButton("انصراف")
            .setTodayButton("امروز")
            .setTodayButtonVisible(true)
            .setInitDate(initYear, initMonth, initDay)
            .setMinYear(1300)
            .setMaxYear(PersianDatePickerDialog.THIS_YEAR)
            .setListener(object : PersianPickerListener {
                override fun onDateSelected(persianPickerDate: PersianPickerDate) {
                    val year = persianPickerDate.persianYear
                    val month = persianPickerDate.persianMonth
                    val day = persianPickerDate.persianDay
                    etRequestDate.setText("$year/$month/$day")
                }
                override fun onDismissed() { }
            })
            .show()
    }

    private fun setupConditionalFields() {
        spinnerUnit.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val selected = units[position]

                // زیرمجموعه
                layoutSubUnit.visibility = if (selected == "مجموعه‌ها") View.VISIBLE else View.GONE

                // داخلی
                val isInternal = selected == "داخلی"
                layoutDeclarationMethod.visibility = if (isInternal) View.GONE else View.VISIBLE
                layoutSystemNumber.visibility = if (isInternal) View.GONE else layoutSystemNumber.visibility
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        spinnerDeclarationMethod.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val selected = declarationMethods[position]
                if (selected == "سامانه تعمیرات") {
                    layoutSystemNumber.visibility = View.VISIBLE
                } else {
                    layoutSystemNumber.visibility = View.GONE
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun loadEditDataIfNeeded() {
        isEdit = intent.getBooleanExtra("IS_EDIT", false)
        taskId = intent.getStringExtra("TASK_ID")

        if (isEdit) {
            btnSubmit.text = "بروزرسانی کار"
            supportActionBar?.title = "ویرایش کار"

            etTitle.setText(intent.getStringExtra("TITLE"))
            etTaskDescription.setText(intent.getStringExtra("DESC"))
            etRequester.setText(intent.getStringExtra("REQUESTER"))
            etInitialReview.setText(intent.getStringExtra("INITIAL_REVIEW"))
            etRequestDate.setText(intent.getStringExtra("REQUEST_DATE"))
            etSystemRequestNumber.setText(intent.getStringExtra("SYSTEM_REQUEST_NUMBER"))

            // ========== واحد ==========
            val unit = intent.getStringExtra("UNIT") ?: ""
            val unitIndex = units.indexOf(unit)
            if (unitIndex >= 0) {
                spinnerUnit.setSelection(unitIndex)

                // نمایش زیرمجموعه اگر واحد == "مجموعه‌ها"
                layoutSubUnit.visibility = if (unit == "مجموعه‌ها") View.VISIBLE else View.GONE

                // غیرفعال کردن نحوه اعلام اگر واحد == "داخلی"
                spinnerDeclarationMethod.isEnabled = unit != "داخلی"
                if (unit == "داخلی") {
                    layoutDeclarationMethod.visibility = View.GONE
                    layoutSystemNumber.visibility = View.GONE
                }
            }

            // ========== زیرمجموعه ==========
            val subUnit = intent.getStringExtra("SUB_UNIT") ?: ""
            val subUnitIndex = subUnits.indexOf(subUnit)
            if (subUnitIndex >= 0) {
                spinnerSubUnit.setSelection(subUnitIndex)
            }

            // ========== نحوه اعلام ==========
            val method = intent.getStringExtra("DECLARATION_METHOD") ?: ""
            val methodIndex = declarationMethods.indexOf(method)
            if (methodIndex >= 0) {
                spinnerDeclarationMethod.setSelection(methodIndex)
                // نمایش شماره سامانه اگر نحوه اعلام == "سامانه تعمیرات"
                layoutSystemNumber.visibility = if (method == "سامانه تعمیرات") View.VISIBLE else View.GONE
            }

            // ========== فوریت ==========
            val urgency = intent.getStringExtra("URGENCY") ?: "عادی"
            when (urgency) {
                "خیلی زیاد" -> rgUrgency.check(R.id.rbUrgencyVeryHigh)
                "زیاد" -> rgUrgency.check(R.id.rbUrgencyHigh)
                else -> rgUrgency.check(R.id.rbUrgencyNormal)
            }
        }
    }

    private fun validateFields(): Boolean {
        val title = etTitle.text.toString().trim()
        if (title.isEmpty()) {
            Toast.makeText(this, "عنوان کار الزامی است", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun sendCreateTaskRequest() {
        val url = "${Config.Endpoints.CREATE_TASK}?_=${System.currentTimeMillis()}"
        //val url = Config.Endpoints.CREATE_TASK
        val sharedPref = getSharedPreferences(Config.PrefKeys.USER_PREFS, MODE_PRIVATE)
        val creatorRowId = sharedPref.getString(Config.PrefKeys.USER_ROW_ID, "") ?: ""

        if (creatorRowId.isEmpty()) {
            Toast.makeText(this, "خطا: شناسه کاربر یافت نشد", Toast.LENGTH_LONG).show()
            resetButton()
            return
        }

        btnSubmit.isEnabled = false
        btnSubmit.text = if (isEdit) "در حال بروزرسانی..." else "در حال ارسال..."

        val title = etTitle.text.toString().trim()
        val description = etTaskDescription.text.toString().trim()
        val requester = etRequester.text.toString().trim()
        val requestDate = etRequestDate.text.toString().trim()
        val initialReview = etInitialReview.text.toString().trim()
        val systemRequestNumber = etSystemRequestNumber.text.toString().trim()

        val unit = spinnerUnit.selectedItem?.toString() ?: ""
        val subUnit = if (layoutSubUnit.visibility == View.VISIBLE) {
            spinnerSubUnit.selectedItem?.toString() ?: ""
        } else ""

        val declarationMethod = if (spinnerDeclarationMethod.isEnabled) {
            spinnerDeclarationMethod.selectedItem?.toString() ?: ""
        } else ""

        val urgency = when (rgUrgency.checkedRadioButtonId) {
            R.id.rbUrgencyVeryHigh -> "خیلی زیاد"
            R.id.rbUrgencyHigh -> "زیاد"
            else -> "عادی"
        }

        val json = JSONObject().apply {
            if (isEdit) {
                put("action", "update")
                put("id", taskId ?: "")
            } else {
                put("action", "createTask")
                put("creator", creatorRowId)
            }
            put("title", title)
            put("urgency", urgency)

            if (description.isNotEmpty()) put("description", description)
            if (unit.isNotEmpty()) put("unit", unit)
            if (subUnit.isNotEmpty()) put("sub_unit", subUnit)
            if (declarationMethod.isNotEmpty()) put("declaration_method", declarationMethod)
            if (requester.isNotEmpty()) put("requester", requester)
            if (requestDate.isNotEmpty()) put("request_date", requestDate)
            if (initialReview.isNotEmpty()) put("initial_review", initialReview)
            if (systemRequestNumber.isNotEmpty()) put("system_request_number", systemRequestNumber)
        }

        val body = RequestBody.create("application/json; charset=utf-8".toMediaType(), json.toString())
        val request = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Cache-Control", "no-cache")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@CreateTaskActivity, "خطای شبکه: ${e.message}", Toast.LENGTH_LONG).show()
                    resetButton()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string() ?: ""
                runOnUiThread {
                    if (response.isSuccessful && responseBody.contains("success")) {
                        val msg = if (isEdit) "تغییرات با موفقیت ذخیره شد" else "کار با موفقیت ثبت شد"
                        Toast.makeText(this@CreateTaskActivity, msg, Toast.LENGTH_LONG).show()
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        Toast.makeText(this@CreateTaskActivity, "خطا: $responseBody", Toast.LENGTH_SHORT).show()
                        resetButton()
                    }
                }
            }
        })
    }

    private fun resetButton() {
        btnSubmit.isEnabled = true
        btnSubmit.text = if (isEdit) "بروزرسانی کار" else "ثبت کار"
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}