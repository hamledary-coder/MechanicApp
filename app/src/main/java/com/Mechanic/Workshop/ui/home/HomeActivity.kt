package com.Mechanic.Workshop.ui.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.Mechanic.Workshop.ui.cartable.CartableActivity
import com.Mechanic.Workshop.R
import com.Mechanic.Workshop.data.remote.Config
import com.Mechanic.Workshop.ui.archive.ArchiveActivity
import com.Mechanic.Workshop.ui.auth.LoginActivity
import com.Mechanic.Workshop.ui.settings.SettingsActivity
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray

class HomeActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var cardCartable: CardView
    private lateinit var cardArchive: CardView
    private lateinit var cardSettings: CardView
    private lateinit var tvWelcome: TextView
    private lateinit var btnLogout: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        initViews()

        // نمایش loading و غیرفعال کردن کارت‌ها تا دریافت کاربران
        showLoading(true)
        setCardsEnabled(false)

        // دریافت کاربران و بعد نمایش صفحه
        fetchAndCacheUsers {
            runOnUiThread {
                showLoading(false)
                setupUI()
                setCardsEnabled(true)
            }
        }
    }

    private fun initViews() {
        progressBar = findViewById(R.id.progressBar)
        tvWelcome = findViewById(R.id.tvWelcomeHome)
        //btnLogout = findViewById(R.id.btnLogout)
        cardCartable = findViewById(R.id.cardCartable)
        cardArchive = findViewById(R.id.cardArchive)
        cardSettings = findViewById(R.id.cardSettings)
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun setCardsEnabled(enabled: Boolean) {
        cardCartable.isEnabled = enabled
        cardArchive.isEnabled = enabled
        cardSettings.isEnabled = enabled

        val alpha = if (enabled) 1.0f else 0.5f
        cardCartable.alpha = alpha
        cardArchive.alpha = alpha
        cardSettings.alpha = alpha
    }

    private fun setupUI() {
        val sharedPref = getSharedPreferences(Config.PrefKeys.USER_PREFS, MODE_PRIVATE)
        val userName = sharedPref.getString(Config.PrefKeys.USERNAME, "کاربر")
        tvWelcome.text = "خوش آمدید، $userName عزیز"



        cardCartable.setOnClickListener {
            val intent = Intent(this, CartableActivity::class.java)
            startActivity(intent)
        }

        cardArchive.setOnClickListener {
            val intent = Intent(this, ArchiveActivity::class.java)
            startActivity(intent)
        }

        cardSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun fetchAndCacheUsers(onComplete: () -> Unit) {
        val url = "${Config.Endpoints.TASKS}?action=getEmployees"
        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val usersArray = JSONArray(response)
                    Config.UserCache.userMap.clear() // جلوگیری از دوبارگی
                    for (i in 0 until usersArray.length()) {
                        val obj = usersArray.getJSONObject(i)
                        val rowId = obj.getString("rowId")
                        val name = obj.getString("name")
                        Config.UserCache.userMap[rowId] = name
                    }
                    Log.d("UserCache", "تعداد کاربران کش شده: ${Config.UserCache.userMap.size}")
                } catch (e: Exception) {
                    Log.e("UserCache", "Error caching users: ${e.message}")
                } finally {
                    onComplete()
                }
            },
            { error ->
                Log.e("UserCache", "Network error: ${error.message}")
                onComplete() // حتی با خطا ادامه بده
            })
        Volley.newRequestQueue(this).add(request)
    }
}