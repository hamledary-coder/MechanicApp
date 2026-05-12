package com.Mechanic.Workshop.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.Mechanic.Workshop.ui.home.HomeActivity
import com.Mechanic.Workshop.R
import com.Mechanic.Workshop.data.remote.Config
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class LoginActivity : AppCompatActivity() {

    // 🔴 تغییر این خط
    private val loginUrl = Config.Endpoints.LOGIN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ۱. چک کردن لاگین قبلی
        // 🔴 تغییر این خط
        val sharedPref = getSharedPreferences(Config.PrefKeys.USER_PREFS, MODE_PRIVATE)
        if (sharedPref.contains(Config.PrefKeys.PERSONNEL_ID)) {
            // اگر قبلاً لاگین کرده، مستقیماً به کارتابل برو
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        val etId = findViewById<EditText>(R.id.etPersonnelID)
        val etPass = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val progress = findViewById<ProgressBar>(R.id.loginProgress)

        btnLogin.setOnClickListener {
            val pId = etId.text.toString().trim()
            val pass = etPass.text.toString().trim()

            if (pId.isNotEmpty() && pass.isNotEmpty()) {
                progress.visibility = View.VISIBLE
                btnLogin.isEnabled = false
                sendLoginRequest(pId, pass, btnLogin, progress)
            } else {
                Toast.makeText(this, "لطفاً فیلدها را پر کنید", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendLoginRequest(pId: String, pass: String, btn: Button, pg: ProgressBar) {
        val client = OkHttpClient()
        val json = JSONObject()
        json.put("action", "login")
        json.put("personnelId", pId)
        json.put("password", pass)

        val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        // 🔴 تغییر این خط
        val request = Request.Builder().url(loginUrl).post(body).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    pg.visibility = View.GONE
                    btn.isEnabled = true
                    Toast.makeText(this@LoginActivity, "خطا در اتصال به شبکه", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val resBody = response.body?.string()
                val code = response.code
                // لاگ برای عیب‌یابی (در Logcat ببینید)
                println("Login Response: $resBody")
                Log.d("LOGIN_TEST", "HTTP Code: $code")
                Log.d("LOGIN_TEST", "Raw Response: $resBody")
                Log.d("LOGIN_TEST", "Response length: ${resBody?.length}")
                Log.d("LOGIN_URL", "Login URL: $loginUrl")
                Log.d("LOGIN_JSON", "Sending: $json")

                try {
                    val jsonRes = JSONObject(resBody ?: "{}")
                    runOnUiThread {
                        pg.visibility = View.GONE
                        btn.isEnabled = true

                        if (jsonRes.optString("status") == "success") {
                            val name = jsonRes.optString("name", "کاربر")
                            val role = jsonRes.optString("role", "3")  // کد 3 = employee
                            val rowId = jsonRes.optString("rowId", "") // کد ردیف از سرور برگشته

                            val sharedPref = getSharedPreferences(Config.PrefKeys.USER_PREFS, MODE_PRIVATE)
                            sharedPref.edit()
                                .putString(Config.PrefKeys.PERSONNEL_ID, pId)  // شماره پرسنلی
                                .putString(Config.PrefKeys.USER_ROW_ID, rowId) // کد ردیف ✅ جدید
                                .putString(Config.PrefKeys.USERNAME, name)
                                .putString(Config.PrefKeys.USER_ROLE, role)
                                .commit()

                            Toast.makeText(this@LoginActivity, "خوش آمدید $name", Toast.LENGTH_LONG).show()
                            // انتقال به صفحه کارتابل
                            val intent = Intent(this@LoginActivity, HomeActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this@LoginActivity, "آیدی یا رمز اشتباه است", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        pg.visibility = View.GONE
                        btn.isEnabled = true
                        Toast.makeText(this@LoginActivity, "خطا در تحلیل داده‌های سرور", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}