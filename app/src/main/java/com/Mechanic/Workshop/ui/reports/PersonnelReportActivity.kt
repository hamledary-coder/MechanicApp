package com.Mechanic.Workshop.ui.reports

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.Mechanic.Workshop.R
import com.Mechanic.Workshop.data.model.PersonnelReport
import com.Mechanic.Workshop.utils.TimeUtils
import com.Mechanic.Workshop.utils.evaluation.EvaluationCalculator
import com.Mechanic.Workshop.utils.evaluation.EvaluationCache
import com.Mechanic.Workshop.utils.evaluation.EvaluationRepository

class PersonnelReportActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var containerReports: LinearLayout
    private val reportList = mutableListOf<PersonnelReport>()
    private val viewStates = mutableMapOf<String, Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_personnel_report)

        setupToolbar()
        initViews()
        loadReport()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "📊 گزارش کارکرد پرسنل"
        }
    }

    private fun initViews() {
        progressBar = findViewById(R.id.progressBar)
        containerReports = findViewById(R.id.containerReports)
    }

    private fun loadReport() {
        showLoading(true)
        containerReports.removeAllViews()

        EvaluationCache.getCoefficients(this) { coefficients ->
            if (coefficients == null) {
                runOnUiThread {
                    showLoading(false)
                    Toast.makeText(this, "خطا در دریافت ضرایب از سرور", Toast.LENGTH_LONG).show()
                }
                return@getCoefficients
            }

            val repository = EvaluationRepository(this)

            repository.getAllTasks { tasks ->
                repository.getAllTaskLogs { allLogs ->
                    val indicators = EvaluationCalculator.aggregateIndicators(allLogs, tasks, coefficients)
                    val displayData = EvaluationCalculator.toDisplayFormat(indicators)

                    val reports = displayData.map { display ->
                        PersonnelReport(
                            userId = display.userId,
                            userName = display.userName,
                            rawResponsibility = display.rawResponsibility,
                            rawTechnical = display.rawTechnical,
                            rawPhysical = display.rawPhysical,
                            rawInspection = display.rawInspection,
                            finalResponsibility = display.finalResponsibility,
                            finalTechnical = display.finalTechnical,
                            finalPhysical = display.finalPhysical,
                            finalInspection = display.finalInspection
                        )
                    }

                    runOnUiThread {
                        showLoading(false)
                        reportList.clear()
                        reportList.addAll(reports)

                        if (reportList.isEmpty()) {
                            Toast.makeText(this, "هیچ داده‌ای یافت نشد", Toast.LENGTH_SHORT).show()
                        } else {
                            displaySections(reportList)
                        }
                    }
                }
            }
        }
    }

    private fun displaySections(reports: List<PersonnelReport>) {
        // 1. حجم کار با مسئولیت مستقیم
        addSectionWithToggle(
            sectionKey = "مسئولیت مستقیم",
            title = "📋 حجم کار با مسئولیت مستقیم",
            reports = reports,
            rawGetter = { it.rawResponsibility },
            finalGetter = { it.finalResponsibility }
        )

        // 2. حجم کار فیزیکی
        addSectionWithToggle(
            sectionKey = "کار فیزیکی",
            title = "💪 حجم کار فیزیکی",
            reports = reports,
            rawGetter = { it.rawPhysical },
            finalGetter = { it.finalPhysical }
        )

        // 3. حجم کار فنی
        addSectionWithToggle(
            sectionKey = "کار فنی",
            title = "🔧 حجم کار فنی",
            reports = reports,
            rawGetter = { it.rawTechnical },
            finalGetter = { it.finalTechnical }
        )

        // 4. کار بررسی (غیرفیزیکی)
        addSectionWithToggle(
            sectionKey = "کار بررسی",
            title = "🔍 کار بررسی (غیرفیزیکی)",
            reports = reports,
            rawGetter = { it.rawInspection },
            finalGetter = { it.finalInspection }
        )
    }

    private fun addSectionWithToggle(
        sectionKey: String,
        title: String,
        reports: List<PersonnelReport>,
        rawGetter: (PersonnelReport) -> String,
        finalGetter: (PersonnelReport) -> String
    ) {
        // فیلتر و مرتب‌سازی با ساعت اعشاری (از زیاد به کم)
        val filteredReports = reports
            .filter {
                val raw = rawGetter(it)
                raw != "00:00" && raw != "0:00" && raw != "0"
            }
            .sortedByDescending { TimeUtils.timeToDecimalHour(finalGetter(it)) } // ← تغییر اینجا

        if (filteredReports.isEmpty()) return

        val isChartMode = viewStates[sectionKey] ?: false

        val sectionView = LayoutInflater.from(this)
            .inflate(R.layout.item_section_report, containerReports, false)

        val tvTitle = sectionView.findViewById<TextView>(R.id.tvSectionTitle)
        val btnToggle = sectionView.findViewById<Button>(R.id.btnToggleView)
        val recyclerView = sectionView.findViewById<RecyclerView>(R.id.rvSectionItems)
        val chartContainer = sectionView.findViewById<LinearLayout>(R.id.chartContainer)
        val tableHeader = sectionView.findViewById<LinearLayout>(R.id.tableHeader)

        tvTitle.text = "$title (${filteredReports.size} نفر)"

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = SectionAdapter(filteredReports, rawGetter, finalGetter)
        recyclerView.isNestedScrollingEnabled = false
        recyclerView.setHasFixedSize(false)

        if (isChartMode) {
            chartContainer.visibility = View.VISIBLE
            tableHeader.visibility = View.GONE
            recyclerView.visibility = View.GONE
            btnToggle.text = "📋 جدول"
            showSimpleChart(chartContainer, filteredReports, finalGetter, title)
        } else {
            chartContainer.visibility = View.GONE
            tableHeader.visibility = View.VISIBLE
            recyclerView.visibility = View.VISIBLE
            btnToggle.text = "📊 نمودار"
        }

        btnToggle.setOnClickListener {
            val newMode = !viewStates.getOrDefault(sectionKey, false)
            viewStates[sectionKey] = newMode

            if (newMode) {
                chartContainer.visibility = View.VISIBLE
                tableHeader.visibility = View.GONE
                recyclerView.visibility = View.GONE
                btnToggle.text = "📋 جدول"
                // با post صدا بزن تا View اندازه‌گیری بشه
                chartContainer.post {
                    showSimpleChart(chartContainer, filteredReports, finalGetter, title)
                }
            } else {
                chartContainer.visibility = View.GONE
                tableHeader.visibility = View.VISIBLE
                recyclerView.visibility = View.VISIBLE
                btnToggle.text = "📊 نمودار"
            }
        }

        containerReports.addView(sectionView)
    }

    private fun showSimpleChart(
        chartContainer: LinearLayout,
        reports: List<PersonnelReport>,
        finalGetter: (PersonnelReport) -> String,
        title: String
    ) {
        // پاک کردن قبلی‌ها
        chartContainer.removeAllViews()

        // اگر ارتفاع صفر هست، منتظر بمان
        if (chartContainer.height == 0) {
            chartContainer.post {
                drawChart(chartContainer, reports, finalGetter, title)
            }
        } else {
            drawChart(chartContainer, reports, finalGetter, title)
        }
    }

    private fun drawChart(
        chartContainer: LinearLayout,
        reports: List<PersonnelReport>,
        finalGetter: (PersonnelReport) -> String,
        title: String
    ) {
        chartContainer.removeAllViews()

        val reportData = reports.map { report ->
            val hour = TimeUtils.timeToDecimalHour(finalGetter(report))
            Pair(report, hour)
        }.filter { it.second > 0 }

        if (reportData.isEmpty()) {
            chartContainer.visibility = View.GONE
            return
        }

        val maxHour = reportData.maxOfOrNull { it.second } ?: 1.0
        val scaleMax = maxOf(maxHour, 1.0)

        val containerHeight = chartContainer.height
        val textHeight = 45
        val maxBarHeight = if (containerHeight > textHeight) {
            containerHeight - textHeight
        } else {
            150
        }

        val itemCount = reportData.size

        reportData.forEachIndexed { index, (report, hour) ->
            val heightPercent = (hour / scaleMax).toFloat()
            val barHeight = (heightPercent * maxBarHeight).toInt()

            val barView = LayoutInflater.from(this)
                .inflate(R.layout.item_bar_chart, chartContainer, false)

            val tvValue = barView.findViewById<TextView>(R.id.tvBarValue)
            val viewBar = barView.findViewById<View>(R.id.barView)
            val tvLabel = barView.findViewById<TextView>(R.id.tvBarLabel)

            tvValue.text = String.format("%.1f", hour)

            val layoutParams = viewBar.layoutParams as LinearLayout.LayoutParams
            layoutParams.height = if (barHeight > 4) barHeight else 4
            layoutParams.topMargin = maxBarHeight - barHeight
            viewBar.layoutParams = layoutParams

            tvLabel.text = report.userName
            tvLabel.maxLines = 2
            tvLabel.minLines = 2

            val color = when {
                title.contains("مسئولیت") -> ContextCompat.getColor(this, R.color.primary_blue)
                title.contains("فنی") -> ContextCompat.getColor(this, R.color.priority_high)
                title.contains("بررسی") -> ContextCompat.getColor(this, R.color.priority_medium)
                title.contains("فیزیکی") -> ContextCompat.getColor(this, R.color.primary_green_dark)
                else -> ContextCompat.getColor(this, R.color.primary_blue)
            }
            viewBar.setBackgroundColor(color)

            // تنظیم وزن برای هر نوار (کمتر = باریک‌تر)
            val barLayoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT)
            barLayoutParams.weight = 0.7f  // عدد کمتر = باریک‌تر
            barView.layoutParams = barLayoutParams

            chartContainer.addView(barView)

            // 🔥 اضافه کردن فاصله بین نوارها (به جز بعد از آخرین نوار)
            if (index < itemCount - 1) {
                val space = View(this)
                val spaceParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT)
                spaceParams.weight = 0.9f  // مقدار فاصله (هرچه بیشتر = فاصله بیشتر)
                space.layoutParams = spaceParams
                chartContainer.addView(space)
            }
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    inner class SectionAdapter(
        private val reports: List<PersonnelReport>,
        private val rawGetter: (PersonnelReport) -> String,
        private val finalGetter: (PersonnelReport) -> String
    ) : RecyclerView.Adapter<SectionAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_section_row, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val report = reports[position]
            holder.tvName.text = report.userName
            holder.tvRaw.text = rawGetter(report)
            holder.tvFinal.text = finalGetter(report)
        }

        override fun getItemCount(): Int = reports.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvName: TextView = itemView.findViewById(R.id.tvName)
            val tvRaw: TextView = itemView.findViewById(R.id.tvRaw)
            val tvFinal: TextView = itemView.findViewById(R.id.tvFinal)
        }
    }
}