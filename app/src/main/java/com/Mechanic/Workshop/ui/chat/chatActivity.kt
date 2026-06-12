package com.Mechanic.Workshop.ui.chat

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.Mechanic.Workshop.R
import com.Mechanic.Workshop.data.remote.Config
import com.Mechanic.Workshop.data.model.ChatMessage
import com.Mechanic.Workshop.utils.UserCache
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class ChatActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var editTextMessage: EditText
    private lateinit var buttonSend: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()
    private var lastId = 0
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var userId: String
    private lateinit var userName: String
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val pollingRunnable = object : Runnable {
        override fun run() {
            fetchNewMessages()
            handler.postDelayed(this, Config.ChatConfig.POLLING_INTERVAL_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "گفتگوی گروهی"

        // دریافت اطلاعات کاربر از SharedPreferences
        val prefs = getSharedPreferences(Config.PrefKeys.USER_PREFS, MODE_PRIVATE)
        userId = prefs.getString(Config.PrefKeys.USER_ROW_ID, "") ?: ""
        userName = prefs.getString(Config.PrefKeys.USERNAME, "کاربر") ?: "کاربر"

        if (userId.isEmpty()) {
            Toast.makeText(this, "خطا در شناسایی کاربر", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupRecyclerView()
        setupSendButton()

        // شروع Polling
        startPolling()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerViewChat)
        editTextMessage = findViewById(R.id.editTextChatMessage)
        buttonSend = findViewById(R.id.buttonSendChat)
        progressBar = findViewById(R.id.progressBarChat)
    }

    private fun setupRecyclerView() {
        adapter = ChatAdapter(messages, userId.toIntOrNull() ?: 0)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupSendButton() {
        buttonSend.setOnClickListener {
            val text = editTextMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessage(text)
                editTextMessage.text.clear()
            }
        }
    }

    private fun sendMessage(message: String) {
        val json = JSONObject().apply {
            put("action", "sendMessage")
            put("roomId", Config.ChatConfig.ROOM_ID)
            put("senderId", userId.toIntOrNull() ?: 0)
            put("senderName", userName)
            put("message", message)
        }

        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(Config.BASE_URL)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@ChatActivity, "خطا در ارسال پیام", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@ChatActivity, "خطا در سرور", Toast.LENGTH_SHORT).show()
                    }
                }
                // نیازی به پردازش بیشتر نیست، چون polling پیام را می‌آورد
            }
        })
    }

    private fun fetchNewMessages() {
        val url = "${Config.BASE_URL}?action=getMessages&roomId=${Config.ChatConfig.ROOM_ID}&lastId=$lastId"
        val request = Request.Builder().url(url).get().build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // خطا نادیده گرفته شود
            }

            override fun onResponse(call: Call, response: Response) {
                val bodyStr = response.body?.string() ?: "[]"
                try {
                    val jsonArray = JSONArray(bodyStr)
                    val newMessages = mutableListOf<ChatMessage>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val msg = ChatMessage(
                            id = obj.getInt("id"),
                            sender_id = obj.getInt("sender_id"),
                            sender_name = obj.getString("sender_name"),
                            message = obj.getString("message"),
                            created_at = obj.getString("created_at")
                        )
                        newMessages.add(msg)
                        if (msg.id > lastId) lastId = msg.id
                    }
                    if (newMessages.isNotEmpty()) {
                        runOnUiThread {
                            messages.addAll(newMessages)
                            adapter.notifyItemRangeInserted(messages.size - newMessages.size, newMessages.size)
                            recyclerView.scrollToPosition(messages.size - 1)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        })
    }

    private fun startPolling() {
        handler.post(pollingRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(pollingRunnable)
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}