package com.Mechanic.Workshop.ui.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.Mechanic.Workshop.ui.cartable.CartableActivity
import com.Mechanic.Workshop.R
import com.Mechanic.Workshop.data.remote.Config
import com.Mechanic.Workshop.ui.archive.ArchiveActivity
import com.Mechanic.Workshop.ui.auth.LoginActivity
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // ✅ انتقال به اینجا
        fetchAndCacheUsers()

        val tvWelcome = findViewById<TextView>(R.id.tvWelcomeHome)
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        val cardCartable = findViewById<CardView>(R.id.cardCartable)

        val sharedPref = getSharedPreferences(Config.PrefKeys.USER_PREFS, MODE_PRIVATE)
        val userName = sharedPref.getString(Config.PrefKeys.USERNAME, "کاربر")
        tvWelcome.text = "خوش آمدید، $userName عزیز"

        btnLogout.setOnClickListener {
            sharedPref.edit().clear().apply()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        cardCartable.setOnClickListener {
            val intent = Intent(this, CartableActivity::class.java)
            startActivity(intent)
        }

        val cardArchive = findViewById<CardView>(R.id.cardArchive)
        cardArchive.setOnClickListener {
            val intent = Intent(this, ArchiveActivity::class.java)
            startActivity(intent)
        }
    }

    // ✅ تابع رو بیار داخل کلاس
    private fun fetchAndCacheUsers() {
        val url = "${Config.Endpoints.TASKS}?action=getEmployees"
        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val usersArray = JSONArray(response)
                    for (i in 0 until usersArray.length()) {
                        val obj = usersArray.getJSONObject(i)
                        Config.UserCache.userMap[obj.getString("rowId")] = obj.getString("name")
                    }
                    Log.d("UserCache", "تعداد کاربران کش شده: ${Config.UserCache.userMap.size}")
                } catch (e: Exception) {
                    Log.e("UserCache", "Error caching users: ${e.message}")
                }
            },
            { error ->
                Log.e("UserCache", "Network error: ${error.message}")
            })
        Volley.newRequestQueue(this).add(request)
    }
}