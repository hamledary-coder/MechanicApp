package com.Mechanic.Workshop.ui.home

import android.content.Intent
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import com.Mechanic.Workshop.R
import com.Mechanic.Workshop.data.remote.Config
import com.Mechanic.Workshop.ui.archive.ArchiveActivity
import com.Mechanic.Workshop.ui.cartable.CartableActivity
import com.Mechanic.Workshop.ui.chat.ChatActivity
import com.Mechanic.Workshop.ui.reports.ReportsActivity
import com.Mechanic.Workshop.ui.settings.SettingsActivity
import com.Mechanic.Workshop.ui.task.quicklog.QuickLogActivity
import com.Mechanic.Workshop.utils.UserCache
import com.Mechanic.Workshop.utils.VolleySingleton
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import org.json.JSONObject

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

    // متغیر برای جلوگیری از درخواست‌های همزمان
    private var isChecking = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        initViews()
        setupToolbar()
        loadUserData()
        setupClickListeners()
    }

    // ====== اضافه کردن onResume برای به‌روزرسانی ======
    override fun onResume() {
        super.onResume()
        // وقتی کاربر از صفحات دیگر برمی‌گردد، وضعیت را مجدداً بررسی کن
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
                // بررسی اولیه
                checkUserHasResponsibleTasks()
            }
        }
    }

    private fun checkUserHasResponsibleTasks() {
        // جلوگیری از درخواست‌های همزمان
        if (isChecking) return

        val sharedPref = getSharedPreferences(Config.PrefKeys.USER_PREFS, MODE_PRIVATE)
        val currentUserId = sharedPref.getString(Config.PrefKeys.USER_ROW_ID, "") ?: ""

        if (currentUserId.isEmpty()) {
            cardQuickLog.isEnabled = false
            cardQuickLog.alpha = 0.5f
            return
        }

        isChecking = true

        val url = "${Config.BASE_URL}?action=getUserResponsibleTasks&userId=$currentUserId"

        val request = JsonArrayRequest(
            Request.Method.GET, url, null,
            { response ->
                isChecking = false
                val hasTasks = response.length() > 0
                runOnUiThread {
                    if (hasTasks) {
                        cardQuickLog.isEnabled = true
                        cardQuickLog.alpha = 1.0f
                    } else {
                        cardQuickLog.isEnabled = false
                        cardQuickLog.alpha = 0.5f
                    }
                }
            },
            { error ->
                isChecking = false
                runOnUiThread {
                    cardQuickLog.isEnabled = false
                    cardQuickLog.alpha = 0.5f
                }
            }
        )

        VolleySingleton.getInstance(this).add(request)
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