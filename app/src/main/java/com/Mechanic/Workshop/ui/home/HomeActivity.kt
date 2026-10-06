package com.Mechanic.Workshop.ui.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import com.Mechanic.Workshop.R
import com.Mechanic.Workshop.data.remote.Config
import com.Mechanic.Workshop.data.sync.TaskSyncer
import com.Mechanic.Workshop.ui.archive.ArchiveActivity
import com.Mechanic.Workshop.ui.cartable.CartableActivity
import com.Mechanic.Workshop.ui.chat.ChatActivity
import com.Mechanic.Workshop.ui.reports.PersonnelReportActivity
import com.Mechanic.Workshop.ui.reports.ReportsActivity
import com.Mechanic.Workshop.ui.settings.SettingsActivity
import com.Mechanic.Workshop.ui.task.quicklog.QuickLogActivity
import com.Mechanic.Workshop.utils.UserCache
import com.Mechanic.Workshop.utils.VolleySingleton
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import org.json.JSONObject
import androidx.lifecycle.lifecycleScope
import com.Mechanic.Workshop.data.local.TaskLocalRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

class HomeActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var progressBar: ProgressBar
    private lateinit var tvWelcome: TextView
    private lateinit var cardReports: CardView
    private lateinit var cardCartable: CardView
    private lateinit var cardArchive: CardView
    private lateinit var cardSettings: CardView
    private lateinit var cardChat: CardView
    private lateinit var cardQuickLog: CardView
    private lateinit var cardPersonnelReport: CardView
    private lateinit var taskLocalRepository: TaskLocalRepository
    private var responsibleTasksJob: Job? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        initViews()
        setupToolbar()
        loadUserData()
        setupClickListeners()

        // ====== اطمینان از خاموش بودن کارت در ابتدا ======
        setQuickLogCardEnabled(false)

        taskLocalRepository = TaskLocalRepository(this)
    }

    override fun onResume() {
        super.onResume()
        // وقتی از صفحه ثبت گزارش برمی‌گردیم، دوباره بررسی کن
        checkUserHasResponsibleTasks()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        progressBar = findViewById(R.id.progressBar)
        tvWelcome = findViewById(R.id.tvWelcomeHome)

        cardReports = findViewById(R.id.cardReports)
        cardCartable = findViewById(R.id.cardCartable)
        cardArchive = findViewById(R.id.cardArchive)
        cardSettings = findViewById(R.id.cardSettings)
        cardChat = findViewById(R.id.cardChat)
        cardQuickLog = findViewById(R.id.cardQuickLog)
        cardPersonnelReport = findViewById(R.id.cardPersonnelReport)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    private fun loadUserData() {
        val sharedPref = getSharedPreferences(Config.PrefKeys.USER_PREFS, MODE_PRIVATE)
        val userName = sharedPref.getString(Config.PrefKeys.USERNAME, "کاربر") ?: "کاربر"
        tvWelcome.text = userName

        showLoading(true)
        disableCards(true)

        UserCache.init(this)
        UserCache.loadAllUsers {
            runOnUiThread {
                showLoading(false)
                disableCards(false)
                checkUserHasResponsibleTasks()
            }

            // ← این خط رو اضافه کن (بیرون از runOnUiThread)
            val userId = sharedPref.getString(Config.PrefKeys.USER_ROW_ID, "") ?: ""
            TaskSyncer.syncTasksFromServer(this, userId) { count ->
                Log.d("HomeActivity", "Synced $count tasks to Room")
            }
        }
    }

    // ====== متد کمکی برای تغییر وضعیت کارت ======
    private fun setQuickLogCardEnabled(enabled: Boolean) {
        cardQuickLog.isEnabled = enabled
        cardQuickLog.alpha = if (enabled) 1.0f else 0.5f
    }

    private fun checkUserHasResponsibleTasks() {
        val sharedPref = getSharedPreferences(Config.PrefKeys.USER_PREFS, MODE_PRIVATE)
        val currentUserId = sharedPref.getString(Config.PrefKeys.USER_ROW_ID, "") ?: ""

        if (currentUserId.isEmpty()) {
            setQuickLogCardEnabled(false)
            return
        }

        // اول غیرفعال کن تا وقتی دیتا اومد
        setQuickLogCardEnabled(false)

        responsibleTasksJob?.cancel()
        responsibleTasksJob = lifecycleScope.launch {
            taskLocalRepository.getResponsibleTasks(currentUserId).collectLatest { tasks ->
                val hasTasks = tasks.isNotEmpty()
                setQuickLogCardEnabled(hasTasks)
                Log.d("HomeActivity", "Responsible tasks: ${tasks.size}, card enabled: $hasTasks")
            }
        }
    }

    private fun setupClickListeners() {
        cardReports.setOnClickListener {
            startActivity(Intent(this, ReportsActivity::class.java))
        }

        cardCartable.setOnClickListener {
            startActivity(Intent(this, CartableActivity::class.java))
        }

        cardArchive.setOnClickListener {
            startActivity(Intent(this, ArchiveActivity::class.java))
        }

        cardSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        cardChat.setOnClickListener {
            startActivity(Intent(this, ChatActivity::class.java))
        }

        cardPersonnelReport.setOnClickListener {
            startActivity(Intent(this, PersonnelReportActivity::class.java))
        }

        cardQuickLog.setOnClickListener {
            if (cardQuickLog.isEnabled) {
                startActivity(Intent(this, QuickLogActivity::class.java))
            } else {
                Toast.makeText(this, "شما مسئول هیچ کاری در حال انجام نیستید", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun disableCards(disable: Boolean) {
        val cards = listOf(
            cardQuickLog,
            cardReports,
            cardCartable,
            cardArchive,
            cardSettings,
            cardChat
        )
        val alpha = if (disable) 0.5f else 1.0f
        cards.forEach { card ->
            card.isEnabled = !disable
            card.alpha = alpha
        }
    }
}