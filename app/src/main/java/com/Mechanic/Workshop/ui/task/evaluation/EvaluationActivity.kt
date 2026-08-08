package com.Mechanic.Workshop.ui.task.evaluation

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.Mechanic.Workshop.R
import com.Mechanic.Workshop.data.remote.Config
import com.Mechanic.Workshop.utils.UserCache
import com.Mechanic.Workshop.utils.VolleySingleton
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import org.json.JSONArray
import org.json.JSONObject


class EvaluationActivity : AppCompatActivity() {

    // ===== ویوها =====
    private lateinit var tvTaskTitle: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvTime: TextView
    private lateinit var tvDuration: TextView
    private lateinit var tvActionDescription: TextView

    private lateinit var rgWorkType: RadioGroup
    private lateinit var rbFixedEquipment: RadioButton
    private lateinit var rbRotatingEquipment: RadioButton
    private lateinit var rbInspection: RadioButton

    private lateinit var cardTeam: View
    private lateinit var rvTeamMembers: RecyclerView


    private lateinit var cardEvaluation: View
    private lateinit var tvEvaluationTitle: TextView
    private lateinit var sliderEvaluation: com.google.android.material.slider.Slider
    private lateinit var tvSliderMinLabel: TextView
    private lateinit var tvSliderMidLabel: TextView
    private lateinit var tvSliderMaxLabel: TextView
    private lateinit var tvSelectedValue: TextView

    private lateinit var seekBarHeat: SeekBar
    private lateinit var seekBarPollution: SeekBar
    private lateinit var tvHeatValue: TextView
    private lateinit var tvPollutionValue: TextView

    private lateinit var etNotes: EditText
    private lateinit var btnSubmit: Button
    private lateinit var btnSkip: Button
    // ===== ویوهای اسلایدر فنی =====
    private lateinit var layoutTechnical: LinearLayout
    private lateinit var sliderTechnical: com.google.android.material.slider.Slider
    private lateinit var tvTechMinLabel: TextView
    private lateinit var tvTechMidLabel: TextView
    private lateinit var tvTechMaxLabel: TextView
    private lateinit var tvTechSelected: TextView

    // ===== داده‌ها =====
    private var taskId: String = ""
    private var logId: String = ""
    private var taskTitle: String = ""
    private var taskDate: String = ""
    private var taskStartTime: String = ""
    private var taskEndTime: String = ""
    private var taskDescription: String = ""
    private var assignedUsers: String = ""
    private var durationMinutes: Int = 0
    private var isEditMode = false
    private var existingPhysicalDifficulty = 0
    private var existingTechnicalComplexity = 0
    private var existingHeatLevel = 30
    private var existingPollutionLevel = 0
    private var existingWorkType = "1"
    private var existingNotes = ""


    private var selectedWorkType: String = "1"  // 1=ثابت, 2=دوار, 3=بررسی
    private var selectedEvaluationValue: Int = 3  // 1-5 (اسلایدر فیزیکی)
    private var technicalComplexityValue: Int = 3  // 1-5 (اسلایدر فنی) ← جدید

    // ===== متغیر ضرایب =====
    private var maxPhysicalPercent: Int = 25
    private var maxTechnicalPercent: Int = 30
    private var maxTemperaturePercent: Int = 15
    private var maxPollutionPercent: Int = 20

    private var teamMembers = mutableListOf<TeamMember>()

    companion object {
        private const val WORK_TYPE_FIXED = "1"
        private const val WORK_TYPE_ROTATING = "2"
        private const val WORK_TYPE_INSPECTION = "3"
    }

    data class TeamMember(
        val userId: String,
        val name: String,
        val rawTimeMinutes: Int
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_evaluation)

