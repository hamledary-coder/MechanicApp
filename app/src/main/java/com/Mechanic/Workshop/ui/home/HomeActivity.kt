package com.Mechanic.Workshop.ui.home

import android.content.Intent
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView  // ← تغییر: استفاده از CardView به جای MaterialCardView
import com.Mechanic.Workshop.R
import com.Mechanic.Workshop.data.remote.Config
import com.Mechanic.Workshop.ui.archive.ArchiveActivity
import com.Mechanic.Workshop.ui.cartable.CartableActivity
import com.Mechanic.Workshop.ui.chat.ChatActivity
import com.Mechanic.Workshop.ui.settings.SettingsActivity
import com.Mechanic.Workshop.utils.UserCache
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley

class HomeActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var progressBar: ProgressBar
    private lateinit var tvWelcome: TextView
    private lateinit var tvCartableCount: TextView
    private lateinit var cardCartable: CardView      // ← تغییر
    private lateinit var cardArchive: CardView       // ← تغییر
    private lateinit var cardSettings: CardView      // ← تغییر
    private lateinit var cardChat: CardView          // ← تغییر

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        initViews()
        setupToolbar()
        loadUserData()
        setupClickListeners()
        loadCartableTasksCount()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        progressBar = findViewById(R.id.progressBar)
        tvWelcome = findViewById(R.id.tvWelcomeHome)
        tvCartableCount = findViewById(R.id.tvCartableCount)
        cardCartable = findViewById(R.id.cardCartable)
        cardArchive = findViewById(R.id.cardArchive)
        cardSettings = findViewById(R.id.cardSettings)
        cardChat = findViewById(R.id.cardChat)
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
            }
        }
    }

    private fun loadCartableTasksCount() {
        val url = "${Config.BASE_URL}?action=getTasksCount&status=1,2,3"

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val count = response.toIntOrNull() ?: 0
                    runOnUiThread {
                        tvCartableCount.text = if (count > 0) "$count کار جدید" else "۰ کار"
                    }
                } catch (e: Exception) {
                    tvCartableCount.text = "۰ کار"
                }
            },
            { error ->
                tvCartableCount.text = "۰ کار"
            }
        )

        Volley.newRequestQueue(this).add(request)
    }

    private fun setupClickListeners() {
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
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun disableCards(disable: Boolean) {
        val cards = listOf(cardCartable, cardArchive, cardSettings, cardChat)
        val alpha = if (disable) 0.5f else 1.0f
        cards.forEach { card ->
            card.isEnabled = !disable
            card.alpha = alpha
        }
    }
}