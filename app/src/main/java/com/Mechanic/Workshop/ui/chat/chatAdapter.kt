package com.Mechanic.Workshop.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.Mechanic.Workshop.R
import com.Mechanic.Workshop.data.model.ChatMessage
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(
    private val messages: List<ChatMessage>,
    private val currentUserId: Int
) : RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {

    companion object {
        private const val TYPE_SELF = 1
        private const val TYPE_OTHER = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].sender_id == currentUserId) TYPE_SELF else TYPE_OTHER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layout = if (viewType == TYPE_SELF) {
            R.layout.item_chat_self
        } else {
            R.layout.item_chat_other
        }
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val msg = messages[position]
        holder.bind(msg)
    }

    override fun getItemCount(): Int = messages.size

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView? = itemView.findViewById(R.id.tvChatName)
        private val tvMessage: TextView = itemView.findViewById(R.id.tvChatMessage)
        private val tvTime: TextView = itemView.findViewById(R.id.tvChatTime)

        fun bind(msg: ChatMessage) {
            tvMessage.text = msg.message
            tvName?.text = msg.sender_name
            tvTime.text = formatTime(msg.created_at)
        }

        private fun formatTime(dateStr: String): String {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val date = inputFormat.parse(dateStr)
                val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                outputFormat.format(date ?: Date())
            } catch (e: Exception) {
                dateStr
            }
        }
    }
}