        loadCoefficientsFromServer()
        setupToolbar()
        getIntentData()
        initViews()              // ← اول ویوها
        loadTeamMembers()        // ← بعد تیم
        checkExistingEvaluation() // ← بعد داده از سرور
        setupListeners()
        updateUI()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "🔍 ارزیابی کار"
        }
    }

    private fun getIntentData() {
        taskId = intent.getStringExtra("TASK_ID") ?: ""
        logId = intent.getStringExtra("LOG_ID") ?: ""
        taskTitle = intent.getStringExtra("TASK_TITLE") ?: ""
        taskDate = intent.getStringExtra("TASK_DATE") ?: ""
        taskStartTime = intent.getStringExtra("TASK_START_TIME") ?: ""
        taskEndTime = intent.getStringExtra("TASK_END_TIME") ?: ""
        taskDescription = intent.getStringExtra("TASK_DESCRIPTION") ?: ""
        assignedUsers = intent.getStringExtra("ASSIGNED_USERS") ?: ""
        durationMinutes = intent.getIntExtra("DURATION_MINUTES", 0)
    }

    private fun initViews() {

        // ===== اسلایدر فنی =====
        layoutTechnical = findViewById(R.id.layoutTechnical)
        sliderTechnical = findViewById(R.id.sliderTechnical)
        tvTechMinLabel = findViewById(R.id.tvTechMinLabel)
        tvTechMidLabel = findViewById(R.id.tvTechMidLabel)
        tvTechMaxLabel = findViewById(R.id.tvTechMaxLabel)
        tvTechSelected = findViewById(R.id.tvTechSelected)
        // اطلاعات گزارش
        tvTaskTitle = findViewById(R.id.tvTaskTitle)
        tvDate = findViewById(R.id.tvDate)
        tvTime = findViewById(R.id.tvTime)
        tvDuration = findViewById(R.id.tvDuration)
        tvActionDescription = findViewById(R.id.tvActionDescription)

        // نوع کار
        rgWorkType = findViewById(R.id.rgWorkType)
        rbFixedEquipment = findViewById(R.id.rbFixedEquipment)
        rbRotatingEquipment = findViewById(R.id.rbRotatingEquipment)
        rbInspection = findViewById(R.id.rbInspection)

        // تیم اجرایی
        cardTeam = findViewById(R.id.cardTeam)
        rvTeamMembers = findViewById(R.id.rvTeamMembers)


        // ارزیابی
        cardEvaluation = findViewById(R.id.cardEvaluation)
        tvEvaluationTitle = findViewById(R.id.tvEvaluationTitle)
        sliderEvaluation = findViewById(R.id.sliderEvaluation)
        tvSliderMinLabel = findViewById(R.id.tvSliderMinLabel)
        tvSliderMidLabel = findViewById(R.id.tvSliderMidLabel)
        tvSliderMaxLabel = findViewById(R.id.tvSliderMaxLabel)
        tvSelectedValue = findViewById(R.id.tvSelectedValue)

        // شرایط محیط کار
        seekBarHeat = findViewById(R.id.seekBarHeat)
        seekBarPollution = findViewById(R.id.seekBarPollution)
        tvHeatValue = findViewById(R.id.tvHeatValue)
        tvPollutionValue = findViewById(R.id.tvPollutionValue)

        // دکمه‌ها
        etNotes = findViewById(R.id.etNotes)
        btnSubmit = findViewById(R.id.btnSubmit)
        btnSkip = findViewById(R.id.btnSkip)

        // تنظیم اطلاعات گزارش
        tvTaskTitle.text = taskTitle
        tvDate.text = "تاریخ: $taskDate"
        tvTime.text = "ساعت: $taskStartTime - $taskEndTime"
        tvDuration.text = "⏱ مدت زمان: ${formatDuration(durationMinutes)}"
        tvActionDescription.text = "شرح اقدام: $taskDescription"
    }

    private fun setupListeners() {
        // انتخاب نوع کار
        rgWorkType.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbFixedEquipment -> {
                    selectedWorkType = WORK_TYPE_FIXED
                    showPhysicalEvaluation()
                }
                R.id.rbRotatingEquipment -> {
                    selectedWorkType = WORK_TYPE_ROTATING
                    showTechnicalEvaluation()
                }
                R.id.rbInspection -> {
                    selectedWorkType = WORK_TYPE_INSPECTION
                    showInspectionInfo()
                }
            }
            updateTeamTable()
        }

        // اسلایدر ارزیابی
        sliderEvaluation.addOnChangeListener { _, value, _ ->
            selectedEvaluationValue = value.toInt()
            updateSliderLabels()
            updateTeamTable()
        }

        // اسلایدر فنی
        sliderTechnical.addOnChangeListener { _, value, _ ->
            technicalComplexityValue = value.toInt()
            updateTechnicalSliderLabels()
            updateTeamTable()
        }

        // شرایط محیط کار
        seekBarHeat.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val heat = 30 + progress
                tvHeatValue.text = "$heat درجه"
                updateTeamTable()  // ← اضافه کن
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekBarPollution.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvPollutionValue.text = "$progress ppm"
                updateTeamTable()  // ← اضافه کن
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // دکمه‌ها
        btnSubmit.setOnClickListener { submitEvaluation() }
        btnSkip.setOnClickListener { finish() }
    }

    private fun loadTeamMembers() {
        Log.d("Evaluation", "=== loadTeamMembers START ===")
        Log.d("Evaluation", "durationMinutes: $durationMinutes")
        Log.d("Evaluation", "assignedUsers: '$assignedUsers'")

        if (assignedUsers.isEmpty() || assignedUsers == "0" || assignedUsers == "null") {
            cardTeam.visibility = View.GONE
            return
        }

        val userIds = assignedUsers.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        Log.d("Evaluation", "userIds after split: $userIds")

        if (userIds.isEmpty()) {
            cardTeam.visibility = View.GONE
            return
        }

        cardTeam.visibility = View.VISIBLE

        val perPersonMinutes = durationMinutes
        Log.d("Evaluation", "perPersonMinutes: $perPersonMinutes")

        teamMembers.clear()
        for (userId in userIds) {
            val name = UserCache.getName(userId)
            Log.d("Evaluation", "userId: '$userId' -> name: '$name'")
            // ✅ همه رو اضافه کن، حتی اگه اسم کامل نباشه
            teamMembers.add(TeamMember(userId, name, perPersonMinutes))
            Log.d("Evaluation", "✅ Added: $userId -> $name")
        }

        Log.d("Evaluation", "teamMembers size: ${teamMembers.size}")
        Log.d("Evaluation", "=== loadTeamMembers END ===")

        setupTeamAdapter()
        updateTeamTable()
    }

    // ===== متد دریافت ضرایب از سرور =====
    private fun loadCoefficientsFromServer() {
        val url = "${Config.BASE_URL}?action=getCoefficients"

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    maxPhysicalPercent = json.optInt("physical_difficulty", 25)
                    maxTechnicalPercent = json.optInt("technical_complexity", 30)
                    maxTemperaturePercent = json.optInt("temperature", 15)
                    maxPollutionPercent = json.optInt("pollution", 20)

                    // بعد از دریافت ضرایب، جدول را به‌روز کن
                    updateTeamTable()
                } catch (e: Exception) {
                    // در صورت خطا، از مقادیر پیش‌فرض استفاده کن
                    Log.e("Evaluation", "Error loading coefficients: ${e.message}")
                }
            },
            { error ->
                Log.e("Evaluation", "Network error: ${error.message}")
            }
        )
        VolleySingleton.getInstance(this).add(request)
    }

    private fun setupTeamAdapter() {
        rvTeamMembers.layoutManager = LinearLayoutManager(this)
        rvTeamMembers.setHasFixedSize(true)
        rvTeamMembers.isNestedScrollingEnabled = true  // ← این رو اضافه کن
        rvTeamMembers.adapter = TeamAdapter(teamMembers)

        // Force layout بعد از setAdapter
        rvTeamMembers.post {
            rvTeamMembers.requestLayout()
            rvTeamMembers.invalidate()
        }
    }

    private fun updateTeamTable() {
        val adapter = rvTeamMembers.adapter as? TeamAdapter

        // دریافت مقادیر
        val physicalValue = selectedEvaluationValue
        val technicalValue = technicalComplexityValue
        val heatProgress = seekBarHeat.progress
        val pollutionProgress = seekBarPollution.progress

        // محاسبه ضرایب با فرمول سهمی
        val physicalCoeff = calculateCoefficient(physicalValue, 5, maxPhysicalPercent)
        val technicalCoeff = calculateCoefficient(technicalValue, 5, maxTechnicalPercent)
        val tempCoeff = calculateCoefficient(heatProgress, 25, maxTemperaturePercent)
        val pollutionCoeff = calculateCoefficient(pollutionProgress, 100, maxPollutionPercent)

        // ضریب نهایی برای هر نفر
        val coefficients = teamMembers.mapIndexed { index, _ ->
            when (selectedWorkType) {
                WORK_TYPE_FIXED -> {
                    // همه نفرات: سختی فیزیکی + دما + آلودگی
                    physicalCoeff + tempCoeff + pollutionCoeff
                }
                WORK_TYPE_ROTATING -> {
                    if (index == 0) {
                        // مسئول کار (نفر اول): همه ضرایب (فیزیکی + فنی + دما + آلودگی)
                        physicalCoeff + technicalCoeff + tempCoeff + pollutionCoeff
                    } else {
                        // سایر نفرات: فقط فیزیکی + دما + آلودگی
                        physicalCoeff + tempCoeff + pollutionCoeff
                    }
                }
                else -> 0.0
            }
        }

        adapter?.updateCoefficients(coefficients)
    }

    private fun formatDuration(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return String.format("%d:%02d", hours, mins)
    }

    private fun showPhysicalEvaluation() {
        cardEvaluation.visibility = View.VISIBLE
        layoutTechnical.visibility = View.GONE  // ← اسلایدر فنی مخفی می‌شود

        tvEvaluationTitle.text = "💪 میزان سختی کار فیزیکی"
        tvSliderMinLabel.text = "خیلی سبک"
        tvSliderMidLabel.text = "متوسط"
        tvSliderMaxLabel.text = "خیلی سنگین"

        if (selectedEvaluationValue == 0) {
            selectedEvaluationValue = 1
            sliderEvaluation.value = 1f
        } else {
            sliderEvaluation.value = selectedEvaluationValue.toFloat()
        }

        updateSliderLabels()
        updateTeamTable()
    }

    private fun showTechnicalEvaluation() {
        cardEvaluation.visibility = View.VISIBLE
        layoutTechnical.visibility = View.VISIBLE

        // ===== اسلایدر فنی =====
        tvTechMinLabel.text = "خیلی کم"
        tvTechMidLabel.text = "متوسط"
        tvTechMaxLabel.text = "خیلی زیاد"
        if (technicalComplexityValue <= 0) {
            technicalComplexityValue = 1
            sliderTechnical.value = 1f
        } else {
            sliderTechnical.value = technicalComplexityValue.toFloat()
        }
        updateTechnicalSliderLabels()

        // ===== اسلایدر فیزیکی =====
        tvEvaluationTitle.text = "💪 میزان سختی کار فیزیکی"
        tvSliderMinLabel.text = "خیلی سبک"
        tvSliderMidLabel.text = "متوسط"
        tvSliderMaxLabel.text = "خیلی سنگین"
        if (selectedEvaluationValue <= 0) {
            selectedEvaluationValue = 1
            sliderEvaluation.value = 1f
        } else {
            sliderEvaluation.value = selectedEvaluationValue.toFloat()
        }
        updateSliderLabels()

        updateTeamTable()
    }

    private fun showInspectionInfo() {
        cardEvaluation.visibility = View.VISIBLE
        layoutTechnical.visibility = View.GONE
        tvEvaluationTitle.text = "ℹ️ این کار از نوع بررسی است"
        sliderEvaluation.visibility = View.GONE
        tvSliderMinLabel.visibility = View.GONE
        tvSliderMidLabel.visibility = View.GONE
        tvSliderMaxLabel.visibility = View.GONE
        tvSelectedValue.text = "نیازی به ارزیابی تکمیلی ندارد"
        selectedEvaluationValue = 0
        technicalComplexityValue = 0
        updateTeamTable()
    }

    private fun updateSliderLabels() {
        val value = selectedEvaluationValue
        val labels = when {
            value == 1 -> "خیلی سبک"
            value == 2 -> "سبک"
            value == 3 -> "متوسط"
            value == 4 -> "سنگین"
            value == 5 -> "خیلی سنگین"
            else -> "متوسط"
        }
        tvSelectedValue.text = "مقدار انتخاب شده: $labels"
        sliderEvaluation.visibility = View.VISIBLE
        tvSliderMinLabel.visibility = View.VISIBLE
        tvSliderMidLabel.visibility = View.VISIBLE
        tvSliderMaxLabel.visibility = View.VISIBLE
    }

    private fun updateTechnicalSliderLabels() {
        val value = technicalComplexityValue
        val labels = when {
            value == 1 -> "خیلی کم"
            value == 2 -> "کم"
            value == 3 -> "متوسط"
            value == 4 -> "زیاد"
            value == 5 -> "خیلی زیاد"
            else -> "متوسط"
        }
        tvTechSelected.text = "مقدار انتخاب شده: $labels"
    }

    private fun updateUI() {
        if (existingPhysicalDifficulty == 0 && existingTechnicalComplexity == 0) {
            rbFixedEquipment.isChecked = true
            selectedWorkType = WORK_TYPE_FIXED
            selectedEvaluationValue = 1
            sliderEvaluation.value = 1f
            showPhysicalEvaluation()
        }
    }

    private fun submitEvaluation() {
        // دریافت مقادیر
        val heat = 30 + seekBarHeat.progress
        val pollution = seekBarPollution.progress
        val notes = etNotes.text.toString().trim()

        val physicalDifficulty = when (selectedWorkType) {
            WORK_TYPE_FIXED -> selectedEvaluationValue
            WORK_TYPE_ROTATING -> selectedEvaluationValue  // ← مقدار اسلایدر فیزیکی
            else -> 0
        }

        val technicalComplexity = when (selectedWorkType) {
            WORK_TYPE_ROTATING -> technicalComplexityValue  // ← مقدار اسلایدر فنی
            else -> 0
        }

        // ===== ارسال به سرور =====
        val url = "${Config.BASE_URL}?action=updateTaskLog"
        val jsonObject = JSONObject().apply {
            put("logId", logId)
            put("date", taskDate)
            put("startTime", taskStartTime)
            put("endTime", taskEndTime)
            put("actionDescription", taskDescription)
            put("assignedUsers", assignedUsers)
            put("newStatus", "2")
            put("notes", notes)
            put("heatLevel", heat)
            put("pollutionLevel", pollution)
            put("workType", selectedWorkType)
            put("physicalDifficulty", physicalDifficulty)
            put("technicalComplexity", technicalComplexity)
        }


        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonObject,
            { response ->
                Log.d("Evaluation", "Response: $response")
                if (response.optString("status") == "success") {
                    Toast.makeText(this, "✅ ارزیابی با موفقیت ثبت شد", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    val message = response.optString("message", "خطا در ثبت ارزیابی")
                    Toast.makeText(this, "❌ $message", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("Evaluation", "Error: ${error.message}")
                Toast.makeText(this, "❌ خطا در اتصال به شبکه: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
        VolleySingleton.getInstance(this).add(request)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun checkExistingEvaluation() {
        // دریافت اطلاعات گزارش از سرور
        val url = "${Config.BASE_URL}?action=getTaskLogs&taskId=$taskId"

        val request = object : StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val jsonArray = JSONArray(response)
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        if (obj.getString("id") == logId) {
                            // پیدا کردن گزارش مربوطه
                            existingPhysicalDifficulty = obj.optInt("physical_difficulty", 0)
                            existingTechnicalComplexity = obj.optInt("technical_complexity", 0)
                            existingHeatLevel = obj.optInt("heat_level", 30)
                            existingPollutionLevel = obj.optInt("pollution_level", 0)
                            existingWorkType = obj.optString("work_type", "1")
                            existingNotes = obj.optString("notes", "")

                            // اگر مقداری وجود داشت، حالت ویرایش فعال می‌شود
                            if (existingPhysicalDifficulty > 0 || existingTechnicalComplexity > 0) {
                                isEditMode = true
                                supportActionBar?.title = "✏️ ویرایش ارزیابی"
                                btnSubmit.text = "✅ بروزرسانی ارزیابی"
                            }

                            // اعمال مقادیر موجود به UI
                            applyExistingData()
                            break
                        }
                    }
                } catch (e: Exception) {
                    Log.e("Evaluation", "Error loading existing data", e)
                }
            },
            { error ->
                Log.e("Evaluation", "Error: ${error.message}")
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                return hashMapOf(
                    "Cache-Control" to "no-cache, no-store, must-revalidate",
                    "Pragma" to "no-cache",
                    "Expires" to "0"
                )
            }
        }

        // استفاده از VolleySingleton
        VolleySingleton.getInstance(this).add(request)
    }

    private fun calculateCoefficient(value: Int, maxValue: Int, maxPercent: Int): Double {
        if (maxPercent == 0) return 0.0
        val x = value.toDouble() / maxValue
        return maxPercent.toDouble() / 100.0 * x
    }

    // ===== متد اعمال داده‌های موجود به UI =====
    private fun applyExistingData() {
        when (existingWorkType) {
            "1" -> {
                rbFixedEquipment.isChecked = true
                selectedWorkType = WORK_TYPE_FIXED
                if (existingPhysicalDifficulty > 0) {
                    selectedEvaluationValue = existingPhysicalDifficulty
                }
                showPhysicalEvaluation()
            }
            "2" -> {
                rbRotatingEquipment.isChecked = true
                selectedWorkType = WORK_TYPE_ROTATING
                technicalComplexityValue = if (existingTechnicalComplexity > 0) existingTechnicalComplexity else 1
                selectedEvaluationValue = if (existingPhysicalDifficulty > 0) existingPhysicalDifficulty else 1
                showTechnicalEvaluation()
            }
            "3" -> {
                rbInspection.isChecked = true
                selectedWorkType = WORK_TYPE_INSPECTION
                showInspectionInfo()
            }
        }

        // 2. تنظیم شرایط محیط کار
        seekBarHeat.progress = existingHeatLevel - 30
        tvHeatValue.text = "$existingHeatLevel درجه"

        seekBarPollution.progress = existingPollutionLevel
        tvPollutionValue.text = "$existingPollutionLevel ppm"

        // 3. تنظیم توضیحات
        etNotes.setText(existingNotes)

        // 4. بروزرسانی جدول
        updateTeamTable()
    }


    // ===== Adapter جدید برای تیم اجرایی =====
    inner class TeamAdapter(private val members: List<TeamMember>) :
        RecyclerView.Adapter<TeamAdapter.ViewHolder>() {

        private var coefficients: List<Double> = members.map { 0.0 }

        fun updateCoefficients(newCoefficients: List<Double>) {
            this.coefficients = newCoefficients
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_evaluation_team, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val member = members[position]
            val coeff = coefficients.getOrElse(position) { 0.0 }
            val finalMinutes = (member.rawTimeMinutes * (1 + coeff)).toInt()

            holder.tvName.text = member.name
            holder.tvRawTime.text = formatDuration(member.rawTimeMinutes)
            holder.tvFinalTime.text = formatDuration(finalMinutes)
            holder.tvPercentage.text = "+${(coeff * 100).toInt()}%"
        }

        override fun getItemCount(): Int = members.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvName: TextView = itemView.findViewById(R.id.tvMemberName)
            val tvRawTime: TextView = itemView.findViewById(R.id.tvRawTime)
            val tvFinalTime: TextView = itemView.findViewById(R.id.tvFinalTime)
            val tvPercentage: TextView = itemView.findViewById(R.id.tvPercentage)
        }
    }
}