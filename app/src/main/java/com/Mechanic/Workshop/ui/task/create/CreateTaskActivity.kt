package com.Mechanic.Workshop.ui.task.create

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.Mechanic.Workshop.R
import com.Mechanic.Workshop.data.remote.Config
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley

class CreateTaskActivity : AppCompatActivity() {

    private var isEdit = false
    private var taskId: String? = null
    // تعریف آرایه‌ها
    private val units = arrayOf("انتخاب کنید", "گاز", "بهره‌برداری", "نمکزدایی", "مجموعه‌ها")
    private val priorities = arrayOf("انتخاب کنید", "اورژانسی", "بالا", "کم")
    private val unitCodes = arrayOf("", "1", "2", "3", "4")
    private val priorityCodes = arrayOf("", "1", "2", "3")
    private lateinit var btnSubmit: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_task)

        // فعال کردن فلش برگشت
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val etTitle = findViewById<EditText>(R.id.etTaskTitle)
        val etDescription = findViewById<EditText>(R.id.etTaskDescription)
        val tvTaskNumber = findViewById<TextView>(R.id.tvTaskNumber)
        val spinnerUnit = findViewById<Spinner>(R.id.spinnerUnit)
        val spinnerPriority = findViewById<Spinner>(R.id.spinnerPriority)

        val unitAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, units)
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerUnit.adapter = unitAdapter

        val priorityAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, priorities)
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPriority.adapter = priorityAdapter

        // ۲. مقداردهی متغیر سراسری (بدون کلمه val)
        btnSubmit = findViewById(R.id.btnSubmitTask)

        isEdit = intent.getBooleanExtra("IS_EDIT", false)
        taskId = intent.getStringExtra("TASK_ID")

        if (isEdit) {
            btnSubmit.text = "بروزرسانی کار"
            supportActionBar?.title = "ویرایش کار"
            etTitle.setText(intent.getStringExtra("TITLE"))
            etDescription.setText(intent.getStringExtra("DESC"))
            tvTaskNumber.text = "شماره کار: $taskId"

            // ✅ ست کردن مقادیر واحد و اهمیت برای ویرایش
            val unitCode = intent.getStringExtra("UNIT") ?: ""
            val priorityCode = intent.getStringExtra("PRIORITY") ?: ""

            if (unitCode.isNotEmpty()) {
                val unitIndex = unitCodes.indexOf(unitCode)
                if (unitIndex >= 0) spinnerUnit.setSelection(unitIndex)
            }

            if (priorityCode.isNotEmpty()) {
                val priorityIndex = priorityCodes.indexOf(priorityCode)
                if (priorityIndex >= 0) spinnerPriority.setSelection(priorityIndex)
            }
        }
        btnSubmit.setOnClickListener {
            val titleText = etTitle.text.toString().trim()
            val descText = etDescription.text.toString().trim()

            if (titleText.isEmpty() || descText.isEmpty()) {
                Toast.makeText(this, "لطفاً تمام فیلدها را پر کنید", Toast.LENGTH_SHORT).show()
            } else {
                val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                val currentUser = sharedPref.getString("username", "ناشناس") ?: "ناشناس"

                btnSubmit.isEnabled = false
                btnSubmit.text = "در حال ارسال..."

                sendTaskToWebscript(titleText, descText, currentUser)
            }
        }
    }

    private fun sendTaskToWebscript(title: String, desc: String, creator: String) {
        // ۱. آدرس URL جدید - 🔴 این خط تغییر کرد
        val url = Config.Endpoints.CREATE_TASK
        val spinnerUnit = findViewById<Spinner>(R.id.spinnerUnit)
        val spinnerPriority = findViewById<Spinner>(R.id.spinnerPriority)

        // دریافت کد ردیف کاربر به جای نام
        val sharedPref = getSharedPreferences(Config.PrefKeys.USER_PREFS, MODE_PRIVATE)
        val creatorRowId = sharedPref.getString(Config.PrefKeys.USER_ROW_ID, "") ?: ""
        val unitPosition = spinnerUnit.selectedItemPosition
        val priorityPosition = spinnerPriority.selectedItemPosition
        val unitCode = if (unitPosition > 0) unitCodes[unitPosition] else ""
        val priorityCode = if (priorityPosition > 0) priorityCodes[priorityPosition] else ""

        val stringRequest = object : StringRequest(Method.POST, url,
            Response.Listener { response ->
                // بررسی پاسخ موفقیت‌آمیز از اسکریپت جدید
                if (response.lowercase().contains("success")) {
                    val msg = if (isEdit) "تغییرات با موفقیت ذخیره شد" else "کار با موفقیت ثبت شد"
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

                    // بستن این صفحه و بازگشت به لیست اصلی
                    setResult(RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(this, "پاسخ سرور: $response", Toast.LENGTH_SHORT).show()
                    btnSubmit.isEnabled = true
                    btnSubmit.text = if (isEdit) "بروزرسانی کار" else "ثبت کار"
                }
            },
            Response.ErrorListener { error ->
                val errorMsg = error.networkResponse?.statusCode ?: error.message
                Toast.makeText(this, "خطای شبکه یا سرور: $errorMsg", Toast.LENGTH_SHORT).show()
                btnSubmit.isEnabled = true
                btnSubmit.text = if (isEdit) "بروزرسانی کار" else "ثبت کار"
            }) {

            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()

                // ۲. ارسال پارامترها دقیقاً مطابق با نیاز اسکریپت گوگل
                if (isEdit) {
                    params["action"] = "update"
                    params["id"] = taskId ?: ""

                } else {
                    params["action"] = "createTask"
                    params["creator"] = creatorRowId
                }

                params["title"] = title
                params["description"] = desc // در اسکریپت جدید هم description است
                params["unit"] = unitCode
                params["priority"] = priorityCode

                return params
            }
        }

        // تنظیم زمان انتظار (Timeout) برای جلوگیری از خطای زودهنگام
        stringRequest.retryPolicy = DefaultRetryPolicy(20000, 0, 1.0f)
        Volley.newRequestQueue(this).add(stringRequest)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}